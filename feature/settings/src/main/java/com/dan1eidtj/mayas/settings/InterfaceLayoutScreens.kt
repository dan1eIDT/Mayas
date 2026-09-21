/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.settings

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.dan1eidtj.mayas.settings.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core.ui.theme.VerticalSlot
import com.dan1eidtj.mayas.core.ui.theme.HorizontalSlot
import com.dan1eidtj.mayas.core.ui.theme.HomeScreenLayoutPrefs
import com.dan1eidtj.mayas.core.ui.theme.SidebarLayoutPrefs


// ---------------------------------------------------------------------------
// Общие мелкие компоненты
// ---------------------------------------------------------------------------

@Composable
private fun LayoutSectionLabel(text: String) {
    Text(
        text = text,
        color = MayasTheme.Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun <T> SegmentedPositionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MayasTheme.SurfaceVariant.copy(alpha = 0.4f)),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MayasTheme.Accent else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        labelOf(option),
                        color = if (isSelected) Color.White else MayasTheme.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MayasTheme.Accent)
        )
    }
}

@Composable
private fun DpiSyncNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.dpi_note), color = MayasTheme.TextSecondary, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Превью главного экрана (пример)
// ---------------------------------------------------------------------------

@Composable
private fun HomeScreenPreview(prefs: HomeScreenLayoutPrefs) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MayasTheme.Background)
            .padding(10.dp)
    ) {
        val searchBar: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(com.dan1eidtj.chat.R.string.search_content_description), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        val folders: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(stringResource(com.dan1eidtj.profile.R.string.all_short), stringResource(R.string.personal_label), stringResource(com.dan1eidtj.chat.R.string.groups)).forEachIndexed { i, name ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (i == 0) MayasTheme.Accent else MayasTheme.SurfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(name, color = if (i == 0) Color.White else MayasTheme.TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        if (prefs.searchPosition == VerticalSlot.TOP) { searchBar(); Spacer(Modifier.height(8.dp)) }
        if (prefs.foldersPosition == VerticalSlot.TOP) { folders(); Spacer(Modifier.height(8.dp)) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MayasTheme.Surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (prefs.avatarPosition == HorizontalSlot.START) Arrangement.Start else Arrangement.End
                ) {
                    val avatar: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .size(if (prefs.compactList) 28.dp else 36.dp)
                                .clip(CircleShape)
                                .background(MayasTheme.Accent.copy(alpha = 0.6f))
                        )
                    }
                    val texts: @Composable () -> Unit = {
                        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(stringResource(R.string.peer_placeholder, i + 1), color = MayasTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.last_message_placeholder), color = MayasTheme.TextSecondary, fontSize = 10.sp)
                        }
                    }
                    if (prefs.avatarPosition == HorizontalSlot.START) {
                        avatar(); texts()
                    } else {
                        texts(); avatar()
                    }
                }
            }
        }

        if (prefs.foldersPosition == VerticalSlot.BOTTOM) { Spacer(Modifier.height(8.dp)); folders() }
        if (prefs.searchPosition == VerticalSlot.BOTTOM) { Spacer(Modifier.height(8.dp)); searchBar() }

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(if (prefs.fabPosition == HorizontalSlot.END) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(top = 8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Экран: Настройка главного экрана
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenLayoutScreen(
    initialPrefs: HomeScreenLayoutPrefs = HomeScreenLayoutPrefs(),
    onSave: (HomeScreenLayoutPrefs) -> Unit,
    onBack: () -> Unit
) {
    var prefs by remember { mutableStateOf(initialPrefs) }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_screen_label), color = MayasTheme.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { onSave(prefs) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_layout))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item { LayoutSectionLabel(stringResource(R.string.example_section)) }
            item { HomeScreenPreview(prefs) }

            item { LayoutSectionLabel(stringResource(R.string.positions_section)) }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.search_bar_label),
                    subtitle = stringResource(R.string.where_to_show_search),
                    options = VerticalSlot.values().toList(),
                    selected = prefs.searchPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(searchPosition = it) }
                )
            }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.ChatBubble,
                    title = stringResource(R.string.chat_folders),
                    subtitle = stringResource(R.string.tabs_position_desc),
                    options = VerticalSlot.values().toList(),
                    selected = prefs.foldersPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(foldersPosition = it) }
                )
            }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.create_chat_button),
                    subtitle = stringResource(R.string.button_side_of_screen),
                    options = HorizontalSlot.values().toList(),
                    selected = prefs.fabPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(fabPosition = it) }
                )
            }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.AccountCircle,
                    title = stringResource(R.string.avatar_in_chat_list),
                    subtitle = stringResource(R.string.left_or_right_of_message),
                    options = HorizontalSlot.values().toList(),
                    selected = prefs.avatarPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(avatarPosition = it) }
                )
            }

            item {
                SwitchRow(
                    icon = Icons.Default.DensitySmall,
                    title = stringResource(R.string.compact_list),
                    subtitle = stringResource(R.string.compact_list_desc),
                    checked = prefs.compactList,
                    onCheckedChange = { prefs = prefs.copy(compactList = it) }
                )
            }

            item { LayoutSectionLabel(stringResource(R.string.visibility_section)) }

            item {
                SwitchRow(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.search_field_in_header),
                    subtitle = stringResource(R.string.if_off_search_moves_to_menu),
                    checked = prefs.showSearchField,
                    onCheckedChange = { prefs = prefs.copy(showSearchField = it) }
                )
            }

            item {
                SwitchRow(
                    icon = Icons.Default.PersonAdd,
                    title = stringResource(R.string.add_friend_button),
                    subtitle = stringResource(R.string.if_off_moves_to_menu),
                    checked = prefs.showAddFriendButton,
                    onCheckedChange = { prefs = prefs.copy(showAddFriendButton = it) }
                )
            }

            item { DpiSyncNote() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Превью боковой панели (пример)
// ---------------------------------------------------------------------------

@Composable
private fun SidebarPreview(prefs: SidebarLayoutPrefs) {
    Column(
        modifier = Modifier
            .fillMaxWidth(if (prefs.compactMode) 0.56f else 0.72f)
            .height(300.dp)
            .clip(RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp))
            .background(MayasTheme.Surface)
            .padding(if (prefs.compactMode) 8.dp else 12.dp)
    ) {
        val profileBlock: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (prefs.compactMode) 30.dp else 40.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.Accent.copy(alpha = 0.6f))
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.my_profile), color = MayasTheme.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(com.dan1eidtj.chats.R.string.online), color = MayasTheme.TextSecondary, fontSize = 10.sp)
                }
            }
        }

        if (prefs.profileBlockPosition == VerticalSlot.TOP) {
            profileBlock()
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MayasTheme.TextSecondary.copy(alpha = 0.1f))
            Spacer(Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val visibleItems = prefs.itemsOrder.filter { label ->
                when (label) {
                    "Мой профиль" -> false
                    "Папки чатов" -> prefs.showFolders
                    "Настройки" -> prefs.showAppSection
                    else -> true
                }
            }
            visibleItems.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (prefs.actionsIconPosition == HorizontalSlot.START) Arrangement.Start else Arrangement.End
                ) {
                    val icon: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MayasTheme.TextSecondary.copy(alpha = 0.4f))
                        )
                    }
                    val text: @Composable () -> Unit = {
                        Text(
                            label,
                            color = MayasTheme.TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                    if (prefs.actionsIconPosition == HorizontalSlot.START) {
                        icon(); text()
                    } else {
                        text(); icon()
                    }
                }
            }
            if (prefs.customLinks.isNotEmpty()) {
                prefs.customLinks.forEach { link ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = MayasTheme.Accent, modifier = Modifier.size(14.dp))
                        Text(
                            link.label,
                            color = MayasTheme.TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }

        if (prefs.profileBlockPosition == VerticalSlot.BOTTOM) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MayasTheme.TextSecondary.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))
            profileBlock()
        }
    }
}

