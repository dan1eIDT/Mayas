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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.imageResource
import com.dan1eidtj.mayas.core_ui.ui.components.ninePatchBackground
import com.dan1eidtj.mayas.core_ui.ui.components.NinePatchInsets
import com.dan1eidtj.mayas.ui.R
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
import com.dan1eidtj.mayas.core_ui.ui.components.FullScreenImageViewer
import com.dan1eidtj.mayas.core_ui.ui.components.MessageStyle
import com.dan1eidtj.mayas.core_ui.ui.components.MessageBubbleContainer
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.storage.B2Image
import com.dan1eidtj.mayas.storage.B2MediaClient
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
    val shareIntent = Intent.createChooser(sendIntent, "Поделиться сообщением")
    context.startActivity(shareIntent)
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
) {
    val isMissedCall = message.type == MessageType.CALL &&
            (message.callStatus == CallStatus.MISSED || message.callStatus == CallStatus.DECLINED)
    val contentColor = if (isMissedCall) MayasTheme.GlowRed else textColor

    Box(
        modifier = Modifier
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
                text = "Только администраторы канала могут публиковать сообщения",
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
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    val chatTitle = if (chatVM.partnerName == "Группа") "" else chatVM.partnerName
    val chatAvatarUrl = rememberResolvedAvatarUrl(chatVM.partnerAvatarUrl, chatVM.partnerUseCustomAvatar)
    val chatUseCustomAvatar = chatVM.partnerUseCustomAvatar
    val chatProfileGlow = chatVM.partnerProfileGlow ?: "purple"
    val chatEmoji = chatVM.partnerEmoji

    // Переход из профиля (вкладка "Закреплённые") с конкретным сообщением — как только
    // список сообщений реально загрузился, проматываем к нему один раз.
    var didScrollToTarget by remember(chatId, scrollToMessageId) { mutableStateOf(false) }
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
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = compressImageBytes(context, it)
                    if (bytes != null) {
                        chatVM.sendMediaMessage(
                            chatId = chatId,
                            text = "",
                            fileBytes = bytes,
                            replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (replyMessage?.mediaUrl != null) "📷 Фотография" else null,
                            replyName = if (replyMessage == null) null
                            else if (replyMessage?.senderId == myUid) "Вы"
                            else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                            else if (chatVM.isGroupChat) replyMessage?.senderName
                            else chatTitle
                        )
                        replyMessage = null
                    } else {
                        withContextMainToast(context, "Не удалось обработать изображение")
                    }
                } catch (e: Exception) {
                    Log.e("ChatScreen", "Ошибка отправки медиа", e)
                    withContextMainToast(context, "Не удалось отправить фото")
                }
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
                                    placeholder = { Text("Поиск в чате...", color = overWallpaperSecondaryColor) },
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
                                            text = { Text("Очистить чат", color = textPrimaryColor) },
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
                                            text = { Text("Заблокировать", color = textPrimaryColor) },
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
                                            text = { Text("Пожаловаться", color = textPrimaryColor) },
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
                                                        "Выбрать тему",
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
                                                    if (current > 0) "Таймер: ${formatTimerDuration(current)}"
                                                    else "Таймер исчезающих сообщений",
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
                                                "Закреплено (${pinnedIndex + 1}/${pinnedMessages.size})"
                                            else "Закрепленное сообщение",
                                            fontSize = 12.sp,
                                            color = MayasTheme.GlowBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = if (pinnedMessages.size > 1) {
                                                Modifier.clickable { showPinnedList = true }
                                            } else Modifier
                                        )
                                        Text(
                                            currentPinned?.text
                                                ?: if (currentPinned?.mediaUrl != null) "📷 Фотография" else "",
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            reverseLayout = true
                        ) {
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
                                        onClick = onSystemClick
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

                                        MessageBubbleContainer(
                                            messageStyle = messageStyle,
                                            bubbleShape = bubbleShape,
                                            messageModifier = messageModifier,
                                            normalPadding = PaddingValues(
                                                start = if (isMe) 14.dp else (if (isLastInChain) 26.dp else 14.dp),
                                                end = if (isMe) (if (isLastInChain) 26.dp else 14.dp) else 14.dp,
                                                top = 8.dp,
                                                bottom = 8.dp
                                            ),
                                            onClick = { selectedMessage = msg }
                                        ) {
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
                                                                    msg.senderId.isNotBlank() && msg.senderId == chatVM.chatOwnerId -> "владелец"
                                                                    msg.senderId.isNotBlank() && msg.senderId in chatVM.chatAdmins -> "админ"
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
                                                                "Переслано от ${msg.forwardedFromName}",
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
                                                                Text(
                                                                    msg.replyToText.orEmpty(),
                                                                    fontSize = 12.sp,
                                                                    color = textSecondaryColor,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (!msg.mediaUrl.isNullOrBlank()) {
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
                                                                            fullScreenImageUrl = B2MediaClient.resolveDownloadUrl(mediaKey)
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

                                                        ClickableText(
                                                            text = parsedText,
                                                            style = TextStyle(
                                                                fontSize = fontSize.sp,
                                                                color = customTextColor
                                                            ),
                                                            onClick = { offset ->
                                                                parsedText.getStringAnnotations(
                                                                    "URL",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    val intent = Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        Uri.parse(annotation.item)
                                                                    )
                                                                    context.startActivity(intent)
                                                                    return@ClickableText
                                                                }
                                                                parsedText.getStringAnnotations(
                                                                    "USERNAME",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    Toast.makeText(
                                                                        context,
                                                                        "@${annotation.item} кликнут",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@ClickableText
                                                                }
                                                                parsedText.getStringAnnotations(
                                                                    "HASHTAG",
                                                                    offset,
                                                                    offset
                                                                ).firstOrNull()?.let { annotation ->
                                                                    Toast.makeText(
                                                                        context,
                                                                        "#${annotation.item} кликнут",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@ClickableText
                                                                }
                                                                selectedMessage = msg
                                                            }
                                                        )
                                                    }
                                                }

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

                                                    if (msg.messageState == MessageState.SCHEDULED) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Schedule,
                                                            contentDescription = "Запланировано",
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }
                                                    if (msg.ttlSeconds > 0) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Timer,
                                                            contentDescription = "Исчезающее сообщение",
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                    }
                                                    if (msg.isSilent) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.NotificationsOff,
                                                            contentDescription = "Без звука",
                                                            tint = secondaryTextColor,
                                                            modifier = Modifier.size(12.dp)
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
                                                        Icon(
                                                            imageVector = statusIcon,
                                                            contentDescription = null,
                                                            tint = if (messageStyle != null) secondaryTextColor else (if (msg.status == 2) MayasTheme.GlowSky else textSecondaryColor),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (msg.reactions.isNotEmpty()) {
                                            val groupedReactions = msg.reactions.values.groupBy { it }.mapValues { it.value.size }
                                            Row(
                                                modifier = Modifier
                                                    .padding(top = 2.dp, start = if (isMe) 0.dp else 40.dp, end = if (isMe) 14.dp else 0.dp)
                                                    .background(surfaceColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                                    .border(1.dp, textPrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                groupedReactions.forEach { (emoji, count) ->
                                                    val isMyReaction = msg.reactions[myUid] == emoji
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isMyReaction) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                chatVM.toggleReaction(chatId, msg.id, emoji)
                                                            }
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(emoji, fontSize = 14.sp)
                                                        if (count > 0) {
                                                            Spacer(Modifier.width(3.dp))
                                                            Text(
                                                                count.toString(),
                                                                fontSize = 11.sp,
                                                                color = if (isMyReaction) accentColor else textPrimaryColor,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = selectedMessage?.id == msg.id,
                                        onDismissRequest = { selectedMessage = null },
                                        modifier = Modifier.background(surfaceColor)
                                    ) {
                                        if (msg.messageState == MessageState.SCHEDULED && msg.senderId == myUid) {
                                            DropdownMenuItem(
                                                text = { Text("Отменить отправку", color = MayasTheme.ErrorRed) },
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
                                            text = { Text("Ответить", color = textPrimaryColor) },
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
                                        val quickReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            quickReactions.forEach { emoji ->
                                                val isSelected = msg.reactions[myUid] == emoji
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MayasTheme.GlowPurple.copy(alpha = 0.2f) else Color.Transparent)
                                                        .clickable {
                                                            chatVM.toggleReaction(chatId, msg.id, emoji)
                                                            selectedMessage = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(emoji, fontSize = 20.sp)
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = textPrimaryColor.copy(0.1f))

                                        DropdownMenuItem(
                                            text = { Text("Копировать", color = textPrimaryColor) },
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
                                                    "Текст скопирован",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedMessage = null
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Поделиться", color = textPrimaryColor) },
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
                                        DropdownMenuItem(
                                            text = { Text("Закрепить", color = textPrimaryColor) },
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
                                                    "Сообщение закреплено",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedMessage = null
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Переслать в Избранное", color = textPrimaryColor) },
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
                                                            "Переслано в Избранное",
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
                                                        "Удалить",
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
                                                        "Сообщение удалено",
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
                                        val replyName = if (reply.senderId == myUid) "Вы"
                                        else if (reply.senderName == "Система" || reply.senderName == "Mayas") "Система"
                                        else if (chatVM.isGroupChat) reply.senderName
                                        else chatTitle
                                        Text(
                                            text = replyName,
                                            fontSize = 11.sp,
                                            color = MayasTheme.GlowPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (!reply.text.isNullOrBlank()) reply.text else "📷 Фотография",
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
                                                if (pendingTimerOverrideSec != null) "Таймер для сообщения: ${formatTimerDuration(effectiveTimerSec)}"
                                                else "Таймер чата: ${formatTimerDuration(effectiveTimerSec)}",
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
                                                    "Сообщение..",
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

                                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                                            Icon(Icons.Default.Image, null, tint = textSecondaryColor)
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
                                                fun doSend(silent: Boolean) {
                                                    chatVM.sendMessage(
                                                        chatId = chatId,
                                                        text = input,
                                                        replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (replyMessage?.mediaUrl != null) "📷 Фотография" else if (replyMessage?.voiceUrl != null) "🎤 Голосовое сообщение" else null,
                                                        replyName = if (replyMessage == null) null
                                                        else if (replyMessage?.senderId == myUid) "Вы"
                                                        else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                                                        else if (chatVM.isGroupChat) replyMessage?.senderName
                                                        else chatTitle,
                                                        timerOverrideSec = pendingTimerOverrideSec,
                                                        silent = silent
                                                    )
                                                    input = ""
                                                    replyMessage = null
                                                    pendingTimerOverrideSec = null
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
                                                                    if (pendingTimerOverrideSec != null) "Таймер: ${formatTimerDuration(pendingTimerOverrideSec!!)}" else "Таймер сообщения",
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
                                                            text = { Text("Отправить без звука", color = textPrimaryColor) },
                                                            leadingIcon = { Icon(Icons.Outlined.NotificationsOff, null, tint = MayasTheme.Accent) },
                                                            onClick = {
                                                                showSendOptionsMenu = false
                                                                doSend(silent = true)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Отправить позже", color = textPrimaryColor) },
                                                            leadingIcon = { Icon(Icons.Outlined.Schedule, null, tint = MayasTheme.Accent) },
                                                            onClick = {
                                                                showSendOptionsMenu = false
                                                                showScheduleDialog = true
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                val isRecording = chatVM.isRecording
                                                val recordingScale by animateFloatAsState(
                                                    targetValue = if (isRecording) 1.2f else 1f,
                                                    animationSpec = if (isRecording) {
                                                        infiniteRepeatable(
                                                            animation = tween(800),
                                                            repeatMode = RepeatMode.Reverse
                                                        )
                                                    } else {
                                                        tween(200)
                                                    },
                                                    label = "micPulse"
                                                )

                                                val recorder = remember { VoiceRecorder(context) }
                                                DisposableEffect(Unit) {
                                                    onDispose {
                                                        recorder.stop()
                                                    }
                                                }
                                                val recordPermissionLauncher = rememberLauncherForActivityResult(
                                                    ActivityResultContracts.RequestPermission()
                                                ) { isGranted ->
                                                    if (isGranted) {

                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (ContextCompat.checkSelfPermission(
                                                                context,
                                                                android.Manifest.permission.RECORD_AUDIO
                                                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                        ) {
                                                            recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                            return@IconButton
                                                        }

                                                        if (!isRecording) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            recorder.start()
                                                            chatVM.startRecording()
                                                        } else {
                                                            val audioFile = recorder.stop()
                                                            val bytes = audioFile?.readBytes()
                                                            chatVM.stopRecording(
                                                                chatId = chatId,
                                                                audioBytes = bytes,
                                                                replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (replyMessage?.mediaUrl != null) "📷 Фотография" else if (replyMessage?.voiceUrl != null) "🎤 Голосовое сообщение" else null,
                                                                replyName = if (replyMessage == null) null
                                                                else if (replyMessage?.senderId == myUid) "Вы"
                                                                else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                                                                else if (chatVM.isGroupChat) replyMessage?.senderName
                                                                else chatTitle
                                                            )
                                                            replyMessage = null
                                                        }
                                                    },
                                                    modifier = Modifier.graphicsLayer {
                                                        scaleX = recordingScale
                                                        scaleY = recordingScale
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Mic,
                                                        null,
                                                        tint = if (isRecording) MayasTheme.ErrorRed else textSecondaryColor
                                                    )
                                                }

                                                if (isRecording) {
                                                    Text(
                                                        "${chatVM.recordingDuration}s",
                                                        color = MayasTheme.ErrorRed,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    )
                                                }
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
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                containerColor = surfaceColor,
                title = { Text("Пожаловаться на пользователя", color = textPrimaryColor) },
                text = {
                    Column {
                        Text("Опишите причину:", color = textSecondaryColor, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reportText,
                            onValueChange = { reportText = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            placeholder = { Text("Спам, оскорбления и т.д.") },
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
                                Toast.makeText(context, "Жалоба отправлена", Toast.LENGTH_SHORT)
                                    .show()
                                showReportDialog = false
                                reportText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowPurple)
                    ) { Text("Отправить") }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Отмена", color = textSecondaryColor)
                    }
                }
            )
        }

        if (showClearChatConfirm) {
            AlertDialog(
                onDismissRequest = { showClearChatConfirm = false },
                containerColor = surfaceColor,
                title = { Text("Очистить чат?", color = textPrimaryColor) },
                text = {
                    Text(
                        "Все сообщения будут удалены без возможности восстановления.",
                        color = textSecondaryColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearChatConfirm = false
                            chatVM.clearChat(chatId) {
                                Toast.makeText(context, "Чат очищен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                    ) { Text("Очистить") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatConfirm = false }) {
                        Text("Отмена", color = textSecondaryColor)
                    }
                }
            )
        }

        if (showBlockUserConfirm) {
            BlockUserConfirmDialog(
                onConfirm = {
                    showBlockUserConfirm = false
                    chatVM.blockUser(myUid ?: "", partnerUid) {
                        Toast.makeText(context, "Пользователь заблокирован", Toast.LENGTH_SHORT)
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
                title = "Таймер исчезающих сообщений",
                subtitle = "Новые сообщения в этом чате будут удаляться автоматически",
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
                title = "Таймер для этого сообщения",
                subtitle = "Действует только на следующее отправленное сообщение",
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
                    if (input.isNotBlank()) {
                        chatVM.sendMessage(
                            chatId = chatId,
                            text = input,
                            replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (replyMessage?.mediaUrl != null) "📷 Фотография" else if (replyMessage?.voiceUrl != null) "🎤 Голосовое сообщение" else null,
                            replyName = if (replyMessage == null) null
                            else if (replyMessage?.senderId == myUid) "Вы"
                            else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                            else if (chatVM.isGroupChat) replyMessage?.senderName
                            else chatTitle,
                            timerOverrideSec = pendingTimerOverrideSec,
                            scheduledFor = scheduledDate
                        )
                        input = ""
                        replyMessage = null
                        pendingTimerOverrideSec = null
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
        title = { Text("Заблокировать пользователя?", color = titleColor) },
        text = {
            Text(
                "Вы больше не будете получать сообщения от этого пользователя.",
                color = textColor
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
            ) { Text("Заблокировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = textColor)
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
                    "Закреплённые сообщения",
                    color = MayasTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (pinnedMessages.size > 1) {
                    TextButton(onClick = onUnpinAll) {
                        Text("Открепить все", color = MayasTheme.ErrorRed, fontSize = 13.sp)
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
                                pinned.text ?: if (pinned.mediaUrl != null) "📷 Фотография" else "",
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
        ChatThemeId.DEFAULT to "Обычная",
        ChatThemeId.PURPLE to "Фиолетовая",
        ChatThemeId.BLUE to "Голубая",
        ChatThemeId.RED to "Красная",
        ChatThemeId.GOLD to "Золотая",
        ChatThemeId.PINK to "Розовая"
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
                "Тема чата",
                color = MayasTheme.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

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
                                            Toast.makeText(context, "Эта тема доступна только в Mayas+", Toast.LENGTH_SHORT).show()
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
                            if (seconds == MessageTimerPreset.OFF) "Выключено" else formatTimerDuration(seconds),
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
            TextButton(onClick = onDismiss) { Text("Отмена") }
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
        title = { Text("Отправить позже", color = textPrimaryColor) },
        text = {
            Column {
                Text(
                    "Сообщение уйдёт автоматически в выбранное время",
                    color = textSecondaryColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text("Через сколько дней", color = textSecondaryColor, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 1, 2, 3, 7).forEach { d ->
                        FilterChip(
                            selected = daysAhead == d,
                            onClick = { daysAhead = d },
                            label = { Text(if (d == 0) "Сегодня" else "+$d") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Время", color = textSecondaryColor, fontSize = 12.sp)
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
                        label = { Text("Ч") }
                    )
                    Text("  :  ", color = textPrimaryColor)
                    OutlinedTextField(
                        value = minutes.toString().padStart(2, '0'),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 0..59) minutes = it } },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        label = { Text("М") }
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
            }) { Text("Запланировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EmojiPicker(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf(

        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "😊",
        "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
        "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳",
        "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩",
        "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶",
        "😱", "😨", "😰", "😥", "😓", "🫣", "🤗", "🫡", "🤔", "🫣", "🤭", "🤫", "🤥",
        "😶", "😶‍🌫️", "😐", "😑", "😬", "🫨", "🫠", "🙄", "😯", "😦", "😧", "😮", "😲",
        "🥱", "😴", "🤤", "😪", "😮‍💨", "😵", "😵‍💫", "🫥", "🤐", "🥴", "🤢", "🤮", "🤧",
        "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️",
        "👽", "👾", "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾",


        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰",
        "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍", "👎",
        "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️",
        "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🧠",
        "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄", "💋", "🩸",


        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹",
        "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "👑", "👒", "🎩", "🎓",
        "🧢", "⛑️", "📿", "💄", "💍", "💼", "🎒", "🧳", "👓", "🕶️", "🥽", "🥼", "🦺", "👔",
        "👕", "👖", "🧣", "🧤", "🧥", "🧦", "👗", "👘", "🥻", "🩱", "🩲", "🩳", "👙",


        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮",
        "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥",
        "🦆", "🦅", "🦉", "🦤", "🪶", "🦩", "🦚", "🦜", "🐊", "🐢", "🦎", "🐍", "🐲", "🐉", "🦕",
        "🦖", "🐳", "🐋", "🐬", "🦭", "🐟", "🐠", "🐡", "🦈", "🐙", "🐚", "🪸", "🐌", "🦋", "🐛",
        "🐜", "🐝", "🪲", "🐞", "🦗", "🕷️", "🕸️", "🦂", "🦟", "🪰", "🪱", "🦠", "💐", "🌸", "💮",
        "🪷", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷", "🌱", "🪴", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿",
        "☘️", "🍀", "🍁", "🍂", "🍃", "🍄", "🌰", "🦀", "🦞", "🦐", "🦑",


        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍",
        "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅",
        "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🥞", "🧇", "🧀", "🍖", "🥩", "🍗", "🍔", "🍟",
        "🍕", "🌭", "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚", "🍳", "🥘", "🍲", "🫕", "🥣", "🥗",
        "🍿", "🧈", "🧂", "🥫", "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍣", "🍤", "🥮", "🍡",
        "🥟", "🥠", "🥡", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬", "🍭",
        "🍮", "🍯", "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾", "🍷", "🍸", "🍹", "🍺", "🍻", "🥂", "🥃",
        "🫗", "🥤", "🧋", "🧃", "🧉", "🧊",


        "🌍", "🌎", "🌏", "🌐", "🗺️", "🗾", "🧭", "🏔️", "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️",
        "🏝️", "🏞️", "🏟️", "🏛️", "🏗️", "🧱", "🪨", "🪵", "🛖", "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣",
        "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽",
        "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋", "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆",
        "🌇", "🌉", "🌌", "🎠", "🎡", "🎢", "🚂", "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉",
        "🚊", "🦽", "🦼", "🚲", "🛵", "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠",
        "🚟", "🚃", "🌌", "🎈", "🎉", "🎊", "🎇", "🎆", "🧨", "✨", "🌟", "⭐", "🌙", "🌛",
        "🌜", "🌚", "🌕", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "❄️", "☃️",
        "⛄", "🌬️", "💨", "🌪️", "🌫️", "🌊", "💧", "💦", "☔", "⚡", "🔥", "💥"
    )
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
                ) { Text(emoji, fontSize = 24.sp) }
            }
        }
    }
}