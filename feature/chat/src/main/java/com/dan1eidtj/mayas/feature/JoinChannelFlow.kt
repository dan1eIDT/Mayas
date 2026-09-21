/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import kotlinx.coroutines.delay


data class InvitePreview(
    val chatId: String,
    val name: String,
    val avatarUrl: String?,
    val icon: String,
    val glowColor: String,
    val membersCount: Int,
    val isChannel: Boolean,
)


sealed class JoinFlowState {
    object Loading : JoinFlowState()
    data class Confirm(val preview: InvitePreview) : JoinFlowState()
    object Joining : JoinFlowState()
    object Success : JoinFlowState()
    data class Error(val message: String) : JoinFlowState()
}


@Composable
fun JoinInviteFlow(
    inviteCode: String,
    onLoadPreview: (code: String, onResult: (InvitePreview?) -> Unit) -> Unit,
    onConfirmJoin: (code: String, onResult: (chatId: String?) -> Unit) -> Unit,
    onFinished: (chatId: String, isChannel: Boolean) -> Unit,
    onCancelled: () -> Unit,
) {
    var state by remember { mutableStateOf<JoinFlowState>(JoinFlowState.Loading) }


    var confirmedPreview by remember { mutableStateOf<InvitePreview?>(null) }
    var joinedChatId by remember { mutableStateOf<String?>(null) }

    val linkInvalidMessage = stringResource(R.string.link_invalid)
    val joinFailedMessage = stringResource(R.string.error_join_failed)

    LaunchedEffect(inviteCode) {
        onLoadPreview(inviteCode) { preview ->
            state = if (preview != null) {
                JoinFlowState.Confirm(preview)
            } else {
                JoinFlowState.Error(linkInvalidMessage)
            }
        }
    }

    when (val s = state) {
        is JoinFlowState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MayasTheme.Accent)
            }
        }

        is JoinFlowState.Confirm -> {
            JoinConfirmDialog(
                preview = s.preview,
                onConfirm = {
                    confirmedPreview = s.preview
                    state = JoinFlowState.Joining
                    onConfirmJoin(inviteCode) { chatId ->
                        if (chatId != null) {
                            joinedChatId = chatId
                            state = JoinFlowState.Success
                        } else {
                            state = JoinFlowState.Error(joinFailedMessage)
                        }
                    }
                },
                onDismiss = onCancelled,
            )
        }

        is JoinFlowState.Joining -> {
            JoinLoadingOverlay(text = stringResource(R.string.joining))
        }

        is JoinFlowState.Success -> {
            JoinSuccessAnimation(
                onAnimationEnd = {
                    val chatId = joinedChatId
                    val isChannel = confirmedPreview?.isChannel ?: false
                    if (chatId != null) {
                        onFinished(chatId, isChannel)
                    } else {
                        onCancelled()
                    }
                }
            )
        }

        is JoinFlowState.Error -> {
            JoinErrorDialog(message = s.message, onDismiss = onCancelled)
        }
    }
}

@Composable
private fun JoinConfirmDialog(
    preview: InvitePreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val glowColor = getGlowColor(preview.glowColor)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MayasAvatar(
                    url = preview.avatarUrl ?: "",
                    icon = preview.icon,
                    glowColor = glowColor,
                    isPremium = false,
                    useCustomAvatar = !preview.avatarUrl.isNullOrBlank(),
                    size = 72.dp,
                    frameType = "none",
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    preview.name,
                    color = MayasTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (preview.isChannel) stringResource(R.string.channel_members_count, preview.membersCount)
                    else stringResource(R.string.group_members_count, preview.membersCount),
                    color = MayasTheme.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (preview.isChannel) stringResource(R.string.join_channel_question)
                    else stringResource(R.string.join_group_question),
                    color = MayasTheme.TextPrimary,
                    fontSize = 15.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(com.dan1eidtj.mayas.ui.R.string.yes), color = MayasTheme.Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.dan1eidtj.mayas.ui.R.string.no), color = MayasTheme.TextSecondary)
            }
        },
    )
}

@Composable
private fun JoinErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.failed), color = MayasTheme.TextPrimary) },
        text = { Text(message, color = MayasTheme.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.ok), color = MayasTheme.Accent) }
        },
    )
}

@Composable
private fun JoinLoadingOverlay(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MayasTheme.Background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MayasTheme.Accent)
            Spacer(Modifier.height(12.dp))
            Text(text, color = MayasTheme.TextSecondary, fontSize = 14.sp)
        }
    }
}


@Composable
fun JoinSuccessAnimation(onAnimationEnd: () -> Unit) {
    val scale = remember { Animatable(0f) }
    var showCheck by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
        showCheck = true
        delay(650)
        onAnimationEnd()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MayasTheme.Background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MayasTheme.Accent)
                .padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = showCheck,
                enter = scaleIn(
                    animationSpec = tween(220, easing = LinearOutSlowInEasing),
                    initialScale = 0.3f,
                ) + fadeIn(tween(150)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150)),
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
