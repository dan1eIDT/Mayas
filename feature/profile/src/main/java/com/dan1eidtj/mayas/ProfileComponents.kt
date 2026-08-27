package com.dan1eidtj.mayas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dan1eidtj.data.ShopConstants.DailyDeal
import com.dan1eidtj.data.ItemRarity
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.data.ShopItem
import com.dan1eidtj.data.ShopRepository
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.ChatBubble
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.dan1eidtj.mayas.feature.formatCompactCount
import kotlinx.coroutines.delay


val PremiumGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFAA00),
        Color(0xFFFF6B35),
        Color(0xFFFF4D8D),
        Color(0xFFE040FB),
        Color(0xFF9C6BFF)
    )
)

val PremiumGradientSubtle = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700).copy(0.15f),
        Color(0xFFFF6B35).copy(0.15f),
        Color(0xFFE040FB).copy(0.15f),
        Color(0xFF9C6BFF).copy(0.15f)
    )
)


@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 20.dp)
    )
}

@Composable
fun EmojiStatusView(status: String, size: androidx.compose.ui.unit.TextUnit = 24.sp) {
    if (status.startsWith("icon:")) {
        val drawableName = status.removePrefix("icon:")
        val context = androidx.compose.ui.platform.LocalContext.current
        val resId = remember(drawableName) {
            context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        }
        if (resId != 0) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.size(size.value.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("🙂", fontSize = size)
        }
    } else {
        Text(status, fontSize = size)
    }
}

@Composable
fun InfoSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface),
        content = content
    )
}

@Composable
fun TelegramInfoRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MayasTheme.Accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MayasTheme.Accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MayasTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

private val linkifyUrlRegex = Regex("""(https?://[^\s]+|www\.[^\s]+\.[^\s]+)""")
private val linkifyMentionRegex = Regex("""@([A-Za-z0-9_]{3,32})""")


@Composable
fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MayasTheme.TextPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    linkColor: Color = MayasTheme.Accent,
    onMentionClick: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    val annotated = remember(text) {
        buildAnnotatedString {
            data class Match(val range: IntRange, val tag: String, val value: String)

            val matches = (
                    linkifyUrlRegex.findAll(text).map { Match(it.range, "URL", it.value) } +
                            linkifyMentionRegex.findAll(text).map { Match(it.range, "MENTION", it.groupValues[1]) }
                    ).sortedBy { it.range.first }.toList()

            var lastIndex = 0
            for (match in matches) {
                if (match.range.first < lastIndex) continue
                append(text.substring(lastIndex, match.range.first))
                val start = length
                append(text.substring(match.range.first, match.range.last + 1))
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start, length
                )
                addStringAnnotation(tag = match.tag, annotation = match.value, start = start, end = length)
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { ann ->
                val url = if (ann.item.startsWith("http")) ann.item else "https://${ann.item}"
                runCatching { uriHandler.openUri(url) }
                return@ClickableText
            }
            annotated.getStringAnnotations(tag = "MENTION", start = offset, end = offset).firstOrNull()?.let { ann ->
                onMentionClick(ann.item)
            }
        }
    )
}


@Composable
fun DescriptionInfoRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onMentionClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MayasTheme.Accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MayasTheme.Accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            LinkifiedText(text = title, onMentionClick = onMentionClick)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MayasTheme.Accent,
            unfocusedBorderColor = MayasTheme.TextSecondary.copy(0.3f),
            focusedLabelColor = MayasTheme.Accent,
            focusedTextColor = MayasTheme.TextPrimary,
            unfocusedTextColor = MayasTheme.TextPrimary
        )
    )
}



