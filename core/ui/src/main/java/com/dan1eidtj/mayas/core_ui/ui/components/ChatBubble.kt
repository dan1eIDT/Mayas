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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme

// --- РЕГУЛЯРНЫЕ ВЫРАЖЕНИЯ (Выделены на уровень файла для производительности) ---
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
    text: String,
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
) {
    val bubbleColor = if (isMe) MayasTheme.BubbleMine else MayasTheme.BubbleOther
    val timeColor = if (isMe) Color(0xAAFFFFFF) else Color(0xFF888888)
    val statusColor = when {
        isSending -> timeColor
        isRead -> Color(0xFFFF0202)
        else -> timeColor
    }

    val linkColor = if (isMe) Color(0xFFC5B4E3) else Color(0xFF8AB4F8)

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
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = if (isMe) 2.dp else 16.dp,
                            bottomStart = if (isMe) 16.dp else 2.dp
                        )
                    )
                    .combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {

                    // --- БЛОК ОТВЕТА (Отображается сверху, если он есть) ---
                    if (!replyToText.isNullOrBlank() && !replyToName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.15f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .background(if (isMe) Color.White else MayasTheme.GlowPurple)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = replyToName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) MayasTheme.TextPrimary else MayasTheme.GlowGreen,
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

                    // --- БЛОК ОСНОВНОГО ТЕКСТА СООБЩЕНИЯ (Связан с RichText) ---
                    RichText(
                        text = text,
                        style = TextStyle(color = MayasTheme.TextPrimary, fontSize = 16.sp, lineHeight = 20.sp),
                        linkColor = linkColor,
                        modifier = Modifier.padding(bottom = 2.dp),
                        onUserClick = onUserClick,
                        onHashtagClick = onHashtagClick,
                        onBubbleClick = { },
                        onBubbleLongClick = onLongClick
                    )

                    // --- ВРЕМЯ И СТАТУС ОТПРАВКИ ---
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSending) "..." else time,
                            style = TextStyle(color = timeColor, fontSize = 11.sp)
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
                                tint = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}