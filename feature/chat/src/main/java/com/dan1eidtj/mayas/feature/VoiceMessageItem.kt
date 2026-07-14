package com.dan1eidtj.mayas.feature

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    progress: Float,
    accentColor: Color
) {
    val barCount = 32
    val heights = remember { List(barCount) { Random.nextFloat() * 0.8f + 0.2f } }
    

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedScales = List(barCount) { index ->
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500 + (index * 50) % 300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            ).value
        } else {
            1f
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val spaceWidth = barWidth / 2

        heights.forEachIndexed { index, heightMultiplier ->
            val barHeight = size.height * heightMultiplier * (if (isPlaying) animatedScales[index] else 1f)
            val x = index * (barWidth + spaceWidth)
            val y = (size.height - barHeight) / 2
            
            val barColor = if (index.toFloat() / barCount <= progress) {
                accentColor
            } else {
                accentColor.copy(alpha = 0.3f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
fun VoiceMessageItem(
    url: String,
    duration: Int,
    isMe: Boolean,
    accentColor: Color,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit
) {

    val formattedDuration = remember(duration) {
        val mins = duration / 60
        val secs = duration % 60
        String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val formattedPosition = remember(progress, duration) {
        val totalSecs = (progress * duration).toInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f))
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            WaveformVisualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                isPlaying = isPlaying,
                progress = progress,
                accentColor = accentColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isPlaying) "$formattedPosition / $formattedDuration" else formattedDuration,
                fontSize = 11.sp,
                color = if (isMe) Color.White.copy(0.7f) else Color.Gray
            )
        }
    }
}
