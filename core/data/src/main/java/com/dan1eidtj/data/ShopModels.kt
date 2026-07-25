package com.dan1eidtj.data

import kotlinx.serialization.Serializable

@Serializable
enum class ItemType {
    BUBBLE,
    COLOR_SCHEME,
    ANIMATION,
    EFFECT,
    FONT,
    EMOJI_STATUS
}

@Serializable
enum class ItemRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

@Serializable
data class ShopItem(
    val id: String,
    val name: String,
    val type: ItemType,
    val price: Int,
    val icon: String? = null,
    val description: String = "",
    val isNew: Boolean = false
)
