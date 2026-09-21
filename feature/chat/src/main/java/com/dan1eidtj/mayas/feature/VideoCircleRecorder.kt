/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Rational
import android.view.Surface
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dan1eidtj.chat.R
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_CIRCLE_DURATION_MS = 60_000L
private const val MIN_CIRCLE_DURATION_MS = 1_000L

private val RecordRed = Color(0xFFFF3B30)
private val ControlGlass = Color(0x33FFFFFF)

private enum class CirclePhase { READY, RECORDING, PAUSED, PREVIEW }

private class RecordingSession(val file: File) {
    @Volatile
    var discard = false
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000L).toInt()
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@OptIn(UnstableApi::class)
@SuppressLint("MissingPermission")
@Composable
fun VideoCircleRecorderDialog(
    onSend: (File, Int) -> Unit,
    onDismiss: () -> Unit,
    autoStart: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val hostView = LocalView.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var phase by remember { mutableStateOf(CirclePhase.READY) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var recordedMs by remember { mutableLongStateOf(0L) }
    var useFront by rememberSaveable { mutableStateOf(true) }
    var torchOn by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var canFlip by remember { mutableStateOf(false) }
    var isFinalizing by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var handedOff by remember { mutableStateOf(false) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var session by remember { mutableStateOf<RecordingSession?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }

    val hasPermissions = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            Toast.makeText(context, context.getString(R.string.circle_permissions_required), Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    DisposableEffect(hostView) {
        val previous = hostView.keepScreenOn
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = previous }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = runCatching { future.get() }.getOrNull()
        }, mainExecutor)
    }

    val cameraActive = phase != CirclePhase.PREVIEW

    LaunchedEffect(cameraProvider, previewView, useFront, cameraActive) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect

        if (!cameraActive) {
            provider.unbindAll()
            camera = null
            videoCapture = null
            return@LaunchedEffect
        }

        val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        canFlip = hasFront && hasBack

        val wantFront = when {
            useFront && hasFront -> true
            !useFront && hasBack -> false
            hasFront -> true
            else -> false
        }
        if (wantFront != useFront) {
            useFront = wantFront
            return@LaunchedEffect
        }

        try {
            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                )
                .build()
            val capture = VideoCapture.withOutput(recorder)
            val rotation = view.display?.rotation ?: Surface.ROTATION_0
            val viewPort = ViewPort.Builder(Rational(1, 1), rotation)
                .setScaleType(ViewPort.FILL_CENTER)
                .build()
            val group = UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(capture)
                .build()

            provider.unbindAll()
            val bound = provider.bindToLifecycle(lifecycleOwner, selector, group)
            camera = bound
            videoCapture = capture
            hasFlash = bound.cameraInfo.hasFlashUnit()
            torchOn = false
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.circle_camera_failed), Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn && hasFlash)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                activeRecording?.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            session?.discard = true
            activeRecording?.stop()
            if (!handedOff) recordedFile?.delete()
            cameraProvider?.unbindAll()
        }
    }

    fun resetToReady() {
        session = null
        elapsedMs = 0L
        phase = CirclePhase.READY
    }

    fun startRecording() {
        val capture = videoCapture ?: return
        if (isFinalizing) return
        val file = File(context.cacheDir, "circle_${System.currentTimeMillis()}.mp4")
        val newSession = RecordingSession(file)
        val options = FileOutputOptions.Builder(file)
            .setDurationLimitMillis(MAX_CIRCLE_DURATION_MS)
            .build()

        try {
            val pending = capture.output.prepareRecording(context, options).withAudioEnabled()
            session = newSession
            elapsedMs = 0L
            activeRecording = pending.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Status -> {
                        elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                    }
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        isFinalizing = false
                        val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                        val hardError = event.hasError() &&
                            event.error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED &&
                            event.error != VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
                        val stillCurrent = session === newSession

                        when {
                            newSession.discard -> {
                                newSession.file.delete()
                            }
                            hardError || !newSession.file.exists() || newSession.file.length() == 0L -> {
                                newSession.file.delete()
                                Toast.makeText(context, context.getString(R.string.circle_record_failed), Toast.LENGTH_SHORT).show()
                                if (stillCurrent) resetToReady()
                            }
                            durationMs < MIN_CIRCLE_DURATION_MS -> {
                                newSession.file.delete()
                                Toast.makeText(context, context.getString(R.string.circle_too_short), Toast.LENGTH_SHORT).show()
                                if (stillCurrent) resetToReady()
                            }
                            stillCurrent -> {
                                recordedFile = newSession.file
                                recordedMs = durationMs
                                session = null
                                phase = CirclePhase.PREVIEW
                            }
                            else -> {
                                newSession.file.delete()
                            }
                        }
                    }
                    else -> Unit
                }
            }
            phase = CirclePhase.RECORDING
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            newSession.file.delete()
            session = null
            Toast.makeText(context, context.getString(R.string.circle_record_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun pauseRecording() {
        activeRecording?.pause()
        phase = CirclePhase.PAUSED
    }

    fun resumeRecording() {
        activeRecording?.resume()
        phase = CirclePhase.RECORDING
    }

    fun finishRecording() {
        if (activeRecording == null || isFinalizing) return
        isFinalizing = true
        activeRecording?.stop()
    }

    fun discardRecording() {
        session?.discard = true
        activeRecording?.stop()
        resetToReady()
    }

    fun rerecord() {
        recordedFile?.delete()
        recordedFile = null
        recordedMs = 0L
        resetToReady()
    }

    fun requestClose() {
        if (phase == CirclePhase.READY) {
            onDismiss()
        } else {
            confirmDiscard = true
        }
    }

    fun sendRecording() {
        val file = recordedFile ?: return
        val seconds = max(1, (recordedMs / 1000.0).roundToInt())
        handedOff = true
        onSend(file, seconds)
        onDismiss()
    }

    var autoStartPending by remember { mutableStateOf(autoStart) }
    LaunchedEffect(videoCapture, phase) {
        if (autoStartPending && videoCapture != null && phase == CirclePhase.READY) {
            autoStartPending = false
            startRecording()
        }
    }

    val circleSize: Dp = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        min(min(configuration.screenWidthDp - 48, configuration.screenHeightDp - 360), 340).coerceAtLeast(200).dp
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2101014))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RecorderIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.circle_close),
                        onClick = { requestClose() },
                        container = ControlGlass,
                        size = 44.dp
                    )

                    TimerChip(
                        text = formatElapsed(if (phase == CirclePhase.PREVIEW) recordedMs else elapsedMs),
                        recording = phase == CirclePhase.RECORDING,
                        visible = phase != CirclePhase.READY
                    )

                    if (hasFlash && phase != CirclePhase.PREVIEW) {
                        RecorderIconButton(
                            icon = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = stringResource(
                                if (torchOn) R.string.circle_flash_off else R.string.circle_flash_on
                            ),
                            onClick = { torchOn = !torchOn },
                            container = if (torchOn) Color(0xFFFFC107) else ControlGlass,
                            tint = if (torchOn) Color.Black else Color.White,
                            size = 44.dp
                        )
                    } else {
                        Spacer(Modifier.size(44.dp))
                    }
                }

                Box(
                    modifier = Modifier.size(circleSize + 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressTarget = (if (phase == CirclePhase.PREVIEW) recordedMs else elapsedMs)
                        .toFloat() / MAX_CIRCLE_DURATION_MS
                    val progress by animateFloatAsState(
                        targetValue = progressTarget.coerceIn(0f, 1f),
                        animationSpec = tween(250),
                        label = "circleProgress"
                    )

                    Canvas(modifier = Modifier.size(circleSize + 16.dp)) {
                        val stroke = 4.dp.toPx()
                        val inset = stroke / 2f
                        drawArc(
                            color = ControlGlass,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke)
                        )
                        if (progress > 0f) {
                            drawArc(
                                color = RecordRed,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    if (phase == CirclePhase.PREVIEW) {
                        val file = recordedFile
                        if (file != null) {
                            CirclePreviewPlayer(
                                file = file,
                                modifier = Modifier.size(circleSize)
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                            },
                            update = { view ->
                                if (previewView !== view) previewView = view
                            },
                            modifier = Modifier
                                .size(circleSize)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hintRes = when (phase) {
                        CirclePhase.READY -> R.string.circle_hint_ready
                        CirclePhase.RECORDING -> R.string.circle_hint_recording
                        CirclePhase.PAUSED -> R.string.circle_hint_paused
                        CirclePhase.PREVIEW -> R.string.circle_hint_preview
                    }
                    AnimatedContent(
                        targetState = hintRes,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                        label = "circleHint"
                    ) { res ->
                        Text(
                            text = stringResource(res),
                            color = Color(0xB3FFFFFF),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.size(20.dp))

                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.92f))
                        },
                        label = "circleControls"
                    ) { current ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (current) {
                                CirclePhase.READY -> {
                                    Spacer(Modifier.size(52.dp))
                                    RecordButton(
                                        contentDescription = stringResource(R.string.circle_record),
                                        onClick = { startRecording() },
                                        enabled = videoCapture != null
                                    )
                                    if (canFlip) {
                                        RecorderIconButton(
                                            icon = Icons.Filled.FlipCameraAndroid,
                                            contentDescription = stringResource(R.string.circle_flip_camera),
                                            onClick = { useFront = !useFront },
                                            container = ControlGlass,
                                            size = 52.dp
                                        )
                                    } else {
                                        Spacer(Modifier.size(52.dp))
                                    }
                                }

                                CirclePhase.RECORDING, CirclePhase.PAUSED -> {
                                    RecorderIconButton(
                                        icon = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.circle_cancel),
                                        onClick = { confirmDiscard = true },
                                        container = ControlGlass,
                                        size = 52.dp
                                    )
                                    RecorderIconButton(
                                        icon = Icons.Filled.Stop,
                                        contentDescription = stringResource(R.string.circle_stop),
                                        onClick = { finishRecording() },
                                        container = RecordRed,
                                        size = 72.dp,
                                        iconSize = 34.dp
                                    )
                                    if (current == CirclePhase.RECORDING) {
                                        RecorderIconButton(
                                            icon = Icons.Filled.Pause,
                                            contentDescription = stringResource(R.string.circle_pause),
                                            onClick = { pauseRecording() },
                                            container = ControlGlass,
                                            size = 52.dp
                                        )
                                    } else {
                                        RecorderIconButton(
                                            icon = Icons.Filled.PlayArrow,
                                            contentDescription = stringResource(R.string.circle_resume),
                                            onClick = { resumeRecording() },
                                            container = ControlGlass,
                                            size = 52.dp
                                        )
                                    }
                                }

                                CirclePhase.PREVIEW -> {
                                    RecorderIconButton(
                                        icon = Icons.Filled.Refresh,
                                        contentDescription = stringResource(R.string.circle_rerecord),
                                        onClick = { rerecord() },
                                        container = ControlGlass,
                                        size = 52.dp
                                    )
                                    RecorderIconButton(
                                        icon = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.circle_send),
                                        onClick = { sendRecording() },
                                        container = Color(0xFF2AABEE),
                                        size = 72.dp,
                                        iconSize = 30.dp
                                    )
                                    RecorderIconButton(
                                        icon = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.circle_cancel),
                                        onClick = { confirmDiscard = true },
                                        container = ControlGlass,
                                        size = 52.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.circle_discard_title)) },
            text = { Text(stringResource(R.string.circle_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    when (phase) {
                        CirclePhase.RECORDING, CirclePhase.PAUSED -> discardRecording()
                        CirclePhase.PREVIEW -> {
                            recordedFile?.delete()
                            recordedFile = null
                        }
                        CirclePhase.READY -> Unit
                    }
                    onDismiss()
                }) {
                    Text(stringResource(R.string.circle_discard_confirm), color = RecordRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.circle_discard_keep))
                }
            }
        )
    }
}