@Composable
fun ColorPicker(
    current: String,
    isPremium: Boolean,
    onSelect: (String) -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val colors = listOf(
        "purple" to MayasTheme.GlowPurple,
        "pink" to MayasTheme.GlowPink,
        "blue" to MayasTheme.GlowBlue,
        "green" to MayasTheme.GlowGreen,
        "red" to MayasTheme.GlowRed,
        "orange" to MayasTheme.GlowOrange,
        "cyan" to MayasTheme.GlowCyan,
        "mint" to MayasTheme.GlowMint,
        "indigo" to MayasTheme.GlowIndigo,
        "lime" to MayasTheme.GlowLime
    )
    val premiumColors = listOf(
        "gold" to Brush.linearGradient(MayasTheme.NicknameGold),
        "rose" to Brush.linearGradient(MayasTheme.NicknameRose),
        "amber" to Brush.linearGradient(MayasTheme.NicknameAmber),
        "sky" to Brush.linearGradient(MayasTheme.NicknameSky),
        "white" to Brush.linearGradient(MayasTheme.NicknameWhite),
        "black" to Brush.linearGradient(MayasTheme.NicknameBlack),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(16.dp)
    ) {
        Text("Стандартные", color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(colors) { (name, color) ->
                val isSelected = current == name
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(name) }
                        .then(
                            if (isSelected) Modifier.border(3.dp, MayasTheme.Background, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(brush = PremiumGradientSubtle)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Эксклюзив для Mayas+",
                style = TextStyle(brush = PremiumGradient),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (!isPremium) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Lock, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(11.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(premiumColors) { (name, brush) ->
                val isSelected = current == name && isPremium
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(brush = brush, alpha = if (isPremium) 1f else 0.35f)
                        .clickable { if (isPremium) onSelect(name) else onNavigateToPremium() }
                        .then(
                            if (isSelected) Modifier.border(3.dp, MayasTheme.Background, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPremium) {
                        Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(13.dp))
                    } else if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}



@Composable
fun PremiumSectionCollapsible(
    isPremium: Boolean,
    verifiedIcon: String,
    avatarFrame: String,
    isInvisible: Boolean,
    nameColor: String = "gold",
    onNavigateToPremium: () -> Unit,
    onInvisibleChange: (Boolean) -> Unit,
    onIconSelect: (String) -> Unit,
    onFrameSelect: (String) -> Unit,
    onNameColorSelect: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280),
        label = "chevron"
    )

    val headerGradient = if (isPremium) PremiumGradient else Brush.linearGradient(
        listOf(Color(0xFF616161), Color(0xFF424242))
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (expanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    else RoundedCornerShape(16.dp)
                )
                .background(brush = headerGradient)
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "MAYAS+",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            if (isPremium) "Подписка активна ✓" else "Узнать о подписке",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronAngle)
                )
            }
        }


        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(280)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(280)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MayasTheme.Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isPremium) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush = PremiumGradient)
                            .clickable { onNavigateToPremium() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stars, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Получить Mayas+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Эксклюзивные функции и кастомизация", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE040FB).copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isInvisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = Color(0xFFE040FB),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Режим невидимки", color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Скрыть статус «в сети»", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = isInvisible,
                            onCheckedChange = onInvisibleChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE040FB),
                                uncheckedThumbColor = MayasTheme.TextSecondary,
                                uncheckedTrackColor = MayasTheme.Background
                            )
                        )
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .padding(14.dp)
                    ) {
                        Text("Иконка верификации", color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val iconOptions = listOf(
                                "verified" to Icons.Default.Verified,
                                "star" to Icons.Default.Star,
                                "diamond" to Icons.Default.Diamond,
                                "auto_awesome" to Icons.Default.AutoAwesome,
                                "crown" to Icons.Default.WorkspacePremium,
                                "bolt" to Icons.Default.Bolt,
                                "fire" to Icons.Default.LocalFireDepartment,
                                "trophy" to Icons.Default.EmojiEvents,
                                "heart" to Icons.Default.Favorite,
                                "shield" to Icons.Default.Shield
                            )

                            iconOptions.chunked(5).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { (key, icon) ->
                                        val isSelected = verifiedIcon == key
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0xFFFFAA00).copy(0.1f) else MayasTheme.Surface)
                                                .then(
                                                    if (isSelected) Modifier.border(2.dp, brush = PremiumGradient, shape = RoundedCornerShape(12.dp))
                                                    else Modifier.border(0.5.dp, MayasTheme.Outline, RoundedCornerShape(12.dp))
                                                )
                                                .clickable { onIconSelect(key) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                icon, null,
                                                tint = if (isSelected) Color(0xFFFFAA00) else MayasTheme.TextSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .padding(14.dp)
                    ) {
                        Text("Обводка аватара", color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val frames = listOf("none", "gold", "rainbow", "neon", "fire", "black")
                            items(frames) { frame ->
                                val isSelected = avatarFrame == frame
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(MayasTheme.Surface)
                                        .clickable { onFrameSelect(frame) }
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, brush = PremiumGradient, shape = CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MayasAvatar(
                                        url = null,
                                        icon = "person",
                                        glowColor = MayasTheme.GlowPurple,
                                        isPremium = true,
                                        size = 54.dp,
                                        useCustomAvatar = false,
                                        frameType = frame
                                    )
                                }
                            }
                        }
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .padding(14.dp)
                    ) {
                        Text("Цвет ника", color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val nameColors = listOf(
                                "gold" to MayasTheme.NicknameSimpleGold,
                                "purple" to MayasTheme.GlowPurple,
                                "pink" to MayasTheme.GlowPink,
                                "blue" to MayasTheme.GlowBlue,
                                "green" to MayasTheme.GlowGreen,
                                "red" to MayasTheme.GlowRed,
                                "orange" to MayasTheme.GlowOrange,
                                "cyan" to MayasTheme.NicknameSimpleCyan,
                                "white" to Color.White,
                                "rainbow" to MayasTheme.GlowAmber
                            )
                            items(nameColors) { (key, color) ->
                                val isSelected = nameColor == key
                                val brush = if (key == "rainbow") PremiumGradient else Brush.linearGradient(listOf(color, color))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(brush)
                                        .clickable { onNameColorSelect(key) }
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MayasTheme.TextPrimary, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = if (key == "white") Color.Black else Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PremiumCustomizationSection(
    isPremium: Boolean,
    currentIcon: String,
    currentFrame: String,
    isInvisible: Boolean,
    onInvisibleChange: (Boolean) -> Unit,
    onIconSelect: (String) -> Unit,
    onFrameSelect: (String) -> Unit
) {
    PremiumSectionCollapsible(
        isPremium = isPremium,
        verifiedIcon = currentIcon,
        avatarFrame = currentFrame,
        isInvisible = isInvisible,
        nameColor = "gold",
        onNavigateToPremium = {},
        onInvisibleChange = onInvisibleChange,
        onIconSelect = onIconSelect,
        onFrameSelect = onFrameSelect,
        onNameColorSelect = {}
    )
}



@Composable
fun EarnAndShopSection(
    adsWatched: Int,
    isAdLoading: Boolean = false,
    onWatchAd: () -> Unit,
    onOpenShop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayasTheme.Accent.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdsClick, null, tint = MayasTheme.Accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Реклама ($adsWatched/5)", color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Смотри и получай 10 🪙", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onWatchAd,
                enabled = adsWatched < 5 && !isAdLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MayasTheme.Accent,
                    disabledContainerColor = MayasTheme.TextSecondary.copy(0.2f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isAdLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MayasTheme.TextPrimary
                    )
                } else {
                    Text("Смотреть", fontSize = 13.sp)
                }
            }
        }

        HorizontalDivider(color = MayasTheme.Divider, thickness = 0.5.dp)


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenShop() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayasTheme.GlowGold.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingBag, null, tint = MayasTheme.GlowGold, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Магазин Маяса", color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Уникальные стили и фишки", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(0.4f), modifier = Modifier.size(18.dp))
        }
    }
}




