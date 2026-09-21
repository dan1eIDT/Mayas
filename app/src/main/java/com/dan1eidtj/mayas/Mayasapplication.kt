package com.dan1eidtj.mayas

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.dan1eidtj.data.ShopRepository
import com.dan1eidtj.mayas.core_ui.emoji.EmojiStyleState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MayasApplication : MultiDexApplication(), CallManagerProvider {

    override val callManager: CallManager by lazy {
        CallManager(
            callRepository = FirestoreCallRepository(),
            webRtcClient = WebRtcClientImpl(applicationContext),
            audioController = SystemAudioController(applicationContext),
            callFeedbackController = CallFeedbackController(applicationContext),
            callPushNotifier = CallPushNotifier(),
            currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() },
            showError = { message ->

                Toast.makeText(
                    applicationContext,
                    message,
                    Toast.LENGTH_LONG
                ).show()

            }
        )
    }



    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        installFirestorePermissionDeniedSafetyNet()
        super.onCreate()
        EmojiStyleState.ensureInit(this)
        configureOfflineCache()
        com.dan1eidtj.mayas.feature.OutboxManager.start(this)
        Thread {
            try {
                com.dan1eidtj.mayas.storage.MediaFileCache.cleanStale(this)
                kotlinx.coroutines.runBlocking { com.dan1eidtj.mayas.storage.MediaFileCache.applyPolicy(this@MayasApplication) }
            } catch (e: Exception) {
                Log.w("MayasApplication", "Не удалось очистить устаревшие временные файлы", e)
            }
        }.start()
        ShopRepository.startListening()
        observeAppForegroundState()
        observeOutgoingCallsToStartService()
    }


    private fun configureOfflineCache() {
        try {
            FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FIRESTORE_CACHE_BYTES)
                        .build()
                )
                .build()
        } catch (e: Exception) {
            Log.w("MayasApplication", "Не удалось настроить офлайн-кэш Firestore", e)
        }
    }

    private fun installFirestorePermissionDeniedSafetyNet() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isStaleFirestorePermissionDenied =
                throwable is FirebaseFirestoreException &&
                        throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

            if (isStaleFirestorePermissionDenied) {
                Log.e(
                    "MayasApplication",
                    "Проглочен PERMISSION_DENIED от уже отписанного Firestore-листенера " +
                            "(гонка со stale ответом сервера) — процесс не убиваем",
                    throwable
                )
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAppForegroundState() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                CallUiVisibility.setAppForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                CallUiVisibility.setAppForeground(false)
            }
        })
    }


    private fun observeOutgoingCallsToStartService() {
        appScope.launch {
            callManager.callState.collect { state ->
                if (state == CallState.OUTGOING) {
                    val session = callManager.activeCall.value ?: return@collect
                    CallConnectionService.startOutgoing(
                        applicationContext,
                        callId = session.callId,
                        receiverId = session.receiverId
                    )
                }
            }
        }
    }
}

private const val FIRESTORE_CACHE_BYTES = 200L * 1024L * 1024L
