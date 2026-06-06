package com.dan1eidtj.mayas.db // Твой пакет

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // Сохраняем пачку сообщений. Если какое-то сообщение уже скачалось ранее, перезаписываем его
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    // Вытаскиваем сообщения только для конкретного chatId.
    // Сортируем по времени DESC (от новых к старым), потому что у тебя в ChatScreen используется ТГ-реверс для LazyColumn
    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    // Удалить конкретное сообщение по его ID (для функции удаления сообщений)
    @Query("DELETE FROM messages_table WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    // Полностью очистить историю конкретного чата
    @Query("DELETE FROM messages_table WHERE chatId = :chatId")
    suspend fun clearChatHistory(chatId: String)
}