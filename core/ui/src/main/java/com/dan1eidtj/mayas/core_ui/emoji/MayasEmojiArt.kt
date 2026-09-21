/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.core_ui.emoji

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private typealias EmojiPainter = DrawScope.() -> Unit

private val Ink = Color(0xFF4A2C12)
private val Tongue = Color(0xFFE5474B)
private val Tear = Color(0xFF5EC7FF)

private class UPath(private val s: Float) {
    val path = Path()
    fun m(x: Float, y: Float) = path.moveTo(x * s, y * s)
    fun l(x: Float, y: Float) = path.lineTo(x * s, y * s)
    fun c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        path.cubicTo(x1 * s, y1 * s, x2 * s, y2 * s, x3 * s, y3 * s)
    fun q(x1: Float, y1: Float, x2: Float, y2: Float) = path.quadraticTo(x1 * s, y1 * s, x2 * s, y2 * s)
    fun z() = path.close()
}

private inline fun DrawScope.upath(build: UPath.() -> Unit): Path =
    UPath(size.minDimension).apply(build).path

private enum class Eyes { DOT, ARC_UP, ARC_DOWN, LINE, SQUINT, HEART, STAR, WIDE, SHADES, WINK, ROLL, HALF, PLEAD, NONE }
private enum class Mouth { GRIN, GRIN_BIG, GRIN_TEETH, SMILE, SMILE_SMALL, FROWN, FROWN_SMALL, OPEN_O, OPEN_SMALL, FLAT, FLAT_SLANT, SMIRK, KISS, TEETH, NONE }
private enum class Brows { NONE, ANGRY, RAISED, WORRIED }
private enum class Base { YELLOW, RED, BLUE, PURPLE }

private data class FaceSpec(
    val eyes: Eyes,
    val mouth: Mouth,
    val brows: Brows = Brows.NONE,
    val base: Base = Base.YELLOW,
    val blush: Boolean = false,
    val tearsJoy: Boolean = false,
    val tearStream: Boolean = false,
    val tear: Boolean = false,
    val sweat: Boolean = false,
    val flip: Boolean = false
)

private fun DrawScope.p(x: Float, y: Float): Offset = Offset(x * size.minDimension, y * size.minDimension)

private fun DrawScope.sz(w: Float, h: Float): Size = Size(w * size.minDimension, h * size.minDimension)

private fun DrawScope.drawTear(cx: Float, cy: Float, r: Float) {
    val s = size.minDimension
    val path = upath {
        m(cx, cy - r * 2.4f)
        c(cx + r * 0.4f, cy - r * 1.4f, cx + r, cy - r * 0.8f, cx + r, cy)
        c(cx + r, cy + r * 0.9f, cx - r, cy + r * 0.9f, cx - r, cy)
        c(cx - r, cy - r * 0.8f, cx - r * 0.4f, cy - r * 1.4f, cx, cy - r * 2.4f)
        z()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Tear, Color(0xFF2196F3)), startY = (cy - r * 2.4f) * s, endY = (cy + r) * s))
}

private fun DrawScope.drawHeartShape(cx: Float, cy: Float, w: Float, top: Color, bottom: Color) {
    val s = size.minDimension
    val h = w
    val path = Path().apply {
        val x0 = (cx - w / 2f) * s
        val y0 = (cy - h / 2f) * s
        val ww = w * s
        val hh = h * s
        moveTo(x0 + ww * 0.5f, y0 + hh * 0.95f)
        cubicTo(x0 + ww * 0.05f, y0 + hh * 0.62f, x0 + ww * 0.0f, y0 + hh * 0.30f, x0 + ww * 0.25f, y0 + hh * 0.16f)
        cubicTo(x0 + ww * 0.40f, y0 + hh * 0.08f, x0 + ww * 0.5f, y0 + hh * 0.20f, x0 + ww * 0.5f, y0 + hh * 0.30f)
        cubicTo(x0 + ww * 0.5f, y0 + hh * 0.20f, x0 + ww * 0.60f, y0 + hh * 0.08f, x0 + ww * 0.75f, y0 + hh * 0.16f)
        cubicTo(x0 + ww * 1.0f, y0 + hh * 0.30f, x0 + ww * 0.95f, y0 + hh * 0.62f, x0 + ww * 0.5f, y0 + hh * 0.95f)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(top, bottom), startY = y(cy - h / 2f), endY = y(cy + h / 2f)))
}

private fun DrawScope.y(v: Float): Float = v * size.minDimension

private fun DrawScope.drawStarShape(cx: Float, cy: Float, outer: Float, inner: Float, fill: Brush) {
    val s = size.minDimension
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val px = (cx + radius * cos(angle).toFloat()) * s
        val py = (cy + radius * sin(angle).toFloat()) * s
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, fill)
    drawPath(path, Color(0x33B26A00), style = Stroke(width = 0.015f * s, join = StrokeJoin.Round))
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color) {
    val path = upath {
        m(cx, cy - r)
        q(cx + r * 0.12f, cy - r * 0.12f, cx + r, cy)
        q(cx + r * 0.12f, cy + r * 0.12f, cx, cy + r)
        q(cx - r * 0.12f, cy + r * 0.12f, cx - r, cy)
        q(cx - r * 0.12f, cy - r * 0.12f, cx, cy - r)
        z()
    }
    drawPath(path, color)
}

