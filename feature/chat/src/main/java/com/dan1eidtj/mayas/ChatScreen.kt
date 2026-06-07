@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dan1eidtj.data.SharedContentManager
import com.dan1eidtj.mayas.core.ui.theme.*
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


// --- КАСТОМНЫЕ ФОРМЫ ДЛЯ ХВОСТИКОВ В СТИЛЕ MAYAS ---
class IncomingBubbleShape(private val cornerRadius: Float = 36f, private val drawTail: Boolean = true) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            if (drawTail) {
                moveTo(cornerRadius + 12f, 0f)
                lineTo(size.width - cornerRadius, 0f)
                quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
                lineTo(size.width, size.height - cornerRadius)
                quadraticBezierTo(size.width, size.height, size.width - cornerRadius, size.height)
                lineTo(16f + cornerRadius, size.height)
                quadraticBezierTo(16f, size.height, 16f, size.height - cornerRadius)
                lineTo(16f, 12f)
                quadraticBezierTo(16f, 0f, 0f, 0f)
                quadraticBezierTo(12f, 0f, cornerRadius + 12f, 0f)
            } else {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerRadius)))
            }
            close()
        }
        return Outline.Generic(path)
    }
}

class OutgoingBubbleShape(private val cornerRadius: Float = 36f, private val drawTail: Boolean = true) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            if (drawTail) {
                moveTo(cornerRadius, 0f)
                lineTo(size.width - cornerRadius - 12f, 0f)
                quadraticBezierTo(size.width - 12f, 0f, size.width - 12f, cornerRadius)
                lineTo(size.width - 12f, size.height - 12f)
                quadraticBezierTo(size.width - 12f, size.height, size.width, size.height)
                quadraticBezierTo(size.width - 16f, size.height, size.width - 16f - cornerRadius, size.height)
                quadraticBezierTo(size.width - 12f - cornerRadius, size.height, size.width - 12f - cornerRadius, size.height)
                lineTo(cornerRadius, size.height)
                quadraticBezierTo(0f, size.height, 0f, size.height - cornerRadius)
                lineTo(0f, cornerRadius)
                quadraticBezierTo(0f, 0f, cornerRadius, 0f)
            } else {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerRadius)))
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// --- УМНОЕ СКАНИРОВАНИЕ И РАЗБОР ССЫЛОК, ХЭШТЕГОВ И ЮЗЕРНЕЙМОВ ---
@Composable
fun rememberParsedMessageText(text: String, accentColor: Color): AnnotatedString {
    return remember(text, accentColor) {
        buildAnnotatedString {
            append(text)

            val urlMatcher = Pattern.compile("(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?)").matcher(text)
            while (urlMatcher.find()) {
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), urlMatcher.start(), urlMatcher.end())
                addStringAnnotation("URL", urlMatcher.group(), urlMatcher.start(), urlMatcher.end())
            }

            val userMatcher = Pattern.compile("@([A-Za-z0-9_]+)").matcher(text)
            while (userMatcher.find()) {
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold), userMatcher.start(), userMatcher.end())
                addStringAnnotation("USERNAME", userMatcher.group(1), userMatcher.start(), userMatcher.end())
            }

            val hashtagMatcher = Pattern.compile("#([A-Za-z0-9_А-Яа-я]+)").matcher(text)
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

