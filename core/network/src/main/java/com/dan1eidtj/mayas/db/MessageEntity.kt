package com.dan1eidtj.mayas.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages_table")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val text: String,
    val senderId: String,
    val senderName: String?,
    val timestamp: Long,

    val replyToText: String? = null,
    val replyToName: String? = null,


    val status: Int = 0,

    val readBy: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val isPremium: Boolean = false,
    val messageStyle: String? = null
)