// ---------------------------------------------------------------------------
// Экран: Настройка боковой панели
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarLayoutScreen(
    initialPrefs: SidebarLayoutPrefs = SidebarLayoutPrefs(),
    onSave: (SidebarLayoutPrefs) -> Unit,
    onBack: () -> Unit
) {
    var prefs by remember { mutableStateOf(initialPrefs) }

    fun moveItem(from: Int, to: Int) {
        if (to < 0 || to >= prefs.itemsOrder.size) return
        val newList = prefs.itemsOrder.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)
        prefs = prefs.copy(itemsOrder = newList)
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sidebar_label), color = MayasTheme.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { onSave(prefs) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_layout))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item { LayoutSectionLabel(stringResource(R.string.example_section)) }
            item {
                Box(modifier = Modifier.fillMaxWidth().background(MayasTheme.Background)) {
                    SidebarPreview(prefs)
                }
            }

            item { LayoutSectionLabel(stringResource(R.string.positions_section)) }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.AccountCircle,
                    title = stringResource(R.string.profile_block),
                    subtitle = stringResource(R.string.show_avatar_name_position),
                    options = VerticalSlot.values().toList(),
                    selected = prefs.profileBlockPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(profileBlockPosition = it) }
                )
            }

            item {
                SegmentedPositionRow(
                    icon = Icons.Default.FormatListBulleted,
                    title = stringResource(R.string.menu_item_icons),
                    subtitle = stringResource(R.string.icon_side_relative_to_text),
                    options = HorizontalSlot.values().toList(),
                    selected = prefs.actionsIconPosition,
                    labelOf = { it.label },
                    onSelect = { prefs = prefs.copy(actionsIconPosition = it) }
                )
            }

            item { LayoutSectionLabel(stringResource(R.string.visibility_section)) }

            item {
                SwitchRow(
                    icon = Icons.Default.ViewCompact,
                    title = stringResource(R.string.compact_panel),
                    subtitle = stringResource(R.string.compact_panel_desc),
                    checked = prefs.compactMode,
                    onCheckedChange = { prefs = prefs.copy(compactMode = it) }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Default.GroupAdd,
                    title = stringResource(R.string.quick_actions),
                    subtitle = stringResource(R.string.quick_actions_desc),
                    checked = prefs.showQuickActions,
                    onCheckedChange = { prefs = prefs.copy(showQuickActions = it) }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.chat_folders),
                    subtitle = stringResource(R.string.folders_block_desc),
                    checked = prefs.showFolders,
                    onCheckedChange = { prefs = prefs.copy(showFolders = it) }
                )
            }
            item {
                SwitchRow(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.settings_and_about_quoted),
                    subtitle = stringResource(R.string.system_items_block_desc),
                    checked = prefs.showAppSection,
                    onCheckedChange = { prefs = prefs.copy(showAppSection = it) }
                )
            }

            item { LayoutSectionLabel(stringResource(R.string.pinned_chats_section)) }

            item {
                SwitchRow(
                    icon = Icons.Default.PushPin,
                    title = stringResource(R.string.show_pinned_chats),
                    subtitle = stringResource(R.string.pinned_chats_block_desc),
                    checked = prefs.showPinnedChats,
                    onCheckedChange = { prefs = prefs.copy(showPinnedChats = it) }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayasTheme.Surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.pin_in_chat_list_desc),
                            color = MayasTheme.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.pin_chats_hint),
                            color = MayasTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item { LayoutSectionLabel(stringResource(R.string.item_order_section)) }

            itemsIndexed(prefs.itemsOrder) { index, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MayasTheme.Surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragIndicator, null, tint = MayasTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    Text(
                        label,
                        color = MayasTheme.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )
                    IconButton(onClick = { moveItem(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) MayasTheme.TextPrimary else MayasTheme.TextSecondary.copy(alpha = 0.3f))
                    }
                    IconButton(onClick = { moveItem(index, index + 1) }, enabled = index < prefs.itemsOrder.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < prefs.itemsOrder.lastIndex) MayasTheme.TextPrimary else MayasTheme.TextSecondary.copy(alpha = 0.3f))
                    }
                }
            }

            item { LayoutSectionLabel(stringResource(R.string.custom_links_section)) }

            item {
                var newLinkLabel by remember { mutableStateOf("") }
                var newLinkUrl by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayasTheme.Surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.add_shortcut_hint),
                        color = MayasTheme.TextSecondary,
                        fontSize = 12.sp
                    )

                    prefs.customLinks.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MayasTheme.SurfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, null, tint = MayasTheme.Accent, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(link.label, color = MayasTheme.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(link.url, color = MayasTheme.TextSecondary, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                prefs = prefs.copy(customLinks = prefs.customLinks.filter { it.id != link.id })
                            }) {
                                Icon(Icons.Default.Close, null, tint = MayasTheme.TextSecondary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newLinkLabel,
                        onValueChange = { newLinkLabel = it },
                        label = { Text(stringResource(com.dan1eidtj.profile.R.string.name_label_generic), color = MayasTheme.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLinkUrl,
                        onValueChange = { newLinkUrl = it },
                        label = { Text(stringResource(R.string.link_placeholder_https), color = MayasTheme.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newLinkLabel.isNotBlank() && newLinkUrl.isNotBlank()) {
                                val rawUrl = newLinkUrl.trim()
                                val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                                    rawUrl
                                } else {
                                    "https://$rawUrl"
                                }
                                prefs = prefs.copy(
                                    customLinks = prefs.customLinks + com.dan1eidtj.mayas.core.ui.theme.SidebarCustomLink(
                                        id = System.currentTimeMillis().toString(),
                                        label = newLinkLabel.trim(),
                                        url = normalizedUrl
                                    )
                                )
                                newLinkLabel = ""
                                newLinkUrl = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_link))
                    }
                }
            }

            item { DpiSyncNote() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}