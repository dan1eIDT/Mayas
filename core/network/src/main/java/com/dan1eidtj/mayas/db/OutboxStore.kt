/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.db

import android.content.Context
import kotlinx.coroutines.flow.Flow

class OutboxStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao: OutboxDao get() = MayasDatabase.getDatabase(appContext).outboxDao()

    suspend fun enqueue(item: OutboxEntity) = dao.insert(item)

    suspend fun active(uid: String): List<OutboxEntity> = dao.getActive(uid)

    fun observe(chatId: String): Flow<List<OutboxEntity>> =
        dao.observeForChat(MayasDatabase.currentUid(), chatId)

    suspend fun get(id: String): OutboxEntity? = dao.get(id)

    suspend fun setState(id: String, state: String, attempts: Int) = dao.updateState(id, state, attempts)

    suspend fun setError(id: String, error: String?) = dao.updateError(id, error)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun retryFailed(chatId: String) = dao.resetFailed(MayasDatabase.currentUid(), chatId)

    suspend fun resetSending() = dao.resetSending()
}
