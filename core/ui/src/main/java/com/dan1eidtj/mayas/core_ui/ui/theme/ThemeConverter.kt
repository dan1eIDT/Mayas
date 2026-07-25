package com.dan1eidtj.mayas.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import kotlin.math.roundToInt


object ThemeConverter {

    fun Color.toHexArgb(): String {
        val a = (alpha * 255).roundToInt()
        val r = (red * 255).roundToInt()
        val g = (green * 255).roundToInt()
        val b = (blue * 255).roundToInt()
        return "#%02X%02X%02X%02X".format(a, r, g, b)
    }

    fun String.hexToColor(): Color {
        val clean = removePrefix("#")
        return when (clean.length) {
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
            }
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
            }
            else -> throw IllegalArgumentException("Некорректный hex-цвет: $this")
        }
    }


    fun MayasColorScheme.toHexMap(): Map<String, String> = linkedMapOf(
        "background" to background.toHexArgb(),
        "surface" to surface.toHexArgb(),
        "surfaceVariant" to surfaceVariant.toHexArgb(),
        "elevatedSurface" to elevatedSurface.toHexArgb(),
        "textPrimary" to textPrimary.toHexArgb(),
        "textSecondary" to textSecondary.toHexArgb(),
        "bubbleMine" to bubbleMine.toHexArgb(),
        "bubbleOther" to bubbleOther.toHexArgb(),
        "divider" to divider.toHexArgb(),
        "outline" to outline.toHexArgb(),
        "linkColor" to linkColor.toHexArgb(),
        "accent" to accent.toHexArgb(),
        "accentLight" to accentLight.toHexArgb(),
        "iconPrimary" to iconPrimary.toHexArgb(),
        "error" to error.toHexArgb(),
        "success" to success.toHexArgb(),
        "online" to online.toHexArgb(),
        "creditsBackground" to creditsBackground.toHexArgb(),
        "creditsText" to creditsText.toHexArgb(),
        "creditsSecondaryText" to creditsSecondaryText.toHexArgb(),
        "purpleGradientStart" to purpleGradientStart.toHexArgb(),
        "purpleGradientEnd" to purpleGradientEnd.toHexArgb(),
        "blueGradientStart" to blueGradientStart.toHexArgb(),
        "blueGradientEnd" to blueGradientEnd.toHexArgb(),
        "redGradientStart" to redGradientStart.toHexArgb(),
        "redGradientEnd" to redGradientEnd.toHexArgb(),
        "goldGradientStart" to goldGradientStart.toHexArgb(),
        "goldGradientEnd" to goldGradientEnd.toHexArgb(),
        "pinkGradientStart" to pinkGradientStart.toHexArgb(),
        "pinkGradientEnd" to pinkGradientEnd.toHexArgb(),
    )


    fun Map<String, String>.toMayasColorScheme(
        fallback: MayasColorScheme = DarkMayasColorScheme
    ): MayasColorScheme {
        fun get(key: String, default: Color): Color =
            this[key]?.let { runCatching { it.hexToColor() }.getOrNull() } ?: default

        return MayasColorScheme(
            background = get("background", fallback.background),
            surface = get("surface", fallback.surface),
            surfaceVariant = get("surfaceVariant", fallback.surfaceVariant),
            elevatedSurface = get("elevatedSurface", fallback.elevatedSurface),
            textPrimary = get("textPrimary", fallback.textPrimary),
            textSecondary = get("textSecondary", fallback.textSecondary),
            bubbleMine = get("bubbleMine", fallback.bubbleMine),
            bubbleOther = get("bubbleOther", fallback.bubbleOther),
            divider = get("divider", fallback.divider),
            outline = get("outline", fallback.outline),
            linkColor = get("linkColor", fallback.linkColor),
            accent = get("accent", fallback.accent),
            accentLight = get("accentLight", fallback.accentLight),
            iconPrimary = get("iconPrimary", fallback.iconPrimary),
            error = get("error", fallback.error),
            success = get("success", fallback.success),
            online = get("online", fallback.online),
            creditsBackground = get("creditsBackground", fallback.creditsBackground),
            creditsText = get("creditsText", fallback.creditsText),
            creditsSecondaryText = get("creditsSecondaryText", fallback.creditsSecondaryText),
            purpleGradientStart = get("purpleGradientStart", fallback.purpleGradientStart),
            purpleGradientEnd = get("purpleGradientEnd", fallback.purpleGradientEnd),
            blueGradientStart = get("blueGradientStart", fallback.blueGradientStart),
            blueGradientEnd = get("blueGradientEnd", fallback.blueGradientEnd),
            redGradientStart = get("redGradientStart", fallback.redGradientStart),
            redGradientEnd = get("redGradientEnd", fallback.redGradientEnd),
            goldGradientStart = get("goldGradientStart", fallback.goldGradientStart),
            goldGradientEnd = get("goldGradientEnd", fallback.goldGradientEnd),
            pinkGradientStart = get("pinkGradientStart", fallback.pinkGradientStart),
            pinkGradientEnd = get("pinkGradientEnd", fallback.pinkGradientEnd),
        )
    }


    fun MayasColorScheme.toJson(themeName: String = "Custom"): String {
        val json = JSONObject()
        json.put("name", themeName)
        val colors = JSONObject()
        toHexMap().forEach { (key, value) -> colors.put(key, value) }
        json.put("colors", colors)
        return json.toString(2)
    }


    fun parseThemeJson(
        jsonString: String,
        fallback: MayasColorScheme = DarkMayasColorScheme
    ): Pair<String, MayasColorScheme> {
        val json = JSONObject(jsonString)
        val name = json.optString("name", "Custom")
        val colorsJson = json.getJSONObject("colors")
        val map = mutableMapOf<String, String>()
        colorsJson.keys().forEach { key -> map[key] = colorsJson.getString(key) }
        return name to map.toMayasColorScheme(fallback)
    }
}
