/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView

object StorySeenState {
    private const val PREFS = "mayas_story_seen"
    private const val KEY = "ids"
    private var loaded = false

    var seen by mutableStateOf<Set<String>>(emptySet())
        private set

    fun ensure(context: Context) {
        if (loaded) return
        loaded = true
        val stored = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
        seen = stored?.toSet() ?: emptySet()
    }

    fun markSeen(context: Context, id: String) {
        ensure(context)
        if (id in seen) return
        var next = seen + id
        if (next.size > 800) next = next.toList().takeLast(600).toSet()
        seen = next
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, next).apply()
    }
}

val StoryRingPresets = listOf(
    "#FF4D8D", "#9559FF", "#2196F3", "#00BFA5", "#FFC107", "#FF5722", "#8BC34A", "#FFFFFF"
)

fun parseRingColor(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

fun StoryGroup.hasUnseen(seen: Set<String>): Boolean = stories.any { it.id !in seen }

fun orderStoryGroups(groups: List<StoryGroup>, seen: Set<String>): List<StoryGroup> {
    val mine = groups.filter { it.isMine }
    val others = groups.filter { !it.isMine }
    val unseen = others.filter { it.hasUnseen(seen) }
    val viewed = others.filter { !it.hasUnseen(seen) }
    return mine + unseen + viewed
}

@Composable
fun StoryAvatarRing(
    owner: StoryOwner,
    hasStory: Boolean,
    hasUnseen: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val custom = parseRingColor(owner.ringColor)
    val gradient = Brush.sweepGradient(listOf(MayasTheme.GlowPurple, MayasTheme.GlowPink, MayasTheme.GlowPurple))
    val brush: Brush = when {
        !hasStory -> SolidColor(Color.Transparent)
        !hasUnseen -> SolidColor(Color.Gray.copy(alpha = 0.55f))
        custom != null -> SolidColor(custom)
        else -> gradient
    }
    val strokeWidth = if (hasUnseen) 3.dp else 2.dp
    val inner = if (hasStory) size - 10.dp else size

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (hasStory) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = strokeWidth.toPx()
                drawCircle(
                    brush = brush,
                    radius = (this.size.minDimension - sw) / 2f,
                    style = Stroke(width = sw)
                )
            }
        }
        UserAvatarView(
            avatarUrl = owner.avatarUrl,
            useCustomAvatar = owner.useCustomAvatar,
            profileIcon = owner.profileIcon,
            profileGlow = owner.profileGlow,
            isPremium = owner.isPremium,
            frameType = owner.avatarFrame,
            size = inner
        )
    }
}

@Composable
fun StoriesRow(
    ordered: List<StoryGroup>,
    myOwner: StoryOwner?,
    seen: Set<String>,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMine = ordered.firstOrNull()?.isMine == true
    val others = if (hasMine) ordered.drop(1) else ordered

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (myOwner != null) {
            item(key = "my_story") {
                Column(
                    modifier = Modifier
                        .width(66.dp)
                        .clickable { if (hasMine) onOpen(0) else onAdd() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val mine = if (hasMine) ordered.first() else null
                    Box {
                        StoryAvatarRing(
                            owner = myOwner,
                            hasStory = mine != null,
                            hasUnseen = mine?.hasUnseen(seen) ?: false,
                            size = 60.dp
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MayasTheme.Accent)
                                .border(2.dp, MayasTheme.Background, CircleShape)
                                .clickable { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.story_my),
                        color = MayasTheme.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        itemsIndexed(others, key = { _, g -> g.owner.uid }) { index, group ->
            val realIndex = if (hasMine) index + 1 else index
            val unseen = group.hasUnseen(seen)
            Column(
                modifier = Modifier
                    .width(66.dp)
                    .clickable { onOpen(realIndex) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StoryAvatarRing(owner = group.owner, hasStory = true, hasUnseen = unseen, size = 60.dp)
                Text(
                    group.owner.name,
                    color = if (unseen) MayasTheme.TextPrimary else MayasTheme.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}


@Composable
fun CompactStoriesBar(
    ordered: List<StoryGroup>,
    myOwner: StoryOwner?,
    seen: Set<String>,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMine = ordered.firstOrNull()?.isMine == true
    val mine = if (hasMine) ordered.first() else null
    val others = if (hasMine) ordered.drop(1) else ordered

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (myOwner != null) {
            item(key = "compact_my_story") {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { if (mine != null) onOpen(0) else onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    StoryAvatarRing(
                        owner = myOwner,
                        hasStory = mine != null,
                        hasUnseen = mine?.hasUnseen(seen) ?: false,
                        size = 34.dp
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(MayasTheme.Accent)
                            .border(1.dp, MayasTheme.Background, CircleShape)
                            .clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
        itemsIndexed(others, key = { _, group -> "compact_${group.owner.uid}" }) { index, group ->
            val sourceIndex = if (hasMine) index + 1 else index
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onOpen(sourceIndex) },
                contentAlignment = Alignment.Center
            ) {
                StoryAvatarRing(
                    owner = group.owner,
                    hasStory = true,
                    hasUnseen = group.hasUnseen(seen),
                    size = 34.dp
                )
            }
        }
    }
}

@Composable
fun StoryRingColorPicker(current: String?, onPick: (String?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(MayasTheme.GlowPurple, MayasTheme.GlowPink, MayasTheme.GlowPurple)))
                .border(if (current == null) 2.dp else 0.dp, MayasTheme.TextPrimary, CircleShape)
                .clickable { onPick(null) },
            contentAlignment = Alignment.Center
        ) {
            if (current == null) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        StoryRingPresets.forEach { hex ->
            val color = parseRingColor(hex) ?: Color.White
            val selected = current.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (selected) 2.dp else 0.5.dp, if (selected) MayasTheme.TextPrimary else MayasTheme.Outline, CircleShape)
                    .clickable { onPick(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(
                    Icons.Default.Check, null,
                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
