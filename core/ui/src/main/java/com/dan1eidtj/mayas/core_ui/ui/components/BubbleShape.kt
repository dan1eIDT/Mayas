package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class BubbleType {
    Incoming, Outgoing
}

class BubbleShape(
    private val type: BubbleType,
    private val cornerRadius: Dp = 12.dp,
    private val drawTail: Boolean = true,
    private val tailWidth: Dp = 12.dp,
    private val tailOffset: Dp = 16.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = with(density) {
        val r = cornerRadius.toPx()
        val tW = tailWidth.toPx()
        val tOff = tailOffset.toPx()

        val path = Path().apply {
            if (drawTail) {
                when (type) {
                    BubbleType.Incoming -> {
                        moveTo(tW + r, 0f)
                        lineTo(size.width - r, 0f)
                        quadraticBezierTo(size.width, 0f, size.width, r)
                        lineTo(size.width, size.height - r)
                        quadraticBezierTo(size.width, size.height, size.width - r, size.height)
                        lineTo(tOff + r, size.height)
                        quadraticBezierTo(tOff, size.height, 0f, size.height)
                        quadraticBezierTo(tW, size.height, tW, size.height - tW)
                        lineTo(tW, r)
                        quadraticBezierTo(tW, 0f, tW + r, 0f)
                    }
                    BubbleType.Outgoing -> {
                        moveTo(r, 0f)
                        lineTo(size.width - r - tW, 0f)
                        quadraticBezierTo(size.width - tW, 0f, size.width - tW, r)
                        lineTo(size.width - tW, size.height - tW)
                        quadraticBezierTo(size.width - tW, size.height, size.width, size.height)
                        quadraticBezierTo(
                            size.width - tOff,
                            size.height,
                            size.width - tOff - r,
                            size.height
                        )
                        lineTo(r, size.height)
                        quadraticBezierTo(0f, size.height, 0f, size.height - r)
                        lineTo(0f, r)
                        quadraticBezierTo(0f, 0f, r, 0f)
                    }
                }
            } else {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
            }
            close()
        }

        Outline.Generic(path)
    }
}