package com.dan1eidtj.mayas.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>



    @Query("""
        SELECT * FROM messages_table 
        WHERE chatId = :chatId 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getMessagesPaged(chatId: String, limit: Int = 50, offset: Int = 0): List<MessageEntity>

    @Query("DELETE FROM messages_table WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages_table WHERE chatId = :chatId")
    suspend fun clearChatHistory(chatId: String)

    @Query("""
        SELECT * FROM messages_table 
        WHERE chatId = :chatId AND text LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages_table 
        WHERE text LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>
}
