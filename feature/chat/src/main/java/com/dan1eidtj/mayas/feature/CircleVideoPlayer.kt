/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.graphics.Bitmap
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.storage.MediaCachePrefs
import com.dan1eidtj.mayas.storage.MediaFileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object CirclePlayback {
    var activeKey by mutableStateOf<String?>(null)
}

private object CircleThumbCache {
    private val cache = android.util.LruCache<String, Bitmap>(24)

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private fun formatCircleDuration(seconds: Int): String =
    String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

private fun extractFirstFrame(file: File): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val maxSide = 420
        if (frame.width > maxSide || frame.height > maxSide) {
            val ratio = maxSide.toFloat() / maxOf(frame.width, frame.height)
            val scaled = Bitmap.createScaledBitmap(
                frame,
                (frame.width * ratio).toInt().coerceAtLeast(1),
                (frame.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== frame) frame.recycle()
            scaled
        } else {
            frame
        }
    } catch (e: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CircleVideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    paused: Boolean = false,
    onEnded: () -> Unit = {},
    onProgress: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = autoPlay
            prepare()
        }
    }

    LaunchedEffect(paused) {
        exoPlayer.playWhenReady = !paused
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                onProgress((exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            }
            delay(80)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        update = { view -> if (view.player !== exoPlayer) view.player = exoPlayer },
        modifier = modifier
    )
}

object CirclePlayedStore {
    private const val FILE = "mayas_circle_played"
    private const val KEY = "ids"
    private const val LIMIT = 500

    fun isPlayed(context: Context, id: String): Boolean =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            ?.contains(id) == true

    fun markPlayed(context: Context, id: String) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val current = LinkedHashSet(prefs.getStringSet(KEY, emptySet()) ?: emptySet())
        if (current.contains(id)) return
        current.add(id)
        while (current.size > LIMIT) {
            current.remove(current.first())
        }
        prefs.edit().putStringSet(KEY, current).apply()
    }
}

private fun formatClock(seconds: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

@Composable
private fun CirclePill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
fun CircleVideoBubble(
    mediaKey: String,
    durationSec: Int,
    modifier: Modifier = Modifier,
    messageId: String = mediaKey,
    isMine: Boolean = false,
    timeText: String? = null,
    status: Int? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val diameter = (LocalConfiguration.current.screenWidthDp * 0.68f).coerceIn(180f, 260f).dp

    var localFile by remember(mediaKey) { mutableStateOf(MediaFileCache.cachedFile(context, mediaKey)) }
    var loading by remember(mediaKey) { mutableStateOf(false) }
    var loadFailed by remember(mediaKey) { mutableStateOf(false) }
    var attempt by remember(mediaKey) { mutableIntStateOf(0) }
    var manual by remember(mediaKey) { mutableStateOf(false) }
    var blocked by remember(mediaKey) { mutableStateOf(false) }
    var paused by remember(mediaKey) { mutableStateOf(false) }
    var progress by remember(mediaKey) { mutableFloatStateOf(0f) }
    var played by remember(messageId) { mutableStateOf(isMine || CirclePlayedStore.isPlayed(context, messageId)) }

    LaunchedEffect(mediaKey, attempt, manual) {
        if (localFile == null) {
            loading = true
            loadFailed = false
            blocked = false
            val file = runCatching { MediaFileCache.obtain(context, mediaKey, auto = !manual) }.getOrNull()
            loading = false
            when {
                file != null -> localFile = file
                !manual && MediaCachePrefs.autoDownloadBlocked(context) -> blocked = true
                else -> loadFailed = true
            }
        }
    }

    val thumbnail by produceState<ImageBitmap?>(initialValue = null, localFile) {
        val file = localFile
        value = if (file == null) {
            null
        } else {
            val cached = CircleThumbCache.get(file.absolutePath)
            if (cached != null) {
                cached.asImageBitmap()
            } else {
                withContext(Dispatchers.IO) {
                    extractFirstFrame(file)?.also { CircleThumbCache.put(file.absolutePath, it) }?.asImageBitmap()
                }
            }
        }
    }

    val isActive = CirclePlayback.activeKey == mediaKey

    DisposableEffect(lifecycleOwner, mediaKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && CirclePlayback.activeKey == mediaKey) {
                CirclePlayback.activeKey = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (CirclePlayback.activeKey == mediaKey) CirclePlayback.activeKey = null
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            if (!played) {
                CirclePlayedStore.markPlayed(context, messageId)
                played = true
            }
        } else {
            paused = false
            progress = 0f
        }
    }

    val playDescription = stringResource(R.string.circle_bubble_play)
    val retryDescription = stringResource(R.string.circle_bubble_retry)
    val mutedDescription = stringResource(R.string.circle_bubble_muted)
    val animatedProgress by animateFloatAsState(progress, tween(90), label = "circleBubbleProgress")
    val soundOn = isActive && !paused
    val shownSeconds = if (isActive) (animatedProgress * durationSec).toInt().coerceAtMost(durationSec) else durationSec

    Box(
        modifier = modifier.size(diameter + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val file = localFile
                    when {
                        file != null -> {
                            if (!isActive) {
                                paused = false
                                CirclePlayback.activeKey = mediaKey
                            } else {
                                paused = !paused
                            }
                        }
                        blocked -> manual = true
                        loadFailed -> attempt++
                        else -> Unit
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val file = localFile
            val frame = thumbnail

            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isActive && file != null) {
                CircleVideoPlayer(
                    uri = Uri.fromFile(file),
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    paused = paused,
                    onEnded = {
                        if (CirclePlayback.activeKey == mediaKey) CirclePlayback.activeKey = null
                    },
                    onProgress = { progress = it }
                )
            }

            when {
                loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                blocked && file == null -> Icon(
                    Icons.Default.Download,
                    contentDescription = retryDescription,
                    tint = Color.White,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(10.dp)
                )
                loadFailed && file == null -> Icon(
                    Icons.Default.Refresh,
                    contentDescription = retryDescription,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                file != null && isActive && paused -> Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = playDescription,
                    tint = Color.White,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(8.dp)
                )
            }
        }

        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val inset = stroke / 2f
                drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        if (!soundOn && localFile != null) {
            CirclePill(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = mutedDescription,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        CirclePill(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = formatClock(shownSeconds),
                color = Color.White,
                fontSize = 12.sp
            )
            if (!played) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        if (timeText != null) {
            CirclePill(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 12.sp
                )
                if (status != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = when (status) {
                            0 -> Icons.Default.AccessTime
                            2 -> Icons.Default.DoneAll
                            else -> Icons.Default.Done
                        },
                        contentDescription = null,
                        tint = if (status == 2) Color(0xFF64B5F6) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
