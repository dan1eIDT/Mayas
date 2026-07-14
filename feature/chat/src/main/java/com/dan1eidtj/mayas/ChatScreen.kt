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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.mayas.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import coil.request.ImageRequest
import com.dan1eidtj.mayas.CallType
import com.dan1eidtj.mayas.core_ui.ui.components.BubbleShape
import com.dan1eidtj.mayas.core_ui.ui.components.BubbleType
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.ui.components.MessageStyle
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.feature.auth.AuthVM
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

            val urlMatcher = Pattern.compile("(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?)").matcher(finalString)
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
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenProfile: (String, Boolean) -> Unit,
    onStartCall: (peerId: String, callType: CallType) -> Unit = { _, _ -> },
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

    val chatTitle = if (chatVM.partnerName == "Группа") "" else chatVM.partnerName
    val chatAvatarUrl = chatVM.partnerAvatarUrl
    val chatUseCustomAvatar = chatVM.partnerUseCustomAvatar
    val chatProfileIcon = chatVM.partnerProfileIcon ?: "default"
    val chatProfileGlow = chatVM.partnerProfileGlow ?: "purple"
    val chatEmoji = chatVM.partnerEmoji

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
    val pinnedMessageId = chatVM.pinnedMessageId
    val pinnedMessageText = chatVM.pinnedMessageText
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val profileTargetId =
                                            (if (chatVM.isGroupChat) chatId else partnerUid).orEmpty()
                                        if (profileTargetId.isNotBlank()) {
                                            onOpenProfile(profileTargetId, chatVM.isGroupChat)
                                        }
                                    }
                                ) {
                                    MayasAvatar(
                                        url = chatAvatarUrl,
                                        icon = chatProfileIcon,
                                        glowColor = partnerGlowColor,
                                        isPremium = partnerIsPremium && !chatVM.isGroupChat,
                                        useCustomAvatar = chatUseCustomAvatar,
                                        size = 40.dp,
                                        frameType = if (!chatVM.isGroupChat) chatVM.partnerAvatarFrame else "none"
                                    )
                                    Spacer(Modifier.width(12.dp))
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

                                            if (partnerIsPremium && !chatVM.isGroupChat) {
                                                Spacer(Modifier.width(4.dp))
                                                val vIcon = when(chatVM.partnerVerifiedIcon) {
                                                    "star" -> Icons.Default.Star
                                                    "diamond" -> Icons.Default.Diamond
                                                    "auto_awesome" -> Icons.Default.AutoAwesome
                                                    else -> Icons.Default.Verified
                                                }
                                                Icon(
                                                    imageVector = vIcon,
                                                    contentDescription = "Premium",
                                                    tint = MayasTheme.GlowGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            if (!chatEmoji.isNullOrBlank()) {
                                                val emoji = chatEmoji
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = emoji,
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
                                    }
                                }
                            }
                        )

                        AnimatedVisibility(visible = !pinnedMessageText.isNullOrBlank()) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor)
                                        .clickable {
                                            messages.indexOfFirst { it.id == pinnedMessageId }
                                                .takeIf { it != -1 }?.let { index ->
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(index)
                                                    }
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
                                            "Закрепленное сообщение",
                                            fontSize = 12.sp,
                                            color = MayasTheme.GlowBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            pinnedMessageText.orEmpty(),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = textSecondaryColor,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { chatVM.unpinMessage(chatId) },
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
                                val isMe = msg.senderId == myUid
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
                                            if (isPremiumMsg) {
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
                                                    Text(
                                                        text = (msg.senderName ?: "").take(1)
                                                            .uppercase(),
                                                        color = textPrimaryColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(40.dp))
                                            }
                                        }

                                        Row(
                                            modifier = messageModifier
                                                .clip(bubbleShape)
                                                .clickable { selectedMessage = msg }
                                                .padding(
                                                    start = if (isMe) 14.dp else (if (isLastInChain) 26.dp else 14.dp),
                                                    end = if (isMe) (if (isLastInChain) 26.dp else 14.dp) else 14.dp,
                                                    top = 8.dp,
                                                    bottom = 8.dp
                                                ),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column(modifier = Modifier.weight(1f, fill = false)) {

                                                if (!isMe && isGroupChat && isLastInChain) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = msg.senderName.orEmpty(),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (msg.isPremium) MayasTheme.GlowGold else MayasTheme.GlowPurple
                                                        )
                                                        if (msg.isPremium) {
                                                            Spacer(Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Verified,
                                                                contentDescription = null,
                                                                tint = MayasTheme.GlowGold,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
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
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(msg.mediaUrl)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .padding(bottom = 6.dp)
                                                            .fillMaxWidth()
                                                            .heightIn(max = 300.dp)
                                                            .clip(RoundedCornerShape(8.dp)),
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
                                                    val customTextColor = when (messageStyle) {
                                                        MessageStyle.ICE -> Color(0xFF006064)
                                                        MessageStyle.MATRIX -> MayasTheme.GlowLime
                                                        MessageStyle.GOLD -> Color(0xFF5D4037)
                                                        MessageStyle.FOREST, MessageStyle.SUNSET, MessageStyle.MIDNIGHT -> Color.White
                                                        else -> {
                                                            if (isMe) Color.White
                                                            else textPrimaryColor
                                                        }
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
                                                val secondaryTextColor = when (messageStyle) {
                                                    MessageStyle.ICE -> Color(0xFF006064).copy(alpha = 0.6f)
                                                    MessageStyle.MATRIX -> MayasTheme.GlowLime.copy(alpha = 0.7f)
                                                    MessageStyle.GOLD -> Color(0xFF5D4037).copy(alpha = 0.7f)
                                                    MessageStyle.FOREST, MessageStyle.SUNSET -> Color.White.copy(alpha = 0.7f)
                                                    MessageStyle.MIDNIGHT -> Color.White.copy(alpha = 0.6f)
                                                    else -> {
                                                        if (isMe) Color.White.copy(alpha = 0.7f)
                                                        else timeColor
                                                    }
                                                }

                                                val timeFormat = msg.timestamp?.let { ts ->
                                                    SimpleDateFormat(
                                                        "HH:mm",
                                                        Locale.getDefault()
                                                    ).format(ts)
                                                } ?: "--:--"

                                                Text(
                                                    text = timeFormat,
                                                    fontSize = 11.sp,
                                                    color = secondaryTextColor,
                                                    textAlign = TextAlign.End
                                                )

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

                        Surface(
                            modifier = Modifier.fillMaxWidth().imePadding(),
                            color = surfaceColor,
                            tonalElevation = 8.dp
                        ) {
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
                                        IconButton(
                                            onClick = {
                                                chatVM.sendMessage(
                                                    chatId = chatId,
                                                    text = input,
                                                    replyText = if (!replyMessage?.text.isNullOrBlank()) replyMessage?.text else if (replyMessage?.mediaUrl != null) "📷 Фотография" else if (replyMessage?.voiceUrl != null) "🎤 Голосовое сообщение" else null,
                                                    replyName = if (replyMessage == null) null
                                                    else if (replyMessage?.senderId == myUid) "Вы"
                                                    else if (replyMessage?.senderName == "Система" || replyMessage?.senderName == "Mayas") "Система"
                                                    else if (chatVM.isGroupChat) replyMessage?.senderName
                                                    else chatTitle
                                                )
                                                input = ""
                                                replyMessage = null
                                            },
                                            modifier = Modifier.clip(CircleShape)
                                                .background(MayasTheme.GlowBlue)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                null,
                                                tint = Color.White
                                            )
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
            AlertDialog(
                onDismissRequest = { showBlockUserConfirm = false },
                containerColor = surfaceColor,
                title = { Text("Заблокировать пользователя?", color = textPrimaryColor) },
                text = {
                    Text(
                        "Вы больше не будете получать сообщения от этого пользователя.",
                        color = textSecondaryColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBlockUserConfirm = false
                            chatVM.blockUser(myUid ?: "", partnerUid) {
                                Toast.makeText(context, "Пользователь заблокирован", Toast.LENGTH_SHORT)
                                    .show()
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                    ) { Text("Заблокировать") }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockUserConfirm = false }) {
                        Text("Отмена", color = textSecondaryColor)
                    }
                }
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
    }
}

private fun withContextMainToast(context: Context, message: String) {
    android.os.Handler(context.mainLooper).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
    val themes = listOf(
        ChatThemeId.DEFAULT to MayasTheme.BubbleOther,
        ChatThemeId.PURPLE to MayasTheme.GlowPurple,
        ChatThemeId.BLUE to MayasTheme.GlowBlue,
        ChatThemeId.RED to MayasTheme.GlowRed,
        ChatThemeId.GOLD to MayasTheme.GlowGold,
        ChatThemeId.PINK to MayasTheme.GlowPink
    )

    val premiumThemes = listOf(ChatThemeId.GOLD, ChatThemeId.PINK)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        title = { Text("Выберите тему чата", color = MayasTheme.TextPrimary) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                themes.forEach { (name, color) ->
                    val isLocked = premiumThemes.contains(name) && !isPremium
                    Box(
                        modifier = Modifier
                            .size(50.dp)
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
                            Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
                        } else if (currentTheme == name) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
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
fun EmojiPicker(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf(
        // Лица и эмоции
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🫣", "🤗", "🫡", "🤔", "🫣", "🤭", "🤫", "🤥", "😶", "😶‍🌫️", "😐", "😑", "😬", "🫨", "🫠", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😮‍💨", "😵", "😵‍💫", "🫥", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾",

        // Жесты и люди
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄", "💋", "🩸",

        // Сердечки и одежда
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "👑", "👒", "🎩", "🎓", "🧢", "⛑️", "📿", "💄", "💍", "💼", "🎒", "🧳", "👓", "🕶️", "🥽", "🥼", "🦺", "👔", "👕", "👖", "🧣", "🧤", "🧥", "🧦", "👗", "👘", "🥻", "🩱", "🩲", "🩳", "👙",

        // Животные и природа
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦤", "🪶", "🦩", "🦚", "🦜", "🐊", "🐢", "🦎", "🐍", "🐲", "🐉", "🦕", "🦖", "🐳", "🐋", "🐬", "🦭", "🐟", "🐠", "🐡", "🦈", "🐙", "🐚", "🪸", "🐌", "🦋", "🐛", "🐜", "🐝", "🪲", "🐞", "🦗", "🕷️", "🕸️", "🦂", "🦟", "🪰", "🪱", "🦠", "💐", "🌸", "💮", "🪷", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷", "🌱", "🪴", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘️", "🍀", "🍁", "🍂", "🍃", "🍄", "🌰", "🦀", "🦞", "🦐", "🦑",

        // Еда и напитки
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🥞", "🧇", "🧀", "🍖", "🥩", "🍗", "🍔", "🍟", "🍕", "🌭", "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚", "🍳", "🥘", "🍲", "🫕", "🥣", "🥗", "🍿", "🧈", "🧂", "🥫", "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍣", "🍤", "🥮", "🍡", "🥟", "🥠", "🥡", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬", "🍭", "🍮", "🍯", "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾", "🍷", "🍸", "🍹", "🍺", "🍻", "🥂", "🥃", "🫗", "🥤", "🧋", "🧃", "🧉", "🧊",

        // Космос, погода, спорт и транспорт
        "🌍", "🌎", "🌏", "🌐", "🗺️", "🗾", "🧭", "🏔️", "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️", "🏞️", "🏟️", "🏛️", "🏗️", "🧱", "🪨", "🪵", "🛖", "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋", "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆", "🌇", "🌉", "🌌", "🎠", "🎡", "🎢", "🚂", "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉", "🚊", "🦽", "🦼", "🚲", "🛵", "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃", "🌌", "🎈", "🎉", "🎊", "🎇", "🎆", "🧨", "✨", "🌟", "⭐", "🌙", "🌛", "🌜", "🌚", "🌕", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "❄️", "☃️", "⛄", "🌬️", "💨", "🌪️", "🌫️", "🌊", "💧", "💦", "☔", "⚡", "🔥", "💥"
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