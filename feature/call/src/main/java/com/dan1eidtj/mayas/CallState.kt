// Файл: CallState.kt
package com.dan1eidtj.mayas

enum class CallState {
    IDLE,
    OUTGOING,
    INCOMING,
    RINGING,
    CONNECTING,
    CONNECTED,
    REJECTED,
    ENDED
}