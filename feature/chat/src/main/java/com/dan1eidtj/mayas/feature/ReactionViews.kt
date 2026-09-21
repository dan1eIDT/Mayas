/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core_ui.emoji.EmojiCatalog
import com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyph
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView

object Reactions {
    const val SEPARATOR = ","
    const val LIMIT_REGULAR = 2
    const val LIMIT_PREMIUM = 10

    fun limitFor(premium: Boolean): Int = if (premium) LIMIT_PREMIUM else LIMIT_REGULAR

    fun parse(raw: String?): List<String> =
        raw.orEmpty().split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun encode(list: List<String>): String = list.distinct().joinToString(SEPARATOR)
}

enum class ReactionResult { ADDED, REMOVED, LIMIT_REACHED, FAILED }

class ReactionGroup(val emoji: String, val userIds: List<String>)

fun Message.reactionsOf(uid: String?): List<String> =
    if (uid == null) emptyList() else Reactions.parse(reactions[uid])

fun Message.reactionGroups(): List<ReactionGroup> {
    val order = LinkedHashMap<String, MutableList<String>>()
    reactions.forEach { (uid, raw) ->
        Reactions.parse(raw).forEach { emoji ->
            order.getOrPut(emoji) { mutableListOf() }.add(uid)
        }
    }
    return order.map { ReactionGroup(it.key, it.value) }
        .sortedByDescending { it.userIds.size }
}

class ReactorProfile(val avatarUrl: String?, val useCustomAvatar: Boolean, val name: String? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionChips(
    message: Message,
    myUid: String?,
    accent: Color,
    contentColor: Color,
    chipBackground: Color,
    profileOf: (String) -> ReactorProfile?,
    onToggle: (String) -> Unit,
    onShowReactors: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val groups = message.reactionGroups()
    FlowRow(
        modifier = modifier.animateContentSize(tween(180)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groups.forEach { group ->
            val mine = myUid != null && group.userIds.contains(myUid)
            ReactionChip(
                group = group,
                mine = mine,
                accent = accent,
                contentColor = contentColor,
                background = chipBackground,
                profileOf = profileOf,
                onClick = { onToggle(group.emoji) },
                onLongClick = { onShowReactors(group.emoji) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReactionChip(
    group: ReactionGroup,
    mine: Boolean,
    accent: Color,
    contentColor: Color,
    background: Color,
    profileOf: (String) -> ReactorProfile?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val description = stringResource(R.string.reaction_chip_description, group.emoji, group.userIds.size)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(shape)
            .background(if (mine) accent.copy(alpha = 0.35f) else background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description }
            .padding(start = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiGlyph(group.emoji, fontSize = 14.sp)
        val showAvatars = group.userIds.size <= 3
        Spacer(Modifier.width(4.dp))
        if (showAvatars) {
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    group.userIds.take(3).forEach { uid ->
                        val profile = profileOf(uid)
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .border(1.dp, if (mine) accent else Color.Transparent, CircleShape)
                        ) {
                            UserAvatarView(
                                avatarUrl = profile?.avatarUrl,
                                useCustomAvatar = profile?.useCustomAvatar ?: true,
                                size = 17.dp
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = group.userIds.size.toString(),
                fontSize = 12.sp,
                color = if (mine) Color.White else contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ReactionPickerPanel(
    selected: List<String>,
    accent: Color,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EmojiCatalog.quickReactions.forEach { emoji ->
                    ReactionPickerCell(emoji, emoji in selected, accent) { onPick(emoji) }
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(if (expanded) R.string.reactions_less else R.string.reactions_more),
                    tint = accent
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.96f, animationSpec = tween(120))
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                EmojiCatalog.extendedReactions.chunked(7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { emoji ->
                            ReactionPickerCell(emoji, emoji in selected, accent) { onPick(emoji) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionPickerCell(
    emoji: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EmojiGlyph(emoji, fontSize = 22.sp)
    }
}

@Composable
fun ReactionLimitToast(
    visible: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.92f, animationSpec = tween(160)),
        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f, animationSpec = tween(160)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xE61C1C1E))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.reactions_limit_premium),
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactorsSheet(
    message: Message,
    initialEmoji: String,
    myUid: String?,
    profileOf: (String) -> ReactorProfile?,
    containerColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accent: Color,
    onDismiss: () -> Unit
) {
    val groups = message.reactionGroups()
    var filter by remember { mutableStateOf<String?>(initialEmoji) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val total = message.reactions.count { Reactions.parse(it.value).isNotEmpty() }

    val people = message.reactions.entries
        .map { it.key to Reactions.parse(it.value) }
        .filter { (_, list) -> list.isNotEmpty() && (filter == null || list.contains(filter)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReactorFilterChip(
                    selected = filter == null,
                    accent = accent,
                    textColor = textColor,
                    onClick = { filter = null }
                ) {
                    Text(
                        "${stringResource(R.string.reactors_all)} $total",
                        color = textColor,
                        fontSize = 13.sp
                    )
                }
                groups.forEach { group ->
                    ReactorFilterChip(
                        selected = filter == group.emoji,
                        accent = accent,
                        textColor = textColor,
                        onClick = { filter = group.emoji }
                    ) {
                        EmojiGlyph(group.emoji, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(group.userIds.size.toString(), color = textColor, fontSize = 13.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                people.forEach { (uid, reactions) ->
                    val profile = profileOf(uid)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatarView(
                            avatarUrl = profile?.avatarUrl,
                            useCustomAvatar = profile?.useCustomAvatar ?: true,
                            size = 40.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (uid == myUid) stringResource(R.string.reactors_you) else (profile?.name ?: ""),
                            color = textColor,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            reactions.forEach { emoji -> EmojiGlyph(emoji, fontSize = 20.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactorFilterChip(
    selected: Boolean,
    accent: Color,
    textColor: Color,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) accent.copy(alpha = 0.28f) else textColor.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
