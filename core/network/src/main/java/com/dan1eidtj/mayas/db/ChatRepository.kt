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

    // --- Читаем из Room (UI подписывается сюда) ---

    fun getChats(): Flow<List<ChatEntity>> = chatDao.getChatsFlow()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)

    // --- Пишем из Firestore в Room ---

    suspend fun syncChatsFromSnapshot(snapshot: QuerySnapshot, userId: String) {
        try {
            val entities = snapshot.documents.mapNotNull { doc ->
                try {
                    val type = doc.getString("type") ?: "DIRECT"
                    val isGroup = type == "GROUP" || (doc.getBoolean("isGroup") ?: false)

                    @Suppress("UNCHECKED_CAST")
                    val admins = (doc.get("adminsList") as? List<*>)
                        ?.map { it.toString() }
                        ?: emptyList()

                    // Находим partnerUid для личных чатов
                    val partnerUid = if (!isGroup) {
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("participants") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.firstOrNull { it != userId }
                    } else null

                    // Сохраняем старые партнёрские данные если новые ещё не пришли
                    // (защита от мигания пустыми аватарками)
                    val existing = if (!isGroup) chatDao.getChatById(doc.id) else null

                    ChatEntity(
                        chatId = doc.id,
                        isGroup = isGroup,
                        groupName = doc.getString("groupName") ?: doc.getString("title"),
                        groupAvatarUrl = doc.getString("groupAvatarUrl") ?: doc.getString("groupAvatar"),
                        groupIcon = doc.getString("groupIcon"),
                        useCustomAvatar = doc.getBoolean("useCustomAvatar") ?: false,
                        lastMessage = doc.getString("lastMessage"),
                        unreadCount = (doc.getLong("unreadCount_$userId") ?: 0L).toInt(),
                        updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                        description = doc.getString("description"),
                        ownerId = doc.getString("ownerId"),
                        adminsList = admins,
                        isPublic = doc.getBoolean("isPublic") ?: false,
                        isPinned = doc.getBoolean("pinned_$userId") ?: false,
                        partnerUid = partnerUid,
                        // Берём закэшированные партнёрские данные чтобы не было мигания
                        partnerName = existing?.partnerName,
                        partnerAvatarUrl = existing?.partnerAvatarUrl,
                        partnerProfileGlow = existing?.partnerProfileGlow,
                        partnerEmoji = existing?.partnerEmoji,
                        typingText = null
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

    /**
     * Вызывается из ChatListViewModel когда приходит обновление профиля партнёра.
     * Обновляет только партнёрские поля — не затрагивает остальные данные чата.
     */
    suspend fun updatePartnerInfoFromSnapshot(chatId: String, userDoc: DocumentSnapshot) {
        try {
            chatDao.updatePartnerInfo(
                chatId = chatId,
                name = userDoc.getString("name") ?: userDoc.getString("username"),
                avatarUrl = userDoc.getString("avatarUrl"),
                glow = userDoc.getString("profileGlow"),
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
                glow = userDoc.getString("profileGlow"),
                emoji = userDoc.getString("emojiStatus")
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка updatePartnerInfoFromFirestore", e)
        }
    }

    /**
     * Синк сообщений конкретного чата.
     * Вызывается при открытии ChatScreen.
     */
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
                        messageStyle = doc.getString("messageStyle")
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

    /** Подгрузка старых сообщений из Room при скролле вверх */
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
    /** Очистить все чаты из Room (например при логауте) */
    suspend fun clearAll() {
        chatDao.clearAllChats()
    }
}