private fun rarityLabel(rarity: ItemRarity): String = when (rarity) {
    ItemRarity.COMMON -> "ОБЫЧНЫЙ"
    ItemRarity.RARE -> "РЕДКИЙ"
    ItemRarity.EPIC -> "ЭПИЧЕСКИЙ"
    ItemRarity.LEGENDARY -> "ЛЕГЕНДАРНЫЙ"
}

@Composable
private fun rarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.COMMON -> MayasTheme.TextSecondary
    ItemRarity.RARE -> MayasTheme.GlowBlue
    ItemRarity.EPIC -> MayasTheme.GlowPurple
    ItemRarity.LEGENDARY -> MayasTheme.GlowGold
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MayasTheme.Accent,
        modifier = modifier
    ) {
        Text(
            "NEW",
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}

@Composable
private fun RarityTag(rarity: ItemRarity, modifier: Modifier = Modifier) {
    if (rarity == ItemRarity.COMMON) return
    val color = rarityColor(rarity)
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Text(
            rarityLabel(rarity),
            color = color,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.05.em,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}


@Composable
private fun ShopItemBadgeRow(item: ShopItem, modifier: Modifier = Modifier) {
    val rarity = ShopConstants.rarityOf(item)
    if (!item.isNew && rarity == ItemRarity.COMMON) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (item.isNew) NewBadge()
        RarityTag(rarity)
    }
}


private enum class ShopCategory(val label: String, val itemType: ItemType?, val icon: ImageVector?) {
    DAILY("Акция", null, Icons.Default.LocalFireDepartment),
    ALL("Всё", null, Icons.Default.Apps),
    BUBBLE("Стили", ItemType.BUBBLE, Icons.AutoMirrored.Filled.Chat),
    EMOJI("Статусы", ItemType.EMOJI_STATUS, Icons.Default.EmojiEmotions),
    FONT("Шрифты", ItemType.FONT, Icons.Default.TextFields),
    EFFECT("Эффекты", ItemType.EFFECT, Icons.Default.AutoAwesome)
}

@Composable
private fun ShopCategoryTabs(
    selected: ShopCategory,
    onSelect: (ShopCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShopCategory.values().forEach { category ->
            val isSelected = category == selected
            val isFire = category == ShopCategory.DAILY
            val activeColor = if (isFire) MayasTheme.GlowGold else MayasTheme.Accent
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) activeColor else MayasTheme.Surface,
                border = BorderStroke(0.5.dp, if (isSelected) activeColor else MayasTheme.Outline),
                modifier = Modifier.clickable { onSelect(category) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    category.icon?.let {
                        Icon(
                            it,
                            null,
                            tint = if (isSelected) Color.White else MayasTheme.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        category.label,
                        color = if (isSelected) Color.White else MayasTheme.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
private fun ShopPreviewActions(
    isEquipped: Boolean,
    isOwned: Boolean,
    price: Int,
    onApply: () -> Unit,
    onBuy: () -> Unit
) {
    when {
        isEquipped -> Surface(
            shape = RoundedCornerShape(10.dp),
            color = MayasTheme.Accent.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(14.dp))
                Text("Используется", color = MayasTheme.Accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        isOwned -> Button(
            onClick = onApply,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Применить", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        else -> Button(
            onClick = onBuy,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowGold),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🪙", fontSize = 12.sp)
                Text("$price", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}


private fun bubbleTailShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = 9.dp, topEnd = 9.dp, bottomStart = 9.dp, bottomEnd = 3.dp
)

@Composable
private fun DailyDealsSection(
    dailyDeals: List<DailyDeal>,
    msUntilReset: Long,
    ownedItems: List<String>,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    if (dailyDeals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Сегодняшняя акция уже закончилась.\nЗагляни завтра — будут новые скидки 🔥",
                color = MayasTheme.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
        return
    }

    val totalMinutes = (msUntilReset / 60_000L).toInt().coerceAtLeast(0)
    val hLeft = totalMinutes / 60
    val mLeft = totalMinutes % 60
    val dayMs = 24 * 60 * 60 * 1000L
    val resetProgress = (1f - (msUntilReset.coerceIn(0, dayMs).toFloat() / dayMs))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "🔥 ЕЖЕДНЕВНЫЙ МАГАЗИН",
            color = MayasTheme.GlowGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            modifier = Modifier.weight(1f)
        )
        Text(
            "обновится через ${hLeft}ч ${mLeft}м",
            color = MayasTheme.TextSecondary,
            fontSize = 10.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { resetProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = MayasTheme.GlowGold,
        trackColor = MayasTheme.GlowGold.copy(alpha = 0.15f),
    )
    Spacer(Modifier.height(14.dp))


    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(dailyDeals, key = { it.item.id }) { deal ->
            val item = deal.item
            val isOwned = ownedItems.contains(item.id)
            val isBubble = item.type == ItemType.BUBBLE
            val previewGradient = when {
                isBubble -> ShopConstants.getStyleGradient(item.id)
                else -> listOf(MayasTheme.Surface, MayasTheme.Surface)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MayasTheme.Surface,
                border = BorderStroke(0.5.dp, MayasTheme.GlowGold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isOwned) onSelectItem(item.id, item.type)
                        else if (item.isAvailableNow()) onBuyItem(item.id, deal.discountedPrice, item.name)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            ItemType.BUBBLE -> Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(bubbleTailShape())
                                    .background(Brush.linearGradient(previewGradient))
                            )
                            ItemType.EMOJI_STATUS -> EmojiStatusView(item.id, size = 22.sp)
                            ItemType.FONT -> Text(
                                "Аа", color = MayasTheme.TextPrimary, fontSize = 18.sp,
                                fontFamily = ShopConstants.getFontFamily(item.id)
                            )
                            ItemType.EFFECT -> Text(item.icon ?: "✨", fontSize = 20.sp)
                            else -> Icon(Icons.Default.Star, null, tint = MayasTheme.GlowGold, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.name, color = MayasTheme.TextPrimary, fontSize = 11.sp,
                        maxLines = 1, textAlign = TextAlign.Center
                    )
                    if (isOwned) {
                        Text("Куплено", color = MayasTheme.TextSecondary, fontSize = 10.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${item.price}", color = MayasTheme.TextSecondary, fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${deal.discountedPrice}🪙", color = MayasTheme.GlowGold,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Text("-${deal.discountPercent}%", color = MayasTheme.Success, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiSection(
    emojis: List<ShopItem>,
    ownedItems: List<String>,
    currentEmoji: String,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    var previewId by remember(currentEmoji) { mutableStateOf(currentEmoji) }
    val previewItem = emojis.find { it.id == previewId }
    val previewRarity = previewItem?.let { ShopConstants.rarityOf(it) } ?: ItemRarity.COMMON

    Text(
        "ПРЕВЬЮ СТАТУСА",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))


    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MayasTheme.Surface,
        border = BorderStroke(
            0.5.dp,
            if (previewRarity != ItemRarity.COMMON) rarityColor(previewRarity).copy(alpha = 0.5f) else MayasTheme.Outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MayasTheme.Accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Твой профиль", color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (previewId.isNotEmpty()) EmojiStatusView(previewId, size = 17.sp)
            }
            if (previewItem != null) {
                ShopPreviewActions(
                    isEquipped = currentEmoji == previewId,
                    isOwned = ownedItems.contains(previewId),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.EMOJI_STATUS) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            } else if (previewId.isEmpty()) {
                Text("Без статуса", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        "ВСЕ СТАТУСЫ",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(emojis, key = { it.id }) { item ->
            val emoji = item.id
            val isOwned = ownedItems.contains(emoji)
            val isEquipped = currentEmoji == emoji
            val isPreviewing = previewId == emoji
            val rarity = ShopConstants.rarityOf(item)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
                border = BorderStroke(
                    if (isPreviewing) 1.5.dp else 0.5.dp,
                    when {
                        isPreviewing -> MayasTheme.Accent
                        rarity != ItemRarity.COMMON -> rarityColor(rarity).copy(alpha = 0.35f)
                        else -> MayasTheme.Outline
                    }
                ),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { previewId = emoji }
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmojiStatusView(emoji, size = 24.sp)
                        Spacer(Modifier.height(3.dp))
                        when {
                            isEquipped -> Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(12.dp))
                            isOwned -> Text("Есть", fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                            !item.isAvailableNow() -> Text("🔒 Скоро", fontSize = 9.sp, color = MayasTheme.TextSecondary)
                            else -> Text("${item.price} 🪙", fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                        }
                    }
                    if (item.isNew) {
                        NewBadge(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleStylesSection(
    styles: List<ShopItem>,
    ownedItems: List<String>,
    messageStyle: String,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    var previewId by remember(messageStyle) { mutableStateOf(messageStyle) }
    val previewItem = styles.find { it.id == previewId }

    Text(
        "ПРЕВЬЮ СТИЛЯ",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MayasTheme.Surface)
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            ChatBubble(
                text = "Так будут выглядеть твои сообщения ✨",
                isMe = true,
                isRead = true,
                time = "12:00",
                onLongClick = {},
                messageStyle = previewId.ifEmpty { null }
            )
        }
        if (previewItem != null) {
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ShopPreviewActions(
                    isEquipped = messageStyle == previewId,
                    isOwned = ownedItems.contains(previewId),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.BUBBLE) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        "ВСЕ СТИЛИ · нажми, чтобы посмотреть",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.04.em
    )
    Spacer(Modifier.height(10.dp))

    styles.forEach { item ->
        val id = item.id
        val isOwned = ownedItems.contains(id)
        val isEquipped = messageStyle == id
        val isPreviewing = previewId == id
        val rarity = ShopConstants.rarityOf(item)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
            border = BorderStroke(
                if (isPreviewing) 1.5.dp else 0.5.dp,
                when {
                    isPreviewing -> MayasTheme.Accent
                    rarity != ItemRarity.COMMON -> rarityColor(rarity).copy(alpha = 0.35f)
                    else -> MayasTheme.Outline
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable { previewId = id }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 22.dp)
                        .clip(bubbleTailShape())
                        .background(Brush.linearGradient(ShopConstants.getStyleGradient(id))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEquipped) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    ShopItemBadgeRow(item, modifier = Modifier.padding(top = 3.dp))
                }
                when {
                    isEquipped -> Text("Используется", color = MayasTheme.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    isOwned -> Text("Куплено", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    !item.isAvailableNow() -> Text("🔒 Скоро", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("🪙", fontSize = 12.sp)
                        Text("${item.price}", color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSection(
    fonts: List<ShopItem>,
    ownedItems: List<String>,
    currentFont: String,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    var previewId by remember(currentFont) { mutableStateOf(currentFont.ifEmpty { "default" }) }
    val previewItem = fonts.find { it.id == previewId }

    Text(
        "ПРЕВЬЮ ШРИФТА",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MayasTheme.Surface)
            .padding(14.dp)
    ) {
        Text(
            "Так будет выглядеть твой текст в сообщениях",
            color = MayasTheme.TextPrimary,
            fontSize = 16.sp,
            fontFamily = ShopConstants.getFontFamily(previewId),
            lineHeight = 20.sp
        )
        if (previewItem != null && previewId != "default") {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ShopPreviewActions(
                    isEquipped = currentFont.ifEmpty { "default" } == previewId,
                    isOwned = ownedItems.contains(previewId),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.FONT) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        "ВСЕ ШРИФТЫ",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))
    fonts.forEach { item ->
        val isOwned = item.id == "default" || ownedItems.contains(item.id)
        val isEquipped = currentFont.ifEmpty { "default" } == item.id
        val isPreviewing = previewId == item.id
        val rarity = ShopConstants.rarityOf(item)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
            border = BorderStroke(
                if (isPreviewing) 1.5.dp else 0.5.dp,
                when {
                    isPreviewing -> MayasTheme.Accent
                    rarity != ItemRarity.COMMON -> rarityColor(rarity).copy(alpha = 0.35f)
                    else -> MayasTheme.Outline
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable { previewId = item.id }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Аа",
                    color = MayasTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontFamily = ShopConstants.getFontFamily(item.id),
                    modifier = Modifier.width(30.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    ShopItemBadgeRow(item, modifier = Modifier.padding(top = 3.dp))
                }
                when {
                    isEquipped -> Text("Используется", color = MayasTheme.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    isOwned -> Text("Куплено", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    !item.isAvailableNow() -> Text("🔒 Скоро", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("🪙", fontSize = 12.sp)
                        Text("${item.price}", color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectSection(
    effects: List<ShopItem>,
    ownedItems: List<String>,
    currentEffect: String,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    var previewId by remember(currentEffect) { mutableStateOf(currentEffect.ifEmpty { "none" }) }
    val previewItem = effects.find { it.id == previewId }

    val infiniteTransition = rememberInfiniteTransition(label = "effect_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "effect_pulse_scale"
    )

    Text(
        "ПРЕВЬЮ ЭФФЕКТА",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MayasTheme.Surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    previewItem?.icon ?: "✨",
                    fontSize = 26.sp,
                    modifier = Modifier.graphicsLayer(
                        scaleX = if (previewId != "none") pulse else 1f,
                        scaleY = if (previewId != "none") pulse else 1f
                    )
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    previewItem?.name ?: "Без эффекта",
                    color = MayasTheme.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Так отправится сообщение",
                    color = MayasTheme.TextSecondary,
                    fontSize = 11.5.sp
                )
            }
            if (previewItem != null && previewId != "none") {
                ShopPreviewActions(
                    isEquipped = currentEffect.ifEmpty { "none" } == previewId,
                    isOwned = ownedItems.contains(previewId),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.EFFECT) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        "ВСЕ ЭФФЕКТЫ",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(effects, key = { it.id }) { item ->
            val isOwned = item.id == "none" || ownedItems.contains(item.id)
            val isEquipped = currentEffect.ifEmpty { "none" } == item.id
            val isPreviewing = previewId == item.id
            val rarity = ShopConstants.rarityOf(item)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
                border = BorderStroke(
                    if (isPreviewing) 1.5.dp else 0.5.dp,
                    when {
                        isPreviewing -> MayasTheme.Accent
                        rarity != ItemRarity.COMMON -> rarityColor(rarity).copy(alpha = 0.35f)
                        else -> MayasTheme.Outline
                    }
                ),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { previewId = item.id }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(item.icon ?: "✨", fontSize = 22.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(item.name, fontSize = 9.5.sp, color = MayasTheme.TextSecondary, maxLines = 1, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    when {
                        isEquipped -> Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(12.dp))
                        isOwned -> Text("Есть", fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                        !item.isAvailableNow() -> Text("🔒 Скоро", fontSize = 9.sp, color = MayasTheme.TextSecondary)
                        else -> Text("${item.price} 🪙", fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
    balance: Int,
    ownedItems: List<String>,
    onDismiss: () -> Unit,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit,
    currentEmoji: String = "",
    messageStyle: String = "",
    currentWallpaper: String = "",
    currentFont: String = "",
    currentEffect: String = ""
) {
    val allItems by ShopRepository.items.collectAsState()
    val emojis = allItems.filter { it.type == ItemType.EMOJI_STATUS }
    val styles = allItems.filter { it.type == ItemType.BUBBLE }
    val fonts = allItems.filter { it.type == ItemType.FONT }
    val effects = allItems.filter { it.type == ItemType.EFFECT }

    val categories = remember { ShopCategory.values().toList() }
    var selectedCategory by remember { mutableStateOf(ShopCategory.DAILY) }

    var dailyDeals by remember { mutableStateOf(ShopConstants.getDailyDeals(pool = allItems)) }
    var msUntilReset by remember { mutableLongStateOf(ShopConstants.millisUntilNextDailyReset()) }
    LaunchedEffect(allItems) {
        while (true) {
            delay(60_000L)
            msUntilReset = ShopConstants.millisUntilNextDailyReset()
            if (msUntilReset > 23 * 60 * 60 * 1000L) {
                dailyDeals = ShopConstants.getDailyDeals(pool = allItems)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MayasTheme.Background,
            border = BorderStroke(0.5.dp, MayasTheme.Outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Магазин",
                        color = MayasTheme.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MayasTheme.GlowGold.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, MayasTheme.GlowGold.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Text(
                                "$balance",
                                color = MayasTheme.GlowGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Divider)

                ShopCategoryTabs(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )

                HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Divider)




                AnimatedContent(
                    targetState = selectedCategory,
                    modifier = Modifier.heightIn(max = 600.dp),
                    transitionSpec = {
                        (fadeIn(tween(150)) togetherWith fadeOut(tween(150)))
                    },
                    label = "shop_category"
                ) { category ->
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        when (category) {
                            ShopCategory.DAILY -> DailyDealsSection(
                                dailyDeals = dailyDeals,
                                msUntilReset = msUntilReset,
                                ownedItems = ownedItems,
                                onBuyItem = onBuyItem,
                                onSelectItem = onSelectItem
                            )

                            ShopCategory.ALL -> {
                                EmojiSection(emojis, ownedItems, currentEmoji, onBuyItem, onSelectItem)
                                Spacer(Modifier.height(20.dp))
                                BubbleStylesSection(styles, ownedItems, messageStyle, onBuyItem, onSelectItem)
                                Spacer(Modifier.height(20.dp))
                                FontSection(fonts, ownedItems, currentFont, onBuyItem, onSelectItem)
                                Spacer(Modifier.height(20.dp))
                                EffectSection(effects, ownedItems, currentEffect, onBuyItem, onSelectItem)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Больше товаров скоро...",
                                    color = MayasTheme.TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            ShopCategory.BUBBLE -> BubbleStylesSection(styles, ownedItems, messageStyle, onBuyItem, onSelectItem)
                            ShopCategory.EMOJI -> EmojiSection(emojis, ownedItems, currentEmoji, onBuyItem, onSelectItem)
                            ShopCategory.FONT -> FontSection(fonts, ownedItems, currentFont, onBuyItem, onSelectItem)
                            ShopCategory.EFFECT -> EffectSection(effects, ownedItems, currentEffect, onBuyItem, onSelectItem)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Surface)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть", color = MayasTheme.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePickerDialog(
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onSystemIcon: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Фото профиля", color = MayasTheme.TextPrimary) },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Выбрать из галереи", color = MayasTheme.TextPrimary) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = MayasTheme.Accent) },
                    modifier = Modifier.clickable { onGallery() },
                    colors = ListItemDefaults.colors(containerColor = MayasTheme.Surface)
                )
                if (!isGroup) {
                    ListItem(
                        headlineContent = { Text("Системная иконка", color = MayasTheme.TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Face, null, tint = MayasTheme.Accent) },
                        modifier = Modifier.clickable { onSystemIcon() },
                        colors = ListItemDefaults.colors(containerColor = MayasTheme.Surface)
                    )
                }
                HorizontalDivider(color = MayasTheme.Divider)
                ListItem(
                    headlineContent = { Text("Удалить фото", color = MayasTheme.ErrorRed) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MayasTheme.ErrorRed) },
                    modifier = Modifier.clickable { onDelete() },
                    colors = ListItemDefaults.colors(containerColor = MayasTheme.Surface)
                )
            }
        }
    )
}



@Composable
fun IconPickerDialog(
    icons: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = MayasTheme.TextSecondary) } },
        title = { Text("Иконка профиля", color = MayasTheme.TextPrimary) },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(icons) { icon ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .clickable { onSelect(icon) },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileIcon(icon, size = 30.dp)
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
    isMyProfile: Boolean,
    isEditing: Boolean,
    canEdit: Boolean,
    balance: Int,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onShopClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                if (isMyProfile) "Я" else "Профиль",
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MayasTheme.IconPrimary
                )
            }
        },
        actions = {
            if (isMyProfile) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MayasTheme.Surface)
                        .clickable { onShopClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = MayasTheme.GlowGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatCompactCount(balance),
                        color = MayasTheme.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = onNavigateToPremium) {
                    Icon(
                        Icons.Default.Diamond,
                        contentDescription = "Mayas+",
                        tint = MayasTheme.GlowGold
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = MayasTheme.IconPrimary
                    )
                }
            }

            if (canEdit) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Сохранить" else "Редактировать",
                        tint = MayasTheme.Accent
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
    )
}


@Composable
fun StatusBadge(value: String, fontSize: androidx.compose.ui.unit.TextUnit = 20.sp) {
    EmojiStatusView(status = value, size = fontSize)
}