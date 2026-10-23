/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationReplySender {
    enum class Failure {
        NONE,
        CHAT_NOT_FOUND,
        NOT_DIRECT_CHAT,
        NOT_PARTICIPANT,
        PROFILE_UNAVAILABLE,
        BLOCKED
    }

    data class Result(
        val accepted: Boolean,
        val failure: Failure = Failure.NONE,
        val peerUid: String? = null,
        val senderName: String = "",
        val messageId: String? = null
    )

    suspend fun send(
        db: FirebaseFirestore,
        chatId: String,
        senderUid: String,
        text: String
    ): Result {
        val chatRef = db.collection("chats").document(chatId)
        val senderRef = db.collection("users").document(senderUid)
        val messageRef = chatRef.collection("messages").document()

        return db.runTransaction { transaction ->
            val chat = transaction.get(chatRef)
            if (!chat.exists()) {
                return@runTransaction Result(false, Failure.CHAT_NOT_FOUND)
            }
            if (!DirectMessagePolicy.isDirectChat(chat)) {
                return@runTransaction Result(false, Failure.NOT_DIRECT_CHAT)
            }

            val participants = ((chat.get("participants") as? List<*>)
                ?: (chat.get("members") as? List<*>))
                ?.filterIsInstance<String>()
                .orEmpty()
            if (senderUid !in participants || participants.size != 2) {
                return@runTransaction Result(false, Failure.NOT_PARTICIPANT)
            }

            val peerUid = DirectMessagePolicy.peerUid(chat, senderUid)
                ?: return@runTransaction Result(false, Failure.NOT_PARTICIPANT)
            val sender = transaction.get(senderRef)
            if (!sender.exists()) {
                return@runTransaction Result(false, Failure.PROFILE_UNAVAILABLE)
            }
            if (!DirectMessagePolicy.canSendInTransaction(transaction, db, chat, senderUid)) {
                return@runTransaction Result(false, Failure.BLOCKED)
            }

            val senderName = sender.getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: sender.getString("username")?.takeIf { it.isNotBlank() }
                ?: senderUid

            transaction.set(
                messageRef,
                mapOf(
                    "text" to text,
                    "senderId" to senderUid,
                    "senderName" to senderName,
                    "readBy" to listOf(senderUid),
                    "timestamp" to FieldValue.serverTimestamp(),
                    "messageState" to MessageState.SENT,
                    "isPremium" to (sender.getBoolean("isPremium") ?: false),
                    "isSilent" to false
                )
            )
            transaction.update(
                chatRef,
                mapOf(
                    "lastMessage" to text,
                    "lastSenderId" to senderUid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "unreadCount_$peerUid" to FieldValue.increment(1)
                )
            )
            Result(true, peerUid = peerUid, senderName = senderName, messageId = messageRef.id)
        }.await()
    }
}
