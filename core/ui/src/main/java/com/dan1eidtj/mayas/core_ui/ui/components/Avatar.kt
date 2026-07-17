package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

@Composable
fun MayasAvatar(
    url: String?,
    icon: String,
    glowColor: Color,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    useCustomAvatar: Boolean = true,
    frameType: String = "rainbow"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PremiumGlow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    val frameBrush = when (frameType) {
        "gold" -> Brush.sweepGradient(MayasTheme.FrameGold)
        "neon" -> Brush.sweepGradient(MayasTheme.FrameNeon)
        "fire" -> Brush.sweepGradient(MayasTheme.FrameFire)
        "black" -> Brush.sweepGradient(MayasTheme.FrameBlack)
        else -> Brush.sweepGradient(MayasTheme.FrameDefault)
    }

    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isPremium && frameType != "none") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (frameType == "black") {
                            drawCircle(
                                color = MayasTheme.FrameBlackHalo,
                                radius = (size.toPx() / 2) - 2.dp.toPx(),
                                style = Stroke(width = 5.dp.toPx())
                            )
                        }
                        drawCircle(
                            brush = frameBrush,
                            radius = (size.toPx() / 2) - 2.dp.toPx(),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawContent()
                    }
                    .graphicsLayer { rotationZ = angle }
            )
        }
        Box(
            modifier = Modifier
                .size(if (isPremium && frameType != "none") size - 12.dp else size)
                .clip(CircleShape)
                .then(
                    if (useCustomAvatar && !url.isNullOrBlank()) {
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    glowColor.copy(alpha = 0.8f),
                                    Color(0xFF1E1F22)
                                )
                            )
                        )
                    }
                )
                .then(
                    if (!isPremium) Modifier.border(2.5.dp, glowColor, CircleShape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (useCustomAvatar && !url.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .listener(
                            onError = { _, result ->
                                android.util.Log.e(
                                    "MayasAvatar",
                                    "Coil не смог загрузить аватар по url=$url",
                                    result.throwable
                                )
                            }
                        )
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                ProfileIcon(icon = icon, size = if (size < 60.dp) 24.dp else 58.dp)
            }
        }
    }
}