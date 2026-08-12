package com.dan1eidtj.mayas.db

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ChatRepository(context: Context) {
    private val database = MayasDatabase.getDatabase(context)
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val firestore = FirebaseFirestore.getInstance()


    fun getChats(): Flow<List<ChatEntity>> = chatDao.getChatsFlow()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)


    suspend fun syncChatsFromSnapshot(snapshot: QuerySnapshot, userId: String) {
        try {
            val entities = snapshot.documents.mapNotNull { doc ->
                try {
                    val type = doc.getString("type") ?: "DIRECT"
                    val isSavedMessages = type == "SAVED"
                    val isGroup = isSavedMessages || type == "GROUP" || (doc.getBoolean("isGroup") ?: false)

                    @Suppress("UNCHECKED_CAST")
                    val admins = (doc.get("admins") as? List<*>)
                        ?.map { it.toString() }
                        ?: emptyList()

                    val partnerUid = if (!isGroup) {
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("participants") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.firstOrNull { it != userId }
                    } else null

                    val existing = if (!isGroup) chatDao.getChatById(doc.id) else null

                    val resolvedGroupAvatarUrl = doc.getString("groupAvatarUrl") ?: doc.getString("groupAvatar")

                    ChatEntity(
                        chatId = doc.id,
                        isGroup = isGroup,
                        chatType = type,
                        groupName = doc.getString("groupName") ?: doc.getString("title"),
                        groupAvatarUrl = resolvedGroupAvatarUrl,
                        groupIcon = doc.getString("profileIcon") ?: doc.getString("groupIcon"),
                        groupProfileGlow = doc.getString("profileGlow") ?: "purple",
                        useCustomAvatar = if (isGroup) {
                            !resolvedGroupAvatarUrl.isNullOrBlank()
                        } else {
                            doc.getBoolean("useCustomAvatar") ?: false
                        },
                        lastMessage = doc.getString("lastMessage"),
                        unreadCount = (doc.getLong("unreadCount_$userId") ?: 0L).toInt(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                        description = doc.getString("description"),
                        ownerId = doc.getString("ownerId"),
                        adminsList = admins,
                        isPublic = doc.getBoolean("isPublic") ?: false,
                        isPinned = doc.getBoolean("pinned_$userId") ?: false,
                        partnerUid = partnerUid,

                        partnerName = existing?.partnerName,
                        partnerAvatarUrl = existing?.partnerAvatarUrl,
                        partnerProfileIcon = existing?.partnerProfileIcon ?: "ghost",
                        partnerProfileGlow = existing?.partnerProfileGlow ?: "purple",
                        partnerUseCustomAvatar = existing?.partnerUseCustomAvatar ?: false,
                        partnerIsPremium = existing?.partnerIsPremium ?: false,
                        partnerAvatarFrame = existing?.partnerAvatarFrame ?: "none",
                        partnerNameColor = existing?.partnerNameColor ?: "gold",
                        partnerEmoji = existing?.partnerEmoji,
                        typingText = null,
                        isSavedMessages = isSavedMessages
                    )
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Ошибка конвертации чата ${doc.id}", e)
                    null
                }
            }
            chatDao.replaceAllChats(entities)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка syncChatsFromSnapshot", e)
        }
    }


    suspend fun updatePartnerInfoFromSnapshot(chatId: String, userDoc: DocumentSnapshot) {
        try {
            chatDao.updatePartnerInfo(
                chatId = chatId,
                name = userDoc.getString("name") ?: userDoc.getString("username"),
                avatarUrl = userDoc.getString("avatarUrl"),
                profileIcon = userDoc.getString("profileIcon") ?: "ghost",
                glow = userDoc.getString("profileGlow") ?: "purple",
                useCustomAvatar = userDoc.getBoolean("useCustomAvatar") ?: false,
                isPremium = userDoc.getBoolean("isPremium") ?: false,
                avatarFrame = userDoc.getString("avatarFrame") ?: "none",
                nameColor = userDoc.getString("nameColor") ?: "gold",
                emoji = userDoc.getString("emojiStatus")
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка updatePartnerInfo для $chatId", e)
        }
    }

    suspend fun updatePartnerInfoFromFirestore(chatId: String, partnerId: String) {
        try {
            val userDoc = firestore.collection("users").document(partnerId).get().await()
            chatDao.updatePartnerInfo(
                chatId = chatId,
                name = userDoc.getString("name") ?: userDoc.getString("username"),
                avatarUrl = userDoc.getString("avatarUrl"),
                profileIcon = userDoc.getString("profileIcon") ?: "ghost",
                glow = userDoc.getString("profileGlow") ?: "purple",
                useCustomAvatar = userDoc.getBoolean("useCustomAvatar") ?: false,
                isPremium = userDoc.getBoolean("isPremium") ?: false,
                avatarFrame = userDoc.getString("avatarFrame") ?: "none",
                nameColor = userDoc.getString("nameColor") ?: "gold",
                emoji = userDoc.getString("emojiStatus")
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка updatePartnerInfoFromFirestore", e)
        }
    }

    suspend fun syncMessages(chatId: String) {
        try {
            val snapshot = firestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val entities = snapshot.documents.mapNotNull { doc ->
                try {
                    val readBy = (doc.get("readBy") as? List<*>)
                        ?.map { it.toString() } ?: emptyList()
                    val status = when {
                        readBy.size > 1 -> 2
                        doc.contains("timestamp") -> 1
                        else -> 0
                    }

                    MessageEntity(
                        messageId = doc.id,
                        chatId = chatId,
                        text = doc.getString("text") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName"),
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
                        replyToText = doc.getString("replyToText"),
                        replyToName = doc.getString("replyToName"),
                        status = status,
                        readBy = readBy,
                        mediaUrl = doc.getString("mediaUrl"),
                        isPremium = doc.getBoolean("isPremium") ?: false,
                        messageStyle = doc.getString("messageStyle"),
                        forwardedFromName = doc.getString("forwardedFromName")
                    )
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Ошибка конвертации сообщения ${doc.id}", e)
                    null
                }
            }
            messageDao.insertMessages(entities)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка syncMessages для $chatId", e)
        }
    }

    suspend fun loadMoreMessages(chatId: String, offset: Int): List<MessageEntity> {
        return messageDao.getMessagesPaged(chatId, limit = 50, offset = offset)
    }
    suspend fun deleteMessageLocally(messageId: String) {
        try {
            messageDao.deleteMessageById(messageId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка удаления сообщения из кэша $messageId", e)
        }
    }

    suspend fun clearChatHistoryLocally(chatId: String) {
        try {
            messageDao.clearChatHistory(chatId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка очистки локальной истории чата $chatId", e)
        }
    }
    suspend fun clearAll() {
        chatDao.clearAllChats()
    }
}
