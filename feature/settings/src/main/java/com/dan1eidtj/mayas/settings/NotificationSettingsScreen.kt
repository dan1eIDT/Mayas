/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.data.NotificationPrefs
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.settings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    vm: AuthVM,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pushEnabled = vm.userData["pushEnabled"] != "false"
    val notifSound = vm.userData["notifSound"] != "false"
    val notifVibration = vm.userData["notifVibration"] != "false"
    val notifPreview = vm.userData["notifPreview"] != "false"
    val notifGroupMessages = vm.userData["notifGroupMessages"] != "false"
    val notifCalls = vm.userData["notifCalls"] != "false"
    val notifReactions = vm.userData["notifReactions"] != "false"

    LaunchedEffect(pushEnabled, notifSound, notifVibration, notifPreview, notifGroupMessages, notifCalls, notifReactions) {
        NotificationPrefs.set(context, NotificationPrefs.KEY_PUSH_ENABLED, pushEnabled)
        NotificationPrefs.set(context, NotificationPrefs.KEY_SOUND, notifSound)
        NotificationPrefs.set(context, NotificationPrefs.KEY_VIBRATION, notifVibration)
        NotificationPrefs.set(context, NotificationPrefs.KEY_PREVIEW, notifPreview)
        NotificationPrefs.set(context, NotificationPrefs.KEY_GROUP_MESSAGES, notifGroupMessages)
        NotificationPrefs.set(context, NotificationPrefs.KEY_CALLS, notifCalls)
        NotificationPrefs.set(context, "notifReactions", notifReactions)
    }

    fun updateFlag(key: String, value: Boolean) {
        vm.updateUserData(key, value.toString())
        NotificationPrefs.set(context, key, value)
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notif_and_sounds_title), color = MayasTheme.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionTitle(stringResource(R.string.general_section))

            NotificationToggleGroup {
                NotificationToggleRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.push_notifications_label),
                    subtitle = stringResource(R.string.master_switch_desc),
                    checked = pushEnabled,
                    onCheckedChange = { updateFlag("pushEnabled", it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle(stringResource(R.string.sound_and_vibration_section))

            NotificationToggleGroup {
                NotificationToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = stringResource(R.string.notification_sound_label),
                    subtitle = stringResource(R.string.play_sound_on_message_desc),
                    checked = notifSound,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifSound", it) }
                )
                HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))
                NotificationToggleRow(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.vibration_label),
                    subtitle = stringResource(R.string.vibrate_on_notification_desc),
                    checked = notifVibration,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifVibration", it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle(stringResource(R.string.content_section))

            NotificationToggleGroup {
                NotificationToggleRow(
                    icon = Icons.Default.Preview,
                    title = stringResource(R.string.show_message_text),
                    subtitle = stringResource(R.string.notification_preview_desc),
                    checked = notifPreview,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifPreview", it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle(stringResource(R.string.notification_types_section))

            NotificationToggleGroup {
                NotificationToggleRow(
                    icon = Icons.Default.Groups,
                    title = stringResource(R.string.group_chats_label),
                    subtitle = stringResource(R.string.notify_group_messages_desc),
                    checked = notifGroupMessages,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifGroupMessages", it) }
                )
                HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))
                NotificationToggleRow(
                    icon = Icons.Default.Call,
                    title = stringResource(R.string.calls_toggle_label),
                    subtitle = stringResource(R.string.notify_incoming_calls_desc),
                    checked = notifCalls,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifCalls", it) }
                )
                HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))
                NotificationToggleRow(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.notif_reactions_label),
                    subtitle = stringResource(R.string.notif_reactions_desc),
                    checked = notifReactions,
                    enabled = pushEnabled,
                    onCheckedChange = { updateFlag("notifReactions", it) }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface),
        content = content
    )
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) MayasTheme.TextPrimary else MayasTheme.TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) MayasTheme.TextPrimary else MayasTheme.TextSecondary.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked && enabled,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.Accent)
        )
    }
}
