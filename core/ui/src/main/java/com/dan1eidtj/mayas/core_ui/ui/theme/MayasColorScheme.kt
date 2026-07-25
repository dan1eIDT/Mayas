package com.dan1eidtj.mayas.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


data class MayasColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,


    val elevatedSurface: Color,

    val textPrimary: Color,
    val textSecondary: Color,

    val bubbleMine: Color,
    val bubbleOther: Color,

    val divider: Color,
    val outline: Color,
    val linkColor: Color,

    val accent: Color,
    val accentLight: Color,
    val iconPrimary: Color,

    val error: Color,
    val success: Color,
    val online: Color,

    val creditsBackground: Color,
    val creditsText: Color,
    val creditsSecondaryText: Color,



    val purpleGradientStart: Color,
    val purpleGradientEnd: Color,
    val blueGradientStart: Color,
    val blueGradientEnd: Color,
    val redGradientStart: Color,
    val redGradientEnd: Color,
    val goldGradientStart: Color,
    val goldGradientEnd: Color,
    val pinkGradientStart: Color,
    val pinkGradientEnd: Color,
) {
    val purpleGradient: List<Color> get() = listOf(purpleGradientStart, purpleGradientEnd)
    val blueGradient: List<Color> get() = listOf(blueGradientStart, blueGradientEnd)
    val redGradient: List<Color> get() = listOf(redGradientStart, redGradientEnd)
    val goldGradient: List<Color> get() = listOf(goldGradientStart, goldGradientEnd)
    val pinkGradient: List<Color> get() = listOf(pinkGradientStart, pinkGradientEnd)

    companion object {

        val FIELD_NAMES = listOf(
            "background", "surface", "surfaceVariant", "elevatedSurface",
            "textPrimary", "textSecondary",
            "bubbleMine", "bubbleOther",
            "divider", "outline", "linkColor",
            "accent", "accentLight", "iconPrimary",
            "error", "success", "online",
            "creditsBackground", "creditsText", "creditsSecondaryText",
            "purpleGradientStart", "purpleGradientEnd",
            "blueGradientStart", "blueGradientEnd",
            "redGradientStart", "redGradientEnd",
            "goldGradientStart", "goldGradientEnd",
            "pinkGradientStart", "pinkGradientEnd",
        )
    }
}


val DarkMayasColorScheme = MayasColorScheme(
    background = Color(0xFF0A0A0C),
    surface = Color(0xFF14141A),
    surfaceVariant = Color(0xFF1E1E26),
    elevatedSurface = Color(0xFF0A0807),

    textPrimary = Color(0xFFF1F1F3),
    textSecondary = Color(0xFF9898A0),

    bubbleMine = Color(0xFF413662),
    bubbleOther = Color(0xFF1E1E26),

    divider = Color(0xFF25252E),
    outline = Color.White.copy(alpha = 0.12f),
    linkColor = Color(0xFFC5B4E3),

    accent = Color(0xFF6D37FF),
    accentLight = Color(0xFF9B6DFF),
    iconPrimary = Color.White,

    error = Color(0xFFAF3A3A),
    success = Color(0xFF54FF36),
    online = Color(0xFF8B5CF6),

    creditsBackground = Color(0xFF050507),
    creditsText = Color(0xFFFFFFFF),
    creditsSecondaryText = Color(0xFF9898A0),

    purpleGradientStart = Color(0xFF1A102B),
    purpleGradientEnd = Color(0xFF0A0A0C),
    blueGradientStart = Color(0xFF0A1630),
    blueGradientEnd = Color(0xFF09090C),
    redGradientStart = Color(0xFF2A0A0A),
    redGradientEnd = Color(0xFF0A0A0C),
    goldGradientStart = Color(0xFF2B2100),
    goldGradientEnd = Color(0xFF0A0A0C),
    pinkGradientStart = Color(0xFF2B0A1A),
    pinkGradientEnd = Color(0xFF0A0A0C),
)


val LightMayasColorScheme = MayasColorScheme(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA),
    elevatedSurface = Color(0xFFFFFFFF),

    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),

    bubbleMine = Color(0xFF6D37FF),
    bubbleOther = Color(0xFFE5E5EA),

    divider = Color(0xFFD1D1D6),
    outline = Color.Black.copy(alpha = 0.08f),
    linkColor = Color(0xFF007AFF),

    accent = Color(0xFF6D37FF),
    accentLight = Color(0xFF9B6DFF),
    iconPrimary = Color.Black,

    error = Color(0xFFAF3A3A),
    success = Color(0xFF54FF36),
    online = Color(0xFF8B5CF6),

    creditsBackground = Color(0xFFF5F5F7),
    creditsText = Color(0xFF111111),
    creditsSecondaryText = Color(0xFF666666),

    purpleGradientStart = Color(0xFFEFEBFF),
    purpleGradientEnd = Color(0xFFF2F2F7),
    blueGradientStart = Color(0xFFE1ECF7),
    blueGradientEnd = Color(0xFFF8F8FC),
    redGradientStart = Color(0xFFFBE6E6),
    redGradientEnd = Color(0xFFF8F8FC),
    goldGradientStart = Color(0xFFFFF7E0),
    goldGradientEnd = Color(0xFFF8F8FC),
    pinkGradientStart = Color(0xFFFDE8F0),
    pinkGradientEnd = Color(0xFFF8F8FC),
)


val LocalMayasColorScheme = staticCompositionLocalOf { DarkMayasColorScheme }
