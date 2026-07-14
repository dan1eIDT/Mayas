package com.dan1eidtj.mayas.core_ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

@Composable
fun getNameColorBrush(colorName: String): Brush {
    if (colorName == "rainbow") {
        val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rainbowOffset"
        )
        return Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700), Color(0xFFFFAA00), Color(0xFFFF6B35), 
                Color(0xFFE040FB), Color(0xFF5AC8FA), Color(0xFFFFD700)
            ),
            start = Offset(offset, offset),
            end = Offset(offset + 500f, offset + 500f),
            tileMode = androidx.compose.ui.graphics.TileMode.Mirror
        )
    }

    return when (colorName) {
        "gold" -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFAA00)))
        "purple" -> Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFFBBADFF)))
        "pink" -> Brush.linearGradient(listOf(Color(0xFFFF4D8D), Color(0xFFFF94B8)))
        "blue" -> Brush.linearGradient(listOf(Color(0xFF5AC8FA), Color(0xFFADE6FF)))
        "green" -> Brush.linearGradient(listOf(Color(0xFF54FF36), Color(0xFFB0FF9E)))
        "red" -> Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF9E9E)))
        "orange" -> Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFFCC80)))
        "cyan" -> Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)))
        "white" -> Brush.linearGradient(listOf(Color.White, Color(0xFFE0E0E0)))
        else -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFAA00)))
    }
}

fun isUserOnline(userData: Map<String, Any?>?): Boolean {
    if (userData == null) return false

    // Если включен режим невидимки, всегда офлайн
    if (userData["isInvisible"] as? Boolean == true) return false

    // 1. Проверяем строковое поле активности
    val activity = userData["activity"] as? String
    if (activity == "в сети" || activity == "online") return true

    // 2. Проверяем Timestamp последнего входа (lastSeen)
    val lastSeen = userData["lastSeen"] as? Timestamp
    if (lastSeen != null) {
        // Берем по модулю (abs) на случай, если время на девайсе спешит относительно Firebase
        val diff = Math.abs(System.currentTimeMillis() - lastSeen.toDate().time)
        return diff < 60_000 // Считаем онлайн, если активность была менее 1 минуты (60 сек) назад
    }
    return false
}

fun formatLastSeen(ts: Timestamp?): String {
    if (ts == null) return "был(а) недавно"
    
    // Проверка на онлайн через ту же логику, что и в isUserOnline, 
    // но упрощенно для текста, если мы знаем что isInvisible=false
    val now = System.currentTimeMillis()
    val last = ts.toDate().time
    val diff = now - last

    if (diff < 60_000) return "в сети"

    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sdfDate = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    return when {
        diff < 60 * 60_000 -> "был(а) ${diff / 60_000} мин. назад"
        diff < 24 * 60 * 60_000 -> "был(а) в ${sdfTime.format(ts.toDate())}"
        else -> "был(а) ${sdfDate.format(ts.toDate())}"
    }
}

fun formatTime(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val now = Calendar.getInstance().time
    val diff = now.time - date.time
    return when {
        diff < 24 * 60 * 60 * 1000 ->
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        diff < 7 * 24 * 60 * 60 * 1000 ->
            SimpleDateFormat("EEE", Locale("ru")).format(date)
        else ->
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}

fun getGlowColor(profileGlow: String): Color {
    return when (profileGlow) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "gold" -> MayasTheme.GlowGold
        "red" -> MayasTheme.GlowRed
        "orange" -> MayasTheme.GlowOrange
        "cyan" -> MayasTheme.GlowCyan
        "mint" -> MayasTheme.GlowMint
        "indigo" -> MayasTheme.GlowIndigo
        "lime" -> MayasTheme.GlowLime
        "rose" -> MayasTheme.GlowRose
        "amber" -> MayasTheme.GlowAmber
        "sky" -> MayasTheme.GlowSky
        "white" -> MayasTheme.GlowWhite
        else -> MayasTheme.GlowPurple
    }
}
