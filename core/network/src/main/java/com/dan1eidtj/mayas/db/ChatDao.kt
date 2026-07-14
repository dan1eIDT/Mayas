package com.dan1eidtj.mayas.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats_table WHERE chatId NOT IN (:activeChatIds)")
    suspend fun deleteChatsNotIn(activeChatIds: List<String>)

    @androidx.room.Transaction
    suspend fun replaceAllChats(chats: List<ChatEntity>) {
        insertChats(chats)
        deleteChatsNotIn(chats.map { it.chatId })
    }
    @Query("SELECT * FROM chats_table ORDER BY updatedAt DESC")
    fun getChatsFlow(): Flow<List<ChatEntity>>


    @Query("""
        UPDATE chats_table 
        SET partnerName = :name,
            partnerAvatarUrl = :avatarUrl,
            partnerProfileGlow = :glow,
            partnerEmoji = :emoji
        WHERE chatId = :chatId
    """)
    suspend fun updatePartnerInfo(
        chatId: String,
        name: String?,
        avatarUrl: String?,
        glow: String?,
        emoji: String?
    )

    @Query("DELETE FROM chats_table")
    suspend fun clearAllChats()

    @Query("SELECT * FROM chats_table WHERE chatId = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?
}