private fun DrawScope.drawFace(spec: FaceSpec) {
    val s = size.minDimension
    val (c1, c2, c3) = when (spec.base) {
        Base.YELLOW -> Triple(Color(0xFFFFF3A6), Color(0xFFFFCB3A), Color(0xFFEE9F14))
        Base.RED -> Triple(Color(0xFFFF9A8A), Color(0xFFF2503E), Color(0xFFC62828))
        Base.BLUE -> Triple(Color(0xFFD6F1FF), Color(0xFF7FCBF5), Color(0xFF3B8FD1))
        Base.PURPLE -> Triple(Color(0xFFD9A8FF), Color(0xFF9C4DDB), Color(0xFF5E2A9C))
    }

    withTransform({ if (spec.flip) rotate(180f, center) }) {
        drawCircle(
            brush = Brush.radialGradient(listOf(c1, c2, c3), center = p(0.38f, 0.30f), radius = s * 0.78f),
            radius = s * 0.5f,
            center = center
        )
        drawCircle(color = Color(0x22000000), radius = s * 0.495f, center = center, style = Stroke(width = s * 0.012f))
        drawOval(
            brush = Brush.verticalGradient(
                listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)),
                startY = y(0.06f),
                endY = y(0.36f)
            ),
            topLeft = p(0.20f, 0.06f),
            size = sz(0.60f, 0.30f)
        )

        if (spec.blush) {
            drawOval(Color(0x66FF5A5A), p(0.10f, 0.52f), sz(0.20f, 0.11f))
            drawOval(Color(0x66FF5A5A), p(0.70f, 0.52f), sz(0.20f, 0.11f))
        }
        if (spec.tearStream) {
            drawRoundRectBand(0.24f, 0.44f, 0.05f, 0.36f)
            drawRoundRectBand(0.71f, 0.44f, 0.05f, 0.36f)
        }

        drawEyes(spec.eyes, c2)
        drawBrows(spec.brows)
        drawMouth(spec.mouth)

        if (spec.tearsJoy) {
            drawTear(0.12f, 0.50f, 0.055f)
            drawTear(0.88f, 0.50f, 0.055f)
        }
        if (spec.tear) drawTear(0.30f, 0.60f, 0.05f)
        if (spec.sweat) drawTear(0.82f, 0.22f, 0.055f)
    }
}

private fun DrawScope.drawRoundRectBand(x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xAA5EC7FF),
        topLeft = p(x, y),
        size = sz(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * size.minDimension / 2f)
    )
}

