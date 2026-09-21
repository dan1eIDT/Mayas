/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun MessageActionsOverlay(
    bounds: Rect?,
    isMine: Boolean,
    surfaceColor: Color,
    accent: Color,
    selectedReactions: List<String>,
    topInsetPx: Float,
    bottomInsetPx: Float,
    onReaction: (String) -> Unit,
    onDismiss: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    val appear by animateFloatAsState(if (visible) 1f else 0f, tween(180), label = "messageOverlayAppear")

    val gap = with(density) { 8.dp.toPx() }
    val margin = with(density) { 8.dp.toPx() }
    val screenW = rootSize.width.toFloat()
    val screenH = rootSize.height.toFloat()
    val ready = rootSize != IntSize.Zero && barSize != IntSize.Zero && menuSize != IntSize.Zero
    val rect = bounds ?: Rect(screenW * 0.2f, screenH * 0.4f, screenW * 0.8f, screenH * 0.5f)

    val barMaxWidth = min(with(density) { 320.dp.toPx() }, max(screenW - 2 * margin, 0f))
    val menuWidth = min(with(density) { 248.dp.toPx() }, max(screenW - 2 * margin, 0f))
    val barH = barSize.height.toFloat()
    val menuH = menuSize.height.toFloat()

    val top = topInsetPx + margin
    val bottom = screenH - bottomInsetPx - margin

    val barAbove = rect.top - gap - barH >= top
    val barY = if (barAbove) rect.top - gap - barH else rect.bottom + gap
    val belowStart = (if (barAbove) rect.bottom else barY + barH) + gap
    val aboveEnd = (if (barAbove) barY else rect.top) - gap
    val availBelow = max(bottom - belowStart, 0f)
    val availAbove = max(aboveEnd - top, 0f)
    val placeBelow = availBelow >= menuH || availBelow >= availAbove
    val menuLimit = max(availBelow, availAbove)
    val menuUsed = min(menuH, if (placeBelow) availBelow else availAbove)
    val menuY = if (placeBelow) belowStart else aboveEnd - menuUsed

    fun alignedX(width: Float): Float {
        val raw = if (isMine) rect.right - width else rect.left
        return raw.coerceIn(margin, max(screenW - margin - width, margin))
    }

    val maxMenuHeight: Dp = with(density) { max(menuLimit, 120f).toDp() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rootSize = it }
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(Color.Black.copy(alpha = 0.55f * appear))
                if (bounds != null) {
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(bounds.left, bounds.top),
                        size = Size(bounds.width, bounds.height),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            val alpha = if (ready) appear else 0f

            Box(
                modifier = Modifier
                    .offset { IntOffset(alignedX(barMaxWidth).roundToInt(), barY.roundToInt()) }
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = 0.9f + 0.1f * appear
                        scaleY = 0.9f + 0.1f * appear
                        transformOrigin = TransformOrigin(if (isMine) 1f else 0f, if (barAbove) 1f else 0f)
                    }
                    .width(with(density) { barMaxWidth.toDp() })
                    .onSizeChanged { barSize = it }
                    .shadow(10.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(surfaceColor)
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                ReactionPickerPanel(
                    selected = selectedReactions,
                    accent = accent,
                    onPick = onReaction
                )
            }

            Column(
                modifier = Modifier
                    .offset { IntOffset(alignedX(menuWidth).roundToInt(), menuY.roundToInt()) }
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = 0.92f + 0.08f * appear
                        scaleY = 0.92f + 0.08f * appear
                        transformOrigin = TransformOrigin(if (isMine) 1f else 0f, if (placeBelow) 0f else 1f)
                    }
                    .width(with(density) { menuWidth.toDp() })
                    .heightIn(max = maxMenuHeight)
                    .onSizeChanged { menuSize = it }
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                actions()
            }
        }
    }
}
