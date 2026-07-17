package com.dan1eidtj.mayas.core_ui.ui.components

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Полноэкранный просмотрщик изображения (аватар или медиа из чата).
 *
 * Возможности:
 *  - pinch-to-zoom и перетаскивание увеличенного изображения;
 *  - двойной тап — быстрый зум/возврат;
 *  - тап по фону или свайп вниз — закрытие;
 *  - опциональные кнопки "Поделиться" и "Скачать".
 *
 * Использование:
 *   var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
 *   ...
 *   Modifier.clickable { fullScreenImageUrl = url }   // на аватарке/картинке
 *   ...
 *   fullScreenImageUrl?.let { url ->
 *       FullScreenImageViewer(imageUrl = url, onDismiss = { fullScreenImageUrl = null })
 *   }
 */
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    onDownload: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var panOffset by remember { mutableStateOf(Offset.Zero) }
        var dragY by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val dismissThresholdPx = 250f
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
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapPos ->
                                if (scale > 1f) {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                            onTap = { onDismiss() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            panOffset = if (newScale <= 1f) Offset.Zero else panOffset + pan
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
                var isLoading by remember { mutableStateOf(true) }
                var isError by remember { mutableStateOf(false) }

                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
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
                    Text("Не удалось загрузить изображение", color = Color.White.copy(alpha = 0.8f))
                }
            }

            // Верхняя панель: закрыть / поделиться / скачать
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