private fun DrawScope.drawEyes(type: Eyes, faceColor: Color) {
    val s = size.minDimension
    val xs = listOf(0.34f, 0.66f)
    val stroke = Stroke(width = 0.032f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun dot(x: Float) {
        drawOval(Ink, p(x - 0.045f, 0.35f), sz(0.09f, 0.14f))
        drawCircle(Color.White, radius = 0.014f * s, center = p(x - 0.012f, 0.385f))
    }

    when (type) {
        Eyes.DOT -> xs.forEach { dot(it) }
        Eyes.ARC_UP -> xs.forEach { x ->
            drawArc(Ink, 200f, 140f, false, p(x - 0.075f, 0.36f), sz(0.15f, 0.14f), style = stroke)
        }
        Eyes.ARC_DOWN -> xs.forEach { x ->
            drawArc(Ink, 20f, 140f, false, p(x - 0.075f, 0.40f), sz(0.15f, 0.12f), style = stroke)
        }
        Eyes.LINE -> xs.forEach { x ->
            drawLine(Ink, p(x - 0.07f, 0.43f), p(x + 0.07f, 0.43f), strokeWidth = 0.032f * s, cap = StrokeCap.Round)
        }
        Eyes.SQUINT -> {
            val left = upath { m(0.26f, 0.35f); l(0.38f, 0.42f); l(0.26f, 0.49f) }
            val right = upath { m(0.74f, 0.35f); l(0.62f, 0.42f); l(0.74f, 0.49f) }
            drawPath(left, Ink, style = stroke)
            drawPath(right, Ink, style = stroke)
        }
        Eyes.HEART -> xs.forEach { x ->
            drawHeartShape(x, 0.43f, 0.22f, Color(0xFFFF6B81), Color(0xFFD81B4A))
        }
        Eyes.STAR -> xs.forEach { x ->
            drawStarShape(
                x, 0.42f, 0.115f, 0.05f,
                Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFF9800)), startY = y(0.30f), endY = y(0.54f))
            )
        }
        Eyes.WIDE -> xs.forEach { x ->
            drawCircle(Color.White, radius = 0.085f * s, center = p(x, 0.42f))
            drawCircle(Ink.copy(alpha = 0.55f), radius = 0.085f * s, center = p(x, 0.42f), style = Stroke(width = 0.012f * s))
            drawCircle(Ink, radius = 0.036f * s, center = p(x, 0.43f))
        }
        Eyes.SHADES -> {
            drawRoundRect(Color(0xFF1B1B1F), p(0.18f, 0.35f), sz(0.29f, 0.16f), androidx.compose.ui.geometry.CornerRadius(0.05f * s))
            drawRoundRect(Color(0xFF1B1B1F), p(0.53f, 0.35f), sz(0.29f, 0.16f), androidx.compose.ui.geometry.CornerRadius(0.05f * s))
            drawLine(Color(0xFF1B1B1F), p(0.46f, 0.38f), p(0.54f, 0.38f), strokeWidth = 0.03f * s)
            drawLine(Color(0x55FFFFFF), p(0.22f, 0.38f), p(0.30f, 0.38f), strokeWidth = 0.02f * s, cap = StrokeCap.Round)
            drawLine(Color(0x55FFFFFF), p(0.57f, 0.38f), p(0.65f, 0.38f), strokeWidth = 0.02f * s, cap = StrokeCap.Round)
        }
        Eyes.WINK -> {
            dot(0.34f)
            drawArc(Ink, 200f, 140f, false, p(0.66f - 0.075f, 0.38f), sz(0.15f, 0.12f), style = stroke)
        }
        Eyes.ROLL -> xs.forEach { x ->
            drawOval(Color.White, p(x - 0.07f, 0.33f), sz(0.14f, 0.16f))
            drawCircle(Ink, radius = 0.03f * s, center = p(x, 0.355f))
        }
        Eyes.HALF -> xs.forEach { x ->
            dot(x)
            drawRect(faceColor, p(x - 0.055f, 0.33f), sz(0.11f, 0.07f))
            drawLine(Ink, p(x - 0.055f, 0.40f), p(x + 0.055f, 0.40f), strokeWidth = 0.02f * s, cap = StrokeCap.Round)
        }
        Eyes.PLEAD -> xs.forEach { x ->
            drawOval(Ink, p(x - 0.07f, 0.33f), sz(0.14f, 0.19f))
            drawCircle(Color.White, radius = 0.024f * s, center = p(x - 0.02f, 0.38f))
            drawCircle(Color.White, radius = 0.012f * s, center = p(x + 0.025f, 0.45f))
        }
        Eyes.NONE -> Unit
    }
}

