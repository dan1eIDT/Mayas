/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.DarkMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.isStandardScheme
import com.dan1eidtj.mayas.core.ui.theme.LightMayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.MayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    currentScheme: MayasColorScheme,
    customThemes: List<Pair<String, MayasColorScheme>> = emptyList(),
    onSelectScheme: (MayasColorScheme) -> Unit,
    onNavigateToEditor: () -> Unit,
    onEditCustomTheme: (String) -> Unit = {},
    followSystem: Boolean = false,
    onFollowSystemChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
) {
    val isStandardActive = currentScheme.isStandardScheme()

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_themes_title),
                        color = MayasTheme.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.BrightnessAuto,
                            iconBackground = MayasTheme.Accent,
                            title = stringResource(R.string.theme_follow_system_title),
                            subtitle = stringResource(
                                if (isStandardActive) R.string.theme_follow_system_desc else R.string.theme_follow_system_disabled
                            ),
                            showDivider = false,
                            enabled = isStandardActive,
                            trailing = {
                                Switch(
                                    checked = followSystem && isStandardActive,
                                    onCheckedChange = { onFollowSystemChange(it) },
                                    enabled = isStandardActive
                                )
                            },
                            onClick = { onFollowSystemChange(!followSystem) }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionTitle(stringResource(R.string.themes_section_standard))
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ThemePreviewCard(
                            title = stringResource(R.string.theme_dark),
                            subtitle = stringResource(R.string.theme_dark_desc),
                            scheme = DarkMayasColorScheme,
                            isSelected = currentScheme == DarkMayasColorScheme,
                            onClick = {
                                if (followSystem && isStandardActive) onFollowSystemChange(false)
                                onSelectScheme(DarkMayasColorScheme)
                            }
                        )
                    }
                    item {
                        ThemePreviewCard(
                            title = stringResource(R.string.theme_light),
                            subtitle = stringResource(R.string.theme_light_desc),
                            scheme = LightMayasColorScheme,
                            isSelected = currentScheme == LightMayasColorScheme,
                            onClick = {
                                if (followSystem && isStandardActive) onFollowSystemChange(false)
                                onSelectScheme(LightMayasColorScheme)
                            }
                        )
                    }
                }
            }

            if (customThemes.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsSectionTitle(stringResource(R.string.themes_section_custom))
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(customThemes) { (name, scheme) ->
                            ThemePreviewCard(
                                title = name,
                                subtitle = stringResource(R.string.theme_custom_label),
                                scheme = scheme,
                                isSelected = currentScheme == scheme,
                                onClick = { onSelectScheme(scheme) },
                                onEditClick = { onEditCustomTheme(name) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Default.Add,
                            iconBackground = MayasTheme.Accent,
                            title = stringResource(R.string.theme_create),
                            subtitle = stringResource(R.string.theme_create_desc),
                            showDivider = false,
                            onClick = onNavigateToEditor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    title: String,
    subtitle: String,
    scheme: MayasColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.width(148.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.background)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) MayasTheme.Accent else MayasTheme.TextSecondary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(scheme.textPrimary.copy(alpha = 0.8f))
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.textSecondary.copy(alpha = 0.8f))
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewBubble(color = scheme.bubbleOther, line = scheme.textPrimary, widthDp = 78, alignEnd = false)
                    PreviewBubble(color = scheme.bubbleMine, line = scheme.textPrimary, widthDp = 66, alignEnd = true)
                    PreviewBubble(color = scheme.bubbleOther, line = scheme.textPrimary, widthDp = 52, alignEnd = false)
                    PreviewBubble(color = scheme.bubbleMine, line = scheme.textPrimary, widthDp = 84, alignEnd = true)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(scheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.theme_selected),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = MayasTheme.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onEditClick != null) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.theme_edit),
                        tint = MayasTheme.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBubble(color: Color, line: Color, widthDp: Int, alignEnd: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(line.copy(alpha = 0.55f))
            )
            Box(
                modifier = Modifier
                    .width((widthDp * 0.6f).dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(line.copy(alpha = 0.35f))
            )
        }
    }
}
