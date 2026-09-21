/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OutboxEntity)

    @Query("SELECT * FROM outbox_table WHERE ownerUid = :uid AND state != 'FAILED' ORDER BY createdAt ASC")
    suspend fun getActive(uid: String): List<OutboxEntity>

    @Query("SELECT * FROM outbox_table WHERE ownerUid = :uid AND chatId = :chatId ORDER BY createdAt ASC")
    fun observeForChat(uid: String, chatId: String): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox_table WHERE id = :id")
    suspend fun get(id: String): OutboxEntity?

    @Query("UPDATE outbox_table SET state = :state, attempts = :attempts WHERE id = :id")
    suspend fun updateState(id: String, state: String, attempts: Int)

    @Query("UPDATE outbox_table SET lastError = :error WHERE id = :id")
    suspend fun updateError(id: String, error: String?)

    @Query("DELETE FROM outbox_table WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE outbox_table SET state = 'PENDING', attempts = 0 WHERE ownerUid = :uid AND chatId = :chatId AND state = 'FAILED'")
    suspend fun resetFailed(uid: String, chatId: String)

    @Query("UPDATE outbox_table SET state = 'PENDING' WHERE state = 'SENDING'")
    suspend fun resetSending()
}