private fun DrawScope.drawBrows(type: Brows) {
    val s = size.minDimension
    val w = 0.035f * s
    when (type) {
        Brows.NONE -> Unit
        Brows.ANGRY -> {
            drawLine(Ink, p(0.22f, 0.27f), p(0.42f, 0.34f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(Ink, p(0.78f, 0.27f), p(0.58f, 0.34f), strokeWidth = w, cap = StrokeCap.Round)
        }
        Brows.RAISED -> {
            drawLine(Ink, p(0.24f, 0.29f), p(0.42f, 0.29f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(Ink, p(0.58f, 0.22f), p(0.76f, 0.19f), strokeWidth = w, cap = StrokeCap.Round)
        }
        Brows.WORRIED -> {
            drawLine(Ink, p(0.22f, 0.31f), p(0.42f, 0.25f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(Ink, p(0.78f, 0.31f), p(0.58f, 0.25f), strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawMouth(type: Mouth) {
    val s = size.minDimension
    val stroke = Stroke(width = 0.032f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun openMouth(bottom: Float, teeth: Boolean) {
        val path = upath {
            m(0.27f, 0.58f)
            l(0.73f, 0.58f)
            c(0.73f, bottom - 0.08f, 0.62f, bottom, 0.5f, bottom)
            c(0.38f, bottom, 0.27f, bottom - 0.08f, 0.27f, 0.58f)
            z()
        }
        drawPath(path, Ink)
        clipPath(path) {
            if (teeth) drawRect(Color.White, p(0.27f, 0.58f), sz(0.46f, 0.07f))
            drawOval(Tongue, p(0.36f, bottom - 0.10f), sz(0.28f, 0.16f))
        }
    }

    when (type) {
        Mouth.GRIN -> openMouth(0.84f, true)
        Mouth.GRIN_BIG -> openMouth(0.90f, true)
        Mouth.GRIN_TEETH -> {
            val rect = androidx.compose.ui.geometry.CornerRadius(0.06f * s)
            drawRoundRect(Color.White, p(0.26f, 0.60f), sz(0.48f, 0.16f), rect)
            drawRoundRect(Ink, p(0.26f, 0.60f), sz(0.48f, 0.16f), rect, style = Stroke(width = 0.026f * s))
            drawLine(Ink, p(0.26f, 0.68f), p(0.74f, 0.68f), strokeWidth = 0.014f * s)
            drawLine(Ink, p(0.5f, 0.60f), p(0.5f, 0.76f), strokeWidth = 0.014f * s)
        }
        Mouth.SMILE -> drawArc(Ink, 25f, 130f, false, p(0.30f, 0.52f), sz(0.40f, 0.28f), style = stroke)
        Mouth.SMILE_SMALL -> drawArc(Ink, 25f, 130f, false, p(0.38f, 0.62f), sz(0.24f, 0.14f), style = stroke)
        Mouth.FROWN -> drawArc(Ink, 215f, 110f, false, p(0.32f, 0.68f), sz(0.36f, 0.24f), style = stroke)
        Mouth.FROWN_SMALL -> drawArc(Ink, 215f, 110f, false, p(0.38f, 0.70f), sz(0.24f, 0.14f), style = stroke)
        Mouth.OPEN_O -> {
            drawOval(Ink, p(0.42f, 0.62f), sz(0.16f, 0.22f))
            drawOval(Tongue, p(0.445f, 0.75f), sz(0.11f, 0.07f))
        }
        Mouth.OPEN_SMALL -> drawOval(Ink, p(0.45f, 0.66f), sz(0.10f, 0.12f))
        Mouth.FLAT -> drawLine(Ink, p(0.36f, 0.70f), p(0.64f, 0.70f), strokeWidth = 0.032f * s, cap = StrokeCap.Round)
        Mouth.FLAT_SLANT -> drawLine(Ink, p(0.36f, 0.73f), p(0.64f, 0.68f), strokeWidth = 0.032f * s, cap = StrokeCap.Round)
        Mouth.SMIRK -> drawPath(upath { m(0.36f, 0.70f); q(0.56f, 0.74f, 0.68f, 0.62f) }, Ink, style = stroke)
        Mouth.KISS -> {
            drawOval(Ink, p(0.46f, 0.66f), sz(0.09f, 0.10f))
            drawOval(Tongue.copy(alpha = 0.85f), p(0.475f, 0.685f), sz(0.05f, 0.05f))
        }
        Mouth.TEETH -> {
            val rect = androidx.compose.ui.geometry.CornerRadius(0.03f * s)
            drawRoundRect(Color.White, p(0.28f, 0.63f), sz(0.44f, 0.14f), rect)
            drawRoundRect(Ink, p(0.28f, 0.63f), sz(0.44f, 0.14f), rect, style = Stroke(width = 0.024f * s))
            for (i in 1..3) {
                val x = 0.28f + 0.11f * i
                drawLine(Ink, p(x, 0.63f), p(x, 0.77f), strokeWidth = 0.012f * s)
            }
        }
        Mouth.NONE -> Unit
    }
}

private fun heartPainter(top: Color, bottom: Color, broken: Boolean = false): EmojiPainter = {
    val s = size.minDimension
    drawHeartShape(0.5f, 0.52f, 0.92f, top, bottom)
    rotate(-35f, p(0.28f, 0.28f)) {
        drawOval(Color(0x66FFFFFF), p(0.16f, 0.21f), sz(0.20f, 0.10f))
    }
    if (broken) {
        val crack = upath {
            m(0.52f, 0.14f); l(0.44f, 0.36f); l(0.58f, 0.46f); l(0.46f, 0.66f); l(0.54f, 0.9f)
        }
        drawPath(crack, Color(0xFFFFFFFF), style = Stroke(width = 0.04f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private val fire: EmojiPainter = {
    val outer = upath {
        m(0.5f, 0.04f)
        c(0.62f, 0.25f, 0.86f, 0.35f, 0.86f, 0.62f)
        c(0.86f, 0.82f, 0.70f, 0.96f, 0.5f, 0.96f)
        c(0.30f, 0.96f, 0.14f, 0.82f, 0.14f, 0.62f)
        c(0.14f, 0.48f, 0.23f, 0.40f, 0.30f, 0.33f)
        c(0.32f, 0.44f, 0.38f, 0.48f, 0.42f, 0.48f)
        c(0.38f, 0.30f, 0.42f, 0.14f, 0.5f, 0.04f)
        z()
    }
    drawPath(outer, Brush.verticalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6D00), Color(0xFFE53935)), startY = y(0.04f), endY = y(0.96f)))
    val inner = upath {
        m(0.5f, 0.46f)
        c(0.60f, 0.58f, 0.68f, 0.64f, 0.68f, 0.77f)
        c(0.68f, 0.89f, 0.60f, 0.94f, 0.5f, 0.94f)
        c(0.40f, 0.94f, 0.32f, 0.89f, 0.32f, 0.77f)
        c(0.32f, 0.66f, 0.42f, 0.60f, 0.5f, 0.46f)
        z()
    }
    drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFCA28)), startY = y(0.46f), endY = y(0.94f)))
}

private fun DrawScope.thumb() {
    val hand = upath {
        m(0.34f, 0.52f)
        l(0.44f, 0.52f)
        c(0.55f, 0.40f, 0.56f, 0.28f, 0.54f, 0.14f)
        c(0.60f, 0.06f, 0.73f, 0.12f, 0.71f, 0.30f)
        l(0.67f, 0.43f)
        l(0.86f, 0.43f)
        c(0.97f, 0.45f, 0.97f, 0.60f, 0.90f, 0.64f)
        c(0.95f, 0.70f, 0.93f, 0.79f, 0.87f, 0.81f)
        c(0.89f, 0.87f, 0.85f, 0.93f, 0.79f, 0.93f)
        l(0.42f, 0.93f)
        c(0.38f, 0.93f, 0.34f, 0.89f, 0.34f, 0.85f)
        z()
    }
    drawPath(hand, Brush.radialGradient(listOf(Color(0xFFFFF3A6), Color(0xFFFFCB3A), Color(0xFFEE9F14)), center = p(0.5f, 0.4f), radius = size.minDimension * 0.7f))
    drawPath(hand, Color(0x22000000), style = Stroke(width = 0.014f * size.minDimension, join = StrokeJoin.Round))
    val cuff = upath { m(0.08f, 0.50f); l(0.30f, 0.50f); l(0.30f, 0.93f); l(0.08f, 0.93f); z() }
    drawPath(cuff, Brush.verticalGradient(listOf(Color(0xFF6FB1F0), Color(0xFF2F6FBF)), startY = y(0.5f), endY = y(0.93f)))
    val s = size.minDimension
    drawLine(Color(0x55FFFFFF), p(0.79f, 0.60f), p(0.90f, 0.60f), strokeWidth = 0.012f * s)
    drawLine(Color(0x33000000), p(0.42f, 0.66f), p(0.86f, 0.66f), strokeWidth = 0.012f * s)
    drawLine(Color(0x33000000), p(0.42f, 0.78f), p(0.84f, 0.78f), strokeWidth = 0.012f * s)
}

private val thumbUp: EmojiPainter = { thumb() }

private val thumbDown: EmojiPainter = {
    withTransform({ scale(1f, -1f, center) }) { thumb() }
}

private val starPainter: EmojiPainter = {
    drawStarShape(
        0.5f, 0.54f, 0.48f, 0.21f,
        Brush.verticalGradient(listOf(Color(0xFFFFF1A8), Color(0xFFFFC107), Color(0xFFFF9800)), startY = y(0.06f), endY = y(0.96f))
    )
}

private val sparkles: EmojiPainter = {
    drawSparkle(0.42f, 0.56f, 0.40f, Color(0xFFFFC107))
    drawSparkle(0.80f, 0.20f, 0.18f, Color(0xFFFFD54F))
    drawSparkle(0.78f, 0.82f, 0.13f, Color(0xFFFFE082))
}

private val snowflake: EmojiPainter = {
    val s = size.minDimension
    val color = Color(0xFF4FC3F7)
    for (i in 0 until 6) {
        val angle = i * PI / 3
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val start = p(0.5f, 0.5f)
        val end = p(0.5f + dx * 0.46f, 0.5f + dy * 0.46f)
        drawLine(color, start, end, strokeWidth = 0.055f * s, cap = StrokeCap.Round)
        val bx = 0.5f + dx * 0.28f
        val by = 0.5f + dy * 0.28f
        for (side in listOf(-1, 1)) {
            val a2 = angle + side * PI / 4
            drawLine(color, p(bx, by), p(bx + cos(a2).toFloat() * 0.13f, by + sin(a2).toFloat() * 0.13f), strokeWidth = 0.04f * s, cap = StrokeCap.Round)
        }
    }
    drawCircle(Color(0xFFB3E5FC), radius = 0.06f * s, center = center)
}

private val party: EmojiPainter = {
    val s = size.minDimension
    val cone = upath { m(0.08f, 0.94f); l(0.32f, 0.30f); l(0.72f, 0.70f); z() }
    drawPath(cone, Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)), start = p(0.08f, 0.94f), end = p(0.72f, 0.40f)))
    drawLine(Color(0xFFFFF3E0), p(0.15f, 0.76f), p(0.42f, 0.55f), strokeWidth = 0.03f * s, cap = StrokeCap.Round)
    drawLine(Color(0xFFFFF3E0), p(0.22f, 0.86f), p(0.55f, 0.66f), strokeWidth = 0.03f * s, cap = StrokeCap.Round)
    drawCircle(Color(0xFFE91E63), 0.04f * s, p(0.60f, 0.18f))
    drawCircle(Color(0xFF29B6F6), 0.035f * s, p(0.84f, 0.34f))
    drawCircle(Color(0xFF66BB6A), 0.035f * s, p(0.46f, 0.08f))
    drawCircle(Color(0xFFFFEB3B), 0.04f * s, p(0.90f, 0.62f))
    drawRect(Color(0xFF7E57C2), p(0.70f, 0.14f), sz(0.06f, 0.10f))
    drawRect(Color(0xFFEF5350), p(0.80f, 0.48f), sz(0.09f, 0.05f))
    drawPath(upath { m(0.50f, 0.36f); q(0.62f, 0.34f, 0.66f, 0.46f) }, Color(0xFFFF7043), style = Stroke(width = 0.025f * s, cap = StrokeCap.Round))
}

private val burst: EmojiPainter = {
    val s = size.minDimension
    val path = Path()
    for (i in 0 until 24) {
        val r = if (i % 2 == 0) 0.48f else 0.27f
        val a = -PI / 2 + i * PI / 12
        val px = (0.5f + r * cos(a).toFloat()) * s
        val py = (0.5f + r * sin(a).toFloat()) * s
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color(0xFFFFF176), Color(0xFFFFA726), Color(0xFFE64A19)), center = center, radius = s * 0.5f))
}

private val bolt: EmojiPainter = {
    val path = upath {
        m(0.60f, 0.02f); l(0.18f, 0.56f); l(0.44f, 0.56f); l(0.36f, 0.98f); l(0.84f, 0.40f); l(0.56f, 0.40f); z()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF9800)), startY = y(0.02f), endY = y(0.98f)))
    drawPath(path, Color(0x33B26A00), style = Stroke(width = 0.015f * size.minDimension, join = StrokeJoin.Round))
}

