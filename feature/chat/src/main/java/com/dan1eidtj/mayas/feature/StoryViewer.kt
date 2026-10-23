/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.storage.MediaFileCache
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private val QuickReactions = listOf("❤️", "🔥", "😂", "😮", "😢", "👍")
private const val IMAGE_DURATION_MS = 5000L

@Composable
fun StoryViewerDialog(
    groups: List<StoryGroup>,
    startIndex: Int,
    myUid: String,
    myName: String,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    if (groups.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, groups.lastIndex)) { groups.size }
        val scope = rememberCoroutineScope()
        val drag = remember { Animatable(0f) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - abs(drag.value) / 1400f).coerceIn(0.2f, 1f)))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (drag.value > 220f) onDismiss() else drag.animateTo(0f, tween(180))
                            }
                        },
                        onDragCancel = { scope.launch { drag.animateTo(0f, tween(180)) } },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = drag.value
                        val s = 1f - (drag.value / 2400f).coerceIn(0f, 0.12f)
                        scaleX = s
                        scaleY = s
                    },
                key = { groups[it].owner.uid }
            ) { page ->
                val offset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            cameraDistance = 12f * density.density
                            transformOrigin = TransformOrigin(if (offset > 0f) 0f else 1f, 0.5f)
                            rotationY = -offset * 55f
                            alpha = 1f - abs(offset).coerceIn(0f, 1f) * 0.35f
                        }
                ) {
                    StoryGroupPage(
                        group = groups[page],
                        isCurrentPage = pagerState.currentPage == page && !pagerState.isScrollInProgress,
                        externalPause = drag.value != 0f,
                        myUid = myUid,
                        myName = myName,
                        onFinishGroup = {
                            if (page < groups.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            } else {
                                onDismiss()
                            }
                        },
                        onPrevGroup = {
                            if (page > 0) scope.launch { pagerState.animateScrollToPage(page - 1) }
                        },
                        onDismiss = onDismiss,
                        onOpenProfile = onOpenProfile
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryGroupPage(
    group: StoryGroup,
    isCurrentPage: Boolean,
    externalPause: Boolean,
    myUid: String,
    myName: String,
    onFinishGroup: () -> Unit,
    onPrevGroup: () -> Unit,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var list by remember(group.owner.uid) { mutableStateOf(group.stories) }
    var index by remember(group.owner.uid) {
        mutableIntStateOf(group.stories.indexOfFirst { it.id !in StorySeenState.seen }.let { if (it < 0) 0 else it })
    }
    val safeIndex = index.coerceIn(0, max(list.lastIndex, 0))
    val story = list.getOrNull(safeIndex)

    var holdPaused by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var showViewers by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var replyText by remember(story?.id) { mutableStateOf("") }
    var floatingEmoji by remember { mutableStateOf<String?>(null) }

    val paused = holdPaused || inputFocused || showReactions || showViewers || showDelete || externalPause || !isCurrentPage
    val pausedNow by rememberUpdatedState(paused)

    var mediaReady by remember(story?.id) { mutableStateOf(false) }
    var videoDuration by remember(story?.id) { mutableLongStateOf(0L) }
    var progress by remember(story?.id) { mutableFloatStateOf(0f) }
    var viewerCount by remember(story?.id) { mutableIntStateOf(0) }

    val nextAction: () -> Unit by rememberUpdatedState(newValue = {
        if (safeIndex < list.lastIndex) index = safeIndex + 1 else onFinishGroup()
    })
    val prevAction: () -> Unit by rememberUpdatedState(newValue = {
        if (safeIndex > 0) index = safeIndex - 1 else onPrevGroup()
    })

    if (story == null) {
        LaunchedEffect(Unit) { onFinishGroup() }
        return
    }

    LaunchedEffect(story.id, isCurrentPage) {
        if (!isCurrentPage) return@LaunchedEffect
        StorySeenState.markSeen(context, story.id)
        StoryRepository.markViewed(story, myUid)
        if (group.isMine) viewerCount = StoryRepository.loadViewers(story.id).size
    }

    LaunchedEffect(story.id, isCurrentPage) {
        if (!isCurrentPage) return@LaunchedEffect
        progress = 0f
        var last = withFrameMillis { it }
        while (progress < 1f) {
            val now = withFrameMillis { it }
            val dt = now - last
            last = now
            if (!pausedNow && mediaReady) {
                val total = if (story.isVideo) max(videoDuration, 1000L) else IMAGE_DURATION_MS
                progress = (progress + dt.toFloat() / total).coerceAtMost(1f)
            }
        }
        nextAction()
    }

    val replyLabel = stringResource(R.string.story_reply_label)
    val sentText = stringResource(R.string.story_reply_sent)

    Box(modifier = Modifier.fillMaxSize()) {
        val model by produceState<String?>(initialValue = MediaFileCache.cachedModel(context, story.mediaKey), story.mediaKey) {
            if (value == null) value = runCatching { MediaFileCache.resolveModel(context, story.mediaKey) }.getOrNull()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(story.id) {
                    detectTapGestures(
                        onPress = {
                            holdPaused = true
                            tryAwaitRelease()
                            holdPaused = false
                        },
                        onLongPress = {},
                        onTap = { offset ->
                            if (inputFocused) {
                                focusManager.clearFocus()
                            } else if (showReactions) {
                                showReactions = false
                            } else if (offset.x < size.width * 0.33f) {
                                prevAction()
                            } else {
                                nextAction()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val loaded = model
            when {
                loaded == null -> {
                    LaunchedEffect(story.id, model) {
                        kotlinx.coroutines.delay(6000L)
                        if (model == null) mediaReady = true
                    }
                    Text(
                        stringResource(R.string.story_media_failed),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                story.isVideo -> {
                    if (isCurrentPage) {
                        StoryVideo(
                            model = loaded,
                            paused = paused,
                            onReady = { mediaReady = true },
                            onDuration = { videoDuration = it }
                        )
                    }
                }
                else -> AsyncImage(
                    model = loaded,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { mediaReady = true },
                    onError = { mediaReady = true }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                list.forEachIndexed { i, _ ->
                    val fill = when {
                        i < safeIndex -> 1f
                        i == safeIndex -> progress
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    scaleX = fill
                                }
                                .background(Color.White)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile(group.owner.uid) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarView(
                        avatarUrl = group.owner.avatarUrl,
                        useCustomAvatar = group.owner.useCustomAvatar,
                        profileIcon = group.owner.profileIcon,
                        profileGlow = group.owner.profileGlow,
                        isPremium = group.owner.isPremium,
                        frameType = group.owner.avatarFrame,
                        size = 36.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        group.owner.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(storyTimeAgo(story.createdAt), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                if (group.isMine) {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.story_delete_title), tint = Color.White)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.story_close), tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (story.caption.isNotBlank()) {
                Text(
                    story.caption,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )
            }
            if (group.isMine) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { showViewers = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.story_viewers_count, viewerCount), color = Color.White, fontSize = 14.sp)
                }
            } else {
                if (showReactions) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickReactions.forEach { emoji ->
                            Text(
                                emoji,
                                fontSize = 30.sp,
                                modifier = Modifier.clickable {
                                    showReactions = false
                                    floatingEmoji = emoji
                                    StoryRepository.react(story, myUid, emoji)
                                    scope.launch {
                                        val result = runCatching {
                                            StoryRepository.sendReply(story, myUid, myName, emoji, replyLabel)
                                        }
                                        if (result.isFailure) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.story_reply_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        if (replyText.isEmpty()) {
                            Text(stringResource(R.string.story_reply_hint), color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it.take(1000) },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { inputFocused = it.isFocused }
                        )
                    }
                    if (replyText.isNotBlank()) {
                        IconButton(onClick = {
                            val text = replyText
                            replyText = ""
                            focusManager.clearFocus()
                            scope.launch {
                                val result = runCatching {
                                    StoryRepository.sendReply(story, myUid, myName, text, replyLabel)
                                }
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) sentText else context.getString(R.string.story_reply_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.story_send), tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { showReactions = !showReactions }) {
                            Icon(Icons.Default.FavoriteBorder, stringResource(R.string.story_react), tint = Color.White)
                        }
                    }
                }
            }
        }

        floatingEmoji?.let { emoji ->
            val scale = remember(emoji) { Animatable(0.4f) }
            val alpha = remember(emoji) { Animatable(1f) }
            LaunchedEffect(emoji) {
                launch { scale.animateTo(2.4f, tween(700)) }
                alpha.animateTo(0f, tween(700))
                floatingEmoji = null
            }
            Text(
                emoji,
                fontSize = 64.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
            )
        }
    }

    if (showViewers) {
        val viewers by produceState<List<StoryViewerEntry>?>(initialValue = null, story.id) {
            value = StoryRepository.loadViewers(story.id)
        }
        ModalBottomSheet(
            onDismissRequest = { showViewers = false },
            containerColor = MayasTheme.Surface
        ) {
            Text(
                stringResource(R.string.story_viewers_title),
                color = MayasTheme.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            val entries = viewers
            if (entries != null && entries.isEmpty()) {
                Text(
                    stringResource(R.string.story_no_viewers),
                    color = MayasTheme.TextSecondary,
                    modifier = Modifier.padding(20.dp)
                )
            }
            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                items(entries.orEmpty(), key = { it.owner.uid }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showViewers = false
                                onOpenProfile(entry.owner.uid)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatarView(
                            avatarUrl = entry.owner.avatarUrl,
                            useCustomAvatar = entry.owner.useCustomAvatar,
                            profileIcon = entry.owner.profileIcon,
                            profileGlow = entry.owner.profileGlow,
                            isPremium = entry.owner.isPremium,
                            frameType = entry.owner.avatarFrame,
                            size = 40.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.owner.name, color = MayasTheme.TextPrimary, fontSize = 15.sp)
                            if (entry.viewedAt > 0L) {
                                Text(storyTimeAgo(entry.viewedAt), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                            }
                        }
                        entry.reaction?.let { Text(it, fontSize = 22.sp) }
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.story_delete_title)) },
            text = { Text(stringResource(R.string.story_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    val target = story
                    scope.launch {
                        runCatching { StoryRepository.deleteStory(target) }
                        list = list.filter { it.id != target.id }
                        if (list.isEmpty()) onFinishGroup() else index = safeIndex.coerceAtMost(list.lastIndex)
                    }
                }) { Text(stringResource(R.string.story_delete_confirm), color = MayasTheme.ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StoryVideo(
    model: String,
    paused: Boolean,
    onReady: () -> Unit,
    onDuration: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val player = remember(model) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(model))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onDuration(player.duration.coerceAtLeast(0L))
                    onReady()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, paused) { player.playWhenReady = !paused }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun storyTimeAgo(millis: Long): String {
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val minutes = (diff / 60_000L).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.story_time_now)
        minutes < 60 -> stringResource(R.string.story_time_minutes, minutes)
        else -> stringResource(R.string.story_time_hours, minutes / 60)
    }
}
