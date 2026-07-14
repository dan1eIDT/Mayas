package com.dan1eidtj.mayas.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats_table")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val isGroup: Boolean,
    val groupName: String?,
    val groupAvatarUrl: String?,
    val groupIcon: String? = null,
    val useCustomAvatar: Boolean = false,
    val lastMessage: String?,
    val unreadCount: Int,
    val updatedAt: Long,
    val description: String? = null,
    val ownerId: String? = null,
    val adminsList: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val isPinned: Boolean = false,
    val partnerUid: String? = null,
    val partnerName: String? = null,
    val partnerAvatarUrl: String? = null,
    val partnerProfileGlow: String? = null,
    val partnerEmoji: String? = null,
    val typingText: String? = null
)
