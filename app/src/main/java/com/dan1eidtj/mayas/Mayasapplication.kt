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

    // Живёт весь процесс, поэтому не тянем это на managerScope внутри CallManager —
    // он про звонок, а не про UI-lifecycle приложения.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        observeAppForegroundState()
        observeOutgoingCallsToStartService()
    }

    /**
     * ProcessLifecycleOwner — это lifecycle всего процесса приложения, а не одной
     * Activity: onStop() сработает ровно тогда, когда пользователь реально ушёл из
     * приложения (свернул его / переключился на другое) — не важно, через какой
     * экран (обычный ChatScreen с CallHost поверх или IncomingCallActivity). Именно
     * этот момент и есть "свернул звонок" в терминах бага — раньше это состояние
     * нигде не отслеживалось, и CallConnectionService всегда показывал одно и то же
     * Ongoing-уведомление с кнопками, даже пока пользователь смотрел на сам
     * CallScreen внутри приложения.
     */
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

    /**
     * Раньше CallConnectionService.startOutgoing() нигде не вызывался — сервис
     * поднимался только на входящей стороне (пушем в MayasMessagingService).
     * Звонящий оставался вообще без foreground-сервиса и без уведомления.
     *
     * Слушаем здесь, на уровне Application, а не в конкретном экране (ChatScreen
     * и т.п.) специально: CallManager.startOutgoingCall() может быть вызван из
     * любого места приложения, и каждое такое место пришлось бы не забыть
     * продублировать вызовом старта сервиса. Подписка на сам CallManager —
     * единственная точка, которая гарантированно не пропустит ни один исходящий
     * звонок независимо от того, откуда он инициирован.
     *
     * На входящей стороне ничего не трогаем: там сервис уже надёжно стартует из
     * пуша ДО того, как CallManager вообще успеет создать сессию — дублировать
     * вызов здесь не нужно.
     */
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
