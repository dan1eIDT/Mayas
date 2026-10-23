/* Copyright (C) 2026 ProjectIDT */
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.dan1eidtj.mayas.WebRtcClient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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



    private var wasConnectedThisCall = false
    private var missedCallLogged = false

    private val db by lazy { FirebaseFirestore.getInstance() }

    private var acceptingCallId: String? = null

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
            wasConnectedThisCall = true
            callFeedbackController.stop()

            managerScope.launch {
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
                    if (!canCommunicateWith(session.callerId, reportError = false)) {
                        Log.i("CallManager", "Входящий звонок ${session.callId} отброшен из-за блокировки")
                        callRepository.updateCallState(session.callId, CallState.REJECTED)
                        callRepository.deleteCall(session.callId)
                        return@collect
                    }
                    _activeCall.value = session
                    _callState.value = CallState.INCOMING
                    currentRole = CallParticipantRole.RECEIVER
                    wasConnectedThisCall = false
                    missedCallLogged = false
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
            if (!canCommunicateWith(receiverId, reportError = true)) return@launch
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
                if (error.message == "CALL_BLOCKED") {
                    showError("Звонок недоступен: один из пользователей заблокировал другого.")
                }
                callFeedbackController.stop()
                _callState.value = CallState.ENDED
                _activeCall.value = null
            }
        }
    }

    private suspend fun canCommunicateWith(peerUid: String, reportError: Boolean): Boolean {
        val currentUid = currentUserIdProvider()
        if (currentUid.isBlank() || peerUid.isBlank() || currentUid == peerUid) {
            if (reportError) showError("Не удалось начать звонок: некорректный собеседник.")
            return false
        }
        return try {
            val currentProfile = db.collection("users").document(currentUid).get().await()
            val peerProfile = db.collection("users").document(peerUid).get().await()
            val blockedByMe = (currentProfile.get("blocked") as? List<*>)?.contains(peerUid) == true
            val blockedMe = (peerProfile.get("blocked") as? List<*>)?.contains(currentUid) == true
            if (blockedByMe || blockedMe) {
                if (reportError) showError("Звонок недоступен: один из пользователей заблокировал другого.")
                false
            } else {
                true
            }
        } catch (error: Exception) {
            Log.e("CallManager", "Не удалось проверить блокировку перед звонком", error)
            if (reportError) showError("Не удалось проверить блокировку. Повторите попытку.")
            false
        }
    }

    fun acceptCall(callId: String) {

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
                resetLocalState()
                return@launch
            }

            _activeCall.value = session

            if (cachedSession == null) {
                attachToCall(callId)
            }

            audioController.start()

            webRtcClient.init(webRtcListener)
            webRtcClient.startLocalAudio()
            observeRemoteCandidates(callId, fromRole = CallParticipantRole.CALLER)

            val offer = session.offer
                ?: withTimeoutOrNull(OFFER_WAIT_TIMEOUT_MS) {
                    callRepository.observeCall(callId).first { it?.offer != null }?.offer
                }

            if (offer == null) {
                endCall()
                return@launch
            }

            webRtcClient.createAnswer(offer)

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
        _activeCall.value?.let { session -> tryLogMissedCall(session) }
        _callState.value = CallState.REJECTED
        managerScope.launch {
            callRepository.updateCallState(callId, CallState.REJECTED)
            kotlinx.coroutines.delay(REJECTED_DISPLAY_DELAY_MS)
            callRepository.deleteCall(callId)
            resetLocalState()
        }
    }


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

                if (session.state == CallState.REJECTED) {
                    _callState.value = CallState.REJECTED
                    callFeedbackController.stop()
                    kotlinx.coroutines.delay(REJECTED_DISPLAY_DELAY_MS)
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
        _activeCall.value?.let { session -> tryLogMissedCall(session) }
        resetLocalState()
    }

    private fun resetLocalState() {
        synchronized(resetLock) {
            activeCallJob?.cancel()
            activeCallJob = null
            remoteCandidatesJob?.cancel()
            remoteCandidatesJob = null

            callFeedbackController.stop()
            webRtcClient.setMuted(false)
            webRtcClient.close()
            audioController.release()

            currentRole = null
            remoteAnswerApplied = false
            remoteDescriptionSet = false
            pendingRemoteCandidates.clear()
            acceptingCallId = null
            wasConnectedThisCall = false
            missedCallLogged = false

            _activeCall.value = null
            _callState.value = CallState.IDLE
            _callError.value = null
            _isMuted.value = false
            _isSpeakerOn.value = false
        }
    }


    private fun tryLogMissedCall(session: CallSession) {
        if (missedCallLogged) return
        if (currentRole != CallParticipantRole.RECEIVER) return
        if (wasConnectedThisCall) return
        missedCallLogged = true

        val receiverId = currentUserIdProvider()
        val callerId = session.callerId
        if (callerId.isBlank() || receiverId.isBlank()) return

        managerScope.launch {
            try {
                val callerDoc = db.collection("users").document(callerId).get().await()
                val callerName = callerDoc.getString("name")
                    ?: callerDoc.getString("username")
                    ?: "Неизвестный"
                val chatId = listOf(callerId, receiverId).sorted().joinToString("_")

                db.collection("users").document(receiverId)
                    .collection("notifications")
                    .add(
                        mapOf(
                            "type" to "missed_call",
                            "title" to "Пропущенный звонок",
                            "body" to callerName,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "isRead" to false,
                            "chatId" to chatId,
                            "callerId" to callerId,
                            "callType" to session.type.name
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e("CallManager", "Не удалось записать пропущенный звонок", e)
            }
        }
    }
}
