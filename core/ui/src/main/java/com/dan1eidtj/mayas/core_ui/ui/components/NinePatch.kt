package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt


data class NinePatchInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    companion object {
        fun all(value: Int) = NinePatchInsets(value, value, value, value)
    }
}


fun Modifier.ninePatchBackground(
    image: ImageBitmap,
    insets: NinePatchInsets
): Modifier = this.drawBehind {
    drawNinePatch(image, insets)
}

private fun DrawScope.drawNinePatch(image: ImageBitmap, insets: NinePatchInsets) {
    val srcW = image.width
    val srcH = image.height

    val dstW = size.width.roundToInt()
    val dstH = size.height.roundToInt()








    val maxInsetW = maxOf(insets.left, insets.right, 1)
    val maxInsetH = maxOf(insets.top, insets.bottom, 1)
    val cornerScale = minOf(
        1f,
        (dstW / 2f) / maxInsetW,
        (dstH / 2f) / maxInsetH
    ).coerceAtLeast(0f)

    val insL = (insets.left * cornerScale).roundToInt().coerceAtMost(srcW / 2)
    val insT = (insets.top * cornerScale).roundToInt().coerceAtMost(srcH / 2)
    val insR = (insets.right * cornerScale).roundToInt().coerceAtMost(srcW / 2)
    val insB = (insets.bottom * cornerScale).roundToInt().coerceAtMost(srcH / 2)

    val srcMidW = (srcW - insL - insR).coerceAtLeast(0)
    val srcMidH = (srcH - insT - insB).coerceAtLeast(0)
    val dstMidW = (dstW - insL - insR).coerceAtLeast(0)
    val dstMidH = (dstH - insT - insB).coerceAtLeast(0)

    fun slice(
        srcX: Int, srcY: Int, srcW2: Int, srcH2: Int,
        dstX: Int, dstY: Int, dstW2: Int, dstH2: Int
    ) {
        if (srcW2 <= 0 || srcH2 <= 0 || dstW2 <= 0 || dstH2 <= 0) return
        drawImage(
            image = image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW2, srcH2),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(dstW2, dstH2)
        )
    }


    slice(0, 0, insL, insT, 0, 0, insL, insT)
    slice(srcW - insR, 0, insR, insT, dstW - insR, 0, insR, insT)
    slice(0, srcH - insB, insL, insB, 0, dstH - insB, insL, insB)
    slice(srcW - insR, srcH - insB, insR, insB, dstW - insR, dstH - insB, insR, insB)


    slice(insL, 0, srcMidW, insT, insL, 0, dstMidW, insT)
    slice(insL, srcH - insB, srcMidW, insB, insL, dstH - insB, dstMidW, insB)


    slice(0, insT, insL, srcMidH, 0, insT, insL, dstMidH)
    slice(srcW - insR, insT, insR, srcMidH, dstW - insR, insT, insR, dstMidH)


    slice(insL, insT, srcMidW, srcMidH, insL, insT, dstMidW, dstMidH)
}

fun Modifier.customFrameTail(
    isMe: Boolean,
    show: Boolean,
    accentColor: Color,
    fillColor: Color = Color.Black,
    strokeWidthDp: Dp = 6.dp
): Modifier = this.drawBehind {
    if (!show) return@drawBehind

    val strokeWidth = strokeWidthDp.toPx()
    val tailWidth = 26.dp.toPx()
    val tailDrop = 16.dp.toPx()
    val sideMargin = 22.dp.toPx()

    val startX = if (isMe) size.width - tailWidth - sideMargin else sideMargin

    val path = Path().apply {
        moveTo(startX, size.height - strokeWidth / 2)
        lineTo(startX + tailWidth, size.height - strokeWidth / 2)
        lineTo(startX + tailWidth * 0.32f, size.height + tailDrop)
        close()
    }

    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = accentColor, style = Stroke(width = strokeWidth))
}