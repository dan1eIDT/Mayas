package com.dan1eidtj.mayas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ViewModel экрана звонка, аутист,
 * 67
 * АМЕРИКА СОСААААТЬ
 */
class CallViewModel(
    private val callManager: CallManager,
    private val currentUserId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private var timerJob: Job? = null
    private val _elapsedSeconds = MutableStateFlow(0L)
    private val _peerProfile = MutableStateFlow(PeerProfile())
    private var lastResolvedPeerId: String? = null

    data class PeerProfile(
        val name: String? = null,
        val avatarUrl: String? = null,
        val useCustomAvatar: Boolean = true,
        val profileIcon: String = "face",
        val profileGlow: String = "purple",
        val isPremium: Boolean = false,
        val verifiedIcon: String = "verified",
        val avatarFrame: String = "none"
    )

    // Вспомогательный класс для объединения потоков из менеджера
    private data class BaseInfo(
        val state: CallState,
        val session: CallSession?,
        val isMuted: Boolean,
        val isSpeakerOn: Boolean,
        val elapsedSeconds: Long
    )

    // Комбинируем ровно 5 потоков, как и положено в стандартном combine
    private val baseInfo = combine(
        callManager.callState,
        callManager.activeCall,
        callManager.isMuted,
        callManager.isSpeakerOn,
        _elapsedSeconds
    ) { state, session, isMuted, isSpeakerOn, elapsedSeconds ->
        BaseInfo(state, session, isMuted, isSpeakerOn, elapsedSeconds)
    }

    val uiState: StateFlow<CallScreenState> = combine(baseInfo, _peerProfile) { base, profile ->
        val session = base.session
        if (session == null || base.state == CallState.IDLE) {
            CallScreenState.NoCall
        } else {
            val isReceiverRole = session.receiverId == currentUserId
            val peerId = if (isReceiverRole) session.callerId else session.receiverId

            if (peerId != lastResolvedPeerId) {
                lastResolvedPeerId = peerId
                viewModelScope.launch { fetchPeerProfile(peerId) }
            }

            // Собираем стейт для экрана CallScreen
            CallScreenState.Active(
                peerId = profile.name ?: peerId, // Пока имя грузится, показываем ID
                avatarUrl = profile.avatarUrl,
                useCustomAvatar = profile.useCustomAvatar,
                profileIcon = profile.profileIcon,
                profileGlow = profile.profileGlow,
                isPremium = profile.isPremium,
                avatarFrame = profile.avatarFrame,
                callType = session.type,
                statusText = statusTextFor(base.state, isReceiverRole, base.elapsedSeconds),
                isIncoming = isReceiverRole && base.state == CallState.INCOMING,
                isRinging = base.state == CallState.RINGING || base.state == CallState.OUTGOING,
                isConnected = base.state == CallState.CONNECTED,
                durationSeconds = base.elapsedSeconds,
                isMuted = base.isMuted,
                isSpeakerOn = base.isSpeakerOn,
                isEnded = base.state == CallState.REJECTED || base.state == CallState.ENDED
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CallScreenState.NoCall
    )

    private suspend fun fetchPeerProfile(peerId: String) {
        runCatching {
            firestore.collection("users").document(peerId).get().await()
        }.onSuccess { doc ->
            if (doc.exists()) {
                _peerProfile.value = PeerProfile(
                    name = doc.getString("name") ?: doc.getString("username"),
                    avatarUrl = doc.getString("avatarUrl"),
                    useCustomAvatar = doc.getBoolean("useCustomAvatar") ?: true,
                    profileIcon = doc.getString("profileIcon") ?: "face",
                    profileGlow = doc.getString("profileGlow") ?: "purple",
                    isPremium = doc.getBoolean("isPremium") ?: false,
                    verifiedIcon = doc.getString("verifiedIcon") ?: "verified",
                    avatarFrame = doc.getString("avatarFrame") ?: "none"
                )
            }
        }.onFailure {
            _peerProfile.value = PeerProfile(name = peerId)
        }
    }

    init {
        // Управление таймером в зависимости от стейта звонка
        viewModelScope.launch {
            callManager.callState.collect { state ->
                if (state == CallState.CONNECTED) {
                    startTimerIfNeeded()
                } else {
                    stopTimer()
                    if (state == CallState.IDLE) {
                        _elapsedSeconds.value = 0L
                    }
                }
            }
        }
    }

    fun onAcceptClicked() {
        val callId = callManager.activeCall.value?.callId ?: return
        callManager.acceptCall(callId)
    }

    fun onDeclineOrEndClicked() {
        // Если звонок входящий и мы его сбрасываем, вызываем rejectCall() для отправки REJECTED статуса
        val currentState = callManager.callState.value
        val session = callManager.activeCall.value
        val isIncoming = session?.receiverId == currentUserId

        if (isIncoming && currentState == CallState.INCOMING) {
            callManager.rejectCall()
        } else {
            callManager.endCall()
        }
    }

    fun onMuteToggleClicked() = callManager.toggleMute()

    fun onSpeakerToggleClicked() = callManager.toggleSpeaker()

    private fun startTimerIfNeeded() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1_000)
                _elapsedSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun statusTextFor(state: CallState, isReceiverRole: Boolean, elapsedSeconds: Long): String =
        when (state) {
            CallState.OUTGOING -> "Вызов..."
            CallState.INCOMING -> if (isReceiverRole) "Входящий звонок" else "Вызов..."
            CallState.RINGING -> "Гудки..."
            CallState.CONNECTING -> "Соединение..."
            CallState.CONNECTED -> elapsedSeconds.toCallDurationText()
            // rejectCall() дергает только принимающая сторона, поэтому isReceiverRole==true
            // здесь всегда значит "я сам только что отклонил" — текст не должен врать про собеседника
            CallState.REJECTED -> if (isReceiverRole) "Звонок отклонён" else "Отклонено собеседником"
            CallState.ENDED, CallState.IDLE -> "Звонок завершён"
        }
}

class CallViewModelFactory(
    private val callManager: CallManager,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CallViewModel(callManager, currentUserId) as T
    }
}

