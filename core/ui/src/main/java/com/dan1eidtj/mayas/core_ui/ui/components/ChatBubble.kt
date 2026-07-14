@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.core_ui.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.border
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.data.ShopConstants

private val MARKDOWN_REGEX = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
private val URL_REGEX = Regex("(?<!@)\\b(?:https?://|www\\.)?[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9-+&@#/%?=~_|!:,.;]*[a-zA-Z0-9-+&@#/%=~_|])?\\b")
private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
private val USER_REGEX = Regex("@[a-zA-Z0-9_]+")
private val HASHTAG_REGEX = Regex("#[\\w\\p{L}]+")

enum class LinkType {
    URL, USER, HASHTAG, EMAIL
}

data class MatchResult(
    val range: IntRange,
    val text: String,
    val value: String,
    val type: LinkType
)

private fun parseText(text: String): List<MatchResult> {
    val matches = mutableListOf<MatchResult>()

    // 1. Markdown ссылки [text](url)
    MARKDOWN_REGEX.findAll(text).forEach { match ->
        val displayText = match.groups[1]?.value ?: ""
        val target = match.groups[2]?.value ?: ""
        val type = if (target.startsWith("mailto:", ignoreCase = true)) LinkType.EMAIL else LinkType.URL
        matches.add(MatchResult(match.range, displayText, target, type))
    }

    // 2. Обычные и простые URL (включая google.com)
    URL_REGEX.findAll(text).forEach { match ->
        matches.add(MatchResult(match.range, match.value, match.value, LinkType.URL))
    }

    // 3. Email адреса
    EMAIL_REGEX.findAll(text).forEach { match ->
        matches.add(MatchResult(match.range, match.value, "mailto:${match.value}", LinkType.EMAIL))
    }

    // 4. Юзернеймы @username
    USER_REGEX.findAll(text).forEach { match ->
        val username = match.value.removePrefix("@")
        matches.add(MatchResult(match.range, match.value, username, LinkType.USER))
    }

    // 5. Хэштеги #hashtags
    HASHTAG_REGEX.findAll(text).forEach { match ->
        val tag = match.value.removePrefix("#")
        matches.add(MatchResult(match.range, match.value, tag, LinkType.HASHTAG))
    }

    // Исключаем наложение (например, чтобы TLD внутри email не распознавался как ссылка на домен)
    val sortedMatches = matches.sortedBy { it.range.first }
    val filteredMatches = mutableListOf<MatchResult>()
    var lastEndIndex = -1

    for (match in sortedMatches) {
        if (match.range.first > lastEndIndex) {
            filteredMatches.add(match)
            lastEndIndex = match.range.last
        }
    }

    return filteredMatches
}

private fun buildRichText(
    text: String,
    matches: List<MatchResult>,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }

            val start = length
            append(match.text)
            val end = length

            addStringAnnotation(
                tag = match.type.name,
                annotation = match.value,
                start = start,
                end = end
            )

            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold
                ),
                start = start,
                end = end
            )

            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

// --- КОНСТАНТЫ ДЛЯ СТИЛЕЙ СООБЩЕНИЙ ---
object MessageStyle {
    const val NEON = "neon"
    const val GOLD = "gold"
    const val FIRE = "fire"
    const val ICE = "ice"
    const val MATRIX = "matrix"
    const val SUNSET = "sunset"
    const val FOREST = "forest"
    const val MIDNIGHT = "midnight"
}

@Composable
fun RichText(
    text: String,
    style: TextStyle,
    linkColor: Color,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onBubbleClick: () -> Unit = {},
    onBubbleLongClick: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val matches = remember(text) { parseText(text) }
    val annotatedString = remember(text, matches, linkColor) {
        buildRichText(text, matches, linkColor)
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures(
                onTap = { offsetPosition ->
                    layoutResult?.let { layout ->
                        val charOffset = layout.getOffsetForPosition(offsetPosition)
                        var handled = false

                        for (type in LinkType.entries) {
                            annotatedString.getStringAnnotations(
                                tag = type.name,
                                start = charOffset,
                                end = charOffset
                            ).firstOrNull()?.let { annotation ->
                                handled = true
                                when (type) {
                                    LinkType.URL -> {
                                        val url = annotation.item
                                        val finalUrl = if (!url.startsWith("http://", ignoreCase = true) &&
                                            !url.startsWith("https://", ignoreCase = true)) {
                                            "https://$url"
                                        } else {
                                            url
                                        }
                                        runCatching { uriHandler.openUri(finalUrl) }
                                    }
                                    LinkType.USER -> onUserClick(annotation.item)
                                    LinkType.HASHTAG -> onHashtagClick(annotation.item)
                                    LinkType.EMAIL -> {
                                        val emailUri = if (annotation.item.startsWith("mailto:")) {
                                            annotation.item
                                        } else {
                                            "mailto:${annotation.item}"
                                        }
                                        runCatching { uriHandler.openUri(emailUri) }
                                    }
                                }
                            }
                            if (handled) break
                        }

                        if (!handled) {
                            onBubbleClick()
                        }
                    }
                },
                onLongPress = { offsetPosition ->
                    layoutResult?.let { layout ->
                        val charOffset = layout.getOffsetForPosition(offsetPosition)
                        var urlCopied = false

                        annotatedString.getStringAnnotations(
                            tag = LinkType.URL.name,
                            start = charOffset,
                            end = charOffset
                        ).firstOrNull()?.let { annotation ->
                            urlCopied = true
                            clipboardManager.setText(AnnotatedString(annotation.item))
                            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                        }

                        if (!urlCopied) {
                            onBubbleLongClick()
                        }
                    }
                }
            )
        }
    )
}