@Composable
private fun RecorderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    container: Color,
    size: Dp,
    tint: Color = Color.White,
    iconSize: Dp = 24.dp
) {
    val animatedContainer by animateColorAsState(container, tween(150), label = "recorderButtonColor")
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(animatedContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun RecordButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.9f, tween(150), label = "recordButtonScale")
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(BorderStroke(4.dp, Color.White), CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (enabled) RecordRed else RecordRed.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun TimerChip(
    text: String,
    recording: Boolean,
    visible: Boolean
) {
    val transition = rememberInfiniteTransition(label = "recordDot")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recordDotAlpha"
    )
    val chipAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(180), label = "timerChipAlpha")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.35f * chipAlpha))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (visible) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(RecordRed.copy(alpha = if (recording) dotAlpha else 1f))
            )
            Spacer(Modifier.width(8.dp))
            Text(text = text, color = Color.White, fontSize = 15.sp)
        } else {
            Spacer(Modifier.size(width = 1.dp, height = 18.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun CirclePreviewPlayer(
    file: File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlaying by remember(file) { mutableStateOf(true) }

    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (player.isPlaying) player.pause() else player.play()
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            update = { view -> if (view.player !== player) view.player = player },
            modifier = Modifier.fillMaxSize()
        )

        val overlayAlpha by animateFloatAsState(if (isPlaying) 0f else 1f, tween(150), label = "previewPauseAlpha")
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f * overlayAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = overlayAlpha),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
