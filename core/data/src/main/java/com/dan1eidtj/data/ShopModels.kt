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
        val now = parseAvailabilityInstant(nowIso, endInclusive = false) ?: return false
        val from = if (availableFrom.isNullOrBlank()) null
            else parseAvailabilityInstant(availableFrom, endInclusive = false) ?: return false
        val to = if (availableTo.isNullOrBlank()) null
            else parseAvailabilityInstant(availableTo, endInclusive = true) ?: return false
        if (from != null && to != null && from.isAfter(to)) return false
        return (from == null || !now.isBefore(from)) && (to == null || !now.isAfter(to))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun parseAvailabilityInstant(value: String, endInclusive: Boolean): java.time.Instant? {
    val text = value.trim()
    if (text.isEmpty()) return null

    runCatching { java.time.Instant.parse(text) }.getOrNull()?.let { return it }
    runCatching { java.time.OffsetDateTime.parse(text).toInstant() }.getOrNull()?.let { return it }
    runCatching {
        val localDateTime = java.time.LocalDateTime.parse(text)
        localDateTime.toInstant(java.time.ZoneOffset.UTC)
    }.getOrNull()?.let { return it }
    return runCatching {
        val date = java.time.LocalDate.parse(text)
        if (endInclusive) {
            date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).minusNanos(1)
        } else {
            date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        }
    }.getOrNull()
}

enum class BubbleCorner(val fileName: String) {
    TOP_LEFT("corner_tl.png"),
    TOP_RIGHT("corner_tr.png"),
    BOTTOM_LEFT("corner_bl.png"),
    BOTTOM_RIGHT("corner_br.png")
}

private fun ShopItem.bubbleAssetUrl(fileName: String): String? =
    bubbleAssetFolderUrl
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotEmpty() }
        ?.let { "$it/$fileName" }

fun ShopItem.bubbleBaseAssetUrl(): String? = bubbleAssetUrl("base.png")

fun ShopItem.bubbleCornerAssetUrl(corner: BubbleCorner): String? =
    bubbleAssetUrl(corner.fileName)

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