private val sun: EmojiPainter = {
    val s = size.minDimension
    for (i in 0 until 8) {
        val a = i * PI / 4
        drawLine(
            Color(0xFFFFB300),
            p(0.5f + cos(a).toFloat() * 0.34f, 0.5f + sin(a).toFloat() * 0.34f),
            p(0.5f + cos(a).toFloat() * 0.47f, 0.5f + sin(a).toFloat() * 0.47f),
            strokeWidth = 0.06f * s,
            cap = StrokeCap.Round
        )
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFFF9800)), center = p(0.42f, 0.42f), radius = s * 0.4f), radius = s * 0.28f, center = center)
}


private val checkMark: EmojiPainter = {
    val s = size.minDimension
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF7BE58A), Color(0xFF2E9E45)), startY = y(0.08f), endY = y(0.92f)),
        p(0.08f, 0.08f),
        sz(0.84f, 0.84f),
        androidx.compose.ui.geometry.CornerRadius(0.2f * s)
    )
    drawPath(
        upath { m(0.27f, 0.52f); l(0.44f, 0.68f); l(0.74f, 0.32f) },
        Color.White,
        style = Stroke(width = 0.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private val crossMark: EmojiPainter = {
    val s = size.minDimension
    val brush = Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFD32F2F)), start = p(0.1f, 0.1f), end = p(0.9f, 0.9f))
    drawLine(brush, p(0.16f, 0.16f), p(0.84f, 0.84f), strokeWidth = 0.17f * s, cap = StrokeCap.Round)
    drawLine(brush, p(0.84f, 0.16f), p(0.16f, 0.84f), strokeWidth = 0.17f * s, cap = StrokeCap.Round)
}

