package com.dan1eidtj.mayas

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.remember


@Composable
fun CallScreen(
    state: CallScreenState,
    onAccept: () -> Unit,
    onDeclineOrEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    if (state !is CallScreenState.Active) return

    val backgroundGradient = MayasTheme.PurpleGradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = backgroundGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallHeader(
                state = state,
                modifier = Modifier.weight(1f)
            )

            if (!state.isEnded) {
                if (state.isIncoming) {
                    IncomingControls(onAccept = onAccept, onDecline = onDeclineOrEnd)
                } else {
                    ActiveCallControls(
                        isMuted = state.isMuted,
                        isSpeakerOn = state.isSpeakerOn,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onEndCall = onDeclineOrEnd
                    )
                }
            }
        }
    }
}

@Composable
private fun CallHeader(
    state: CallScreenState.Active, // Передаем весь стейт для удобства
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Определяем цвет свечения точно так же, как в ChatScreen
    val glowColor = when (state.profileGlow) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "gold" -> MayasTheme.GlowGold
        "red" -> MayasTheme.GlowRed
        else -> MayasTheme.GlowPurple
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Пульсирующий фоновый круг — теперь использует ЛИЧНЫЙ цвет свечения юзера
            Box(
                modifier = Modifier
                    .size(130.dp) // Чуть больше аватара
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.4f))
            )

            // Основной аватар, который используется везде по приложению
            MayasAvatar(
                url = state.avatarUrl,
                icon = state.profileIcon,
                glowColor = glowColor,
                isPremium = state.isPremium,
                useCustomAvatar = state.useCustomAvatar,
                size = 120.dp,
                frameType = state.avatarFrame
            )
        }

        // Рендеринг градиентного или обычного имени, как в ChatScreen
        val titleColor = if (state.isPremium) {
            remember {
                Brush.linearGradient(colors = listOf(MayasTheme.GlowGold, Color(0xFFFFE082)))
            }
        } else null

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (titleColor != null) {
                Text(
                    text = state.peerId,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(brush = titleColor)
                )
            } else {
                Text(
                    text = state.peerId,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MayasTheme.TextPrimary
                )
            }

            if (state.isPremium) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Verified, // Или кастомная иконка state.verifiedIcon
                    contentDescription = "Premium",
                    tint = MayasTheme.GlowGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        val statusTextColor by animateColorAsState(
            targetValue = if (state.isEnded) MayasTheme.Error else MayasTheme.TextSecondary,
            animationSpec = tween(300)
        )

        Text(
            text = if (state.callType == CallType.VIDEO) "Видеозвонок · ${state.statusText}" else state.statusText,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 15.sp,
            color = statusTextColor
        )
    }
}

@Composable
private fun IncomingControls(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CallActionButton(
            icon = Icons.Filled.CallEnd,
            backgroundColor = MayasTheme.Error,
            contentDescription = "Отклонить",
            onClick = onDecline
        )
        CallActionButton(
            icon = Icons.Filled.Call,
            backgroundColor = MayasTheme.Success,
            contentDescription = "Принять",
            onClick = onAccept
        )
    }
}

@Composable
private fun ActiveCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CallToggleButton(
            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            isActive = isMuted,
            contentDescription = "Микрофон",
            onClick = onToggleMute
        )
        CallActionButton(
            icon = Icons.Filled.CallEnd,
            backgroundColor = MayasTheme.Error,
            contentDescription = "Завершить звонок",
            onClick = onEndCall
        )
        CallToggleButton(
            icon = Icons.Filled.VolumeUp,
            isActive = isSpeakerOn,
            contentDescription = "Громкая связь",
            onClick = onToggleSpeaker
        )
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}

@Composable
private fun CallToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val background = if (isActive) MayasTheme.Accent else MayasTheme.SurfaceVariant.copy(alpha = 0.5f)
    val tint = if (isActive) Color.White else MayasTheme.TextPrimary

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        }
    }
}