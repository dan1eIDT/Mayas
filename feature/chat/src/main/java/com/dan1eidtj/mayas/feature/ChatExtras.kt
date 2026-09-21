/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyph
import com.dan1eidtj.mayas.storage.MediaFileCache
import kotlinx.coroutines.delay
import java.io.File

private class AttachmentAction(
    val icon: ImageVector,
    val label: Int,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    containerColor: Color,
    contentColor: Color,
    onPickGallery: () -> Unit,
    onRecordVideoMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actions = listOf(
        AttachmentAction(Icons.Filled.PhotoLibrary, R.string.attach_gallery, Color(0xFF3D9BE9), onPickGallery),
        AttachmentAction(Icons.Filled.Videocam, R.string.attach_video_message, Color(0xFFFF6B6B), onRecordVideoMessage)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.attach_title),
                color = contentColor,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    AttachmentActionItem(action, contentColor, index)
                }
            }
        }
    }
}

@Composable
private fun AttachmentActionItem(action: AttachmentAction, contentColor: Color, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60L * index)
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "attachmentItemScale"
    )
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(160), label = "attachmentItemAlpha")

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = action.onClick)
            .padding(8.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(action.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(action.label),
            color = contentColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChatEmptyState(
    textColor: Color,
    hintColor: Color,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "chatEmptyAlpha")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.85f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chatEmptyScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EmojiGlyph("\uD83D\uDC4B", fontSize = 48.sp)
        Text(
            text = stringResource(R.string.chat_empty_title),
            color = textColor,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.chat_empty_subtitle),
            color = hintColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoDialog(
    mediaKey: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var file by remember(mediaKey) { mutableStateOf<File?>(MediaFileCache.cachedFile(context, mediaKey)) }
    var failed by remember(mediaKey) { mutableStateOf(false) }

    LaunchedEffect(mediaKey) {
        if (file == null) {
            val obtained = runCatching { MediaFileCache.obtain(context, mediaKey) }.getOrNull()
            if (obtained == null) failed = true else file = obtained
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val ready = file
            when {
                ready != null -> VideoPlayerSurface(ready, Modifier.fillMaxSize())
                failed -> Text(
                    text = stringResource(R.string.error_load_media),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
                else -> CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.video_viewer_close),
                    tint = Color.White
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerSurface(file: File, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                this.player = player
            }
        },
        update = { view -> if (view.player !== player) view.player = player },
        modifier = modifier
    )
}
