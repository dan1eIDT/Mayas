package com.dan1eidtj.mayas.db // Проверь свой package!

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages_table")
data class MessageEntity(
    @PrimaryKey val messageId: String, // Уникальный ID самого сообщения
    val chatId: String,                 // К какому именно чату относится это сообщение (чтобы связывать их)
    val text: String,                   // Сам текст сообщения
    val senderId: String,               // Кто отправил (твой UID или собеседника)
    val senderName: String?,            // Имя отправителя (критично для групп, чтобы сразу отображать)
    val timestamp: Long,                // Время отправки в миллисекундах (для сортировки истории от старых к новым)

    // Поля для ответов (Reply), прямо как у тебя в Firestore
    val replyToText: String?,
    val replyToName: String?
)