/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.core_ui.emoji

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

class CustomEmoji(val id: String, val fallback: String, val painter: (DrawScope.() -> Unit)?)

object CustomEmojiRegistry {
    private val items = mutableMapOf<String, CustomEmoji>()

    fun register(emoji: CustomEmoji) {
        items[emoji.id] = emoji
    }

    fun resolve(id: String): CustomEmoji? = items[id]

    fun displayFallback(id: String): String = items[id]?.fallback ?: id
}

private fun paintFor(emoji: String): (DrawScope.() -> Unit)? {
    val custom = CustomEmojiRegistry.resolve(emoji)
    if (custom != null) {
        return custom.painter ?: MayasEmojiArt.painter(custom.fallback)
    }
    return MayasEmojiArt.painter(emoji)
}

@Composable
fun EmojiGlyph(
    emoji: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    EmojiStyleState.ensureInit(context)
    val custom = CustomEmojiRegistry.resolve(emoji)
    val painter = if (EmojiStyleState.style == EmojiStyle.MAYAS || custom?.painter != null) paintFor(emoji) else null

    if (painter != null) {
        val boxSize = with(LocalDensity.current) { (fontSize * 1.15f).toDp() }
        Canvas(modifier = modifier.size(boxSize)) { painter(this) }
    } else {
        Text(text = CustomEmojiRegistry.displayFallback(emoji), fontSize = fontSize, color = color, modifier = modifier)
    }
}

@Composable
fun EmojiGlyphStyled(
    emoji: String,
    fontSize: TextUnit,
    style: EmojiStyle,
    modifier: Modifier = Modifier
) {
    val painter = if (style == EmojiStyle.MAYAS) MayasEmojiArt.painter(emoji) else null
    if (painter != null) {
        val boxSize = with(LocalDensity.current) { (fontSize * 1.15f).toDp() }
        Canvas(modifier = modifier.size(boxSize)) { painter(this) }
    } else {
        Text(text = emoji, fontSize = fontSize, modifier = modifier)
    }
}

@Composable
fun MayasText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    val context = LocalContext.current
    EmojiStyleState.ensureInit(context)
    val mayas = EmojiStyleState.style == EmojiStyle.MAYAS
    val effectiveSize = if (fontSize == TextUnit.Unspecified) 14.sp else fontSize
    val inline = remember(text, mayas, effectiveSize) {
        if (mayas) buildInlineMayasEmoji(AnnotatedString(text), effectiveSize) else null
    }

    if (inline == null) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        Text(
            text = inline.text,
            inlineContent = inline.content,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

class InlineEmojiResult(
    val text: AnnotatedString,
    val content: Map<String, InlineTextContent>
)

private fun isSequenceContinuation(text: String, index: Int): Boolean {
    if (index >= text.length) return false
    val cp = text.codePointAt(index)
    return cp == 0x200D || cp in 0x1F3FB..0x1F3FF
}

fun buildInlineMayasEmoji(source: AnnotatedString, fontSize: TextUnit): InlineEmojiResult? {
    val raw = source.text
    if (raw.isEmpty()) return null
    val keys = MayasEmojiArt.keys()

    class Match(val start: Int, val end: Int, val key: String)

    val matches = mutableListOf<Match>()
    var i = 0
    while (i < raw.length) {
        var found: Match? = null
        for (key in keys) {
            if (raw.startsWith(key, i)) {
                var end = i + key.length
                if (end < raw.length && raw[end] == '\uFE0F') end++
                if (!isSequenceContinuation(raw, end)) {
                    found = Match(i, end, key)
                }
                break
            }
        }
        if (found != null) {
            matches.add(found)
            i = found.end
        } else {
            i++
        }
    }
    if (matches.isEmpty()) return null

    val map = IntArray(raw.length + 1)
    val builder = AnnotatedString.Builder()
    val content = mutableMapOf<String, InlineTextContent>()
    var newLen = 0
    var oi = 0

    fun copyUntil(limit: Int) {
        while (oi < limit) {
            map[oi] = newLen
            builder.append(raw[oi])
            newLen++
            oi++
        }
    }

    val size = fontSize * 1.25f
    for (m in matches) {
        copyUntil(m.start)
        for (k in m.start until m.end) map[k] = newLen
        val id = "mayas_emoji_${m.key}"
        if (!content.containsKey(id)) {
            val painter = MayasEmojiArt.painter(m.key)
            if (painter != null) {
                content[id] = InlineTextContent(
                    Placeholder(size, size, PlaceholderVerticalAlign.TextCenter)
                ) {
                    val dp = with(LocalDensity.current) { size.toDp() }
                    Canvas(modifier = Modifier.size(dp)) { painter(this) }
                }
            }
        }
        builder.appendInlineContent(id, "\uFFFD")
        newLen += 1
        oi = m.end
    }
    copyUntil(raw.length)
    map[raw.length] = newLen

    source.spanStyles.forEach { builder.addStyle(it.item, map[it.start], map[it.end]) }
    source.paragraphStyles.forEach { builder.addStyle(it.item, map[it.start], map[it.end]) }
    source.getStringAnnotations(0, raw.length).forEach {
        builder.addStringAnnotation(it.tag, it.item, map[it.start], map[it.end])
    }

    return InlineEmojiResult(builder.toAnnotatedString(), content)
}

@Composable
fun MayasClickableText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onClick: (AnnotatedString, Int) -> Unit
) {
    val context = LocalContext.current
    EmojiStyleState.ensureInit(context)
    val mayas = EmojiStyleState.style == EmojiStyle.MAYAS
    val fontSize = if (style.fontSize == TextUnit.Unspecified) 16.sp else style.fontSize
    val inline = remember(text, mayas, fontSize) {
        if (mayas) buildInlineMayasEmoji(text, fontSize) else null
    }

    if (inline == null) {
        ClickableText(
            text = text,
            style = style,
            modifier = modifier,
            onClick = { offset -> onClick(text, offset) }
        )
    } else {
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        BasicText(
            text = inline.text,
            style = style,
            inlineContent = inline.content,
            onTextLayout = { layout = it },
            modifier = modifier.pointerInput(inline) {
                detectTapGestures { position ->
                    val result = layout
                    if (result != null) onClick(inline.text, result.getOffsetForPosition(position))
                }
            }
        )
    }
}
