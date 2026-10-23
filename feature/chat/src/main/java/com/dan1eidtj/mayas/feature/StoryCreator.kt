/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core.ui.components.ImageCropEditorDialog
import com.dan1eidtj.mayas.core.ui.components.ImageEditAspect
import com.dan1eidtj.mayas.core.ui.components.cleanupEditedImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File

private const val STORY_PREFS = "mayas_story_prefs"
private const val KEY_PRIVACY = "default_privacy"
private const val KEY_DURATION_DAYS = "duration_days"

@Composable
fun StoryCreatorDialog(
    myUid: String,
    initialRingColor: String?,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var privacy by remember {
        mutableStateOf(
            runCatching {
                StoryPrivacy.valueOf(
                    context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE)
                        .getString(KEY_PRIVACY, StoryPrivacy.ALL.name) ?: StoryPrivacy.ALL.name
                )
            }.getOrDefault(StoryPrivacy.ALL)
        )
    }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ringColor by remember { mutableStateOf(initialRingColor) }
    var durationDays by remember {
        mutableIntStateOf(
            context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_DURATION_DAYS, 1).let { stored -> if (stored in listOf(1, 2, 3, 7)) stored else 1 }
        )
    }
    var photoToEdit by remember { mutableStateOf<Uri?>(null) }
    var publishing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }
    var pendingCaptureIsVideo by remember { mutableStateOf(false) }

    val tooLongText = stringResource(R.string.story_video_too_long)
    val publishFailedText = stringResource(R.string.story_publish_failed)

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val selectedIsVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
            if (selectedIsVideo) {
                mediaUri = uri
                isVideo = true
            } else {
                photoToEdit = uri
            }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            pendingCapture?.let { photoToEdit = it }
            isVideo = false
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (ok) {
            mediaUri = pendingCapture
            isVideo = true
        }
    }

    fun launchCapture(video: Boolean) {
        val dir = File(context.cacheDir, "story_capture").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.${if (video) "mp4" else "jpg"}")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCapture = uri
        pendingCaptureIsVideo = video
        if (video) videoLauncher.launch(uri) else photoLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCapture(pendingCaptureIsVideo)
    }

    fun requestCapture(video: Boolean) {
        pendingCaptureIsVideo = video
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCapture(video)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!publishing) {
                mediaUri?.let { cleanupEditedImage(context, it) }
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val uri = mediaUri
            if (uri == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.story_create_title),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(28.dp))
                    CreatorOption(Icons.Default.PhotoCamera, stringResource(R.string.story_option_photo)) { requestCapture(false) }
                    Spacer(Modifier.height(12.dp))
                    CreatorOption(Icons.Default.Videocam, stringResource(R.string.story_option_video)) { requestCapture(true) }
                    Spacer(Modifier.height(12.dp))
                    CreatorOption(Icons.Default.PhotoLibrary, stringResource(R.string.story_option_gallery)) {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = Color.White.copy(alpha = 0.7f))
                    }
                }
            } else {
                if (isVideo) {
                    StoryPreviewVideo(uri)
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = {
                        if (!publishing) {
                            mediaUri?.let { cleanupEditedImage(context, it) }
                            mediaUri = null
                        }
                    },
                    modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (caption.isEmpty()) {
                            Text(stringResource(R.string.story_caption_hint), color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = caption,
                            onValueChange = { caption = it.take(StoryRepository.MAX_CAPTION) },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.story_privacy_title), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrivacyChip(stringResource(R.string.story_privacy_all), privacy == StoryPrivacy.ALL) { privacy = StoryPrivacy.ALL }
                        PrivacyChip(stringResource(R.string.story_privacy_contacts), privacy == StoryPrivacy.CONTACTS) { privacy = StoryPrivacy.CONTACTS }
                        PrivacyChip(stringResource(R.string.story_privacy_selected), privacy == StoryPrivacy.SELECTED) { privacy = StoryPrivacy.SELECTED }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.story_duration_title), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 7).forEach { days ->
                            val label = when (days) {
                                1 -> stringResource(R.string.story_duration_1)
                                2 -> stringResource(R.string.story_duration_2)
                                3 -> stringResource(R.string.story_duration_3)
                                else -> stringResource(R.string.story_duration_7)
                            }
                            PrivacyChip(label, durationDays == days) { durationDays = days }
                        }
                    }

                    if (privacy == StoryPrivacy.SELECTED) {
                        val contacts by produceState<List<StoryContact>?>(initialValue = null, myUid) {
                            value = StoryRepository.loadContacts(myUid)
                        }
                        Spacer(Modifier.height(8.dp))
                        val list = contacts
                        when {
                            list == null -> CircularProgressIndicator(
                                modifier = Modifier.size(22.dp).align(Alignment.CenterHorizontally),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            list.isEmpty() -> Text(
                                stringResource(R.string.story_no_contacts),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            else -> LazyColumn(modifier = Modifier.height(150.dp)) {
                                items(list, key = { it.uid }) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selected = if (contact.uid in selected) selected - contact.uid else selected + contact.uid
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = contact.uid in selected,
                                            onCheckedChange = {
                                                selected = if (it) selected + contact.uid else selected - contact.uid
                                            }
                                        )
                                        Text(contact.name, color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.story_ring_color), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    StoryRingColorPicker(
                        current = ringColor,
                        onPick = { hex ->
                            ringColor = hex
                            FirebaseFirestore.getInstance().collection("users").document(myUid)
                                .update("storyRingColor", hex ?: FieldValue.delete())
                        }
                    )

                    errorText?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MayasTheme.ErrorRed, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        enabled = !publishing && (privacy != StoryPrivacy.SELECTED || selected.isNotEmpty()),
                        onClick = {
                            publishing = true
                            errorText = null
                            context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE)
                                .edit().putString(KEY_PRIVACY, privacy.name).putInt(KEY_DURATION_DAYS, durationDays).apply()
                            scope.launch {
                                runCatching {
                                    StoryRepository.publish(context, uri, isVideo, caption, privacy, selected, durationDays)
                                }.onSuccess {
                                    publishing = false
                                    cleanupEditedImage(context, uri)
                                    onPublished()
                                }.onFailure { e ->
                                    publishing = false
                                    errorText = if (e.message == "video_too_long") tooLongText else publishFailedText
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (publishing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.story_publish))
                        }
                    }
                }
            }
        }
    }

    photoToEdit?.let { source ->
        ImageCropEditorDialog(
            sourceUri = source,
            aspect = ImageEditAspect.STORY,
            onDismiss = { photoToEdit = null },
            onApply = { edited ->
                mediaUri = edited
                isVideo = false
                photoToEdit = null
            }
        )
    }
}

@Composable
private fun CreatorOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun PrivacyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MayasTheme.Accent else Color.White.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun StoryPreviewVideo(uri: Uri) {
    val ctx = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
