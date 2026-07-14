package com.dan1eidtj.mayas

import com.dan1eidtj.mayas.CallType

/**
 * ы
 * э
 * а
 * н
 */
sealed interface CallScreenState {

    /** Звонка нет — экран звонка вообще не должен показываться. */
    data object NoCall : CallScreenState

    data class Active(
        val peerId: String,
        val avatarUrl: String? = null,
        val useCustomAvatar: Boolean = true,
        val profileIcon: String = "face",
        val profileGlow: String = "purple",
        val isPremium: Boolean = false,
        val avatarFrame: String = "none",
        val callType: CallType,
        val statusText: String,
        val isIncoming: Boolean,
        val isRinging: Boolean,
        val isConnected: Boolean,
        val durationSeconds: Long,
        val isMuted: Boolean,
        val isSpeakerOn: Boolean,
        /** true — звонок уже завершён (отклонён/окончен) и просто донашивает статус на экране. */
        val isEnded: Boolean
    ) : CallScreenState
}

/** Форматирует секунды в mm:ss для таймера разговора. */
fun Long.toCallDurationText(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}
