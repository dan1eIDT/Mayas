/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.mesh.MeshManager
import com.dan1eidtj.mayas.mesh.MeshMessage
import com.dan1eidtj.mayas.mesh.MeshPermissions
import com.dan1eidtj.mayas.mesh.MeshReason
import com.dan1eidtj.mayas.mesh.MeshStatus
import com.dan1eidtj.mayas.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by MeshManager.state.collectAsState()
    var permissionsGranted by remember { mutableStateOf(MeshPermissions.hasAll(context)) }
    var nameDraft by remember(state.displayName) { mutableStateOf(state.displayName) }
    var messageDraft by remember { mutableStateOf("") }
    var targetId by remember { mutableStateOf("") }
    var chatTab by remember { mutableStateOf(MeshChatTab.BROADCAST) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsGranted = MeshPermissions.hasAll(context)
        MeshManager.onPermissionsChanged()
    }

    val statusText = when (state.status) {
        MeshStatus.OFF -> stringResource(R.string.mesh_status_off)
        MeshStatus.NEEDS_PERMISSION -> stringResource(R.string.mesh_status_needs_permission)
        MeshStatus.NEEDS_BLUETOOTH -> stringResource(R.string.mesh_status_needs_bluetooth)
        MeshStatus.STARTING -> stringResource(R.string.mesh_status_starting)
        MeshStatus.RECONNECTING -> stringResource(R.string.mesh_status_reconnecting)
        MeshStatus.ACTIVE -> stringResource(R.string.mesh_status_active)
        MeshStatus.ERROR -> stringResource(R.string.mesh_status_error)
    }
    val reasonText = when (state.reason) {
        MeshReason.NONE -> stringResource(R.string.mesh_reason_none)
        MeshReason.MANUAL -> stringResource(R.string.mesh_reason_manual)
        MeshReason.AUTO_OFFLINE -> stringResource(R.string.mesh_reason_auto)
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Вынесено из LazyColumn: remember тут вызывается в composable-контексте
    val visibleMessages = remember(state.messages, chatTab, targetId) {
        state.messages.filter { m ->
            if (chatTab == MeshChatTab.BROADCAST) {
                m.dst == "*"
            } else {
                targetId.isNotBlank() && (m.srcId == targetId || (m.outgoing && m.dst == targetId))
            }
        }.asReversed()
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mesh_title), color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MeshCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mesh_master_switch), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.mesh_master_hint), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = state.enabled, onCheckedChange = { MeshManager.setEnabled(it) })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mesh_auto_switch), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.mesh_auto_hint), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = state.autoEnable, onCheckedChange = { MeshManager.setAutoEnable(it) })
                    }
                    if (!permissionsGranted) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { launcher.launch(MeshPermissions.required()) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.mesh_grant_permissions))
                        }
                    }
                }
            }

            item {
                MeshCard {
                    Text(stringResource(R.string.mesh_diagnostics), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    MeshInfoRow(stringResource(R.string.mesh_state_label), statusText)
                    MeshInfoRow(stringResource(R.string.mesh_reason_label), reasonText)
                    MeshInfoRow(
                        stringResource(R.string.mesh_internet_label),
                        stringResource(if (state.hasInternet) R.string.mesh_yes else R.string.mesh_no)
                    )
                    MeshInfoRow(stringResource(R.string.mesh_own_id), state.ownId)
                    MeshInfoRow(
                        stringResource(R.string.mesh_ping),
                        state.averagePingMs?.let { stringResource(R.string.mesh_ping_value, it) } ?: "—"
                    )
                    MeshInfoRow(stringResource(R.string.mesh_found_count), state.discovered.size.toString())
                    MeshInfoRow(stringResource(R.string.mesh_connected_count), state.peers.size.toString())
                    MeshInfoRow(stringResource(R.string.mesh_sent_count), state.sentCount.toString())
                    MeshInfoRow(stringResource(R.string.mesh_received_count), state.receivedCount.toString())
                    MeshInfoRow(stringResource(R.string.mesh_relayed_count), state.relayedCount.toString())
                    state.lastError?.let { MeshInfoRow(stringResource(R.string.mesh_last_error), it) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(24) },
                        label = { Text(stringResource(R.string.mesh_display_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { MeshManager.setDisplayName(nameDraft) }) {
                        Text(stringResource(R.string.mesh_save_name))
                    }
                }
            }

            item {
                MeshCard {
                    Text(stringResource(R.string.mesh_devices), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (state.peers.isEmpty() && state.discovered.isEmpty()) {
                        Text(stringResource(R.string.mesh_no_devices), color = MayasTheme.TextSecondary, fontSize = 13.sp)
                    }
                    state.peers.forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(peer.name, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                                    if (peer.mayasUid != null) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Link,
                                            stringResource(R.string.mesh_linked_account),
                                            tint = MayasTheme.Accent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(peer.meshId, color = MayasTheme.TextSecondary, fontSize = 11.sp)
                            }
                            Text(
                                peer.pingMs?.let { stringResource(R.string.mesh_ping_value, it) } ?: "…",
                                color = MayasTheme.GlowGreen,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = {
                                chatTab = MeshChatTab.DIRECT
                                targetId = peer.meshId
                            }) { Text(stringResource(R.string.mesh_write)) }
                        }
                    }
                    state.discovered.forEach { found ->
                        Text(
                            stringResource(R.string.mesh_connecting_to, found.name, found.meshId),
                            color = MayasTheme.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            item {
                MeshCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MeshTabButton(
                            label = stringResource(R.string.mesh_tab_broadcast),
                            selected = chatTab == MeshChatTab.BROADCAST,
                            modifier = Modifier.weight(1f)
                        ) { chatTab = MeshChatTab.BROADCAST }
                        Spacer(Modifier.width(8.dp))
                        MeshTabButton(
                            label = stringResource(R.string.mesh_tab_direct),
                            selected = chatTab == MeshChatTab.DIRECT,
                            modifier = Modifier.weight(1f)
                        ) { chatTab = MeshChatTab.DIRECT }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.mesh_chat_warning), color = MayasTheme.TextSecondary, fontSize = 11.sp)
                    if (chatTab == MeshChatTab.DIRECT) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.mesh_direct_warning), color = MayasTheme.GlowGold, fontSize = 11.sp)
                        val selectedPeer = state.peers.firstOrNull { it.meshId == targetId }
                        if (!selectedPeer?.mayasUid.isNullOrBlank() && selectedPeer?.identityVerified != true) {
                            Text(stringResource(R.string.mesh_peer_identity_pending), color = MayasTheme.ErrorRed, fontSize = 11.sp)
                        } else if (selectedPeer?.identityVerified == true) {
                            Text(stringResource(R.string.mesh_peer_identity_verified), color = MayasTheme.GlowGreen, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (chatTab == MeshChatTab.DIRECT) {
                        if (state.peers.isEmpty()) {
                            Text(stringResource(R.string.mesh_no_devices), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.peers.forEach { peer ->
                                    MeshTabButton(
                                        label = if (peer.identityVerified) "${peer.name} ✓" else "${peer.name} · ?",
                                        selected = targetId == peer.meshId,
                                        modifier = Modifier.weight(1f)
                                    ) { targetId = peer.meshId }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it.take(1000) },
                        label = { Text(stringResource(R.string.mesh_message_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val selectedPeerForSend = state.peers.firstOrNull { it.meshId == targetId }
                    val requiresIdentityVerification = chatTab == MeshChatTab.DIRECT &&
                        !selectedPeerForSend?.mayasUid.isNullOrBlank() && selectedPeerForSend?.identityVerified != true
                    val canSendDirect = (chatTab == MeshChatTab.BROADCAST || targetId.isNotBlank()) && !requiresIdentityVerification
                    Button(
                        enabled = state.isActive && state.peers.isNotEmpty() && messageDraft.isNotBlank() && canSendDirect,
                        onClick = {
                            val dst = if (chatTab == MeshChatTab.BROADCAST) "" else targetId
                            val directPeer = state.peers.firstOrNull { it.meshId == targetId }
                            val sent = if (chatTab == MeshChatTab.DIRECT && !directPeer?.mayasUid.isNullOrBlank()) {
                                MeshManager.sendDirectMessage(directPeer!!.mayasUid.orEmpty(), messageDraft)
                            } else {
                                MeshManager.sendMessage(dst, messageDraft)
                            }
                            if (sent) messageDraft = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) { Text(stringResource(R.string.mesh_send)) }
                }
            }

            items(visibleMessages, key = { it.id }) { message ->
                MeshMessageRow(message, timeFormat)
            }
        }
    }
}

private enum class MeshChatTab { BROADCAST, DIRECT }

@Composable
private fun MeshTabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) MayasTheme.Accent else MayasTheme.Surface,
            contentColor = if (selected) Color.White else MayasTheme.TextSecondary
        )
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun MeshCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun MeshInfoRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(label, color = MayasTheme.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = MayasTheme.TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun MeshMessageRow(message: MeshMessage, timeFormat: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (message.outgoing) MayasTheme.Accent.copy(alpha = 0.15f) else MayasTheme.Surface)
            .padding(12.dp)
    ) {
        Text(
            stringResource(
                R.string.mesh_message_meta,
                if (message.outgoing) stringResource(R.string.mesh_me) else message.srcName,
                timeFormat.format(Date(message.timestamp)),
                message.hops
            ),
            color = Color(0xFFFFC107),
            fontSize = 11.sp
        )
        Text(message.text, color = MayasTheme.TextPrimary, fontSize = 14.sp)
        if (message.outgoing && message.privateChat) {
            val deliveryText = when {
                message.deliveryFailed -> stringResource(R.string.mesh_message_failed)
                message.delivered -> stringResource(R.string.mesh_message_delivered)
                else -> stringResource(R.string.mesh_message_pending)
            }
            Text(
                text = deliveryText,
                color = if (message.deliveryFailed) MayasTheme.ErrorRed else MayasTheme.TextSecondary,
                fontSize = 11.sp
            )
        }
        if (message.outgoing && message.privateChat) {
            val deliveryText = when {
                message.deliveryFailed -> stringResource(R.string.mesh_message_failed)
                message.delivered -> stringResource(R.string.mesh_message_delivered)
                else -> stringResource(R.string.mesh_message_pending)
            }
            Text(
                text = deliveryText,
                color = if (message.deliveryFailed) MayasTheme.ErrorRed else MayasTheme.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}