package com.dan1eidtj.mayas.db // Твой пакет

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    // Вытаскиваем все чаты и сортируем их по времени (updatedAt) от самых свежих к старым
    @Query("SELECT * FROM chats_table ORDER BY updatedAt DESC")
    fun getChatsFlow(): Flow<List<ChatEntity>>

    // Очистить все чаты (нужно, например, если пользователь разлогинился)
    @Query("DELETE FROM chats_table")
    suspend fun clearAllChats()
}