private val questionMark: EmojiPainter = {
    val s = size.minDimension
    val red = Brush.verticalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFD32F2F)), startY = y(0.05f), endY = y(0.95f))
    drawPath(
        upath { m(0.28f, 0.28f); c(0.28f, 0.06f, 0.72f, 0.06f, 0.72f, 0.28f); c(0.72f, 0.46f, 0.5f, 0.46f, 0.5f, 0.64f) },
        red,
        style = Stroke(width = 0.14f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawCircle(red, radius = 0.075f * s, center = p(0.5f, 0.86f))
}

private val exclamationMark: EmojiPainter = {
    val s = size.minDimension
    val red = Brush.verticalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFD32F2F)), startY = y(0.05f), endY = y(0.95f))
    drawPath(
        upath { m(0.38f, 0.06f); l(0.62f, 0.06f); l(0.56f, 0.64f); l(0.44f, 0.64f); z() },
        red
    )
    drawCircle(red, radius = 0.08f * s, center = p(0.5f, 0.85f))
}

private val waterDrop: EmojiPainter = {
    drawTear(0.5f, 0.62f, 0.3f)
}

private val gem: EmojiPainter = {
    val s = size.minDimension
    val outline = upath { m(0.22f, 0.12f); l(0.78f, 0.12f); l(0.96f, 0.38f); l(0.5f, 0.92f); l(0.04f, 0.38f); z() }
    drawPath(outline, Brush.linearGradient(listOf(Color(0xFFB3E5FC), Color(0xFF29B6F6), Color(0xFF0277BD)), start = p(0.1f, 0.1f), end = p(0.9f, 0.9f)))
    val facet = Color(0x66FFFFFF)
    drawLine(facet, p(0.04f, 0.38f), p(0.96f, 0.38f), strokeWidth = 0.02f * s)
    drawLine(facet, p(0.22f, 0.12f), p(0.36f, 0.38f), strokeWidth = 0.02f * s)
    drawLine(facet, p(0.78f, 0.12f), p(0.64f, 0.38f), strokeWidth = 0.02f * s)
    drawLine(facet, p(0.36f, 0.38f), p(0.5f, 0.92f), strokeWidth = 0.02f * s)
    drawLine(facet, p(0.64f, 0.38f), p(0.5f, 0.92f), strokeWidth = 0.02f * s)
}

