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
import com.dan1eidtj.mayas.core_ui.ui.theme.MayasTypography

object MayasTheme {
    val IconPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else Color.Black

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

    val GoldGradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF2B2100), Color(0xFF0A0A0C))
        } else {
            listOf(Color(0xFFFFF7E0), Color(0xFFF8F8FC))
        }

    val PinkGradient: List<Color>
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF2B0A1A), Color(0xFF0A0A0C))
        } else {
            listOf(Color(0xFFFDE8F0), Color(0xFFF8F8FC))
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

    val Accent2 = Color(0xFF9B6DFF)
    val ErrorRed = Color(0xFFAF3A3A)
    val RedAccent = Color(0xFF6500FF)
    val TextGrey = Color(0xFF9898A0)

    // Основные акценты
    val Accent = Color(0xFF6D37FF)
    val AccentLight = Color(0xFF9B6DFF)
    val Online = Color(0xFF8B5CF6)
    val Error = Color(0xFFAF3A3A)
    val Success = Color(0xFF54FF36)

    // Свечения и статусы
    val GlowBlack = Color(0xFF151515)
    val GlowPurple = Color(0xFF7B61FF)
    val GlowPink = Color(0xFFFF4D8D)
    val GlowBlue = Color(0xFF5AC8FA)
    val GlowGreen = Color(0xFF54FF36)
    val GlowGold = Color(0xFFFFB800)
    val GlowRed = Color(0xFFFF5252)
    val GlowOrange = Color(0xFFFF9500)
    val GlowCyan = Color(0xFF251060)
    val GlowMint = Color(0xFF00C7BE)
    val GlowIndigo = Color(0xFF5856D6)
    val GlowLime = Color(0xFFD0FF00)
    val GlowRose = Color(0xFFFF2D55)
    val GlowAmber = Color(0xFFFFC400)
    val GlowSky = Color(0xFF00B2FF)
    val GlowWhite = Color(0xFFFFFFFF)


    val NeonBlueStart = Color(0xFF00F2FE)
    val NeonBlueEnd = Color(0xFF4FACFE)
    val GoldStart = Color(0xFFFFD700)
    val GoldEnd = Color(0xFFFFA500)
    val FireStart = Color(0xFFFF4500)
    val FireEnd = Color(0xFFFF8C00)
    val IceStart = Color(0xFFE0F7FA)
    val IceEnd = Color(0xFF80DEEA)
    val SunsetStart = Color(0xFFFF5F6D)
    val SunsetEnd = Color(0xFFFFC371)
    val ForestStart = Color(0xFF11998E)
    val ForestEnd = Color(0xFF38EF7D)
    val MidnightStart = Color(0xFF232526)
    val MidnightEnd = Color(0xFF414345)

    // Праздничные
    val HolidayVictory = Color(0xFFFF9C06)
    val HolidaySummer = Color(0xFF00FFC2)
    val HolidayNewYear = Color(0xFF00B1FF)
    val HolidayLove = Color(0xFFFF4081)

    // ─── Рамки аватара (MayasAvatar, frameType) ─────────────────────────
    // Раньше жили как локальные Color.Cyan/Color.Red и т.д. прямо в Avatar.kt —
    // перенесены сюда, чтобы все цвета персонализации были в одном месте.
    val FrameGold: List<Color> = listOf(GlowGold, Color(0xFFFFE082), GlowGold)
    val FrameNeon: List<Color> = listOf(Color(0xFF00F2FE), Color(0xFFFF00FF), Color(0xFF00F2FE))
    val FrameFire: List<Color> = listOf(Color(0xFFFF3B30), Color(0xFFFFD60A), Color(0xFFFF3B30))
    val FrameDefault: List<Color> = listOf(GlowPurple, GlowBlue, GlowPink, GlowPurple)

    // Чёрная рамка: чистый 0xFF000000 сливается с тёмным фоном приложения
    // (Background в тёмной теме — тоже почти чёрный). Поэтому это не
    // однотонный чёрный, а тёмное графитовое кольцо со светлым металлическим
    // бликом, который едет по кругу вместе с анимацией вращения рамки.
    val FrameBlack: List<Color> = listOf(
        Color(0xFF3A3A3A),
        Color(0xFF0D0D0D),
        Color(0xFFB5B5B5), // блик
        Color(0xFF0D0D0D),
        Color(0xFF3A3A3A)
    )
    // Статичная полупрозрачная белая обводка под рамкой — гарантирует, что
    // чёрная рамка видна ВСЕГДА, даже в те моменты вращения, когда блик
    // (FrameBlack) находится в противоположной точке круга.
    val FrameBlackHalo = Color.White.copy(alpha = 0.15f)

    // ─── Премиум-цвета ника (ColorPicker) ────────────────────────────────
    val NicknameGold: List<Color> = listOf(Color(0xFFFFE57F), GlowGold, Color(0xFFB8860B))
    val NicknameRose: List<Color> = listOf(Color(0xFFFF8FAB), GlowRose, Color(0xFFB0003A))
    val NicknameAmber: List<Color> = listOf(Color(0xFFFFE082), GlowAmber, Color(0xFFC77800))
    val NicknameSky: List<Color> = listOf(Color(0xFF7FE0FF), GlowSky, Color(0xFF0066CC))
    val NicknameWhite: List<Color> = listOf(Color.White, Color(0xFFE0E0E0), Color(0xFFB0B0B0))
    // Тот же принцип, что и с рамкой — не чистый чёрный, а с читаемым переходом
    val NicknameBlack: List<Color> = listOf(Color(0xFF4A4A4A), GlowBlack, Color(0xFF000000))

    // ─── Простые (не-премиум) цвета ника ─────────────────────────────────
    val NicknameSimpleGold = Color(0xFFFFD700)
    val NicknameSimpleCyan = Color(0xFF00F2FE)

    val Background: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)

    val Surface: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF14141A) else Color(0xFFFFFFFF)

    val SurfaceVariant: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1E1E26) else Color(0xFFE5E5EA)

    val TextPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFF1F1F3) else Color(0xFF1C1C1E)

    val TextSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF9898A0) else Color(0xFF8E8E93)

    val BubbleMine: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF413662) else Color(0xFF6D37FF)

    val BubbleOther: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1E1E26) else Color(0xFFE5E5EA)

    val Divider: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF25252E) else Color(0xFFD1D1D6)

    val Outline: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    val LinkColor: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFC5B4E3) else Color(0xFF007AFF)

    // Градиенты фонов
    val PurpleGradient: List<Color>
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) listOf(Color(0xFF1A102B), Color(0xFF0A0A0C))
        else listOf(Color(0xFFEFEBFF), Color(0xFFF2F2F7))

    // Геометрия
    val CardRadius = RoundedCornerShape(18.dp)
    val BubbleRadius = RoundedCornerShape(20.dp)
    val ScreenPadding = 16.dp

    // Схемы Material 3
    val DarkColors = darkColorScheme(
        primary = MayasTheme.Accent,
        secondary = MayasTheme.AccentLight,
        background = Color(0xFF0A0A0C),
        surface = Color(0xFF14141A),
        onPrimary = Color.White,
        onBackground = Color(0xFFF1F1F3),
        onSurface = Color(0xFFF1F1F3),
        surfaceVariant = Color(0xFF1E1E26),
        onSurfaceVariant = Color(0xFF9898A0),
        outline = Color.White.copy(alpha = 0.12f)
    )

    val LightColors = lightColorScheme(
        primary = MayasTheme.Accent,
        secondary = MayasTheme.AccentLight,
        background = Color(0xFFF2F2F7),
        surface = Color.White,
        onPrimary = Color.White,
        onBackground = Color(0xFF1C1C1E),
        onSurface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF8E8E93),
        outline = Color.Black.copy(alpha = 0.08f)
    )
}

@Composable
fun MayasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MayasTheme.DarkColors else MayasTheme.LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MayasTypography,
        content = content
    )
}