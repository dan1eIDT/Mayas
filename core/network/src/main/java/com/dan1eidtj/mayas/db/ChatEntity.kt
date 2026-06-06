package com.dan1eidtj.mayas.db // Пакет должен совпадать с Dao, если они в одной папке!

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats_table")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val isGroup: Boolean,
    val groupName: String?,
    val groupAvatarUrl: String?,
    val lastMessage: String?,
    val unreadCount: Int,
    val updatedAt: Long,

    // Новые поля, которые мы обсуждали для профилей и групп:
    val description: String? = "",
    val ownerId: String? = "",
    val adminsList: String? = "",
    val isPublic: Boolean = false,

    // Кэш для личных чатов
    val partnerName: String?,
    val partnerAvatarUrl: String?,
    val partnerProfileGlow: String?,
    val partnerEmoji: String?
)