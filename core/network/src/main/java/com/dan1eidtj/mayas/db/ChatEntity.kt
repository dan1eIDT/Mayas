package com.dan1eidtj.mayas.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats_table")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val isGroup: Boolean,
    val groupName: String?,
    val chatType: String = "DIRECT",
    val groupAvatarUrl: String?,
    val groupIcon: String? = null,
    val groupProfileGlow: String? = "purple",
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
    val partnerProfileIcon: String? = "ghost",
    val partnerProfileGlow: String? = "purple",
    val partnerUseCustomAvatar: Boolean = false,
    val partnerIsPremium: Boolean = false,
    val partnerAvatarFrame: String? = "none",
    val partnerNameColor: String? = "gold",
    val partnerEmoji: String? = null,
    // Кэш верификации/ранга партнёра для офлайн-показа в списке чатов.
    // Плоские поля вместо VerificationInfo/Rank — без лишнего TypeConverter'а.
    val partnerVerified: Boolean = false,
    val partnerVerificationType: String? = null,
    val partnerVerifiedBy: String? = null,
    val partnerRank: Int = 0,
    val typingText: String? = null,
    val isSavedMessages: Boolean = false
)