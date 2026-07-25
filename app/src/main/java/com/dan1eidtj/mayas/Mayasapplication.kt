package com.dan1eidtj.mayas

import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
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

    override fun onCreate() {
        super.onCreate()
        observeAppForegroundState()
        observeOutgoingCallsToStartService()
    }


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
