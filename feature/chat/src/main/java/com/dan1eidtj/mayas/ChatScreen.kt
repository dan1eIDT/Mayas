/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.feature

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.data.ShopConstants
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dan1eidtj.chat.R
import com.dan1eidtj.mayas.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.dan1eidtj.mayas.CallType
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationBadge
import com.dan1eidtj.mayas.core_ui.ui.components.AdminLevelBadge
import com.dan1eidtj.mayas.core_ui.ui.components.BubbleShape
import com.dan1eidtj.mayas.core_ui.ui.components.BubbleType
import com.dan1eidtj.mayas.core_ui.ui.components.FrameStyles
import com.dan1eidtj.mayas.core_ui.ui.components.MessageEffects
import com.dan1eidtj.mayas.core_ui.emoji.EmojiCatalog
import com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyph
import com.dan1eidtj.mayas.core_ui.emoji.MayasClickableText
import com.dan1eidtj.mayas.core_ui.emoji.MayasText
import com.dan1eidtj.mayas.core_ui.ui.components.MessageEffectOverlay
import com.dan1eidtj.mayas.core_ui.ui.components.FullScreenImageViewer
import com.dan1eidtj.mayas.core_ui.ui.components.MessageStyle
import com.dan1eidtj.mayas.core_ui.ui.components.MessageBubbleContainer
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.storage.B2Image
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.MediaFileCache
import com.dan1eidtj.mayas.storage.rememberResolvedAvatarUrl
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.absoluteValue
import kotlin.math.roundToInt



object ChatThemeId {
    const val DEFAULT = "default"
    const val PURPLE = "purple"
    const val BLUE = "blue"
    const val RED = "red"
    const val GOLD = "gold"
    const val PINK = "pink"
}

@Composable
fun rememberParsedMessageText(text: String, accentColor: Color): AnnotatedString {
    return remember(text, accentColor) {
        buildAnnotatedString {
            val finalSb = StringBuilder()
            val spans = mutableListOf<Pair<IntRange, SpanStyle>>()

            val combinedRegex = Pattern.compile("(\\*\\*|__|\\*|_)(.*?)\\1")
            val matcher = combinedRegex.matcher(text)
            var lastEnd = 0
            while (matcher.find()) {
                finalSb.append(text.substring(lastEnd, matcher.start()))
                val marker = matcher.group(1)
                val content = matcher.group(2)
                val start = finalSb.length
                finalSb.append(content)
                val end = finalSb.length

                val style = when (marker) {
                    "**", "__" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "*", "_" -> SpanStyle(fontStyle = FontStyle.Italic)
                    else -> SpanStyle()
                }
                spans.add(IntRange(start, end - 1) to style)
                lastEnd = matcher.end()
            }
            finalSb.append(text.substring(lastEnd))

            val finalString = finalSb.toString()
            append(finalString)

            spans.forEach { (range, style) ->
                addStyle(style, range.first, range.last + 1)
            }

            val urlMatcher = Pattern.compile(MESSAGE_URL_REGEX).matcher(finalString)
            while (urlMatcher.find()) {
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), urlMatcher.start(), urlMatcher.end())
                addStringAnnotation("URL", urlMatcher.group(), urlMatcher.start(), urlMatcher.end())
            }

            val userMatcher = Pattern.compile("@([A-Za-z0-9_]+)").matcher(finalString)
            while (userMatcher.find()) {
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold), userMatcher.start(), userMatcher.end())
                addStringAnnotation("USERNAME", userMatcher.group(1), userMatcher.start(), userMatcher.end())
            }

            val hashtagMatcher = Pattern.compile("#([A-Za-z0-9_А-Яа-я]+)").matcher(finalString)
            while (hashtagMatcher.find()) {
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold), hashtagMatcher.start(), hashtagMatcher.end())
                addStringAnnotation("HASHTAG", hashtagMatcher.group(1), hashtagMatcher.start(), hashtagMatcher.end())
            }
        }
    }
}

private fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_message))
    context.startActivity(shareIntent)
}


