/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dan1eidtj.chat.R
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.abs

enum class RecordMode { VOICE, VIDEO }

enum class RecordRelease { SEND, CANCEL, LOCK }

object RecordModePrefs {
    private const val FILE = "mayas_chat_input"
    private const val KEY = "record_mode"

    fun load(context: Context): RecordMode {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return if (stored == RecordMode.VIDEO.name) RecordMode.VIDEO else RecordMode.VOICE
    }

    fun save(context: Context, mode: RecordMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }
}

private const val CANCEL_DISTANCE_DP = 96
private const val LOCK_DISTANCE_DP = 72

@Composable
fun RecordModeButton(
    mode: RecordMode,
    recording: Boolean,
    idleTint: Color,
    activeColor: Color,
    onToggleMode: () -> Unit,
    onHoldStart: () -> Boolean,
    onHoldMove: (Float, Float) -> Unit,
    onHoldRelease: (RecordRelease) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMode by rememberUpdatedState(mode)
    val currentToggle by rememberUpdatedState(onToggleMode)
    val currentStart by rememberUpdatedState(onHoldStart)
    val currentMove by rememberUpdatedState(onHoldMove)
    val currentRelease by rememberUpdatedState(onHoldRelease)
    val scale by animateFloatAsState(if (recording) 1.45f else 1f, tween(160), label = "recordButtonScale")
    val container by animateFloatAsState(if (recording) 1f else 0f, tween(160), label = "recordButtonContainer")

    val description = stringResource(
        if (mode == RecordMode.VOICE) R.string.record_switch_to_video else R.string.record_switch_to_voice
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                val cancelDistance = CANCEL_DISTANCE_DP.dp.toPx()
                val lockDistance = LOCK_DISTANCE_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var released = false
                    val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        val change = waitForUpOrCancellation()
                        released = true
                        change
                    }
                    if (released) {
                        if (up != null) {
                            up.consume()
                            currentToggle()
                        }
                        return@awaitEachGesture
                    }

                    val started = currentStart()
                    if (!started || currentMode == RecordMode.VIDEO) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    var result = RecordRelease.SEND
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            result = RecordRelease.CANCEL
                            break
                        }
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        currentMove(dx, dy)
                        change.consume()
                        if (dx < -cancelDistance) {
                            result = RecordRelease.CANCEL
                            break
                        }
                        if (dy < -lockDistance) {
                            result = RecordRelease.LOCK
                            break
                        }
                    }
                    currentRelease(result)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(activeColor.copy(alpha = 0.9f * container)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.6f))
                },
                label = "recordModeIcon"
            ) { current ->
                Icon(
                    imageVector = if (current == RecordMode.VOICE) Icons.Filled.Mic else Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = if (recording) Color.White else idleTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (recording) {
            LockHint()
        }
    }
}

@Composable
private fun LockHint() {
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -260),
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xCC1C1C1E))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun VoiceRecordingBar(
    seconds: Int,
    locked: Boolean,
    slideDx: Float,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    accent: Color,
    textColor: Color,
    hintColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voiceDot")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "voiceDotAlpha"
    )
    val slideAlpha = (1f - (abs(slideDx) / 260f)).coerceIn(0.15f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = dotAlpha))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60),
            color = textColor,
            fontSize = 16.sp
        )
        Spacer(Modifier.weight(1f))

        if (locked) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.voice_cancel), color = accent)
            }
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.voice_send),
                    tint = accent
                )
            }
        } else {
            Row(
                modifier = Modifier.graphicsLayer { alpha = slideAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = hintColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.voice_slide_to_cancel),
                    color = hintColor,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(56.dp))
        }
    }
}
