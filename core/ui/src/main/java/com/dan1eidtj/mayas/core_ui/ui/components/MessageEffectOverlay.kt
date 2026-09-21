/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.dan1eidtj.mayas.core_ui.emoji.EmojiStyle
import com.dan1eidtj.mayas.core_ui.emoji.EmojiStyleState
import com.dan1eidtj.mayas.core_ui.emoji.MayasEmojiArt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private data class EffectParticle(
    val xFraction: Float,
    val delayMs: Int,
    val driftPx: Float,
    val rotationDeg: Float
)

@Composable
fun MessageEffectOverlay(
    effectKey: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val spec = MessageEffects.registry[effectKey]
    if (spec == null) {
        LaunchedEffect(effectKey) { onFinished() }
        return
    }

    val particles = remember(effectKey) {
        List(spec.particleCount) {
            EffectParticle(
                xFraction = Random.nextFloat().coerceIn(0.08f, 0.92f),
                delayMs = Random.nextInt(0, (spec.durationMs * 0.35f).toInt()),
                driftPx = Random.nextInt(-70, 70).toFloat(),
                rotationDeg = Random.nextInt(-25, 25).toFloat()
            )
        }
    }

    val progress = remember(effectKey) { Animatable(0f) }
    LaunchedEffect(effectKey) {
        progress.animateTo(1f, animationSpec = tween(spec.durationMs, easing = LinearEasing))
        onFinished()
    }

    val density = LocalDensity.current
    val emojiContext = LocalContext.current
    EmojiStyleState.ensureInit(emojiContext)
    val artPainter = if (EmojiStyleState.style == EmojiStyle.MAYAS) MayasEmojiArt.painter(spec.emoji) else null
    val layerPaint = remember { Paint() }
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val elapsedMs = progress.value * spec.durationMs
        val riseDistancePx = with(density) { spec.riseDistanceDp.dp.toPx() }
        val startSizePx = with(density) { spec.startFontSizeSp.sp.toPx() }
        val endSizePx = with(density) { spec.endFontSizeSp.sp.toPx() }

        particles.forEach { particle ->
            val activeMs = elapsedMs - particle.delayMs
            val activeDurationMs = spec.durationMs * 0.75f
            if (activeMs <= 0f) return@forEach
            val localT = (activeMs / activeDurationMs).coerceIn(0f, 1f)

            val alpha = when {
                localT < 0.15f -> localT / 0.15f
                localT > 0.8f -> (1f - (localT - 0.8f) / 0.2f).coerceIn(0f, 1f)
                else -> 1f
            }
            if (alpha <= 0f) return@forEach

            val y = size.height - localT * riseDistancePx
            val x = particle.xFraction * size.width + particle.driftPx * localT
            val fontSizePx = startSizePx + (endSizePx - startSizePx) * localT

            if (artPainter != null) {
                val d = fontSizePx * 1.15f
                val left = x - d / 2f
                val top = y - d
                rotate(particle.rotationDeg * localT, Offset(x, y - d / 2f)) {
                    layerPaint.alpha = alpha
                    drawIntoCanvas { canvas -> canvas.saveLayer(Rect(left, top, left + d, top + d), layerPaint) }
                    inset(left, top, size.width - left - d, size.height - top - d) {
                        artPainter()
                    }
                    drawIntoCanvas { canvas -> canvas.restore() }
                }
            } else {
                paint.textSize = fontSizePx
                paint.alpha = (alpha * 255).toInt()

                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(particle.rotationDeg * localT, x, y)
                    drawText(spec.emoji, x, y, paint)
                    restore()
                }
            }
        }
    }
}
