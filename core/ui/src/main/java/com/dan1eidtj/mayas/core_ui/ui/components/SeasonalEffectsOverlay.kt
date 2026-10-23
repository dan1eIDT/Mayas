package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun SeasonalEffectsOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!isForeground) return

    val effect = remember(isForeground) {
        val calendar = java.util.Calendar.getInstance()
        seasonalEffectForDate(calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
    }
    val transition = rememberInfiniteTransition(label = "seasonal-effects")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seasonal-particle-progress"
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val count = 18
        repeat(count) { index ->
            val seed = (index * 73 + 19) % 101
            val fraction = seed / 101f
            val speed = 0.55f + (index % 5) * 0.12f
            val y = ((fraction + progress * speed) % 1f) * size.height
            val sway = kotlin.math.sin((progress * 6.28318f + index * 1.7f).toDouble()).toFloat() * 18f
            val x = (fraction * size.width + sway).coerceIn(2f, size.width - 2f)
            val alpha = 0.12f + (index % 4) * 0.035f
            when (effect) {
                SeasonalEffectKind.SNOW -> {
                    drawCircle(Color.White.copy(alpha = alpha.coerceAtMost(0.28f)), radius = 1.5f + (index % 3), center = Offset(x, y))
                }
                SeasonalEffectKind.PETALS -> {
                    rotate(degrees = (progress * 360f + index * 23f) % 360f, pivot = Offset(x, y)) {
                        drawOval(
                            color = Color(0xFFFF91B7).copy(alpha = alpha + 0.08f),
                            topLeft = Offset(x - 2.3f, y - 4f),
                            size = Size(4.6f, 8f)
                        )
                    }
                }
                SeasonalEffectKind.SPARKLES -> {
                    val radius = 2f + (index % 3)
                    drawLine(Color(0xFFFFD166).copy(alpha = alpha + 0.1f), Offset(x - radius, y), Offset(x + radius, y), strokeWidth = 1.1f)
                    drawLine(Color(0xFFFFD166).copy(alpha = alpha + 0.1f), Offset(x, y - radius), Offset(x, y + radius), strokeWidth = 1.1f)
                }
                SeasonalEffectKind.HEARTS -> {
                    val radius = 2f + (index % 2)
                    val color = Color(0xFFFF6B9C).copy(alpha = alpha + 0.09f)
                    drawCircle(color, radius, Offset(x - radius * 0.55f, y - radius * 0.2f))
                    drawCircle(color, radius, Offset(x + radius * 0.55f, y - radius * 0.2f))
                    rotate(45f, pivot = Offset(x, y + radius * 0.3f)) {
                        drawRect(color, topLeft = Offset(x - radius, y - radius * 0.3f), size = Size(radius * 2f, radius * 2f))
                    }
                }
                SeasonalEffectKind.LEAVES -> {
                    rotate(degrees = (progress * 240f + index * 29f) % 360f, pivot = Offset(x, y)) {
                        drawOval(
                            color = Color(0xFFE6A23C).copy(alpha = alpha + 0.09f),
                            topLeft = Offset(x - 2.5f, y - 4f),
                            size = Size(5f, 8f)
                        )
                        drawLine(Color(0xFF8B5A2B).copy(alpha = alpha), Offset(x, y - 3f), Offset(x, y + 3f), strokeWidth = 0.7f)
                    }
                }
            }
        }
    }
}
