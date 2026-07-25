package com.dan1eidtj.mayas.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dan1eidtj.mayas.core_ui.ui.theme.MayasTypography

object MayasTheme {


    val colors: MayasColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalMayasColorScheme.current

    val Background: Color @Composable @ReadOnlyComposable get() = colors.background
    val Surface: Color @Composable @ReadOnlyComposable get() = colors.surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = colors.surfaceVariant
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = colors.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = colors.textSecondary
    val BubbleMine: Color @Composable @ReadOnlyComposable get() = colors.bubbleMine
    val BubbleOther: Color @Composable @ReadOnlyComposable get() = colors.bubbleOther
    val Divider: Color @Composable @ReadOnlyComposable get() = colors.divider
    val Outline: Color @Composable @ReadOnlyComposable get() = colors.outline
    val LinkColor: Color @Composable @ReadOnlyComposable get() = colors.linkColor
    val Accent: Color @Composable @ReadOnlyComposable get() = colors.accent
    val AccentLight: Color @Composable @ReadOnlyComposable get() = colors.accentLight
    val IconPrimary: Color @Composable @ReadOnlyComposable get() = colors.iconPrimary
    val CreditsBackground: Color @Composable @ReadOnlyComposable get() = colors.creditsBackground
    val CreditsText: Color @Composable @ReadOnlyComposable get() = colors.creditsText
    val CreditsSecondaryText: Color @Composable @ReadOnlyComposable get() = colors.creditsSecondaryText
    val PurpleGradient: List<Color> @Composable @ReadOnlyComposable get() = colors.purpleGradient
    val BlueGradient: List<Color> @Composable @ReadOnlyComposable get() = colors.blueGradient
    val RedGradient: List<Color> @Composable @ReadOnlyComposable get() = colors.redGradient
    val GoldGradient: List<Color> @Composable @ReadOnlyComposable get() = colors.goldGradient
    val PinkGradient: List<Color> @Composable @ReadOnlyComposable get() = colors.pinkGradient



    val Accent2 = Color(0xFF9B6DFF)
    val ErrorRed = Color(0xFFAF3A3A)
    val RedAccent = Color(0xFF6500FF)
    val TextGrey = Color(0xFF9898A0)

    val GlowBlack = Color(0xFF151515)
    val GlowPurple = Color(0xFF9559FF)
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

    val NeonBlueStart = Color(0xFF003D6B)
    val NeonBlueEnd = Color(0xFF320085)
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

    val HolidayVictory = Color(0xFFFF9C06)
    val HolidaySummer = Color(0xFF00FFC2)
    val HolidayNewYear = Color(0xFF00B1FF)
    val HolidayLove = Color(0xFFFF4081)

    val Error = Color(0xFFAF3A3A)
    val Success = Color(0xFF54FF36)

    val FrameGold: List<Color> = listOf(GlowGold, Color(0xFFFFE082), GlowGold)
    val FrameNeon: List<Color> = listOf(Color(0xFF0063FF), Color(0xFFFF00FF), Color(0xFF0063FF))
    val FrameFire: List<Color> = listOf(Color(0xFFFF3B30), Color(0xFFFFD60A), Color(0xFFFF3B30))
    val FrameDefault: List<Color> = listOf(GlowPurple, GlowBlue, GlowPink, GlowPurple)

    val FrameBlack: List<Color> = listOf(
        Color(0xFF3A3A3A),
        Color(0xFF0D0D0D),
        Color(0xFFB5B5B5),
        Color(0xFF0D0D0D),
        Color(0xFF3A3A3A)
    )

    val FrameBlackHalo = Color.White.copy(alpha = 0.15f)

    val NicknameGold: List<Color> = listOf(Color(0xFFFFE57F), GlowGold, Color(0xFFB8860B))
    val NicknameRose: List<Color> = listOf(Color(0xFFFF8FAB), GlowRose, Color(0xFFB0003A))
    val NicknameAmber: List<Color> = listOf(Color(0xFFFFE082), GlowAmber, Color(0xFFC77800))
    val NicknameSky: List<Color> = listOf(Color(0xFF7FE0FF), GlowSky, Color(0xFF0066CC))
    val NicknameWhite: List<Color> = listOf(Color.White, Color(0xFFE0E0E0), Color(0xFFB0B0B0))
    val NicknameBlack: List<Color> = listOf(Color(0xFF4A4A4A), GlowBlack, Color(0xFF000000))

    val NicknameSimpleGold = Color(0xFFFFD700)
    val NicknameSimpleCyan = Color(0xFF00F2FE)


    val CardRadius = RoundedCornerShape(18.dp)
    val BubbleRadius = RoundedCornerShape(20.dp)
    val ScreenPadding = 16.dp


    val DarkColors = darkColorScheme(
        primary = Accent2,
        secondary = Accent2,
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
        primary = Accent2,
        secondary = Accent2,
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
    colorScheme: MayasColorScheme = if (darkTheme) DarkMayasColorScheme else LightMayasColorScheme,
    content: @Composable () -> Unit
) {
    val materialColors = if (darkTheme) MayasTheme.DarkColors else MayasTheme.LightColors

    CompositionLocalProvider(LocalMayasColorScheme provides colorScheme) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = MayasTypography,
            content = content
        )
    }
}