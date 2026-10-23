/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chats.R
import com.dan1eidtj.data.ChatSwipeAction
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

private fun swipeColor(action: ChatSwipeAction): Color = when (action) {
    ChatSwipeAction.READ -> Color(0xFF3994E8)
    ChatSwipeAction.ARCHIVE -> Color(0xFF7D8590)
    ChatSwipeAction.MUTE -> Color(0xFFE09A2F)
    ChatSwipeAction.PIN -> Color(0xFF20A46B)
}

private fun swipeIcon(action: ChatSwipeAction, isArchived: Boolean, isMuted: Boolean): ImageVector = when (action) {
    ChatSwipeAction.READ -> Icons.Default.DoneAll
    ChatSwipeAction.ARCHIVE -> if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive
    ChatSwipeAction.MUTE -> if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff
    ChatSwipeAction.PIN -> Icons.Default.PushPin
}

private fun swipeLabelRes(action: ChatSwipeAction, isArchived: Boolean, isMuted: Boolean, isPinned: Boolean): Int =
    when (action) {
        ChatSwipeAction.READ -> R.string.chat_swipe_label_read
        ChatSwipeAction.ARCHIVE ->
            if (isArchived) R.string.chat_swipe_label_unarchive else R.string.chat_swipe_label_archive
        ChatSwipeAction.MUTE ->
            if (isMuted) R.string.chat_swipe_label_unmute else R.string.chat_swipe_label_mute
        ChatSwipeAction.PIN ->
            if (isPinned) R.string.chat_swipe_label_unpin else R.string.chat_swipe_label_pin
    }

@Composable
fun ChatSwipeRow(
    action: ChatSwipeAction,
    enabled: Boolean,
    isArchived: Boolean,
    isMuted: Boolean,
    isPinned: Boolean,
    hasUnread: Boolean,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val latestTrigger by rememberUpdatedState(onTrigger)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) latestTrigger()
            false
        }
    )
    val canSwipe = enabled && !(action == ChatSwipeAction.READ && !hasUnread)

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canSwipe,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                val armed = state.targetValue == SwipeToDismissBoxValue.EndToStart
                val background by animateColorAsState(
                    targetValue = if (armed) swipeColor(action) else MayasTheme.SurfaceVariant,
                    label = "swipeRowBackground"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = swipeIcon(action, isArchived, isMuted),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(swipeLabelRes(action, isArchived, isMuted, isPinned)),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        content = {
            Box(modifier = Modifier.background(MayasTheme.Background)) {
                content()
            }
        }
    )
}

@Composable
fun ArchiveEntryRow(
    preview: String,
    unreadChats: Int,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 54.dp)
                .clip(CircleShape)
                .background(MayasTheme.TextSecondary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = MayasTheme.TextPrimary,
                modifier = Modifier.size(if (compact) 20.dp else 26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.chat_archive_title),
                color = MayasTheme.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    color = MayasTheme.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (unreadChats > 0) {
            Box(
                modifier = Modifier
                    .widthIn(min = 22.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.TextSecondary.copy(alpha = 0.45f))
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadChats.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ArchiveHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MayasTheme.Surface.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
        }
        Text(
            text = stringResource(R.string.chat_archive_title),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MayasTheme.TextPrimary
        )
    }
}