@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,

) {

    val chatVM: ChatVM = viewModel()
    LaunchedEffect(chatId) {
        chatVM.clearUnreadCount(chatId)
    }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
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

    val messages = chatVM.messages.reversed()
    var input by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var replyMessage by remember { mutableStateOf<Message?>(null) }

    LaunchedEffect(SharedContentManager.sharedText) {
        SharedContentManager.sharedText?.let { sharedText ->
            input = sharedText
            SharedContentManager.sharedText = null
        }
    }

    LaunchedEffect(chatId) {
        chatVM.observeChat(chatId)
    }

    LaunchedEffect(input) {

        chatVM.setTyping(chatId, input.isNotBlank())
    }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        while (true) {
            db.collection("users").document(myUid)
                .update("lastSeen", FieldValue.serverTimestamp())
            delay(30_000)
        }
    }

    LaunchedEffect(messages.isNotEmpty()) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val partnerName = chatVM.partnerName
    val lastSeenText = chatVM.lastSeenText
    val typingText = chatVM.typingText
    val partnerUid = chatVM.partnerUid
    val pinnedMessage = chatVM.pinnedMessage
    val partnerAvatarUrl = chatVM.partnerAvatarUrl
    val partnerEmoji = chatVM.partnerEmoji

    val partnerUseCustomAvatar = chatVM.partnerUseCustomAvatar
    val partnerProfileIcon = chatVM.partnerProfileIcon
    val partnerProfileGlow = chatVM.partnerProfileGlow

    val partnerGlowColor = when (partnerProfileGlow) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "gold" -> MayasTheme.GlowGold
        "red" -> MayasTheme.GlowRed
        else -> MayasTheme.GlowPurple
    }

    var expanded by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Toast.makeText(context, "Фото выбрано: $it", Toast.LENGTH_SHORT).show()
        }
    }

    fun playMessageSound() {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            // TODO: MediaPlayer.create(context, R.raw.new_msg).start()
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textPrimaryColor)
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val profileTargetId = (if (chatVM.isGroupChat) chatId else partnerUid).orEmpty()
                                if (profileTargetId.isNotBlank()) {
                                    onOpenProfile(profileTargetId)
                                }
                            }
                        ) {
                            // --- КРУГЛАЯ АВАТАРКА С ПОДДЕРЖКОЙ ЗАГРУЗКИ ИЗ СЕТИ ---
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(partnerGlowColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (partnerUseCustomAvatar && partnerAvatarUrl?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = partnerAvatarUrl,
                                        contentDescription = "Partner Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        partnerGlowColor.copy(alpha = 0.7f),
                                                        Color(0xFF111214)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProfileIcon(partnerProfileIcon)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                // --- НИКНЕЙМ + СИНХРОНИЗИРОВАННЫЙ ЭМОДЗИ РЯДОМ ---
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = partnerName.orEmpty(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!partnerEmoji.isNullOrBlank()) {
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = partnerEmoji,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                val statusText = if (!typingText.isNullOrBlank()) typingText else lastSeenText.orEmpty()
                                AnimatedContent<String>(
                                    targetState = statusText,
                                    label = "StatusAnimation",
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                                    }
                                ) { text ->
                                    Text(text, fontSize = 12.sp, color = textSecondaryColor)
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { Toast.makeText(context, "Звонки скоро будут!", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Call, null, tint = textPrimaryColor)
                        }

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, null, tint = textPrimaryColor)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(surfaceColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Очистить чат", color = textPrimaryColor) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MayasTheme.ErrorRed) },
                                    onClick = {
                                        expanded = false
                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("chats").document(chatId)
                                            .collection("messages").get().addOnSuccessListener { snapshot ->
                                                val batch = db.batch()
                                                snapshot.documents.forEach { batch.delete(it.reference) }
                                                batch.commit()
                                            }
                                        Toast.makeText(context, "Чат очищен", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Заблокировать", color = textPrimaryColor) },
                                    leadingIcon = { Icon(Icons.Default.Block, null, tint = MayasTheme.ErrorRed) },
                                    onClick = {
                                        expanded = false
                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("users").document(myUid)
                                            .set(mapOf("blocked" to FieldValue.arrayUnion(partnerUid)), SetOptions.merge())
                                        Toast.makeText(context, "Пользователь заблокирован", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Пожаловаться", color = textPrimaryColor) },
                                    leadingIcon = { Icon(Icons.Default.Report, null, tint = MayasTheme.GlowGold) },
                                    onClick = { expanded = false; showReportDialog = true }
                                )
                            }
                        }
                    }
                )

                AnimatedVisibility(visible = !pinnedMessage.isNullOrBlank()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceColor)
                                .clickable {
                                    val index = messages.indexOfFirst { it.text == pinnedMessage }
                                    if (index != -1) {
                                        coroutineScope.launch { listState.animateScrollToItem(index) }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = MayasTheme.GlowBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Закрепленное сообщение", fontSize = 12.sp, color = MayasTheme.GlowBlue, fontWeight = FontWeight.Bold)
                                Text(pinnedMessage.orEmpty(), fontSize = 13.sp, maxLines = 1, color = textSecondaryColor, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { chatVM.unpinMessage(chatId) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = textSecondaryColor)
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = textPrimaryColor.copy(0.1f))
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(chatBackground)) {
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
                            val isLastInChain = nextMsg == null || nextMsg.senderId != msg.senderId

                            val bubbleColor = if (isMe) bubbleMineColor else bubbleOtherColor
                            val timeColor = textSecondaryColor

                            val bubbleShape = if (isMe) OutgoingBubbleShape(drawTail = isLastInChain) else IncomingBubbleShape(drawTail = isLastInChain)

                            val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart

                            val startPadding = if (isMe) 60.dp else 0.dp
                            val endPadding = if (isMe) 0.dp else 60.dp

                            val replyThreshold = with(density) { 50.dp.toPx() }
                            val maxOffsetX = with(density) { 80.dp.toPx() }
                            var offsetX by remember { mutableStateOf(0f) }
                            var hasVibrated by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = startPadding, end = endPadding)
                                    .pointerInput(msg.id) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (offsetX < -replyThreshold) {
                                                    replyMessage = msg
                                                }
                                                offsetX = 0f
                                                hasVibrated = false
                                            },
                                            onDragCancel = {
                                                offsetX = 0f
                                                hasVibrated = false
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                if (dragAmount < 0 || offsetX < 0) {
                                                    val newOffset = offsetX + dragAmount
                                                    offsetX = newOffset.coerceIn(-maxOffsetX, 0f)

                                                    if (offsetX < -replyThreshold && !hasVibrated) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        hasVibrated = true
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = alignment
                            ) {
                                if (offsetX < 0) {
                                    val alpha = (offsetX.absoluteValue / replyThreshold).coerceIn(0f, 1f)
                                    val scale = (offsetX.absoluteValue / replyThreshold).coerceIn(0.6f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 16.dp)
                                            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = null,
                                            tint = MayasTheme.GlowPurple
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) },
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
                                                    text = (msg.senderName ?: "").take(1).uppercase(),
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
                                        modifier = Modifier
                                            .clip(bubbleShape)
                                            .background(bubbleColor)
                                            .clickable { selectedMessage = msg }
                                            .padding(
                                                start = if (isMe) 14.dp else if (isLastInChain) 22.dp else 14.dp,
                                                end = if (isMe) (if (isLastInChain) 22.dp else 14.dp) else 14.dp,
                                                top = 8.dp,
                                                bottom = 8.dp
                                            ),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f, fill = false)) {

                                            if (!isMe && isGroupChat && isLastInChain) {
                                                Text(
                                                    text = msg.senderName.orEmpty(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MayasTheme.GlowPurple,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }

                                            if (!msg.replyToText.isNullOrBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(bottom = 6.dp)
                                                        .background(textPrimaryColor.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.width(3.dp).height(26.dp).background(MayasTheme.GlowPurple))
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(msg.replyToName.orEmpty(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MayasTheme.GlowPurple)
                                                        Text(msg.replyToText.orEmpty(), fontSize = 12.sp, color = textSecondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }

                                            val parsedText = rememberParsedMessageText(text = msg.text.orEmpty(), accentColor = accentColor)

                                            ClickableText(
                                                text = parsedText,
                                                style = TextStyle(fontSize = 16.sp, color = textPrimaryColor),
                                                onClick = { offset ->
                                                    parsedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                                        context.startActivity(intent)
                                                        return@ClickableText
                                                    }
                                                    parsedText.getStringAnnotations("USERNAME", offset, offset).firstOrNull()?.let { annotation ->
                                                        Toast.makeText(context, "@${annotation.item} кликнут", Toast.LENGTH_SHORT).show()
                                                        return@ClickableText
                                                    }
                                                    parsedText.getStringAnnotations("HASHTAG", offset, offset).firstOrNull()?.let { annotation ->
                                                        Toast.makeText(context, "#${annotation.item} кликнут", Toast.LENGTH_SHORT).show()
                                                        return@ClickableText
                                                    }
                                                    selectedMessage = msg
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            val timeFormat = msg.timestamp?.let { ts ->
                                                val date = when (ts) {
                                                    is java.util.Date -> ts
                                                    is Timestamp -> ts.toDate()
                                                    else -> {
                                                        try {
                                                            val method = ts.javaClass.getMethod("toDate")
                                                            method.invoke(ts) as? java.util.Date
                                                        } catch (e: Exception) {
                                                            null
                                                        }
                                                    }
                                                }
                                                date?.let {
                                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                                                }
                                            } ?: "--:--"

                                            Text(
                                                text = timeFormat,
                                                fontSize = 11.sp,
                                                color = timeColor,
                                                textAlign = TextAlign.End
                                            )

                                            if (isMe) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (msg.readBy.size > 1) Icons.Default.DoneAll else Icons.Default.Done,
                                                    contentDescription = null,
                                                    tint = MayasTheme.Online,
                                                    modifier = Modifier.size(15.dp)
                                                )
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
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = textSecondaryColor) },
                                        onClick = {
                                            replyMessage = msg
                                            selectedMessage = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Копировать", color = textPrimaryColor) },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = textSecondaryColor) },
                                        onClick = {
                                            val clip = ClipData.newPlainText("MayasMessage", msg.text.orEmpty())
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                                            selectedMessage = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Поделиться", color = textPrimaryColor) },
                                        leadingIcon = { Icon(Icons.Outlined.Share, null, tint = textSecondaryColor) },
                                        onClick = {
                                            shareText(context, msg.text.orEmpty())
                                            selectedMessage = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Закрепить", color = textPrimaryColor) },
                                        leadingIcon = { Icon(Icons.Outlined.PushPin, null, tint = textSecondaryColor) },
                                        onClick = {
                                            chatVM.pinMessage(chatId, msg.text.orEmpty())
                                            Toast.makeText(context, "Сообщение закреплено", Toast.LENGTH_SHORT).show()
                                            selectedMessage = null
                                        }
                                    )
                                    if (isMe) {
                                        HorizontalDivider(color = textPrimaryColor.copy(0.1f))
                                        DropdownMenuItem(
                                            text = { Text("Удалить", color = MayasTheme.ErrorRed) },
                                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MayasTheme.ErrorRed) },
                                            onClick = {
                                                val db = FirebaseFirestore.getInstance()
                                                db.collection("chats").document(chatId)
                                                    .collection("messages").document(msg.id).delete()
                                                Toast.makeText(context, "Сообщение удалено", Toast.LENGTH_SHORT).show()
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
                                Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp), tint = MayasTheme.Surface)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (reply.senderId == myUid) "Ответ себе" else "Ответ пользователю ${partnerName.orEmpty()}",
                                        fontSize = 11.sp,
                                        color = MayasTheme.GlowPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = reply.text.orEmpty(),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = textSecondaryColor,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { replyMessage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = textSecondaryColor)
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = textPrimaryColor.copy(0.1f))
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).navigationBarsPadding(),
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
                                placeholder = { Text("Сообщение..", color = textSecondaryColor) },
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
                                label = "SendButtonAnimation"
                            ) { isSending ->
                                if (isSending) {
                                    IconButton(
                                        onClick = {
                                            chatVM.sendMessage(
                                                chatId = chatId,
                                                text = input,
                                                replyText = replyMessage?.text,
                                                replyName = if (replyMessage?.senderId == myUid) "Вы" else partnerName.orEmpty()
                                            )
                                            input = ""
                                            replyMessage = null
                                        },
                                        modifier = Modifier.clip(CircleShape).background(MayasTheme.GlowBlue)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                                    }
                                } else {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Голосовые скоро!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Mic, null, tint = textSecondaryColor)
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp)
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
                        val db = FirebaseFirestore.getInstance()
                        db.collection("reports").add(
                            mapOf(
                                "reporterUid" to myUid,
                                "targetUid" to partnerUid,
                                "chatId" to chatId,
                                "reason" to reportText,
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                        )
                        Toast.makeText(context, "Жалоба отправлена", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                        reportText = ""
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
}

@Composable
fun EmojiPicker(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("😀", "😂", "🥰", "😎", "🤔", "😊", "🔥", "👍", "🙌", "🤡", "❤️", "🚀", "💀", "😭")
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