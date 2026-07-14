package com.dan1eidtj.mayas

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.dan1eidtj.mayas.WebRtcClient

class CallManager(
    private val callRepository: CallRepository,
    private val webRtcClient: WebRtcClient,
    private val audioController: AudioController,
    private val callFeedbackController: CallFeedbackController,
    private val callPushNotifier: CallPushNotifier,
    private val currentUserIdProvider: () -> String,
    private val showError: (String) -> Unit,
) {

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeCallJob: Job? = null
    private var incomingCallsJob: Job? = null
    private var remoteCandidatesJob: Job? = null

    companion object {
        private const val OFFER_WAIT_TIMEOUT_MS = 15_000L
        private const val REJECTED_DISPLAY_DELAY_MS = 2000L
    }

    private var currentRole: CallParticipantRole? = null
    private var remoteAnswerApplied = false
    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidateData>()

    // Гвард от повторного acceptCall() на тот же callId. Кнопка "Принять" на экране
    // не пропадает мгновенно — она пропадает только когда локальный _callState дойдёт
    // до CONNECTING/CONNECTED, а это зависит от round-trip записи в Firestore. Пока
    // этот round-trip не завершился, пользователь видит всё ту же кнопку и может
    // нажать её ещё раз (или сработает повторный EXTRA_AUTO_ACCEPT). Без гварда
    // повторный вызов заново гонял webRtcClient.init()/startLocalAudio() поверх уже
    // идущего соединения — WebRtcClientImpl теперь тоже идемпотентен (см. его код),
    // но дублировать саму попытку accept всё равно незачем.
    private var acceptingCallId: String? = null

    // resetLocalState() может быть вызван почти одновременно с двух разных корутин
    // на managerScope (Dispatchers.IO — то есть буквально с разных потоков):
    // 1) локально из endCall()/rejectCall() сразу после deleteCall(),
    // 2) из attachToCall() -> handleCallEndedRemotely(), которую триггерит тот же
    //    deleteCall() через снапшот-листенер Firestore практически мгновенно.
    // Без лока webRtcClient.close() и audioController.release() могли исполняться
    // параллельно — в WebRtcClientImpl.close() это раньше приводило к исключению на
    // повторном dispose() нативного объекта, которое обрывало close() ДО
    // peerConnectionFactory?.dispose() (реально останавливающего микрофон). Итог —
    // мик оставался открытым после завершения звонка. Лок здесь безопасен: внутри
    // resetLocalState() нет suspend-вызовов, только синхронные close()/release().
    private val resetLock = Any()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _callError = MutableStateFlow<String?>(null)
    val callError: StateFlow<String?> = _callError.asStateFlow()
    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val webRtcListener = object : WebRtcClient.Listener {
        override fun onLocalIceCandidate(candidate: IceCandidateData) {
            val callId = _activeCall.value?.callId ?: return
            val role = currentRole ?: return
            managerScope.launch {
                callRepository.addIceCandidate(callId, role, candidate)
            }
        }

        override fun onLocalOfferCreated(sdp: String) {
            val callId = _activeCall.value?.callId ?: return
            managerScope.launch {
                callRepository.updateOffer(callId, sdp)
            }
        }

        override fun onLocalAnswerCreated(sdp: String) {
            val callId = _activeCall.value?.callId ?: return
            managerScope.launch {
                callRepository.updateAnswer(callId, sdp)
            }
        }

        override fun onRemoteDescriptionSet() {
            remoteDescriptionSet = true
            pendingRemoteCandidates.forEach { webRtcClient.addRemoteIceCandidate(it) }
            pendingRemoteCandidates.clear()
        }

        @RequiresPermission(Manifest.permission.VIBRATE)
        override fun onIceConnected() {
            val callId = _activeCall.value?.callId ?: return
            callFeedbackController.stop()

            managerScope.launch {
                // Аудио уже реально соединено через ICE на этом моменте — ошибка записи
                // сюда влияет только на текстовый статус/таймер в UI обеих сторон, но не
                // на сам разговор. Поэтому только логируем и предупреждаем, но НЕ рвём
                // звонок из-за неё (в отличие от ошибки на этапе CONNECTING ниже, где
                // без успешной записи соединение вообще не имеет шанса подняться).
                callRepository.updateCallState(callId, CallState.CONNECTED).onFailure { error ->
                    Log.e("CallManager", "Не удалось обновить статус звонка на CONNECTED", error)
                    showError("Звонок соединён, но статус не синхронизировался с сервером.")
                }
            }
        }

        override fun onIceFailed() {
            endCall()
        }

        override fun onError(message: String) {
            showError(message)
            endCall()
        }
    }

    fun startListeningForIncomingCalls() {
        incomingCallsJob?.cancel()
        incomingCallsJob = managerScope.launch {
            callRepository.observeIncomingCalls(currentUserIdProvider()).collect { session ->
                if (session != null && _activeCall.value == null) {
                    val age = System.currentTimeMillis() - session.createdAt
                    if (age > 60_000) {
                        managerScope.launch { callRepository.deleteCall(session.callId) }
                        return@collect
                    }
                    _activeCall.value = session
                    _callState.value = CallState.INCOMING
                    currentRole = CallParticipantRole.RECEIVER
                    callFeedbackController.startIncoming()
                    attachToCall(session.callId)
                }
            }
        }
    }

    fun stopListeningForIncomingCalls() {
        incomingCallsJob?.cancel()
        incomingCallsJob = null
    }

    fun startOutgoingCall(receiverId: String, type: CallType) {
        managerScope.launch {
            val result = callRepository.createOutgoingCall(
                callerId = currentUserIdProvider(),
                receiverId = receiverId,
                type = type
            )
            result.onSuccess { session ->
                _activeCall.value = session
                _callState.value = CallState.OUTGOING
                currentRole = CallParticipantRole.CALLER
                callFeedbackController.startOutgoing()
                managerScope.launch {
                    when (val result = callPushNotifier.notifyIncomingCall(session)) {
                        is PushNotifyResult.Success -> {
                            Log.d(
                                "CallManager",
                                "Пуш отправлен"
                            )
                        }


                        is PushNotifyResult.Error -> {

                            showError(
                                "⚠️ ${result.message}\n\n" +
                                        "Попросите собеседника открыть приложение " +
                                        "или свяжитесь другим способом."
                            )

                        }
                    }
                }
                audioController.start()

                webRtcClient.init(webRtcListener)
                webRtcClient.startLocalAudio()
                webRtcClient.createOffer()

                observeRemoteCandidates(session.callId, fromRole = CallParticipantRole.RECEIVER)
                attachToCall(session.callId)
            }.onFailure { error ->
                Log.e("CallManager", "startOutgoingCall failed1", error)
                callFeedbackController.stop()
                _callState.value = CallState.ENDED
                _activeCall.value = null
            }
        }
    }

    fun acceptCall(callId: String) {
        // Повторный вызов на тот же callId (второй тап по "Принять" пока UI ещё не
        // успел обновиться, либо повторный EXTRA_AUTO_ACCEPT) — тихо игнорируем.
        if (acceptingCallId == callId) {
            Log.d("CallManager", "acceptCall($callId) проигнорирован — уже в процессе/принят")
            return
        }
        acceptingCallId = callId

        callFeedbackController.stop()

        managerScope.launch {
            currentRole = CallParticipantRole.RECEIVER
            remoteDescriptionSet = false
            remoteAnswerApplied = false
            pendingRemoteCandidates.clear()

            val cachedSession = _activeCall.value?.takeIf { it.callId == callId }
            val session = cachedSession
                ?: withTimeoutOrNull(OFFER_WAIT_TIMEOUT_MS) {
                    callRepository.observeCall(callId).first { it != null }
                }

            if (session == null) {
                // Так и не дождались документа звонка (собеседник отменил его до того,
                // как мы успели подписаться, либо реальный сетевой сбой) — откатываемся
                // в чистое состояние, а не оставляем экран висеть.
                resetLocalState()
                return@launch
            }

            _activeCall.value = session

            // Слушатель на этот callId мог уже быть поднят через
            // startListeningForIncomingCalls() -> attachToCall() ДО того, как мы сюда
            // попали — это обычный, не гоночный сценарий (cachedSession != null, мы его
            // просто переиспользовали). В этом случае НЕ трогаем уже работающий
            // activeCallJob — иначе каждый accept на ровном месте рвёт и пересоздаёт
            // рабочую Firestore-подписку. Переподписываемся только когда cachedSession
            // == null — то есть в самом гоночном сценарии, когда сессию пришлось
            // добывать напрямую из Firestore, минуя обычный слушатель CallManager.
            if (cachedSession == null) {
                attachToCall(callId)
            }

            audioController.start()

            webRtcClient.init(webRtcListener)
            webRtcClient.startLocalAudio()
            observeRemoteCandidates(callId, fromRole = CallParticipantRole.CALLER)

            // withTimeoutOrNull: если звонящая сторона отвалилась ещё до записи offer
            // в репозиторий (упало приложение, пропала сеть), ждать вечно нельзя —
            // раньше это подвешивало корутину навсегда и endCall() ниже не вызывался.
            val offer = session.offer
                ?: withTimeoutOrNull(OFFER_WAIT_TIMEOUT_MS) {
                    callRepository.observeCall(callId).first { it?.offer != null }?.offer
                }

            if (offer == null) {
                endCall()
                return@launch
            }

            webRtcClient.createAnswer(offer)

            // Раньше результат этой записи нигде не проверялся: если она падала
            // (например, PERMISSION_DENIED от Firestore Security Rules), локальный
            // _callState никогда не покидал INCOMING — кнопка "Принять" оставалась
            // на экране, пользователь жал её снова, и весь acceptCall() гонялся по
            // новой поверх уже идущей попытки. Теперь ошибка явно показывается и
            // звонок аккуратно откатывается, а не зависает молча.
            callRepository.updateCallState(callId, CallState.CONNECTING).onFailure { error ->
                Log.e("CallManager", "Не удалось обновить статус звонка на CONNECTING", error)
                showError("Не удалось подключиться к звонку. Проверьте соединение и попробуйте снова.")
                endCall()
            }
        }
    }

    fun endCall() {
        val callId = _activeCall.value?.callId ?: return
        managerScope.launch {
            callRepository.updateCallState(callId, CallState.ENDED)
            callRepository.deleteCall(callId)
            resetLocalState()
        }
    }

    fun toggleMute() {
        val newValue = !_isMuted.value
        webRtcClient.setMuted(newValue)
        _isMuted.value = newValue
    }

    fun toggleSpeaker() {
        val newValue = !_isSpeakerOn.value
        audioController.setLoudspeakerEnabled(newValue)
        _isSpeakerOn.value = newValue
    }

    fun rejectCall() {
        val callId = _activeCall.value?.callId ?: return
        _callState.value = CallState.REJECTED
        managerScope.launch {
            callRepository.updateCallState(callId, CallState.REJECTED)
            kotlinx.coroutines.delay(REJECTED_DISPLAY_DELAY_MS)
            callRepository.deleteCall(callId)
            resetLocalState()
        }
    }

    // Изменяем attachToCall, чтобы ловить REJECTED на звонящей стороне
    private fun attachToCall(callId: String) {
        activeCallJob?.cancel()
        activeCallJob = managerScope.launch {
            callRepository.observeCall(callId).collect { session ->
                if (session == null) {
                    handleCallEndedRemotely()
                    return@collect
                }
                _activeCall.value = session

                _callState.value = if (currentRole == CallParticipantRole.RECEIVER &&
                    session.state == CallState.OUTGOING
                ) {
                    CallState.INCOMING
                } else {
                    session.state
                }

                if (currentRole == CallParticipantRole.CALLER && !remoteAnswerApplied) {
                    session.answer?.let { answerSdp ->
                        remoteAnswerApplied = true
                        webRtcClient.setRemoteAnswer(answerSdp)
                    }
                }

                // ЕСЛИ СОБЕСЕДНИК ОТКЛОНИЛ
                if (session.state == CallState.REJECTED) {
                    _callState.value = CallState.REJECTED
                    callFeedbackController.stop() // моментально тушим гудки
                    kotlinx.coroutines.delay(REJECTED_DISPLAY_DELAY_MS) // держим экран с надписью "Отклонено"
                    resetLocalState()
                }

                if (session.state == CallState.ENDED) {
                    handleCallEndedRemotely()
                }
            }
        }
    }

    private fun observeRemoteCandidates(callId: String, fromRole: CallParticipantRole) {
        remoteCandidatesJob?.cancel()
        remoteCandidatesJob = managerScope.launch {
            callRepository.observeIceCandidates(callId, fromRole).collect { candidate ->
                if (remoteDescriptionSet) {
                    webRtcClient.addRemoteIceCandidate(candidate)
                } else {
                    pendingRemoteCandidates.add(candidate)
                }
            }
        }
    }

    private fun handleCallEndedRemotely() {
        resetLocalState()
    }

    private fun resetLocalState() {
        synchronized(resetLock) {
            activeCallJob?.cancel()
            activeCallJob = null
            remoteCandidatesJob?.cancel()
            remoteCandidatesJob = null

            callFeedbackController.stop()
            webRtcClient.close()
            webRtcClient.setMuted(false)
            audioController.release()

            currentRole = null
            remoteAnswerApplied = false
            remoteDescriptionSet = false
            pendingRemoteCandidates.clear()
            acceptingCallId = null

            _activeCall.value = null
            _callState.value = CallState.IDLE
            _callError.value = null
            _isMuted.value = false
            _isSpeakerOn.value = false
        }
    }
}