private val crown: EmojiPainter = {
    val s = size.minDimension
    val body = upath { m(0.08f, 0.78f); l(0.04f, 0.28f); l(0.3f, 0.5f); l(0.5f, 0.14f); l(0.7f, 0.5f); l(0.96f, 0.28f); l(0.92f, 0.78f); z() }
    drawPath(body, Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFFC107), Color(0xFFFF8F00)), startY = y(0.14f), endY = y(0.78f)))
    drawRoundRect(Color(0xFFFF8F00), p(0.08f, 0.78f), sz(0.84f, 0.1f), androidx.compose.ui.geometry.CornerRadius(0.03f * s))
    drawCircle(Color(0xFFE53935), 0.045f * s, p(0.5f, 0.6f))
    drawCircle(Color(0xFF1E88E5), 0.035f * s, p(0.27f, 0.66f))
    drawCircle(Color(0xFF43A047), 0.035f * s, p(0.73f, 0.66f))
}

private val balloon: EmojiPainter = {
    val s = size.minDimension
    drawPath(
        upath { m(0.5f, 0.86f); q(0.44f, 0.94f, 0.36f, 0.98f); q(0.5f, 0.92f, 0.5f, 0.86f) },
        Color(0xFFD32F2F)
    )
    drawPath(
        upath { m(0.5f, 0.9f); c(0.42f, 0.94f, 0.58f, 0.96f, 0.5f, 1.0f) },
        Color(0x88000000),
        style = Stroke(width = 0.012f * s)
    )
    drawOval(
        Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFE53935), Color(0xFFB71C1C)), center = p(0.38f, 0.3f), radius = s * 0.6f),
        p(0.16f, 0.04f),
        sz(0.68f, 0.82f)
    )
    drawOval(Color(0x66FFFFFF), p(0.26f, 0.14f), sz(0.14f, 0.2f))
}

private val crescent: EmojiPainter = {
    val s = size.minDimension
    val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0.08f * s, 0.08f * s, 0.92f * s, 0.92f * s)) }
    val inner = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0.34f * s, 0.0f * s, 1.06f * s, 0.78f * s)) }
    val moon = Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, outer, inner)
    drawPath(moon, Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFF9A825)), start = p(0.1f, 0.1f), end = p(0.6f, 0.9f)))
}

private val cloud: EmojiPainter = {
    val s = size.minDimension
    val brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5)), startY = y(0.2f), endY = y(0.85f))
    drawCircle(brush, 0.2f * s, p(0.3f, 0.6f))
    drawCircle(brush, 0.26f * s, p(0.48f, 0.48f))
    drawCircle(brush, 0.2f * s, p(0.7f, 0.58f))
    drawRoundRect(brush, p(0.12f, 0.58f), sz(0.76f, 0.24f), androidx.compose.ui.geometry.CornerRadius(0.12f * s))
}

private fun face(
    eyes: Eyes,
    mouth: Mouth,
    brows: Brows = Brows.NONE,
    base: Base = Base.YELLOW,
    blush: Boolean = false,
    tearsJoy: Boolean = false,
    tearStream: Boolean = false,
    tear: Boolean = false,
    sweat: Boolean = false,
    flip: Boolean = false
): EmojiPainter {
    val spec = FaceSpec(eyes, mouth, brows, base, blush, tearsJoy, tearStream, tear, sweat, flip)
    return { drawFace(spec) }
}

object MayasEmojiArt {

