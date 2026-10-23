/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.data.ChatSwipeAction
import com.dan1eidtj.data.ChatSwipePrefs
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun ChatSwipeAction.icon(): ImageVector = when (this) {
    ChatSwipeAction.READ -> Icons.Default.DoneAll
    ChatSwipeAction.ARCHIVE -> Icons.Default.Archive
    ChatSwipeAction.MUTE -> Icons.Default.NotificationsOff
    ChatSwipeAction.PIN -> Icons.Default.PushPin
}

private fun ChatSwipeAction.labelRes(): Int = when (this) {
    ChatSwipeAction.READ -> R.string.chat_swipe_option_read
    ChatSwipeAction.ARCHIVE -> R.string.chat_swipe_option_archive
    ChatSwipeAction.MUTE -> R.string.chat_swipe_option_mute
    ChatSwipeAction.PIN -> R.string.chat_swipe_option_pin
}

@Composable
fun ChatSwipeActionSetting() {
    val context = LocalContext.current
    remember(context) { ChatSwipePrefs.ensureInit(context) }
    val current by ChatSwipePrefs.action.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Swipe, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.chat_swipe_title),
                color = MayasTheme.TextPrimary,
                fontSize = 14.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MayasTheme.Background.copy(alpha = 0.6f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SwipePreview(action = current, modifier = Modifier.weight(1f))
            SwipeActionWheel(
                selected = current,
                onSelect = { ChatSwipePrefs.set(context, it) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(R.string.chat_swipe_desc),
            color = MayasTheme.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun SwipePreview(action: ChatSwipeAction, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "swipePreview")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipePreviewProgress"
    )
    val revealWidth = 64.dp
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Background)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(revealWidth)
                .background(Color(0xFFE4E4E4)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = action, label = "swipePreviewIcon") { shown ->
                Icon(
                    imageVector = shown.icon(),
                    contentDescription = null,
                    tint = Color(0xFF1C1C1E),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(-(revealPx * progress).roundToInt(), 0) }
                .background(MayasTheme.Surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.TextSecondary.copy(alpha = 0.25f))
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.TextSecondary.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.TextSecondary.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun SwipeActionWheel(
    selected: ChatSwipeAction,
    onSelect: (ChatSwipeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { ChatSwipeAction.values().toList() }
    val itemHeight = 44.dp
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(selected))
    val fling = rememberSnapFlingBehavior(state)
    val scope = rememberCoroutineScope()
    val latestOnSelect by rememberUpdatedState(onSelect)
    val centered by remember {
        derivedStateOf {
            val raw = state.firstVisibleItemIndex +
                    if (state.firstVisibleItemScrollOffset > itemPx / 2f) 1 else 0
            raw.coerceIn(0, items.lastIndex)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect { latestOnSelect(items[centered]) }
    }

    LaunchedEffect(selected) {
        val target = items.indexOf(selected)
        if (!state.isScrollInProgress && target != centered) state.animateScrollToItem(target)
    }

    Box(modifier = modifier.height(itemHeight * 3)) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                val isCentered = index == centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(item.labelRes()),
                        color = if (isCentered) MayasTheme.TextPrimary
                        else MayasTheme.TextSecondary.copy(alpha = 0.45f),
                        fontSize = 16.sp,
                        fontWeight = if (isCentered) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
        HorizontalDivider(
            color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
            thickness = 1.5.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = itemHeight)
                .padding(horizontal = 8.dp)
        )
        HorizontalDivider(
            color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
            thickness = 1.5.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = itemHeight * 2)
                .padding(horizontal = 8.dp)
        )
    }
}
