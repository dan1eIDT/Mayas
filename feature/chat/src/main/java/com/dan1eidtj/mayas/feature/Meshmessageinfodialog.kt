/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.mesh.MeshMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MeshMessageInfoDialog(
    message: MeshMessage,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    val hops = message.hops.coerceAtLeast(1)
    val routeText = if (hops <= 1) {
        stringResource(R.string.mesh_info_route_direct)
    } else {
        stringResource(R.string.mesh_info_route_relayed, hops - 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        title = {
            Text(
                text = stringResource(R.string.mesh_info_title),
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                MeshInfoLine(
                    stringResource(R.string.mesh_info_transport),
                    stringResource(R.string.mesh_info_transport_value),
                    MayasTheme.GlowGold
                )
                MeshInfoLine(stringResource(R.string.mesh_info_hops), hops.toString())
                MeshInfoLine(stringResource(R.string.mesh_info_route), routeText)
                MeshInfoLine(
                    stringResource(R.string.mesh_info_direction),
                    stringResource(if (message.outgoing) R.string.mesh_info_outgoing else R.string.mesh_info_incoming)
                )
                MeshInfoLine(
                    stringResource(R.string.mesh_info_from),
                    if (message.srcName.isBlank()) message.srcId else "${message.srcName} (${message.srcId})"
                )
                MeshInfoLine(stringResource(R.string.mesh_info_to), message.dst)
                MeshInfoLine(
                    stringResource(R.string.mesh_info_identity),
                    stringResource(if (message.identityVerified) R.string.mesh_info_identity_ok else R.string.mesh_info_identity_no),
                    if (message.identityVerified) MayasTheme.GlowGreen else MayasTheme.ErrorRed
                )
                if (message.outgoing) {
                    val statusRes = when {
                        message.deliveryFailed -> R.string.mesh_info_status_failed
                        message.delivered -> R.string.mesh_info_status_delivered
                        else -> R.string.mesh_info_status_sent
                    }
                    MeshInfoLine(
                        stringResource(R.string.mesh_info_status),
                        stringResource(statusRes),
                        when {
                            message.deliveryFailed -> MayasTheme.ErrorRed
                            message.delivered -> MayasTheme.GlowGreen
                            else -> MayasTheme.TextPrimary
                        }
                    )
                }
                MeshInfoLine(stringResource(R.string.mesh_info_sent_at), dateFormat.format(Date(message.timestamp)))
                if (!message.outgoing && message.receivedAt > 0L) {
                    MeshInfoLine(stringResource(R.string.mesh_info_received_at), dateFormat.format(Date(message.receivedAt)))
                }
                if (message.outgoing && message.deliveredAt > 0L) {
                    MeshInfoLine(stringResource(R.string.mesh_info_delivered_at), dateFormat.format(Date(message.deliveredAt)))
                }
                if (message.outgoing && message.attempts > 0) {
                    MeshInfoLine(stringResource(R.string.mesh_info_attempts), message.attempts.toString())
                }
                MeshInfoLine(stringResource(R.string.mesh_info_id), message.id.take(8))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mesh_info_close), color = MayasTheme.GlowPurple)
            }
        }
    )
}

@Composable
private fun MeshInfoLine(
    label: String,
    value: String,
    valueColor: Color = MayasTheme.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MayasTheme.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.42f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.58f)
        )
    }
    Spacer(Modifier.height(1.dp))
}