@Composable
private fun AlbumCell(
    mediaUrl: String,
    mediaKind: String,
    modifier: Modifier = Modifier,
    overlayCount: Int? = null,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    Box(modifier = modifier) {
        if (mediaKind == MediaKind.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { onVideoClick(mediaUrl) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            B2Image(
                key = mediaUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(mediaUrl) },
                contentScale = ContentScale.Crop
            )
        }
        if (overlayCount != null && overlayCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { if (mediaKind == MediaKind.VIDEO) onVideoClick(mediaUrl) else onImageClick(mediaUrl) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overlayCount",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    mediaUrls: List<String>,
    mediaTypes: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    fun kindOf(index: Int) = mediaTypes.getOrNull(index) ?: MediaKind.IMAGE
    val gap = 3.dp

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        when (mediaUrls.size) {
            1 -> {
                AlbumCell(
                    mediaUrl = mediaUrls[0],
                    mediaKind = kindOf(0),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).height(220.dp),
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick
                )
            }
            2 -> {
                Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (i in 0..1) {
                        AlbumCell(
                            mediaUrl = mediaUrls[i],
                            mediaKind = kindOf(i),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    AlbumCell(
                        mediaUrl = mediaUrls[0],
                        mediaKind = kindOf(0),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick
                    )
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        AlbumCell(
                            mediaUrl = mediaUrls[1],
                            mediaKind = kindOf(1),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                        AlbumCell(
                            mediaUrl = mediaUrls[2],
                            mediaKind = kindOf(2),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
            else -> {
                val visibleCount = 4
                val extra = (mediaUrls.size - visibleCount).coerceAtLeast(0)
                Column(modifier = Modifier.fillMaxWidth().height(240.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        AlbumCell(
                            mediaUrl = mediaUrls[0],
                            mediaKind = kindOf(0),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                        AlbumCell(
                            mediaUrl = mediaUrls[1],
                            mediaKind = kindOf(1),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        AlbumCell(
                            mediaUrl = mediaUrls[2],
                            mediaKind = kindOf(2),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                        AlbumCell(
                            mediaUrl = mediaUrls[3],
                            mediaKind = kindOf(3),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            overlayCount = extra,
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
        }
    }
}

private fun compressImageBytes(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 1600,
    quality: Int = 82
): ByteArray? {
    return try {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimensionPx || bounds.outHeight / sampleSize > maxDimensionPx) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return rawBytes

        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            bitmap.recycle()
            output.toByteArray()
        }
    } catch (e: Exception) {
        Log.e("ChatScreen", "Не удалось сжать изображение", e)
        null
    }
}


@Composable
private fun StatusBadge(
    value: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    iconSize: Dp = fontSize.value.dp
) {
    if (value.isNullOrBlank()) return
    if (ShopConstants.isIconStatus(value)) {
        val context = LocalContext.current
        val resourceName = ShopConstants.iconStatusResourceName(value)
        val resId = remember(resourceName) {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = modifier.size(iconSize)
            )
        }
    } else {
        Text(text = value, fontSize = fontSize, modifier = modifier)
    }
}

private fun systemMessageIcon(message: Message): ImageVector? = when {
    message.type == MessageType.CALL -> when (message.callStatus) {
        CallStatus.MISSED, CallStatus.DECLINED -> Icons.Default.CallMissed
        else -> if (message.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call
    }
    message.systemAction == SystemAction.PINNED -> Icons.Default.PushPin
    message.systemAction == SystemAction.UNPINNED -> Icons.Outlined.PushPin
    message.systemAction == SystemAction.MEMBER_ADDED -> Icons.Default.PersonAdd
    message.systemAction == SystemAction.MEMBER_REMOVED || message.systemAction == SystemAction.MEMBER_LEFT -> Icons.Default.PersonRemove
    message.systemAction == SystemAction.GROUP_CREATED -> Icons.Default.Groups
    message.systemAction == SystemAction.PROMOTED_ADMIN || message.systemAction == SystemAction.PROMOTED_MODERATOR -> Icons.Default.AdminPanelSettings
    message.systemAction == SystemAction.DEMOTED_ADMIN || message.systemAction == SystemAction.DEMOTED_MODERATOR -> Icons.Default.RemoveModerator
    else -> null
}






@Composable
private fun SystemMessageRow(
    message: Message,
    chipColor: Color,
    textColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isMissedCall = message.type == MessageType.CALL &&
            (message.callStatus == CallStatus.MISSED || message.callStatus == CallStatus.DECLINED)
    val contentColor = if (isMissedCall) MayasTheme.GlowRed else textColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(chipColor)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            systemMessageIcon(message)?.let { icon ->
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor.copy(alpha = 0.9f)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = message.text.orEmpty(),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ChannelReadOnlyBar(
    surfaceColor: Color,
    textSecondaryColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
        color = surfaceColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Campaign,
                null,
                tint = textSecondaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.only_admins_can_post),
                color = textSecondaryColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenProfile: (String, Boolean) -> Unit,
    onStartCall: (peerId: String, callType: CallType) -> Unit = { _, _ -> },
    scrollToMessageId: String? = null,
) {
    val chatVM: ChatVM = viewModel()
    val authVM: AuthVM = viewModel()
    LaunchedEffect(chatId) {
        chatVM.clearUnreadCount(chatId)
    }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid
    if (myUid == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val overlayDensity = androidx.compose.ui.platform.LocalDensity.current
    val overlayTopInsetPx = WindowInsets.statusBars.getTop(overlayDensity).toFloat()
    val overlayBottomInsetPx = WindowInsets.navigationBars.getBottom(overlayDensity).toFloat()
    val bubbleBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    val chatBackground = MayasTheme.Background
    val surfaceColor = MayasTheme.Surface
    val textPrimaryColor = MayasTheme.TextPrimary
    val textSecondaryColor = MayasTheme.TextSecondary
    val bubbleMineColor = MayasTheme.BubbleMine
    val bubbleOtherColor = MayasTheme.BubbleOther
    val accentColor = MayasTheme.Accent

    val fontSize = authVM.fontSize

    val chatTheme = chatVM.chatTheme
    val userWallpaper = authVM.userData["wallpaper"] ?: "default"

    val overWallpaperColor = remember(userWallpaper, textPrimaryColor) {
        if (userWallpaper != "default" && userWallpaper != "none") Color.White else textPrimaryColor
    }
    val overWallpaperSecondaryColor = remember(userWallpaper, textSecondaryColor) {
        if (userWallpaper != "default" && userWallpaper != "none") Color.White.copy(0.7f) else textSecondaryColor
    }

    val purpleGradient = MayasTheme.PurpleGradient
    val blueGradient = MayasTheme.BlueGradient
    val redGradient = MayasTheme.RedGradient
    val goldGradient = MayasTheme.GoldGradient
    val pinkGradient = MayasTheme.PinkGradient

    val backgroundBrush = remember(chatTheme, userWallpaper, purpleGradient, blueGradient, redGradient, goldGradient, pinkGradient) {
        if (userWallpaper != "default") {
            when (userWallpaper) {
                "dark_mesh" -> Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1E1E1E)))
                "abstract_blue" -> Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))
                "geometric" -> Brush.sweepGradient(listOf(Color(0xFF212121), Color(0xFF424242)))
                "stars" -> Brush.verticalGradient(listOf(Color(0xFF000011), Color(0xFF000033)))
                else -> null
            }
        } else {
            when (chatTheme) {
                ChatThemeId.PURPLE -> Brush.verticalGradient(purpleGradient)
                ChatThemeId.BLUE -> Brush.verticalGradient(blueGradient)
                ChatThemeId.RED -> Brush.verticalGradient(redGradient)
                ChatThemeId.GOLD -> Brush.verticalGradient(goldGradient)
                ChatThemeId.PINK -> Brush.verticalGradient(pinkGradient)
                else -> null
            }
        }
    }

    val messages = remember(chatVM.messages) { chatVM.messages.reversed() }
    var input by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var replyMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showVideoCircleRecorder by remember { mutableStateOf(false) }
    var autoStartCircle by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var fullScreenVideoKey by remember { mutableStateOf<String?>(null) }
    var recordMode by remember { mutableStateOf(RecordModePrefs.load(context)) }
    var voiceHold by remember { mutableStateOf(false) }
    var voiceLocked by remember { mutableStateOf(false) }
    var voiceSlideDx by remember { mutableFloatStateOf(0f) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    val editFailedText = stringResource(R.string.message_edit_failed)
    val editForbiddenText = stringResource(R.string.message_edit_forbidden)

    val chatTitle = if (chatVM.partnerName == "Группа") "" else chatVM.partnerName
    val youLabel = stringResource(R.string.you)
    val photoMessageFallback = stringResource(R.string.photo_message)
    val voiceMessageFallback = stringResource(R.string.voice_message)
    val albumMessageFallback = stringResource(R.string.album_message)
    val circleVideoMessageFallback = stringResource(R.string.circle_video_message)
    val voicePermissionText = stringResource(R.string.voice_permission_required)
    val voiceFailedText = stringResource(R.string.voice_record_failed)
    val circlePermissionsText = stringResource(R.string.circle_permissions_required)
    val mediaSendFailedText = stringResource(R.string.error_send_media)
    val mediaLoadFailedText = stringResource(R.string.error_load_media)
    val recordModeVoiceText = stringResource(R.string.record_mode_voice_toast)
    val recordModeVideoText = stringResource(R.string.record_mode_video_toast)

    fun replyTextOf(): String? =
        if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text
        else if (!replyMessage?.mediaUrls.isNullOrEmpty()) albumMessageFallback
        else if (replyMessage?.mediaUrl != null) photoMessageFallback
        else if (replyMessage?.circleVideoUrl != null) circleVideoMessageFallback
        else if (replyMessage?.voiceUrl != null) voiceMessageFallback
        else null

    fun replyNameOf(): String? =
        if (replyMessage == null) null
        else if (replyMessage?.senderId == myUid) youLabel
        else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
        else if (chatVM.isGroupChat) replyMessage?.senderName
        else chatTitle

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, voicePermissionText, Toast.LENGTH_LONG).show()
    }

    val circlePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            autoStartCircle = false
            showVideoCircleRecorder = true
        } else {
            Toast.makeText(context, circlePermissionsText, Toast.LENGTH_LONG).show()
        }
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun startVoiceHold(): Boolean {
        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return false
        }
        if (!voiceRecorder.start()) {
            Toast.makeText(context, voiceFailedText, Toast.LENGTH_SHORT).show()
            return false
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        chatVM.startRecording()
        voiceHold = true
        voiceLocked = false
        voiceSlideDx = 0f
        return true
    }

    fun finishVoice(send: Boolean) {
        val file = voiceRecorder.stop()
        voiceHold = false
        voiceLocked = false
        voiceSlideDx = 0f
        if (send) {
            chatVM.stopRecording(
                chatId = chatId,
                audioFile = file,
                replyText = replyTextOf(),
                replyName = replyNameOf()
            )
            replyMessage = null
        } else {
            file?.delete()
            chatVM.cancelRecording()
        }
    }

    fun openCircleRecorder(autoStart: Boolean): Boolean {
        val missing = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        ).filter { !hasPermission(it) }
        if (missing.isNotEmpty()) {
            circlePermissionsLauncher.launch(missing.toTypedArray())
            return false
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        autoStartCircle = autoStart
        showVideoCircleRecorder = true
        return true
    }

    fun startVideoHold(): Boolean = openCircleRecorder(true)

    var showReactionLimit by remember { mutableStateOf(false) }
    var reactorsFor by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun handleReactionToggle(messageId: String, emoji: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val result = chatVM.toggleReaction(chatId, messageId, emoji)
        if (result == ReactionResult.LIMIT_REACHED) showReactionLimit = true
    }

    LaunchedEffect(showReactionLimit) {
        if (showReactionLimit) {
            delay(3200)
            showReactionLimit = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancel()
            chatVM.cancelRecording()
        }
    }

    val recordLifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(recordLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && voiceRecorder.isActive) {
                finishVoice(false)
            }
        }
        recordLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { recordLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(chatVM.mediaSendFailures) {
        if (chatVM.mediaSendFailures > 0) {
            Toast.makeText(context, mediaSendFailedText, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(chatVM.mediaLoadFailures) {
        if (chatVM.mediaLoadFailures > 0) {
            Toast.makeText(context, mediaLoadFailedText, Toast.LENGTH_SHORT).show()
        }
    }
    val processImageErrorText = stringResource(R.string.error_process_image)
    val sendPhotoErrorText = stringResource(R.string.error_send_photo)
    val chatAvatarUrl = rememberResolvedAvatarUrl(chatVM.partnerAvatarUrl, chatVM.partnerUseCustomAvatar)
    val chatUseCustomAvatar = chatVM.partnerUseCustomAvatar
    val chatProfileGlow = chatVM.partnerProfileGlow ?: "purple"
    val chatEmoji = chatVM.partnerEmoji

    // Переход из профиля (вкладка "Закреплённые") с конкретным сообщением — как только
    // список сообщений реально загрузился, проматываем к нему один раз.
    var didScrollToTarget by remember(chatId, scrollToMessageId) { mutableStateOf(false) }

    var effectsInitialized by remember(chatId) { mutableStateOf(false) }
    var playedEffectIds by remember(chatId) { mutableStateOf(setOf<String>()) }
    var activeEffectKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(messages, chatVM.messagesLoaded) {
        if (!chatVM.messagesLoaded) return@LaunchedEffect
        if (!effectsInitialized) {
            playedEffectIds = messages.mapNotNull { msg -> msg.messageEffect?.let { msg.id } }.toSet()
            effectsInitialized = true
        } else {
            val newEffectMessage = messages.lastOrNull { it.messageEffect != null && it.id !in playedEffectIds }
            if (newEffectMessage != null) {
                playedEffectIds = playedEffectIds + newEffectMessage.id
                activeEffectKey = newEffectMessage.messageEffect
            }
        }
    }

    val replayEvent = chatVM.effectReplayEvent
    LaunchedEffect(replayEvent?.id) {
        val event = replayEvent ?: return@LaunchedEffect
        activeEffectKey = event.effect
        chatVM.consumeEffectReplay(event.id)
    }

    LaunchedEffect(messages, scrollToMessageId) {
        if (didScrollToTarget || scrollToMessageId.isNullOrBlank()) return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == scrollToMessageId }
        if (index != -1) {
            didScrollToTarget = true
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(SharedContentManager.sharedText) {
        SharedContentManager.sharedText?.let { sharedText ->
            input = sharedText
            SharedContentManager.sharedText = null
        }
    }

    LaunchedEffect(chatId) {
        chatVM.observeChat(chatId)
    }

    LaunchedEffect(chatVM.draftText) {
        val draft = chatVM.draftText
        if (!draft.isNullOrBlank() && input.isBlank() && editingMessage == null) {
            input = draft
        }
        chatVM.consumeDraft()
    }

    LaunchedEffect(chatId) {
        snapshotFlow { input }
            .distinctUntilChanged()
            .collectLatest { text ->
                if (text.isNotBlank()) {
                    chatVM.setTyping(chatId, true)
                    delay(2000)
                    chatVM.setTyping(chatId, false)
                } else {
                    chatVM.setTyping(chatId, false)
                }
            }
    }

    LaunchedEffect(chatId) {
        snapshotFlow { input }
            .distinctUntilChanged()
            .collectLatest { text ->
                if (editingMessage == null) {
                    chatVM.onComposingTextChanged(chatId, text)
                }
            }
    }

    val lastSeenText = chatVM.lastSeenText
    val typingText = chatVM.typingText
    val isPartnerTyping = !typingText.isNullOrBlank()
    val partnerUid = chatVM.partnerUid
    val pinnedMessages = chatVM.pinnedMessages
    var pinnedIndex by remember(chatId) { mutableStateOf(0) }
    val currentPinned = pinnedMessages.getOrNull(pinnedIndex.coerceIn(0, (pinnedMessages.size - 1).coerceAtLeast(0)))
    var showPinnedList by remember { mutableStateOf(false) }
    LaunchedEffect(pinnedMessages.size) {
        if (pinnedIndex >= pinnedMessages.size) pinnedIndex = 0
    }
    val partnerIsPremium = chatVM.partnerIsPremium
    val myIsPremium = chatVM.myIsPremium

    val partnerGlowColor = getGlowColor(chatProfileGlow)
    val chatNameColor = chatVM.partnerNameColor

    var expanded by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var showBlockUserConfirm by remember { mutableStateOf(false) }
    var showChatTimerPicker by remember { mutableStateOf(false) }


    var showSendOptionsMenu by remember { mutableStateOf(false) }
    var showMessageTimerPicker by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    var pendingTimerOverrideSec by remember { mutableStateOf<Long?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val currentReplyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (!replyMessage?.mediaUrls.isNullOrEmpty()) albumMessageFallback else if (replyMessage?.mediaUrl != null) photoMessageFallback else null
        val currentReplyName = if (replyMessage == null) null
            else if (replyMessage?.senderId == myUid) youLabel
            else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
            else if (chatVM.isGroupChat) replyMessage?.senderName
            else chatTitle

        if (uris.size == 1) {
            val uri = uris.first()
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            if (!mimeType.startsWith("video/")) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val bytes = compressImageBytes(context, uri)
                        if (bytes != null) {
                            chatVM.sendMediaMessage(
                                chatId = chatId,
                                text = "",
                                fileBytes = bytes,
                                replyText = currentReplyText,
                                replyName = currentReplyName
                            )
                            replyMessage = null
                        } else {
                            withContextMainToast(context, processImageErrorText)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Ошибка отправки медиа", e)
                        withContextMainToast(context, sendPhotoErrorText)
                    }
                }
                return@rememberLauncherForActivityResult
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val items = uris.mapNotNull { uri ->
                    val mimeType = context.contentResolver.getType(uri).orEmpty()
                    if (mimeType.startsWith("video/")) {
                        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        rawBytes?.let { ChatVM.AlbumItem(it, "video/mp4", MediaKind.VIDEO) }
                    } else {
                        val bytes = compressImageBytes(context, uri)
                        bytes?.let { ChatVM.AlbumItem(it, "image/jpeg", MediaKind.IMAGE) }
                    }
                }
                if (items.isNotEmpty()) {
                    chatVM.sendAlbumMessage(
                        chatId = chatId,
                        text = "",
                        items = items,
                        replyText = currentReplyText,
                        replyName = currentReplyName
                    )
                    replyMessage = null
                } else {
                    withContextMainToast(context, processImageErrorText)
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Ошибка отправки альбома", e)
                withContextMainToast(context, sendPhotoErrorText)
            }
        }
    }

    fun playMessageSound() {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {

        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size > 1) {
            val lastMsg = messages.first()
            if (lastMsg.senderId != myUid) {
                playMessageSound()
            }
            if (listState.firstVisibleItemIndex <= 2) {
                listState.animateScrollToItem(0)
            }
        }
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush)
                else Modifier.background(chatBackground)
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.background(if (backgroundBrush != null) Color.Transparent else surfaceColor)) {
                    if (showSearch) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (backgroundBrush != null) Color.Transparent else surfaceColor
                            ),
                            navigationIcon = {
                                IconButton(onClick = {
                                    showSearch = false
                                    searchQuery = ""
                                    chatVM.clearSearch()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = overWallpaperColor)
                                }
                            },
                            title = {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        chatVM.searchMessages(chatId, it)
                                    },
                                    placeholder = { Text(stringResource(R.string.search_in_chat), color = overWallpaperSecondaryColor) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = overWallpaperColor),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = accentColor
                                    )
                                )
                                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                            },
                            actions = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        chatVM.clearSearch()
                                    }) {
                                        Icon(Icons.Default.Close, null, tint = overWallpaperSecondaryColor)
                                    }
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (backgroundBrush != null) Color.Transparent else surfaceColor
                            ),
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = overWallpaperColor
                                    )
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                val profileTargetId =
                                                    (if (chatVM.isGroupChat) chatId else partnerUid).orEmpty()
                                                if (profileTargetId.isNotBlank()) {
                                                    onOpenProfile(profileTargetId, chatVM.isGroupChat)
                                                }
                                            },
                                            onLongClick = {
                                                if (chatUseCustomAvatar && !chatAvatarUrl.isNullOrBlank()) {
                                                    fullScreenImageUrl = chatAvatarUrl
                                                }
                                            }
                                        )
                                    ) {
                                        UserAvatarView(
                                            avatarUrl = chatVM.partnerAvatarUrl,
                                            useCustomAvatar = chatVM.partnerUseCustomAvatar,
                                            profileIcon = chatVM.partnerProfileIcon,
                                            profileGlow = chatVM.partnerProfileGlow,
                                            isPremium = chatVM.partnerIsPremium && !chatVM.isGroupChat,
                                            frameType = if (!chatVM.isGroupChat) {
                                                chatVM.partnerAvatarFrame
                                            } else {
                                                "none"
                                            },
                                            size = 40.dp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier.clickable {
                                            val profileTargetId =
                                                (if (chatVM.isGroupChat) chatId else partnerUid).orEmpty()
                                            if (profileTargetId.isNotBlank()) {
                                                onOpenProfile(profileTargetId, chatVM.isGroupChat)
                                            }
                                        }
                                    ) {
                                        Column {

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {

                                                val titleColor =
                                                    if (partnerIsPremium && !chatVM.isGroupChat) {
                                                        getNameColorBrush(chatNameColor)
                                                    } else {
                                                        null
                                                    }

                                                if (titleColor != null) {
                                                    Text(
                                                        text = chatTitle,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        style = TextStyle(brush = titleColor),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Text(
                                                        text = chatTitle,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = overWallpaperColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                // Верификация — независима от Premium, показывается
                                                // и для юзеров, и для каналов (chatVM.isGroupChat не
                                                // фильтрует канал, только группы без верификации).
                                                if (chatVM.partnerVerification.verified) {
                                                    Spacer(Modifier.width(4.dp))
                                                    VerificationBadge(
                                                        info = chatVM.partnerVerification,
                                                        size = 15.dp
                                                    )
                                                }
                                                if (chatVM.partnerRank != com.dan1eidtj.data.Rank.USER && !chatVM.isGroupChat) {
                                                    Spacer(Modifier.width(4.dp))
                                                    AdminLevelBadge(rank = chatVM.partnerRank, size = 15.dp)
                                                }

                                                if (!chatEmoji.isNullOrBlank()) {
                                                    Spacer(Modifier.width(4.dp))
                                                    StatusBadge(
                                                        value = chatEmoji,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }

                                            val statusText =
                                                if (!typingText.isNullOrBlank()) typingText else lastSeenText.orEmpty()
                                            AnimatedContent<String>(
                                                targetState = statusText,
                                                label = "StatusAnimation",
                                                transitionSpec = {
                                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(
                                                        animationSpec = tween(200)
                                                    )
                                                }
                                            ) { text ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text,
                                                        fontSize = 12.sp,
                                                        color = overWallpaperSecondaryColor
                                                    )
                                                    if (isPartnerTyping) {
                                                        Spacer(Modifier.width(4.dp))
                                                        TypingIndicator(
                                                            dotColor = overWallpaperSecondaryColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (!chatVM.isGroupChat && !partnerUid.isNullOrBlank()) {
                                    IconButton(onClick = {
                                        onStartCall(partnerUid, CallType.AUDIO)
                                    }) {
                                        Icon(Icons.Default.Call, null, tint = overWallpaperColor)
                                    }
                                }

                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, null, tint = overWallpaperColor)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(surfaceColor)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.clear_chat), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    null,
                                                    tint = MayasTheme.ErrorRed
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                showClearChatConfirm = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.block_user), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Block,
                                                    null,
                                                    tint = MayasTheme.ErrorRed
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                showBlockUserConfirm = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.report), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Report,
                                                    null,
                                                    tint = MayasTheme.GlowGold
                                                )
                                            },
                                            onClick = { expanded = false; showReportDialog = true }
                                        )
                                        if (partnerIsPremium || chatVM.myIsPremium) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.choose_theme),
                                                        color = textPrimaryColor
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Palette,
                                                        null,
                                                        tint = MayasTheme.Accent
                                                    )
                                                },
                                                onClick = { expanded = false; showThemePicker = true }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                val current = chatVM.chatDisappearingTimerSec
                                                Text(
                                                    if (current > 0) stringResource(R.string.timer_menu_label_active, formatTimerDuration(current))
                                                    else stringResource(R.string.disappearing_timer_bare_label),
                                                    color = textPrimaryColor
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Timer,
                                                    null,
                                                    tint = MayasTheme.Accent
                                                )
                                            },
                                            onClick = { expanded = false; showChatTimerPicker = true }
                                        )
                                    }
                                }
                            }
                        )

                        AnimatedVisibility(visible = currentPinned != null) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor)
                                        .clickable {
                                            val pinned = currentPinned ?: return@clickable
                                            messages.indexOfFirst { it.id == pinned.id }
                                                .takeIf { it != -1 }?.let { index ->
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(index)
                                                    }
                                                }
                                            if (pinnedMessages.size > 1) {
                                                pinnedIndex = (pinnedIndex + 1) % pinnedMessages.size
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MayasTheme.GlowBlue
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (pinnedMessages.size > 1)
                                                stringResource(R.string.pinned_count_label, pinnedIndex + 1, pinnedMessages.size)
                                            else stringResource(R.string.pinned_message),
                                            fontSize = 12.sp,
                                            color = MayasTheme.GlowBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = if (pinnedMessages.size > 1) {
                                                Modifier.clickable { showPinnedList = true }
                                            } else Modifier
                                        )
                                        Text(
                                            currentPinned?.text
                                                ?: if (!currentPinned?.mediaUrls.isNullOrEmpty()) albumMessageFallback else if (currentPinned?.circleVideoUrl != null) circleVideoMessageFallback else if (currentPinned?.mediaUrl != null) photoMessageFallback else "",
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = textSecondaryColor,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            currentPinned?.let { chatVM.unpinMessage(chatId, it.id) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = textSecondaryColor
                                        )
                                    }
                                }
                                HorizontalDivider(thickness = 1.dp, color = textPrimaryColor.copy(0.1f))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (chatVM.messagesLoaded && messages.isEmpty()) {
                            ChatEmptyState(
                                textColor = textPrimaryColor,
                                hintColor = textSecondaryColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (!chatVM.messagesLoaded && messages.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = textSecondaryColor
                            )
                        }

                        if (chatVM.activeUploads > 0) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = accentColor,
                                trackColor = Color.Transparent
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            reverseLayout = true
                        ) {
                            itemsIndexed(
                                chatVM.outbox.asReversed(),
                                key = { _, item -> "outbox_" + item.id }
                            ) { _, item ->
                                OutboxBubble(
                                    item = item,
                                    bubbleColor = bubbleMineColor,
                                    textColor = textPrimaryColor,
                                    secondaryColor = textSecondaryColor,
                                    errorColor = MayasTheme.ErrorRed,
                                    onRetry = { chatVM.retryOutbox(chatId) },
                                    onCancel = { chatVM.cancelOutbox(item.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                                if (msg.type == MessageType.SYSTEM || msg.type == MessageType.CALL) {
                                    val onSystemClick: (() -> Unit)? = when {
                                        msg.systemAction == SystemAction.PINNED && msg.systemRefMessageId != null -> {
                                            {
                                                messages.indexOfFirst { it.id == msg.systemRefMessageId }
                                                    .takeIf { it != -1 }
                                                    ?.let { idx ->
                                                        coroutineScope.launch { listState.animateScrollToItem(idx) }
                                                    }
                                            }
                                        }
                                        msg.type == MessageType.CALL && !chatVM.isGroupChat -> {
                                            {
                                                val ct = runCatching {
                                                    CallType.valueOf(msg.callType ?: "AUDIO")
                                                }.getOrDefault(CallType.AUDIO)
                                                onStartCall(chatVM.partnerUid, ct)
                                            }
                                        }
                                        else -> null
                                    }

                                    SystemMessageRow(
                                        message = msg,
                                        chipColor = surfaceColor,
                                        textColor = textSecondaryColor,
                                        onClick = onSystemClick,
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(220),
                                            fadeOutSpec = tween(150),
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    )
                                    return@itemsIndexed
                                }

                                val isMe = msg.senderId == myUid
                                val isChannelPost = chatVM.chatType == "CHANNEL"
                                val isGroupChat = chatVM.isGroupChat

                                val nextMsg = messages.getOrNull(index - 1)
                                val isLastInChain =
                                    nextMsg == null || nextMsg.senderId != msg.senderId

                                val isPremiumMsg = msg.isPremium

                                val bubbleShape = remember(isMe, isLastInChain) {
                                    BubbleShape(
                                        type = if (isMe) BubbleType.Outgoing else BubbleType.Incoming,
                                        drawTail = isLastInChain
                                    )
                                }

                                val bubbleColor = if (isMe) bubbleMineColor else bubbleOtherColor
                                val timeColor = textSecondaryColor


                                val messageStyle = msg.messageStyle

                                val messageModifier = remember(messageStyle, isPremiumMsg, bubbleColor, bubbleShape) {
                                    when (messageStyle) {
                                        MessageStyle.NEON -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.NeonBlueStart, MayasTheme.NeonBlueEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 2.dp,
                                                brush = Brush.sweepGradient(
                                                    colors = listOf(MayasTheme.GlowCyan, MayasTheme.GlowRose, MayasTheme.GlowCyan)
                                                ),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.GOLD -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.GoldStart, MayasTheme.GoldEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.5.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.White, MayasTheme.GoldStart)
                                                ),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.FIRE -> {
                                            Modifier.background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(MayasTheme.FireStart, MayasTheme.FireEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 2.dp,
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(MayasTheme.GlowRed, MayasTheme.GlowAmber)
                                                ),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.ICE -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.IceStart, MayasTheme.IceEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.MATRIX -> {
                                            Modifier.background(
                                                color = Color.Black,
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(MayasTheme.GlowLime, Color.Black)
                                                ),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.SUNSET -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.SunsetStart, MayasTheme.SunsetEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.2f),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.FOREST -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.ForestStart, MayasTheme.ForestEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.2f),
                                                shape = bubbleShape
                                            )
                                        }
                                        MessageStyle.MIDNIGHT -> {
                                            Modifier.background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(MayasTheme.MidnightStart, MayasTheme.MidnightEnd)
                                                ),
                                                shape = bubbleShape
                                            ).border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.3f),
                                                shape = bubbleShape
                                            )
                                        }
                                        else -> {
                                            if (messageStyle != null) {
                                                val accent = ShopConstants.getStyleColor(messageStyle)
                                                Modifier.background(
                                                    brush = Brush.linearGradient(
                                                        colors = ShopConstants.getStyleGradient(messageStyle)
                                                    ),
                                                    shape = bubbleShape
                                                ).border(
                                                    width = 1.dp,
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(Color.White.copy(alpha = 0.25f), accent)
                                                    ),
                                                    shape = bubbleShape
                                                )
                                            } else if (isPremiumMsg) {
                                                Modifier.background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(bubbleColor, MayasTheme.GlowGold.copy(alpha = 0.2f))
                                                    ),
                                                    shape = bubbleShape
                                                ).border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(Color.Transparent, MayasTheme.GlowGold)
                                                    ),
                                                    shape = bubbleShape
                                                )
                                            } else {
                                                Modifier.background(
                                                    bubbleColor,
                                                    bubbleShape
                                                )
                                            }
                                        }
                                    }
                                }

                                val alignment =
                                    if (isMe) Alignment.CenterEnd else Alignment.CenterStart

                                val tailWidth = 12.dp
                                val startPadding =
                                    if (isMe) 60.dp else (if (isLastInChain) 0.dp else tailWidth)
                                val endPadding =
                                    if (isMe) (if (isLastInChain) 0.dp else tailWidth) else 60.dp

                                val replyThreshold = with(density) { 50.dp.toPx() }
                                val maxOffsetX = with(density) { 80.dp.toPx() }
                                val offsetXAnim = remember(msg.id) { Animatable(0f) }
                                var hasVibrated by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec = tween(220),
                                            fadeOutSpec = tween(150),
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                        .padding(start = startPadding, end = endPadding)
                                        .pointerInput(msg.id) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    if (offsetXAnim.value < -replyThreshold) {
                                                        replyMessage = msg
                                                    }
                                                    coroutineScope.launch {
                                                        offsetXAnim.animateTo(
                                                            0f,
                                                            animationSpec = tween(200)
                                                        )
                                                    }
                                                    hasVibrated = false
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        offsetXAnim.animateTo(
                                                            0f,
                                                            animationSpec = tween(200)
                                                        )
                                                    }
                                                    hasVibrated = false
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    if (dragAmount < 0 || offsetXAnim.value < 0) {
                                                        val newOffset =
                                                            (offsetXAnim.value + dragAmount)
                                                                .coerceIn(-maxOffsetX, 0f)

                                                        coroutineScope.launch {
                                                            offsetXAnim.snapTo(newOffset)
                                                        }

                                                        if (newOffset < -replyThreshold && !hasVibrated) {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            hasVibrated = true
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = alignment
                                ) {
                                    if (offsetXAnim.value < 0) {
                                        val alpha =
                                            (offsetXAnim.value.absoluteValue / replyThreshold).coerceIn(
                                                0f,
                                                1f
                                            )
                                        val scale =
                                            (offsetXAnim.value.absoluteValue / replyThreshold).coerceIn(
                                                0.6f,
                                                1f
                                            )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 16.dp)
                                                .graphicsLayer(
                                                    alpha = alpha,
                                                    scaleX = scale,
                                                    scaleY = scale
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                                contentDescription = null,
                                                tint = MayasTheme.GlowPurple
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.offset {
                                            IntOffset(
                                                offsetXAnim.value.roundToInt(),
                                                0
                                            )
                                        },
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        if (!isMe && isGroupChat) {
                                            if (isLastInChain) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(bottom = 4.dp, end = 8.dp)
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MayasTheme.GlowPurple.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChannelPost && chatVM.partnerUseCustomAvatar && !chatVM.partnerAvatarUrl.isNullOrBlank()) {
                                                        B2Image(
                                                            key = chatVM.partnerAvatarUrl!!,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else if (isChannelPost) {
                                                        Text(
                                                            text = chatVM.partnerEmoji ?: "📢",
                                                            fontSize = 14.sp
                                                        )
                                                    } else {
                                                        Text(
                                                            text = (msg.senderName ?: "").take(1)
                                                                .uppercase(),
                                                            color = textPrimaryColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(40.dp))
                                            }
                                        }

                                        val plainCircle = msg.circleVideoUrl != null &&
                                            msg.text.isNullOrBlank() &&
                                            msg.replyToText == null &&
                                            msg.forwardedFromName == null
                                        val circleTimeText = msg.timestamp?.let { ts ->
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts)
                                        } ?: "--:--"

                                        MessageBubbleContainer(
                                            messageStyle = messageStyle,
                                            bubbleShape = bubbleShape,
                                            messageModifier = messageModifier,
                                            plain = plainCircle,
                                            normalPadding = PaddingValues(
                                                start = if (isMe) 14.dp else (if (isLastInChain) 26.dp else 14.dp),
                                                end = if (isMe) (if (isLastInChain) 26.dp else 14.dp) else 14.dp,
                                                top = 8.dp,
                                                bottom = 8.dp
                                            ),
                                            onClick = { selectedMessage = msg },
                                            onDoubleClick = { handleReactionToggle(msg.id, "❤️") },
                                            onScreenBounds = { bubbleBounds[msg.id] = it }
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {

                                                    if (!isMe && isGroupChat && isLastInChain) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isChannelPost) chatVM.partnerName else msg.senderName.orEmpty(),
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (msg.isPremium && !isChannelPost) MayasTheme.GlowGold else MayasTheme.GlowPurple
                                                            )
                                                            if (msg.isPremium && !isChannelPost) {
                                                                Spacer(Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = Icons.Default.Verified,
                                                                    contentDescription = null,
                                                                    tint = MayasTheme.GlowGold,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                            if (chatVM.chatType != "CHANNEL") {
                                                                val role = when {
                                                                    msg.senderId.isNotBlank() && msg.senderId == chatVM.chatOwnerId -> stringResource(R.string.chat_role_owner)
                                                                    msg.senderId.isNotBlank() && msg.senderId in chatVM.chatAdmins -> stringResource(R.string.chat_role_admin)
                                                                    else -> null
                                                                }
                                                                if (role != null) {
                                                                    Spacer(Modifier.width(6.dp))
                                                                    Text(
                                                                        text = role,
                                                                        fontSize = 11.sp,
                                                                        color = MayasTheme.TextSecondary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (!msg.forwardedFromName.isNullOrBlank()) {
                                                        Row(
                                                            modifier = Modifier.padding(bottom = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Outlined.Bookmark,
                                                                null,
                                                                tint = (if (isMe) Color.White else MayasTheme.GlowPurple).copy(alpha = 0.7f),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                stringResource(R.string.forwarded_from, msg.forwardedFromName ?: ""),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = (if (isMe) Color.White else MayasTheme.GlowPurple).copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                    if (!msg.replyToText.isNullOrBlank()) {
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(bottom = 6.dp)
                                                                .background(
                                                                    textPrimaryColor.copy(alpha = 0.05f),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(
                                                                    horizontal = 8.dp,
                                                                    vertical = 4.dp
                                                                ),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.width(3.dp)
                                                                    .height(26.dp)
                                                                    .background(MayasTheme.GlowPurple)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Column {
                                                                Text(
                                                                    msg.replyToName.orEmpty(),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MayasTheme.GlowPurple
                                                                )
                                                                MayasText(
                                                                    msg.replyToText.orEmpty(),
                                                                    fontSize = 12.sp,
                                                                    color = textSecondaryColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (msg.circleVideoUrl != null) {
                                                        CircleVideoBubble(
                                                            mediaKey = msg.circleVideoUrl,
                                                            durationSec = msg.circleVideoDuration,
                                                            messageId = msg.id,
                                                            isMine = isMe,
                                                            timeText = if (plainCircle) circleTimeText else null,
                                                            status = if (plainCircle && isMe) msg.status else null,
                                                            modifier = Modifier.padding(bottom = if (plainCircle) 0.dp else 6.dp)
                                                        )
                                                    } else if (msg.mediaUrls.isNotEmpty()) {
                                                        AlbumGrid(
                                                            mediaUrls = msg.mediaUrls,
                                                            mediaTypes = msg.mediaTypes,
                                                            modifier = Modifier
                                                                .padding(bottom = 6.dp)
                                                                .fillMaxWidth(),
                                                            onImageClick = { key ->
                                                                if (key.startsWith("http")) {
                                                                    fullScreenImageUrl = key
                                                                } else {
                                                                    coroutineScope.launch {
                                                                        fullScreenImageUrl = MediaFileCache.resolveModel(context, key)
                                                                    }
                                                                }
                                                            },
                                                            onVideoClick = { key -> fullScreenVideoKey = key }
                                                        )
                                                    } else if (!msg.mediaUrl.isNullOrBlank()) {
                                                        B2Image(
                                                            key = msg.mediaUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .padding(bottom = 6.dp)
                                                                .fillMaxWidth()
                                                                .heightIn(max = 300.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    val mediaKey = msg.mediaUrl!!
                                                                    if (mediaKey.startsWith("http")) {
                                                                        fullScreenImageUrl = mediaKey
                                                                    } else {
                                                                        coroutineScope.launch {
                                                                            fullScreenImageUrl = MediaFileCache.resolveModel(context, mediaKey)
                                                                        }
                                                                    }
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                    if (!msg.voiceUrl.isNullOrBlank()) {
                                                        VoiceMessageItem(
                                                            url = msg.voiceUrl,
                                                            duration = msg.voiceDuration,
                                                            isMe = isMe,
                                                            accentColor = if (isMe) Color.White else MayasTheme.GlowPurple,
                                                            isPlaying = chatVM.playingUrl == msg.voiceUrl && chatVM.isVoicePlaying,
                                                            progress = if (chatVM.playingUrl == msg.voiceUrl) chatVM.voiceProgress else 0f,
                                                            onPlayPause = { chatVM.playVoice(msg.voiceUrl!!) }
                                                        )
                                                    }

                                                    if (!msg.text.isNullOrBlank()) {
                                                        val customTextColor = FrameStyles.registry[messageStyle]?.textColor ?: when (messageStyle) {
                                                            MessageStyle.ICE -> Color(0xFF006064)
                                                            MessageStyle.MATRIX -> MayasTheme.GlowLime
                                                            MessageStyle.GOLD -> Color(0xFF5D4037)
                                                            MessageStyle.FOREST, MessageStyle.SUNSET, MessageStyle.MIDNIGHT -> Color.White
                                                            null -> {
                                                                if (isMe) Color.White
                                                                else textPrimaryColor
                                                            }
                                                            else -> ShopConstants.getStyleTextColor(messageStyle)
                                                        }

                                                        val parsedText = rememberParsedMessageText(
                                                            text = msg.text.orEmpty(),
                                                            accentColor = if (messageStyle != null) {
                                                                customTextColor.copy(alpha = 0.8f)
                                                            } else if (isMe) {
                                                                Color.White.copy(alpha = 0.9f)
                                                            } else {
                                                                MayasTheme.LinkColor
                                                            }
                                                        )

                                                        val usernameClickedTemplate = stringResource(R.string.username_clicked)
                                                        val hashtagClickedTemplate = stringResource(R.string.hashtag_clicked)
                                                        MayasClickableText(
                                                            text = parsedText,
                                                            style = TextStyle(
                                                                fontSize = fontSize.sp,
                                                                color = customTextColor
                                                            ),
                                                            onClick = { annotated, offset ->
                                                                annotated.getStringAnnotations(
                                                                    "URL",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    val intent = Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        Uri.parse(annotation.item)
                                                                    )
                                                                    context.startActivity(intent)
                                                                    return@MayasClickableText
                                                                }
                                                                annotated.getStringAnnotations(
                                                                    "USERNAME",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    Toast.makeText(
                                                                        context,
                                                                        String.format(usernameClickedTemplate, annotation.item),
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@MayasClickableText
                                                                }
                                                                annotated.getStringAnnotations(
                                                                    "HASHTAG",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    Toast.makeText(
                                                                        context,
                                                                        String.format(hashtagClickedTemplate, annotation.item),
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@MayasClickableText
                                                                }
                                                                selectedMessage = msg
                                                            }
                                                        )
                                                    }
                                                }

                                                if (!plainCircle) {
                                                Spacer(modifier = Modifier.width(12.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    val secondaryTextColor = FrameStyles.registry[messageStyle]?.textColor?.copy(alpha = 0.6f) ?: when (messageStyle) {
                                                        MessageStyle.ICE -> Color(0xFF006064).copy(alpha = 0.6f)
                                                        MessageStyle.MATRIX -> MayasTheme.GlowLime.copy(alpha = 0.7f)
                                                        MessageStyle.GOLD -> Color(0xFF5D4037).copy(alpha = 0.7f)
                                                        MessageStyle.FOREST, MessageStyle.SUNSET -> Color.White.copy(alpha = 0.7f)
                                                        MessageStyle.MIDNIGHT -> Color.White.copy(alpha = 0.6f)
                                                        null -> {
                                                            if (isMe) Color.White.copy(alpha = 0.7f)
                                                            else timeColor
                                                        }
                                                        else -> ShopConstants.getStyleTextColor(messageStyle).copy(alpha = 0.7f)
                                                    }

                                                    val timeFormat = msg.timestamp?.let { ts ->
                                                        SimpleDateFormat(
                                                            "HH:mm",
                                                            Locale.getDefault()
                                                        ).format(ts)
                                                    } ?: "--:--"

                                                    val effectEmoji = msg.messageEffect?.let { MessageEffects.registry[it]?.emoji }
                                                    if (effectEmoji != null) {
                                                        MessageEffectBadge(
                                                            emoji = effectEmoji,
                                                            background = secondaryTextColor.copy(alpha = 0.16f),
                                                            onClick = {
                                                                activeEffectKey = msg.messageEffect
                                                                chatVM.replayMessageEffect(chatId, msg)
                                                            }
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                    }

                                                    if (msg.messageState == MessageState.SCHEDULED) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Schedule,
                                                            contentDescription = stringResource(R.string.scheduled_indicator),
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }
                                                    if (msg.ttlSeconds > 0) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Timer,
                                                            contentDescription = stringResource(R.string.disappearing_message_indicator),
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }
                                                    if (msg.isSilent) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.NotificationsOff,
                                                            contentDescription = stringResource(R.string.silent_indicator),
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }

                                                    if (msg.isEdited && msg.messageState != MessageState.SCHEDULED) {
                                                        Text(
                                                            text = stringResource(R.string.edited_label),
                                                            fontSize = 11.sp,
                                                            color = secondaryTextColor,
                                                            fontStyle = FontStyle.Italic
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }

                                                    Text(
                                                        text = if (msg.messageState == MessageState.SCHEDULED) {
                                                            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(msg.scheduledFor ?: msg.timestamp ?: java.util.Date())
                                                        } else timeFormat,
                                                        fontSize = 11.sp,
                                                        color = secondaryTextColor,
                                                        textAlign = TextAlign.End
                                                    )

                                                    if (chatVM.chatType == "CHANNEL") {
                                                        Spacer(Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.RemoveRedEye,
                                                            contentDescription = null,
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(2.dp))
                                                        Text(
                                                            text = formatCompactCount(msg.viewedBy.size),
                                                            fontSize = 11.sp,
                                                            color = secondaryTextColor,
                                                            textAlign = TextAlign.End
                                                        )
                                                    }

                                                    if (isMe) {
                                                        if (msg.isPremium) {
                                                            Icon(
                                                                imageVector = Icons.Default.Verified,
                                                                contentDescription = null,
                                                                tint = if (messageStyle == MessageStyle.GOLD) Color(0xFF5D4037) else MayasTheme.GlowGold,
                                                                modifier = Modifier.size(14.dp)
                                                                    .padding(end = 4.dp)
                                                            )
                                                        }
                                                        val statusIcon = when (msg.status) {
                                                            0 -> Icons.Default.AccessTime
                                                            2 -> Icons.Default.DoneAll
                                                            else -> Icons.Default.Done
                                                        }
                                                        AnimatedContent(
                                                            targetState = statusIcon,
                                                            transitionSpec = {
                                                                (scaleIn(initialScale = 0.6f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)))
                                                                    .togetherWith(fadeOut(animationSpec = tween(120)))
                                                            },
                                                            label = "messageStatusIcon"
                                                        ) { icon ->
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = null,
                                                                tint = if (messageStyle != null) secondaryTextColor else (if (msg.status == 2) MayasTheme.GlowSky else textSecondaryColor),
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                }
                                            }
                                                if (msg.reactions.isNotEmpty()) {
                                                    val chipsSecondary = FrameStyles.registry[messageStyle]?.textColor?.copy(alpha = 0.6f) ?: if (isMe) Color.White.copy(alpha = 0.7f) else timeColor
                                                    MessageReactionChips(
                                                        message = msg,
                                                        myUid = myUid,
                                                        accent = accentColor,
                                                        contentColor = chipsSecondary,
                                                        chipBackground = chipsSecondary.copy(alpha = 0.16f),
                                                        profileOf = { uid -> chatVM.reactorProfile(uid) },
                                                        onToggle = { emoji -> handleReactionToggle(msg.id, emoji) },
                                                        onShowReactors = { emoji -> reactorsFor = msg.id to emoji },
                                                        modifier = Modifier.padding(top = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                    }

                                    if (selectedMessage?.id == msg.id) {
                                        MessageActionsOverlay(
                                            bounds = bubbleBounds[msg.id],
                                            isMine = isMe,
                                            surfaceColor = surfaceColor,
                                            accent = MayasTheme.GlowPurple,
                                            selectedReactions = msg.reactionsOf(myUid),
                                            topInsetPx = overlayTopInsetPx,
                                            bottomInsetPx = overlayBottomInsetPx,
                                            onReaction = { emoji ->
                                                handleReactionToggle(msg.id, emoji)
                                                selectedMessage = null
                                            },
                                            onDismiss = { selectedMessage = null }
                                        ) {
                                        if (msg.messageState == MessageState.SCHEDULED && msg.senderId == myUid) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.cancel_send_action), color = MayasTheme.ErrorRed) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Schedule,
                                                        null,
                                                        tint = MayasTheme.ErrorRed
                                                    )
                                                },
                                                onClick = {
                                                    chatVM.cancelScheduledMessage(chatId, msg.id)
                                                    selectedMessage = null
                                                }
                                            )
                                            HorizontalDivider(color = textPrimaryColor.copy(0.1f))
                                        }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.reply), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                                    null,
                                                    tint = textSecondaryColor
                                                )
                                            },
                                            onClick = {
                                                replyMessage = msg
                                                selectedMessage = null
                                            }
                                        )
                                        if (isMe && msg.type == MessageType.TEXT && msg.messageState != MessageState.SCHEDULED) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit_action), color = textPrimaryColor) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        null,
                                                        tint = textSecondaryColor
                                                    )
                                                },
                                                onClick = {
                                                    editingMessage = msg
                                                    replyMessage = null
                                                    input = msg.text.orEmpty()
                                                    selectedMessage = null
                                                }
                                            )
                                        }
                                        val textCopiedText = stringResource(R.string.text_copied)
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.copy), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ContentCopy,
                                                    null,
                                                    tint = textSecondaryColor
                                                )
                                            },
                                            onClick = {
                                                val clip = ClipData.newPlainText(
                                                    "MayasMessage",
                                                    msg.text.orEmpty()
                                                )
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(
                                                    context,
                                                    textCopiedText,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedMessage = null
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.share), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Share,
                                                    null,
                                                    tint = textSecondaryColor
                                                )
                                            },
                                            onClick = {
                                                shareText(context, msg.text.orEmpty())
                                                selectedMessage = null
                                            }
                                        )
                                        val messagePinnedText = stringResource(R.string.message_pinned)
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.pin_action), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.PushPin,
                                                    null,
                                                    tint = textSecondaryColor
                                                )
                                            },
                                            onClick = {
                                                chatVM.pinMessage(chatId, msg)
                                                Toast.makeText(
                                                    context,
                                                    messagePinnedText,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedMessage = null
                                            }
                                        )
                                        val forwardedToSavedText = stringResource(R.string.forwarded_to_saved)
                                        val messageDeletedText = stringResource(R.string.message_deleted)
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.forward_to_saved), color = textPrimaryColor) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Bookmark,
                                                    null,
                                                    tint = textSecondaryColor
                                                )
                                            },
                                            onClick = {
                                                selectedMessage = null
                                                val uidForForward = myUid
                                                if (uidForForward != null) {
                                                    coroutineScope.launch {
                                                        val savedChatId = chatVM.ensureSavedMessagesChat(uidForForward)
                                                        chatVM.forwardMessage(msg, savedChatId)
                                                        Toast.makeText(
                                                            context,
                                                            forwardedToSavedText,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        )
                                        if (isMe) {
                                            HorizontalDivider(color = textPrimaryColor.copy(0.1f))
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.delete),
                                                        color = MayasTheme.ErrorRed
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        null,
                                                        tint = MayasTheme.ErrorRed
                                                    )
                                                },
                                                onClick = {
                                                    chatVM.deleteMessage(chatId, msg.id)
                                                    Toast.makeText(
                                                        context,
                                                        messageDeletedText,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    selectedMessage = null
                                                }
                                            )
                                        }
                                                                            }
                                    }
                                }
                            }
                        }
                    }
                    Column {
                        AnimatedVisibility(
                            visible = replyMessage != null,
                            enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                        ) {
                            replyMessage?.let { reply ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor)
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MayasTheme.Surface
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val replyName = if (reply.senderId == myUid) youLabel
                                        else if (reply.senderName == "Система" || reply.senderName == "Mayas") "Система"
                                        else if (chatVM.isGroupChat) reply.senderName
                                        else chatTitle
                                        Text(
                                            text = replyName,
                                            fontSize = 11.sp,
                                            color = MayasTheme.GlowPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                        MayasText(
                                            text = if (!reply.text.isNullOrBlank()) reply.text else if (reply.mediaUrls.isNotEmpty()) albumMessageFallback else if (reply.circleVideoUrl != null) circleVideoMessageFallback else photoMessageFallback,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = textSecondaryColor,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = textSecondaryColor
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = textPrimaryColor.copy(0.1f)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = editingMessage != null,
                            enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceColor)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MayasTheme.GlowPurple
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.edit_action),
                                        fontSize = 11.sp,
                                        color = MayasTheme.GlowPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = editingMessage?.text.orEmpty(),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = textSecondaryColor,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingMessage = null
                                        input = ""
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = textSecondaryColor
                                    )
                                }
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = textPrimaryColor.copy(0.1f)
                            )
                        }

                        AnimatedVisibility(visible = showEmojiPicker) {
                            EmojiPicker { input += it }
                        }

                        if (chatVM.canPostInChat) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().imePadding(),
                                color = surfaceColor,
                                tonalElevation = 8.dp
                            ) {
                                Column {
                                    val effectiveTimerSec = pendingTimerOverrideSec ?: chatVM.chatDisappearingTimerSec
                                    AnimatedVisibility(visible = pendingTimerOverrideSec != null || chatVM.chatDisappearingTimerSec > 0) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                .clickable { showMessageTimerPicker = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Timer,
                                                null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MayasTheme.Accent
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                if (pendingTimerOverrideSec != null) stringResource(R.string.timer_for_message, formatTimerDuration(effectiveTimerSec))
                                                else stringResource(R.string.timer_for_chat, formatTimerDuration(effectiveTimerSec)),
                                                fontSize = 12.sp,
                                                color = MayasTheme.Accent
                                            )
                                            if (pendingTimerOverrideSec != null) {
                                                Spacer(Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { pendingTimerOverrideSec = null },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MayasTheme.Accent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                            .navigationBarsPadding(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (voiceHold) {
                                            VoiceRecordingBar(
                                                seconds = chatVM.recordingDuration,
                                                locked = voiceLocked,
                                                slideDx = voiceSlideDx,
                                                onCancel = { finishVoice(false) },
                                                onSend = { finishVoice(true) },
                                                accent = MayasTheme.ErrorRed,
                                                textColor = textPrimaryColor,
                                                hintColor = textSecondaryColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                        IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                                            Icon(
                                                if (showEmojiPicker) Icons.Default.Keyboard else Icons.Outlined.EmojiEmotions,
                                                null,
                                                tint = textSecondaryColor
                                            )
                                        }

                                        OutlinedTextField(
                                            value = input,
                                            onValueChange = { input = it },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    if (editingMessage != null) stringResource(R.string.edit_message_hint) else stringResource(R.string.message_hint),
                                                    color = textSecondaryColor
                                                )
                                            },
                                            maxLines = 5,
                                            shape = RoundedCornerShape(24.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedContainerColor = chatBackground.copy(alpha = 0.5f),
                                                unfocusedContainerColor = chatBackground.copy(alpha = 0.5f),
                                                focusedTextColor = textPrimaryColor,
                                                unfocusedTextColor = textPrimaryColor,
                                                cursorColor = MayasTheme.GlowPurple
                                            )
                                        )

                                        IconButton(onClick = { showAttachSheet = true }) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                stringResource(R.string.attach_open),
                                                tint = textSecondaryColor
                                            )
                                        }
                                        }

                                        AnimatedContent<Boolean>(
                                            targetState = input.isNotBlank(),
                                            label = "SendButtonAnimation",
                                            transitionSpec = {
                                                (scaleIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn())
                                                    .togetherWith(scaleOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut())
                                            }
                                        ) { isSending ->
                                            if (isSending) {
                                                fun doSend(silent: Boolean, effect: String? = null) {
                                                    val editTarget = editingMessage
                                                    if (editTarget != null) {
                                                        val textToSave = input
                                                        chatVM.editMessage(
                                                            chatId = chatId,
                                                            messageId = editTarget.id,
                                                            newText = textToSave,
                                                            onError = { reason ->
                                                                val message = if (reason == "forbidden") editForbiddenText else editFailedText
                                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                        input = ""
                                                        editingMessage = null
                                                        return
                                                    }
                                                    chatVM.sendMessage(
                                                        chatId = chatId,
                                                        text = input,
                                                        replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (!replyMessage?.mediaUrls.isNullOrEmpty()) albumMessageFallback else if (replyMessage?.mediaUrl != null) photoMessageFallback else if (replyMessage?.circleVideoUrl != null) circleVideoMessageFallback else if (replyMessage?.voiceUrl != null) voiceMessageFallback else null,
                                                        replyName = if (replyMessage == null) null
                                                        else if (replyMessage?.senderId == myUid) youLabel
                                                        else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                                                        else if (chatVM.isGroupChat) replyMessage?.senderName
                                                        else chatTitle,
                                                        timerOverrideSec = pendingTimerOverrideSec,
                                                        silent = silent,
                                                        effect = effect
                                                    )
                                                    input = ""
                                                    replyMessage = null
                                                    pendingTimerOverrideSec = null
                                                    chatVM.clearDraftNow(chatId)
                                                }

                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(MayasTheme.GlowBlue)
                                                            .combinedClickable(
                                                                onClick = { doSend(silent = false) },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    showSendOptionsMenu = true
                                                                }
                                                            )
                                                            .padding(12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.Send,
                                                            null,
                                                            tint = Color.White
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = showSendOptionsMenu,
                                                        onDismissRequest = { showSendOptionsMenu = false },
                                                        modifier = Modifier.background(surfaceColor)
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    if (pendingTimerOverrideSec != null) stringResource(R.string.timer_menu_label_active, formatTimerDuration(pendingTimerOverrideSec!!)) else stringResource(R.string.timer_message_label_inactive),
                                                                    color = textPrimaryColor
                                                                )
                                                            },
                                                            leadingIcon = { Icon(Icons.Outlined.Timer, null, tint = MayasTheme.Accent) },
                                                            onClick = {
                                                                showSendOptionsMenu = false
                                                                showMessageTimerPicker = true
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.send_silently_action), color = textPrimaryColor) },
                                                            leadingIcon = { Icon(Icons.Outlined.NotificationsOff, null, tint = MayasTheme.Accent) },
                                                            onClick = {
                                                                showSendOptionsMenu = false
                                                                doSend(silent = true)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.send_later_action), color = textPrimaryColor) },
                                                            leadingIcon = { Icon(Icons.Outlined.Schedule, null, tint = MayasTheme.Accent) },
                                                            onClick = {
                                                                showSendOptionsMenu = false
                                                                showScheduleDialog = true
                                                            }
                                                        )
                                                        HorizontalDivider(thickness = 1.dp, color = textPrimaryColor.copy(0.08f))
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                                .fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            MessageEffects.registry.forEach { (key, spec) ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(CircleShape)
                                                                        .background(MayasTheme.SurfaceVariant.copy(alpha = 0.4f))
                                                                        .clickable {
                                                                            showSendOptionsMenu = false
                                                                            doSend(silent = false, effect = key)
                                                                        }
                                                                        .padding(8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    EmojiGlyph(spec.emoji, fontSize = 18.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (voiceLocked) {
                                                IconButton(onClick = { finishVoice(true) }) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Send,
                                                        stringResource(R.string.voice_send),
                                                        tint = MayasTheme.Accent
                                                    )
                                                }
                                            } else {
                                                RecordModeButton(
                                                    mode = recordMode,
                                                    recording = voiceHold,
                                                    idleTint = textSecondaryColor,
                                                    activeColor = MayasTheme.ErrorRed,
                                                    onToggleMode = {
                                                        recordMode = if (recordMode == RecordMode.VOICE) RecordMode.VIDEO else RecordMode.VOICE
                                                        RecordModePrefs.save(context, recordMode)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        Toast.makeText(
                                                            context,
                                                            if (recordMode == RecordMode.VIDEO) recordModeVideoText else recordModeVoiceText,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onHoldStart = {
                                                        if (recordMode == RecordMode.VOICE) startVoiceHold() else startVideoHold()
                                                    },
                                                    onHoldMove = { dx, _ -> voiceSlideDx = dx },
                                                    onHoldRelease = { release ->
                                                        when (release) {
                                                            RecordRelease.SEND -> finishVoice(true)
                                                            RecordRelease.CANCEL -> finishVoice(false)
                                                            RecordRelease.LOCK -> voiceLocked = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            ChannelReadOnlyBar(
                                surfaceColor = surfaceColor,
                                textSecondaryColor = textSecondaryColor
                            )
                        }
                    }
                }

                ReactionLimitToast(
                    visible = showReactionLimit,
                    accent = MayasTheme.GlowGold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = 96.dp)
                )

                val showScrollDown by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 3 }
                }

                AnimatedVisibility(
                    visible = showScrollDown,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = surfaceColor,
                        modifier = Modifier.size(45.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = textPrimaryColor)
                    }
                }
            }
        }

        if (showReportDialog) {
            var reportText by remember { mutableStateOf("") }
            val reportSentText = stringResource(R.string.report_sent)
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                containerColor = surfaceColor,
                title = { Text(stringResource(R.string.report_user_title), color = textPrimaryColor) },
                text = {
                    Column {
                        Text(stringResource(R.string.report_reason_label), color = textSecondaryColor, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reportText,
                            onValueChange = { reportText = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text(stringResource(R.string.report_reason_hint)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MayasTheme.GlowPurple,
                                unfocusedBorderColor = textSecondaryColor,
                                focusedTextColor = textPrimaryColor,
                                unfocusedTextColor = textPrimaryColor
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            chatVM.reportUser(myUid, partnerUid, chatId, reportText) {
                                Toast.makeText(context, reportSentText, Toast.LENGTH_SHORT)
                                    .show()
                                showReportDialog = false
                                reportText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowPurple)
                    ) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.send)) }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = textSecondaryColor)
                    }
                }
            )
        }

        if (showClearChatConfirm) {
            val chatClearedText = stringResource(R.string.chat_cleared)
            AlertDialog(
                onDismissRequest = { showClearChatConfirm = false },
                containerColor = surfaceColor,
                title = { Text(stringResource(R.string.clear_chat_confirm_title), color = textPrimaryColor) },
                text = {
                    Text(
                        stringResource(R.string.clear_chat_confirm_desc),
                        color = textSecondaryColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearChatConfirm = false
                            chatVM.clearChat(chatId) {
                                Toast.makeText(context, chatClearedText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                    ) { Text(stringResource(R.string.clear)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatConfirm = false }) {
                        Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = textSecondaryColor)
                    }
                }
            )
        }

        if (showBlockUserConfirm) {
            val userBlockedText = stringResource(R.string.user_blocked)
            BlockUserConfirmDialog(
                onConfirm = {
                    showBlockUserConfirm = false
                    chatVM.blockUser(myUid ?: "", partnerUid) {
                        Toast.makeText(context, userBlockedText, Toast.LENGTH_SHORT)
                            .show()
                        onBack()
                    }
                },
                onDismiss = { showBlockUserConfirm = false },
                containerColor = surfaceColor,
                titleColor = textPrimaryColor,
                textColor = textSecondaryColor
            )
        }

        if (showThemePicker) {
            ThemePickerDialog(
                currentTheme = chatTheme ?: ChatThemeId.DEFAULT,
                isPremium = myIsPremium,
                onDismiss = { showThemePicker = false },
                onSelect = { theme ->
                    chatVM.setChatTheme(chatId, theme)
                    showThemePicker = false
                }
            )
        }

        if (showPinnedList) {
            PinnedMessagesSheet(
                pinnedMessages = pinnedMessages,
                onDismiss = { showPinnedList = false },
                onJumpTo = { pinned ->
                    showPinnedList = false
                    messages.indexOfFirst { it.id == pinned.id }
                        .takeIf { it != -1 }?.let { index ->
                            coroutineScope.launch { listState.animateScrollToItem(index) }
                        }
                },
                onUnpin = { pinned -> chatVM.unpinMessage(chatId, pinned.id) },
                onUnpinAll = {
                    chatVM.unpinAllMessages(chatId)
                    showPinnedList = false
                }
            )
        }

        if (showChatTimerPicker) {
            MessageTimerPickerDialog(
                title = stringResource(R.string.disappearing_timer_bare_label),
                subtitle = stringResource(R.string.disappearing_timer_dialog_desc),
                currentSec = chatVM.chatDisappearingTimerSec,
                surfaceColor = surfaceColor,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onDismiss = { showChatTimerPicker = false },
                onSelect = { seconds ->
                    chatVM.setDisappearingTimer(chatId, seconds)
                    showChatTimerPicker = false
                }
            )
        }

        if (showMessageTimerPicker) {
            MessageTimerPickerDialog(
                title = stringResource(R.string.per_message_timer_title),
                subtitle = stringResource(R.string.per_message_timer_desc),
                currentSec = pendingTimerOverrideSec ?: chatVM.chatDisappearingTimerSec,
                surfaceColor = surfaceColor,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onDismiss = { showMessageTimerPicker = false },
                onSelect = { seconds ->
                    pendingTimerOverrideSec = seconds
                    showMessageTimerPicker = false
                }
            )
        }

        if (showScheduleDialog) {
            ScheduleSendDialog(
                surfaceColor = surfaceColor,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onDismiss = { showScheduleDialog = false },
                onConfirm = { scheduledDate ->
                    val editTarget = editingMessage
                    if (editTarget != null) {
                        if (input.isNotBlank()) {
                            chatVM.editMessage(
                                chatId = chatId,
                                messageId = editTarget.id,
                                newText = input,
                                onError = { reason ->
                                    val message = if (reason == "forbidden") editForbiddenText else editFailedText
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                            input = ""
                            editingMessage = null
                        }
                        showScheduleDialog = false
                        return@ScheduleSendDialog
                    }
                    if (input.isNotBlank()) {
                        chatVM.sendMessage(
                            chatId = chatId,
                            text = input,
                            replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (!replyMessage?.mediaUrls.isNullOrEmpty()) albumMessageFallback else if (replyMessage?.mediaUrl != null) photoMessageFallback else if (replyMessage?.circleVideoUrl != null) circleVideoMessageFallback else if (replyMessage?.voiceUrl != null) voiceMessageFallback else null,
                            replyName = if (replyMessage == null) null
                            else if (replyMessage?.senderId == myUid) youLabel
                            else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                            else if (chatVM.isGroupChat) replyMessage?.senderName
                            else chatTitle,
                            timerOverrideSec = pendingTimerOverrideSec,
                            scheduledFor = scheduledDate
                        )
                        input = ""
                        replyMessage = null
                        pendingTimerOverrideSec = null
                        chatVM.clearDraftNow(chatId)
                    }
                    showScheduleDialog = false
                }
            )
        }

        fullScreenImageUrl?.let { url ->
            FullScreenImageViewer(
                imageUrl = url,
                onDismiss = { fullScreenImageUrl = null }
            )
        }

        activeEffectKey?.let { key ->
            MessageEffectOverlay(
                effectKey = key,
                modifier = Modifier.fillMaxSize(),
                onFinished = { activeEffectKey = null }
            )
        }

        if (showAttachSheet) {
            AttachmentSheet(
                containerColor = surfaceColor,
                contentColor = textPrimaryColor,
                onPickGallery = {
                    showAttachSheet = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onRecordVideoMessage = {
                    showAttachSheet = false
                    openCircleRecorder(false)
                },
                onDismiss = { showAttachSheet = false }
            )
        }

        reactorsFor?.let { (messageId, emoji) ->
            val target = messages.find { it.id == messageId }
            if (target == null || target.reactions.isEmpty()) {
                reactorsFor = null
            } else {
                ReactorsSheet(
                    message = target,
                    initialEmoji = emoji,
                    myUid = myUid,
                    profileOf = { uid -> chatVM.reactorProfile(uid) },
                    containerColor = surfaceColor,
                    textColor = textPrimaryColor,
                    secondaryColor = textSecondaryColor,
                    accent = MayasTheme.GlowPurple,
                    onDismiss = { reactorsFor = null }
                )
            }
        }

        fullScreenVideoKey?.let { key ->
            FullScreenVideoDialog(mediaKey = key, onDismiss = { fullScreenVideoKey = null })
        }

        if (showVideoCircleRecorder) {
            VideoCircleRecorderDialog(
                autoStart = autoStartCircle,
                onSend = { file, durationSec ->
                    chatVM.sendCircleVideoMessage(
                        chatId = chatId,
                        videoFile = file,
                        durationSec = durationSec,
                        replyText = replyTextOf(),
                        replyName = replyNameOf()
                    )
                    replyMessage = null
                },
                onDismiss = {
                    showVideoCircleRecorder = false
                    autoStartCircle = false
                }
            )
        }
    }
}

private fun withContextMainToast(context: Context, message: String) {
    android.os.Handler(context.mainLooper).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun BlockUserConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    containerColor: Color = MayasTheme.Surface,
    titleColor: Color = MayasTheme.TextPrimary,
    textColor: Color = MayasTheme.TextSecondary
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        title = { Text(stringResource(R.string.block_user_confirm_title), color = titleColor) },
        text = {
            Text(
                stringResource(R.string.block_user_confirm_desc),
                color = textColor
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
            ) { Text(stringResource(R.string.block_user)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = textColor)
            }
        }
    )
}

@Composable
fun PinnedMessagesSheet(
    pinnedMessages: List<Message>,
    onDismiss: () -> Unit,
    onJumpTo: (Message) -> Unit,
    onUnpin: (Message) -> Unit,
    onUnpinAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MayasTheme.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MayasTheme.TextSecondary.copy(0.4f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.pinned_messages_title),
                    color = MayasTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (pinnedMessages.size > 1) {
                    TextButton(onClick = onUnpinAll) {
                        Text(stringResource(R.string.unpin_all_action), color = MayasTheme.ErrorRed, fontSize = 13.sp)
                    }
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(pinnedMessages, key = { _, item -> item.id }) { _, pinned ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpTo(pinned) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin, null,
                            modifier = Modifier.size(16.dp),
                            tint = MayasTheme.GlowBlue
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (pinned.senderName.isNotBlank()) {
                                Text(
                                    pinned.senderName,
                                    fontSize = 12.sp,
                                    color = MayasTheme.GlowBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                pinned.text ?: if (pinned.mediaUrl != null) stringResource(R.string.photo_message) else "",
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = MayasTheme.TextSecondary,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onUnpin(pinned) }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Close, null,
                                modifier = Modifier.size(16.dp),
                                tint = MayasTheme.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePickerDialog(
    currentTheme: String,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val themeNames = mapOf(
        ChatThemeId.DEFAULT to stringResource(R.string.chat_theme_default),
        ChatThemeId.PURPLE to stringResource(R.string.chat_theme_purple),
        ChatThemeId.BLUE to stringResource(R.string.chat_theme_blue),
        ChatThemeId.RED to stringResource(R.string.chat_theme_red),
        ChatThemeId.GOLD to stringResource(R.string.chat_theme_gold),
        ChatThemeId.PINK to stringResource(R.string.chat_theme_pink)
    )
    val themes = listOf(
        ChatThemeId.DEFAULT to MayasTheme.BubbleOther,
        ChatThemeId.PURPLE to MayasTheme.GlowPurple,
        ChatThemeId.BLUE to MayasTheme.GlowBlue,
        ChatThemeId.RED to MayasTheme.GlowRed,
        ChatThemeId.GOLD to MayasTheme.GlowGold,
        ChatThemeId.PINK to MayasTheme.GlowPink
    )

    val premiumThemes = listOf(ChatThemeId.GOLD, ChatThemeId.PINK)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MayasTheme.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MayasTheme.TextSecondary.copy(0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.chat_theme_title),
                color = MayasTheme.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val themeLockedText = stringResource(R.string.theme_locked_desc)
            themes.chunked(3).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowThemes.forEach { (name, color) ->
                        val isLocked = premiumThemes.contains(name) && !isPremium
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentTheme == name) 3.dp else 1.dp,
                                        color = if (currentTheme == name) MayasTheme.Accent else MayasTheme.TextSecondary.copy(0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (isLocked) {
                                            Toast.makeText(context, themeLockedText, Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSelect(name)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLocked) {
                                    Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(22.dp))
                                } else if (currentTheme == name) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                themeNames[name] ?: "",
                                color = MayasTheme.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(3 - rowThemes.size) { Spacer(Modifier.width(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun MessageTimerPickerDialog(
    title: String,
    subtitle: String,
    currentSec: Long,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = { Text(title, color = textPrimaryColor) },
        text = {
            Column {
                Text(subtitle, color = textSecondaryColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                MessageTimerPreset.all.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (seconds == MessageTimerPreset.OFF) stringResource(R.string.timer_disabled_label) else formatTimerDuration(seconds),
                            color = textPrimaryColor
                        )
                        if (currentSec == seconds) {
                            Icon(Icons.Default.Check, null, tint = MayasTheme.Accent)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel)) }
        }
    )
}

@Composable
fun ScheduleSendDialog(
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (java.util.Date) -> Unit
) {
    val calendar = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 5) } }
    var hours by remember { mutableStateOf(calendar.get(java.util.Calendar.HOUR_OF_DAY)) }
    var minutes by remember { mutableStateOf(calendar.get(java.util.Calendar.MINUTE)) }
    var daysAhead by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = { Text(stringResource(R.string.send_later_action), color = textPrimaryColor) },
        text = {
            Column {
                Text(
                    stringResource(R.string.scheduled_send_desc),
                    color = textSecondaryColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(stringResource(R.string.days_from_now_label), color = textSecondaryColor, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayLabel = stringResource(R.string.today_label)
                    listOf(0, 1, 2, 3, 7).forEach { d ->
                        FilterChip(
                            selected = daysAhead == d,
                            onClick = { daysAhead = d },
                            label = { Text(if (d == 0) todayLabel else "+$d") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.time_label), color = textSecondaryColor, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = hours.toString().padStart(2, '0'),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 0..23) hours = it } },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.hour_abbrev)) }
                    )
                    Text("  :  ", color = textPrimaryColor)
                    OutlinedTextField(
                        value = minutes.toString().padStart(2, '0'),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 0..59) minutes = it } },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.minute_abbrev)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, daysAhead)
                    set(java.util.Calendar.HOUR_OF_DAY, hours)
                    set(java.util.Calendar.MINUTE, minutes)
                    set(java.util.Calendar.SECOND, 0)
                }
                var date = target.time


                if (date.before(java.util.Date())) {
                    target.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    date = target.time
                }
                onConfirm(date)
            }) { Text(stringResource(R.string.schedule_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel)) }
        }
    )
}

@Composable
fun EmojiPicker(onEmojiSelected: (String) -> Unit) {
    val emojis = EmojiCatalog.all
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),

        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MayasTheme.Surface)
    ) {
        LazyVerticalGrid(columns = GridCells.Adaptive(45.dp), modifier = Modifier.padding(8.dp)) {
            gridItems(emojis) { emoji ->
                Box(
                    Modifier.size(45.dp).clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) { EmojiGlyph(emoji, fontSize = 24.sp) }
            }
        }
    }
}