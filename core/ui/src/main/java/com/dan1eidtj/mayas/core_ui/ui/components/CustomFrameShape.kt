package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

sealed class CornerStyle(val radius: Dp = 0.dp) {
    data class Rounded(val r: Dp) : CornerStyle(r)
    data class Cut(val r: Dp) : CornerStyle(r)
    object Sharp : CornerStyle(0.dp)
}

class CustomFrameShape(
    private val topStart: CornerStyle = CornerStyle.Sharp,
    private val topEnd: CornerStyle = CornerStyle.Sharp,
    private val bottomEnd: CornerStyle = CornerStyle.Sharp,
    private val bottomStart: CornerStyle = CornerStyle.Sharp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height

        val rTS = with(density) { topStart.radius.toPx() }.coerceAtMost(minOf(w, h) / 2)
        val rTE = with(density) { topEnd.radius.toPx() }.coerceAtMost(minOf(w, h) / 2)
        val rBE = with(density) { bottomEnd.radius.toPx() }.coerceAtMost(minOf(w, h) / 2)
        val rBS = with(density) { bottomStart.radius.toPx() }.coerceAtMost(minOf(w, h) / 2)

        val path = Path().apply {

            moveTo(rTS.forStart(topStart), 0f)


            lineTo(w - rTE.forStart(topEnd), 0f)
            addCorner(this, topEnd, x = w, y = 0f, cx = -1f, cy = 1f, r = rTE)


            lineTo(w, h - rBE.forStart(bottomEnd))
            addCorner(this, bottomEnd, x = w, y = h, cx = -1f, cy = -1f, r = rBE)


            lineTo(rBS.forStart(bottomStart), h)
            addCorner(this, bottomStart, x = 0f, y = h, cx = 1f, cy = -1f, r = rBS)


            lineTo(0f, rTS.forStart(topStart))
            addCorner(this, topStart, x = 0f, y = 0f, cx = 1f, cy = 1f, r = rTS)

            close()
        }

        return Outline.Generic(path)
    }


    private fun Float.forStart(style: CornerStyle): Float = if (style is CornerStyle.Sharp) 0f else this


    private fun addCorner(
        path: Path,
        style: CornerStyle,
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
        r: Float
    ) {
        when (style) {
            is CornerStyle.Rounded -> {
                path.quadraticTo(x, y, x + r * cx, y + r * cy)
            }
            is CornerStyle.Cut -> {



                path.lineTo(x + r * cx, y)
                path.lineTo(x, y + r * cy)
            }
            CornerStyle.Sharp -> {
                path.lineTo(x, y)
            }
        }
    }
}