@Composable
fun ChatBubble(
    text: String?,
    isMe: Boolean,
    isRead: Boolean,
    time: String,
    onLongClick: () -> Unit,
    animateIn: Boolean = true,
    isSending: Boolean = false,
    replyToText: String? = null,
    replyToName: String? = null,
    onUserClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    mediaUrl: String? = null,
    isPremium: Boolean = false,
    messageStyle: String? = null,
    isLastInChain: Boolean = true,
) {
    val bubbleColor = if (isMe) MayasTheme.BubbleMine else MayasTheme.BubbleOther
    val timeColor = MayasTheme.TextSecondary
    val statusColor = when {
        isSending -> timeColor
        isRead -> MayasTheme.GlowSky
        else -> timeColor
    }

    val linkColor = MayasTheme.LinkColor

    val bubbleShape = remember(isMe, isLastInChain) {
        BubbleShape(
            type = if (isMe) BubbleType.Outgoing else BubbleType.Incoming,
            drawTail = isLastInChain
        )
    }

    // --- ДИНАМИЧЕСКИЙ СТИЛЬ СООБЩЕНИЯ (ПОКУПНОЙ) ---
    val messageModifier = when (messageStyle) {
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
        null -> {
            if (isPremium) {
                Modifier.background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            bubbleColor,
                            MayasTheme.GlowGold.copy(alpha = 0.2f)
                        )
                    ),
                    shape = bubbleShape
                ).border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            MayasTheme.GlowGold
                        )
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

        else -> {
            val accent = ShopConstants.getStyleColor(messageStyle)
            Modifier.background(
                brush = Brush.linearGradient(
                    colors = ShopConstants.getStyleGradient(messageStyle)
                ),
                shape = bubbleShape
            ).border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), accent)
                ),
                shape = bubbleShape
            )
        }
    }

    var visible by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(Unit) {
        if (animateIn) {
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(expandFrom = if (isMe) Alignment.End else Alignment.Start) + fadeIn(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 1.dp, horizontal = 8.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick
                    )
                    .then(messageModifier)
                    .padding(
                        start = if (isMe) 12.dp else (if (isLastInChain) 24.dp else 12.dp),
                        end = if (isMe) (if (isLastInChain) 24.dp else 12.dp) else 12.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
            ) {
                Column {

                    // --- БЛОК ОТВЕТА ---
                    if (!replyToText.isNullOrBlank() && !replyToName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.1f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(if (isMe) Color.White else MayasTheme.GlowPurple)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = replyToName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else MayasTheme.GlowPurple,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = replyToText,
                                    fontSize = 12.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MayasTheme.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // --- МЕДИА-КОНТЕНТ ---
                    if (!mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    // --- БЛОК ОСНОВНОГО ТЕКСТА СООБЩЕНИЯ ---
                    if (!text.isNullOrBlank()) {
                        val customTextColor = when (messageStyle) {
                            MessageStyle.ICE -> Color(0xFF006064)
                            MessageStyle.MATRIX -> MayasTheme.GlowLime
                            MessageStyle.GOLD -> Color(0xFF5D4037)
                            MessageStyle.FOREST, MessageStyle.SUNSET, MessageStyle.MIDNIGHT -> Color.White
                            null -> if (isMe) Color.White else MayasTheme.TextPrimary
                            // Купленные стили из магазина без ручного описания —
                            // берём читаемый цвет текста из ShopConstants (например,
                            // для folly тёмно-красный текст на светлом фоне и т.п.)
                            else -> ShopConstants.getStyleTextColor(messageStyle)
                        }
                        RichText(
                            text = text,
                            style = TextStyle(
                                color = customTextColor,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            ),
                            linkColor = if (messageStyle != null) customTextColor.copy(alpha = 0.8f) else linkColor,
                            modifier = Modifier.padding(bottom = 2.dp),
                            onUserClick = onUserClick,
                            onHashtagClick = onHashtagClick,
                            onBubbleClick = { },
                            onBubbleLongClick = onLongClick
                        )
                    }

                    // --- ВРЕМЯ И СТАТУС ОТПРАВКИ ---
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val secondaryTextColor = when (messageStyle) {
                            MessageStyle.ICE -> Color(0xFF006064).copy(alpha = 0.6f)
                            MessageStyle.MATRIX -> MayasTheme.GlowLime.copy(alpha = 0.7f)
                            MessageStyle.GOLD -> Color(0xFF5D4037).copy(alpha = 0.7f)
                            MessageStyle.FOREST, MessageStyle.SUNSET -> Color.White.copy(alpha = 0.7f)
                            MessageStyle.MIDNIGHT -> Color.White.copy(alpha = 0.6f)
                            null -> timeColor
                            // Тот же цвет текста стиля, но приглушённый — для новых
                            // купленных стилей из магазина
                            else -> ShopConstants.getStyleTextColor(messageStyle).copy(alpha = 0.7f)
                        }

                        if (isPremium && isMe) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (messageStyle == MessageStyle.GOLD) Color(0xFF5D4037) else MayasTheme.GlowGold,
                                modifier = Modifier.size(12.dp).padding(end = 2.dp)
                            )
                        }

                        Text(
                            text = if (isSending) "..." else time,
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )
                        )

                        if (isMe) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = when {
                                    isSending -> Icons.Outlined.Schedule
                                    isRead -> Icons.Default.DoneAll
                                    else -> Icons.Default.Done
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (messageStyle != null) secondaryTextColor else statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}