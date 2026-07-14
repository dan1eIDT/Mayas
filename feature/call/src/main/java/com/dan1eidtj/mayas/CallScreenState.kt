package com.dan1eidtj.mayas

import com.dan1eidtj.mayas.CallType

/**
 * ы
 * э
 * а
 * н
 */
sealed interface CallScreenState {


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

        val isEnded: Boolean
    ) : CallScreenState
}


fun Long.toCallDurationText(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}
