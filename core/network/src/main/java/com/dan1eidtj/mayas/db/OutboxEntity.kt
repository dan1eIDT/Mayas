/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox_table")
data class OutboxEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val chatId: String,
    val kind: String,
    val createdAt: Long,
    val state: String,
    val attempts: Int,
    val filesJson: String,
    val fieldsJson: String,
    val targetField: String,
    val previewText: String,
    val partnerUid: String,
    val isGroup: Boolean,
    val caption: String?,
    val durationSec: Int,
    val lastError: String? = null
) {
    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_SENDING = "SENDING"
        const val STATE_FAILED = "FAILED"
        const val KIND_IMAGE = "IMAGE"
        const val KIND_ALBUM = "ALBUM"
        const val KIND_CIRCLE = "CIRCLE"
        const val KIND_VOICE = "VOICE"
    }
}
