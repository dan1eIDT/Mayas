package com.dan1eidtj.mayas.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun MayasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        MayasTheme.DarkColors
    } else {
        MayasTheme.LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

object MayasTheme {

    // --- ДИНАМИЧЕСКИЕ ЦВЕТА (Автоматически переключаются в зависимости от системной темы) ---

    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A0A0C) else Color(0xFFF8F8FC)

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF14141A) else Color(0xFFFFFFFF)

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFF1F1F3) else Color(0xFF111111)

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF9898A0) else Color(0xFF666666)

    val BubbleOther: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1E1E26) else Color(0xFFEAEAF2)

    val PurpleGradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF1A102B), Color(0xFF0A0A0C))
        } else {
            listOf(Color(0xFFE8E1F5), Color(0xFFF8F8FC))
        }

    val dan1eYTHB: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A0807) else Color(0xFFFFFFFF)

    val BlueGradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF0A1630), Color(0xFF09090C))
        } else {
            listOf(Color(0xFFE1ECF7), Color(0xFFF8F8FC))
        }

    val RedGradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF2A0A0A), Color(0xFF0A0A0C))
        } else {
            listOf(Color(0xFFFBE6E6), Color(0xFFF8F8FC))
        }

    val CreditsBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF050507) else Color(0xFFF5F5F7)

    val CreditsText: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF111111)

    val CreditsSecondaryText: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF9898A0) else Color(0xFF666666)


    // --- СТАТИЧЕСКИЕ ЦВЕТА (Остаются неизменными на любой теме) ---

    val Accent = Color(0xFF6D37FF)
    val Accent2 = Color(0xFF9B6DFF)
    val Online = Color(0xFF8B5CF6)
    val ErrorRed = Color(0xFFAF3A3A)
    val BubbleMine : Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF413662) else Color(0xFF7F68E5)


    val GlowPurple = Color(0xFF7B61FF)
    val GlowPink = Color(0xFFFF4D8D)
    val GlowBlue = Color(0xFF5AC8FA)
    val GlowGreen = Color(0xFF54FF36)
    val GlowGold = Color(0xFFFFB800)
    val GlowRed = Color(0xFFFF5252)

    val RedAccent = Color(0xFF6500FF)
    val TextGrey = Color(0xFF9898A0)

    // --- ГЕОМЕТРИЯ ---
    val CardRadius = RoundedCornerShape(18.dp)
    val BubbleRadius = RoundedCornerShape(20.dp)
    val ScreenPadding = 16.dp


    // --- СХЕМЫ ДЛЯ MATERIALTHEME (Для поддержки стандартных компонентов) ---

    val DarkColors = darkColorScheme(
        primary = Accent,
        secondary = Accent2,
        background = Color(0xFF0A0A0C),
        surface = Color(0xFF14141A),
        onPrimary = Color.White,
        onBackground = Color(0xFFF1F1F3),
        onSurface = Color(0xFFF1F1F3),
        surfaceVariant = Color(0xFF1E1E26),
        onSurfaceVariant = Color(0xFF9898A0)
    )

    val LightColors = lightColorScheme(
        primary = Accent,
        secondary = Accent2,
        background = Color(0xFFF8F8FC),
        surface = Color.White,
        onPrimary = Color.White,
        onBackground = Color(0xFF111111),
        onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFEAEAF2),
        onSurfaceVariant = Color(0xFF666666)
    )
}