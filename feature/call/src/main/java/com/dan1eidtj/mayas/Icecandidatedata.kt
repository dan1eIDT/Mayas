package com.dan1eidtj.mayas


enum class CallParticipantRole {
    CALLER,
    RECEIVER
}


data class IceCandidateData(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val sdp: String
)