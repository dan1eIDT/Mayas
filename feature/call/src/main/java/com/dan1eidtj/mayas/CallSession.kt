package com.dan1eidtj.mayas

import com.google.firebase.firestore.DocumentSnapshot

data class CallSession(
    val callId: String,
    val callerId: String,
    val receiverId: String,
    val state: CallState,
    val type: CallType,
    val offer: String? = null,
    val answer: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_CALLER_ID to callerId,
        FIELD_RECEIVER_ID to receiverId,
        FIELD_STATE to state.name,
        FIELD_TYPE to type.name,
        FIELD_OFFER to offer,
        FIELD_ANSWER to answer,
        FIELD_CREATED_AT to createdAt
    )

    companion object {
        const val COLLECTION_NAME = "calls"

        private const val FIELD_CALLER_ID = "callerId"
        private const val FIELD_RECEIVER_ID = "receiverId"
        private const val FIELD_STATE = "state"
        private const val FIELD_TYPE = "type"
        private const val FIELD_OFFER = "offer"
        private const val FIELD_ANSWER = "answer"
        private const val FIELD_CREATED_AT = "createdAt"

        fun fromDocument(document: DocumentSnapshot): CallSession? {
            if (!document.exists()) return null

            val callerId = document.getString(FIELD_CALLER_ID) ?: return null
            val receiverId = document.getString(FIELD_RECEIVER_ID) ?: return null
            val stateRaw = document.getString(FIELD_STATE) ?: return null
            val typeRaw = document.getString(FIELD_TYPE) ?: return null

            val state = runCatching { CallState.valueOf(stateRaw) }.getOrElse { return null }
            val type = runCatching { CallType.valueOf(typeRaw) }.getOrElse { return null }

            return CallSession(
                callId = document.id,
                callerId = callerId,
                receiverId = receiverId,
                state = state,
                type = type,
                offer = document.getString(FIELD_OFFER),
                answer = document.getString(FIELD_ANSWER),
                createdAt = document.getLong(FIELD_CREATED_AT) ?: 0L
            )
        }
    }
}