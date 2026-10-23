/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

@Stable
class CollapsingHeaderState {
    var offsetPx by mutableFloatStateOf(0f)
    var heightPx: Float = 0f
    var enabled: Boolean = true

    val fraction: Float
        get() = if (heightPx <= 0f) 0f else (-offsetPx / heightPx).coerceIn(0f, 1f)

    fun reset() {
        offsetPx = 0f
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!enabled) {
                if (offsetPx != 0f) offsetPx = 0f
                return Offset.Zero
            }
            val delta = available.y
            if (delta == 0f || heightPx <= 0f) return Offset.Zero
            val next = (offsetPx + delta).coerceIn(-heightPx, 0f)
            offsetPx = next
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    suspend fun settle() {
        if (heightPx <= 0f) return
        val target = if (-offsetPx > heightPx * 0.45f) -heightPx else 0f
        if (offsetPx == target) return
        animate(
            initialValue = offsetPx,
            targetValue = target,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) { value, _ -> offsetPx = value }
    }
}

@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState = remember { CollapsingHeaderState() }

fun Modifier.collapsingHeader(state: CollapsingHeaderState): Modifier = this
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.heightPx = placeable.height.toFloat()
        val offset = state.offsetPx.coerceIn(-placeable.height.toFloat(), 0f)
        val visibleHeight = (placeable.height + offset).roundToInt().coerceAtLeast(0)
        layout(placeable.width, visibleHeight) {
            placeable.placeRelative(0, offset.roundToInt())
        }
    }
    .graphicsLayer { alpha = 1f - state.fraction * 0.6f }
