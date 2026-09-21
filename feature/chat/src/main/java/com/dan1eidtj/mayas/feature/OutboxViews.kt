/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.db.OutboxEntity
import org.json.JSONArray
import java.io.File
import java.util.Locale

private fun firstPath(item: OutboxEntity): String? =
    try {
        val array = JSONArray(item.filesJson)
        if (array.length() > 0) array.getJSONObject(0).getString("path") else null
    } catch (e: Exception) {
        null
    }

private fun fileCount(item: OutboxEntity): Int =
    try {
        JSONArray(item.filesJson).length()
    } catch (e: Exception) {
        0
    }

@Composable
fun OutboxBubble(
    item: OutboxEntity,
    bubbleColor: Color,
    textColor: Color,
    secondaryColor: Color,
    errorColor: Color,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val failed = item.state == OutboxEntity.STATE_FAILED
    val statusText = when (item.state) {
        OutboxEntity.STATE_FAILED -> stringResource(R.string.outbox_failed)
        OutboxEntity.STATE_SENDING -> stringResource(R.string.outbox_sending)
        else -> stringResource(R.string.outbox_waiting_network)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor.copy(alpha = 0.75f))
                .clickable(enabled = failed, onClick = onRetry)
                .padding(10.dp)
        ) {
            when (item.kind) {
                OutboxEntity.KIND_IMAGE, OutboxEntity.KIND_ALBUM -> {
                    val path = firstPath(item)
                    Box {
                        if (path != null && item.kind == OutboxEntity.KIND_IMAGE) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = stringResource(R.string.outbox_photo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text(
                                text = "${stringResource(R.string.outbox_album)} · ${fileCount(item)}",
                                color = textColor,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                OutboxEntity.KIND_CIRCLE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(secondaryColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Videocam, null, tint = textColor)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${stringResource(R.string.outbox_circle)} · ${formatOutboxDuration(item.durationSec)}",
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }

                OutboxEntity.KIND_VOICE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Mic, null, tint = textColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.outbox_voice)} · ${formatOutboxDuration(item.durationSec)}",
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            val caption = item.caption
            if (!caption.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                MayasTextForOutbox(caption, textColor)
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (failed) Icons.Filled.ErrorOutline else Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = if (failed) errorColor else secondaryColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                val errorText = item.lastError
                Text(
                    text = if (!errorText.isNullOrBlank() && item.state != OutboxEntity.STATE_SENDING) "$statusText: $errorText" else statusText,
                    color = if (failed) errorColor else secondaryColor,
                    fontSize = 11.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.outbox_cancel),
                    tint = secondaryColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCancel)
                )
            }
        }
    }
}

@Composable
private fun MayasTextForOutbox(text: String, color: Color) {
    com.dan1eidtj.mayas.core_ui.emoji.MayasText(text = text, color = color, fontSize = 15.sp)
}

private fun formatOutboxDuration(seconds: Int): String =
    String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