    private val registry: Map<String, EmojiPainter> = buildMap {
        put("😀", face(Eyes.DOT, Mouth.GRIN))
        put("😃", face(Eyes.DOT, Mouth.GRIN_BIG))
        put("😄", face(Eyes.ARC_UP, Mouth.GRIN))
        put("😁", face(Eyes.ARC_UP, Mouth.GRIN_TEETH))
        put("😆", face(Eyes.SQUINT, Mouth.GRIN_BIG))
        put("😅", face(Eyes.SQUINT, Mouth.GRIN, sweat = true))
        put("😂", face(Eyes.ARC_UP, Mouth.GRIN_BIG, tearsJoy = true))
        put("🤣", face(Eyes.SQUINT, Mouth.GRIN_BIG, tearsJoy = true))
        put("😊", face(Eyes.ARC_UP, Mouth.SMILE, blush = true))
        put("🙂", face(Eyes.DOT, Mouth.SMILE))
        put("🙃", face(Eyes.DOT, Mouth.SMILE, flip = true))
        put("😉", face(Eyes.WINK, Mouth.SMILE))
        put("😍", face(Eyes.HEART, Mouth.GRIN))
        put("🥰", face(Eyes.ARC_UP, Mouth.SMILE, blush = true))
        put("😘", face(Eyes.WINK, Mouth.KISS, blush = true))
        put("😎", face(Eyes.SHADES, Mouth.SMILE))
        put("🤩", face(Eyes.STAR, Mouth.GRIN))
        put("🥳", face(Eyes.ARC_UP, Mouth.GRIN_BIG, blush = true))
        put("😏", face(Eyes.HALF, Mouth.SMIRK))
        put("😒", face(Eyes.HALF, Mouth.FLAT_SLANT))
        put("😞", face(Eyes.DOT, Mouth.FROWN_SMALL, Brows.WORRIED))
        put("😔", face(Eyes.ARC_DOWN, Mouth.FROWN_SMALL, Brows.WORRIED))
        put("😢", face(Eyes.DOT, Mouth.FROWN, Brows.WORRIED, tear = true))
        put("😭", face(Eyes.ARC_DOWN, Mouth.GRIN_BIG, Brows.WORRIED, tearStream = true))
        put("🥺", face(Eyes.PLEAD, Mouth.FROWN_SMALL, Brows.WORRIED))
        put("😠", face(Eyes.DOT, Mouth.FROWN, Brows.ANGRY))
        put("😡", face(Eyes.DOT, Mouth.FROWN, Brows.ANGRY, base = Base.RED))
        put("😱", face(Eyes.WIDE, Mouth.OPEN_O, Brows.WORRIED, base = Base.BLUE))
        put("😨", face(Eyes.WIDE, Mouth.OPEN_SMALL, Brows.WORRIED))
        put("😮", face(Eyes.DOT, Mouth.OPEN_O))
        put("😲", face(Eyes.WIDE, Mouth.OPEN_O, Brows.RAISED))
        put("😴", face(Eyes.LINE, Mouth.OPEN_SMALL))
        put("🤔", face(Eyes.DOT, Mouth.FLAT_SLANT, Brows.RAISED))
        put("🤗", face(Eyes.ARC_UP, Mouth.GRIN, blush = true))
        put("😐", face(Eyes.DOT, Mouth.FLAT))
        put("😑", face(Eyes.LINE, Mouth.FLAT))
        put("😶", face(Eyes.DOT, Mouth.NONE))
        put("🙄", face(Eyes.ROLL, Mouth.FLAT))
        put("😬", face(Eyes.DOT, Mouth.TEETH))
        put("😳", face(Eyes.WIDE, Mouth.FLAT, blush = true))
        put("🥵", face(Eyes.HALF, Mouth.OPEN_SMALL, base = Base.RED, sweat = true))
        put("🥶", face(Eyes.WIDE, Mouth.TEETH, base = Base.BLUE))
        put("😈", face(Eyes.DOT, Mouth.SMIRK, Brows.ANGRY, base = Base.PURPLE))
        put("❤", heartPainter(Color(0xFFFF6B6B), Color(0xFFD32F2F)))
        put("🧡", heartPainter(Color(0xFFFFB74D), Color(0xFFF57C00)))
        put("💛", heartPainter(Color(0xFFFFF176), Color(0xFFFFB300)))
        put("💚", heartPainter(Color(0xFF81E28A), Color(0xFF2E9E45)))
        put("💙", heartPainter(Color(0xFF6CB8FF), Color(0xFF1E6FD9)))
        put("💜", heartPainter(Color(0xFFC792F5), Color(0xFF7B2FBF)))
        put("🖤", heartPainter(Color(0xFF5A5A63), Color(0xFF16161A)))
        put("🤍", heartPainter(Color(0xFFFFFFFF), Color(0xFFCFD8DC)))
        put("🤎", heartPainter(Color(0xFFB98A6A), Color(0xFF6D4C41)))
        put("💔", heartPainter(Color(0xFFFF6B6B), Color(0xFFD32F2F), broken = true))
        put("🔥", fire)
        put("👍", thumbUp)
        put("👎", thumbDown)
        put("⭐", starPainter)
        put("🌟", starPainter)
        put("✨", sparkles)
        put("❄", snowflake)
        put("🎉", party)
        put("🎊", party)
        put("💥", burst)
        put("⚡", bolt)
        put("☀", sun)
        put("✅", checkMark)
        put("❌", crossMark)
        put("❓", questionMark)
        put("❗", exclamationMark)
        put("💧", waterDrop)
        put("💎", gem)
        put("👑", crown)
        put("🎈", balloon)
        put("🌙", crescent)
        put("☁", cloud)
        put("😌", face(Eyes.ARC_DOWN, Mouth.SMILE_SMALL, blush = true))
        put("😜", face(Eyes.WINK, Mouth.GRIN))
        put("😋", face(Eyes.DOT, Mouth.GRIN))
        put("😥", face(Eyes.DOT, Mouth.FROWN_SMALL, Brows.WORRIED, sweat = true))
        put("😓", face(Eyes.HALF, Mouth.FROWN_SMALL, Brows.WORRIED, sweat = true))
        put("😩", face(Eyes.SQUINT, Mouth.GRIN_BIG, Brows.WORRIED))
        put("😪", face(Eyes.ARC_DOWN, Mouth.FROWN_SMALL, tear = true))
        put("🤭", face(Eyes.ARC_UP, Mouth.SMILE_SMALL, blush = true))
        put("😇", face(Eyes.ARC_UP, Mouth.SMILE, blush = true))
    }

    private val orderedKeys: List<String> = registry.keys.sortedByDescending { it.length }

    fun normalize(emoji: String): String = emoji.replace("\uFE0F", "")

    fun has(emoji: String): Boolean = registry.containsKey(normalize(emoji))

    fun keys(): List<String> = orderedKeys

    fun painter(emoji: String): (DrawScope.() -> Unit)? = registry[normalize(emoji)]
}
