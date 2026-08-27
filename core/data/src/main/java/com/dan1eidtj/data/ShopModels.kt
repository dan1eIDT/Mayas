/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.data

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

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
    val isNew: Boolean = false,
    val colorHex: String? = null,
    val gradientHex: List<String> = emptyList(),
    val textColorHex: String? = null,
    val availableFrom: String? = null,
    val availableTo: String? = null,
    val imageUrl: String? = null,
    val bundleItemIds: List<String> = emptyList(),
    val bubbleAssetFolderUrl: String? = null
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun isAvailableNow(nowIso: String = java.time.Instant.now().toString()): Boolean {
        val from = availableFrom
        val to = availableTo
        if (from == null && to == null) return true
        if (from != null && nowIso < from) return false
        if (to != null && nowIso > to) return false
        return true
    }
}

enum class BubbleCorner(val fileName: String) {
    TOP_LEFT("corner_tl.png"),
    TOP_RIGHT("corner_tr.png"),
    BOTTOM_LEFT("corner_bl.png"),
    BOTTOM_RIGHT("corner_br.png")
}

fun ShopItem.bubbleBaseAssetUrl(): String? =
    bubbleAssetFolderUrl?.trimEnd('/')?.let { "$it/base.png" }

fun ShopItem.bubbleCornerAssetUrl(corner: BubbleCorner): String? =
    bubbleAssetFolderUrl?.trimEnd('/')?.let { "$it/${corner.fileName}" }

fun ShopItem.hasCustomBubbleAssets(): Boolean =
    !bubbleAssetFolderUrl.isNullOrBlank()

private val shopCatalogJson = kotlinx.serialization.json.Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun encodeShopItemsToJson(items: List<ShopItem>): String =
    shopCatalogJson.encodeToString(items)

fun decodeShopItemsFromJson(text: String): List<ShopItem> =
    shopCatalogJson.decodeFromString(text)
