package com.dan1eidtj.mayas.core_ui.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    onDownload: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
        var panOffset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
        var dragY by remember(imageUrl) { mutableFloatStateOf(0f) }
        var isDragging by remember(imageUrl) { mutableStateOf(false) }
        var reloadKey by remember(imageUrl) { mutableIntStateOf(0) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        val dismissThresholdPx = with(density) { 100.dp.toPx() }
        val dragProgress = (kotlin.math.abs(dragY) / dismissThresholdPx).coerceIn(0f, 1f)

        val animatedDragY by animateFloatAsState(targetValue = dragY, label = "dragY")
        val backgroundAlpha by animateFloatAsState(
            targetValue = 1f - dragProgress * 0.7f,
            label = "bgAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapPos ->
                                if (scale > 1f) {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    val newScale = 2.5f
                                    scale = newScale

                                    val centerX = containerSize.width / 2f
                                    val centerY = containerSize.height / 2f
                                    val rawOffset = Offset(
                                        (centerX - tapPos.x) * (newScale - 1f),
                                        (centerY - tapPos.y) * (newScale - 1f)
                                    )
                                    val maxX = containerSize.width * (newScale - 1f) / 2f
                                    val maxY = containerSize.height * (newScale - 1f) / 2f
                                    panOffset = Offset(
                                        rawOffset.x.coerceIn(-maxX, maxX),
                                        rawOffset.y.coerceIn(-maxY, maxY)
                                    )
                                }
                            },
                            onTap = { if (scale <= 1f) onDismiss() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            panOffset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                val maxX = containerSize.width * (newScale - 1f) / 2f
                                val maxY = containerSize.height * (newScale - 1f) / 2f
                                val candidate = panOffset + pan
                                Offset(
                                    candidate.x.coerceIn(-maxX, maxX),
                                    candidate.y.coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    }
                    .pointerInput(scale) {
                        if (scale <= 1f) {
                            detectVerticalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    if (kotlin.math.abs(dragY) > dismissThresholdPx) {
                                        onDismiss()
                                    } else {
                                        dragY = 0f
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragY = 0f
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    dragY += amount
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                var isLoading by remember(imageUrl, reloadKey) { mutableStateOf(true) }
                var isError by remember(imageUrl, reloadKey) { mutableStateOf(false) }

                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .setParameter("reload", reloadKey)
                        .build(),
                    onState = { state ->
                        isLoading = state is AsyncImagePainter.State.Loading
                        isError = state is AsyncImagePainter.State.Error
                    }
                )

                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val settle = 1f - dragProgress * 0.15f
                            scaleX = scale * settle
                            scaleY = scale * settle
                            translationX = panOffset.x
                            translationY = panOffset.y + if (isDragging) dragY else animatedDragY
                        }
                )

                if (isLoading) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f))
                }
                if (isError) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Не удалось загрузить изображение",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            IconButton(onClick = { reloadKey++ }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Повторить",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }

            if (onShare != null || onDownload != null) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)
                ) {
                    if (onShare != null) {
                        IconButton(onClick = { onShare(imageUrl) }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = Color.White)
                        }
                    }
                    if (onDownload != null) {
                        IconButton(onClick = { onDownload(imageUrl) }) {
                            Icon(Icons.Default.Download, contentDescription = "Скачать", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}