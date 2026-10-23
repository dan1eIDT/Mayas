/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas

import com.dan1eidtj.profile.R
import androidx.compose.ui.res.stringResource
import android.os.Build
import androidx.annotation.RequiresApi
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
import android.widget.Toast
import com.dan1eidtj.mayas.ads.AdsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.dan1eidtj.mayas.feature.Message
import com.dan1eidtj.mayas.storage.B2Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dan1eidtj.data.ShopConstants.DailyDeal
import com.dan1eidtj.data.ItemRarity
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.data.gradientFor
import com.dan1eidtj.data.ShopItem
import com.dan1eidtj.data.ShopRepository
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.data.AdRewardResult
import com.dan1eidtj.data.claimAdRewardViaBackend
import com.dan1eidtj.data.NotMayasPlan
import com.dan1eidtj.data.NotMayasStatus
import com.dan1eidtj.data.ShopBanner
import com.dan1eidtj.data.ShopBannerRepository
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core.ui.theme.NicknameStyles
import com.dan1eidtj.mayas.core.ui.theme.nickStyle
import com.dan1eidtj.mayas.core_ui.ui.components.ChatBubble
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationBadge
import com.dan1eidtj.mayas.core_ui.ui.components.AdminLevelBadge
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationInfoDialog
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.dan1eidtj.mayas.feature.formatCompactCount
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext


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
        com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyph(status, fontSize = size)
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
        Text(stringResource(R.string.standard_label), color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                stringResource(R.string.mayas_plus_exclusive),
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
    avatarFrame: String,
    nameColor: String = "gold",
    onNavigateToPremium: () -> Unit,
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "MAYAS+",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (isPremium) stringResource(R.string.active_checkmark) else stringResource(R.string.appearance_and_style),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
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
                            Text(stringResource(R.string.get_mayas_plus), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(stringResource(R.string.exclusive_features_and_customization), color = Color.White.copy(0.8f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {



                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .padding(14.dp)
                    ) {
                        Text(stringResource(R.string.avatar_frame_label), color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val frames = listOf("none", "gold", "rainbow", "neon", "fire", "black", "sakura", "lilac", "mintpink", "coffee", "ice", "pinkred")
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
                        Text(stringResource(R.string.nickname_color_label), color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val nameColors: List<Pair<String, Brush>> = listOf(
                                "gold" to MayasTheme.NicknameSimpleGold,
                                "purple" to MayasTheme.GlowPurple,
                                "pink" to MayasTheme.GlowPink,
                                "blue" to MayasTheme.GlowBlue,
                                "green" to MayasTheme.GlowGreen,
                                "red" to MayasTheme.GlowRed,
                                "orange" to MayasTheme.GlowOrange,
                                "cyan" to MayasTheme.NicknameSimpleCyan,
                                "white" to Color.White,
                            ).map { (key, color) -> key to Brush.linearGradient(listOf(color, color)) } +
                                ("rainbow" to PremiumGradient) +

                                NicknameStyles.map { it.key to Brush.linearGradient(it.colors) }
                            items(nameColors, key = { it.first }) { (key, brush) ->
                                val isSelected = nameColor == key
                                val isLight = key == "white" ||
                                    nickStyle(key)?.colors?.map { it.luminance() }?.average()?.let { it > 0.6 } == true
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(brush)
                                        .border(0.5.dp, MayasTheme.TextPrimary.copy(alpha = 0.15f), CircleShape)
                                        .clickable { onNameColorSelect(key) }
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MayasTheme.TextPrimary, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = if (isLight) Color.Black else Color.White, modifier = Modifier.size(14.dp))
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
    currentFrame: String,
    onFrameSelect: (String) -> Unit
) {
    PremiumSectionCollapsible(
        isPremium = isPremium,
        avatarFrame = currentFrame,
        nameColor = "gold",
        onNavigateToPremium = {},
        onFrameSelect = onFrameSelect,
        onNameColorSelect = {}
    )
}



@Composable
fun AdEarnSection(embedded: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) {
        var current: android.content.Context? = context
        while (current != null && current !is android.app.Activity) {
            current = (current as? android.content.ContextWrapper)?.baseContext
        }
        current as? android.app.Activity
    }
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    var adsWatchedToday by remember { mutableIntStateOf(0) }
    var adsResetAt by remember { mutableStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var balance by remember { mutableIntStateOf(0) }
    var isAdLoading by remember { mutableStateOf(false) }
    var isRewardRequestPending by remember { mutableStateOf(false) }

    val adLimitReachedText = stringResource(R.string.ad_limit_reached)
    val adStillLoadingText = stringResource(R.string.ad_still_loading)
    val notSignedInText = stringResource(R.string.ad_not_authorized)
    val adUnavailableHereText = stringResource(R.string.ad_cannot_open_here)
    val adRewardCoins = com.dan1eidtj.data.ShopConstants.AD_REWARD_COINS
    val adRewardText = stringResource(R.string.ad_reward_granted, adRewardCoins)
    val creditFailedText = stringResource(R.string.error_credit_failed)

    DisposableEffect(myUid) {
        if (myUid.isBlank()) return@DisposableEffect onDispose {}
        val registration = db.collection("users").document(myUid)
            .addSnapshotListener { doc, _ ->
                val data = doc?.data ?: return@addSnapshotListener
                adsWatchedToday = (data["adsWatchedToday"] as? Number)?.toInt() ?: 0
                adsResetAt = (data["adsResetAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                balance = (data["balance"] as? Number)?.toInt() ?: 0
            }
        onDispose { registration.remove() }
    }

    LaunchedEffect(adsResetAt) {
        nowMs = System.currentTimeMillis()
        val waitMs = adsResetAt - nowMs
        if (waitMs > 0L) {
            delay(waitMs)
            nowMs = maxOf(System.currentTimeMillis(), adsResetAt)
        }
    }

    val effectiveWatched = if (adsResetAt <= nowMs) 0 else adsWatchedToday

    Column(modifier = if (embedded) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (embedded) {
            Text(
                stringResource(R.string.earnings_section),
                color = MayasTheme.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em
            )
            Spacer(Modifier.height(10.dp))
        } else {
            SectionTitle(stringResource(R.string.earnings_section))
        }
        EarnAndShopSection(
            adsWatched = effectiveWatched,
            isAdLoading = isAdLoading,
            onWatchAd = {
                if (myUid.isBlank()) {
                    Toast.makeText(context, notSignedInText, Toast.LENGTH_SHORT).show()
                    return@EarnAndShopSection
                }
                if (isAdLoading) return@EarnAndShopSection
                if (effectiveWatched >= com.dan1eidtj.data.ShopConstants.AD_WATCH_DAILY_LIMIT) {
                    Toast.makeText(context, adLimitReachedText, Toast.LENGTH_SHORT).show()
                    return@EarnAndShopSection
                }

                if (!AdsManager.isRewardedAvailable()) {
                    android.util.Log.d("AdsDebug", "Реклама ещё не загружена, пробуем загрузить и подождать")
                    Toast.makeText(context, adStillLoadingText, Toast.LENGTH_SHORT).show()
                    AdsManager.loadRewarded()
                    return@EarnAndShopSection
                }

                val hostActivity = activity
                if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
                    Toast.makeText(context, adUnavailableHereText, Toast.LENGTH_SHORT).show()
                    return@EarnAndShopSection
                }

                isAdLoading = true
                android.util.Log.d("AdsDebug", "Показываем rewarded, состояние доступности: ${AdsManager.isRewardedAvailable()}")
                val rewardRequestId = java.util.UUID.randomUUID().toString()
                isRewardRequestPending = false

                AdsManager.showRewarded(
                    activity = hostActivity,
                    onReward = {
                        isRewardRequestPending = true
                        coroutineScope.launch {
                            try {
                                val reward = withContext(NonCancellable) { claimAdRewardViaBackend(rewardRequestId) }
                                when (reward) {
                                    is AdRewardResult.Success -> {
                                        adsWatchedToday = reward.adsWatchedToday
                                        adsResetAt = reward.resetAtMillis
                                        balance = reward.newBalance
                                        isRewardRequestPending = false
                                        isAdLoading = false
                                        if (!reward.duplicate) {
                                            Toast.makeText(context, adRewardText, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    AdRewardResult.LimitReached -> {
                                        isRewardRequestPending = false
                                        isAdLoading = false
                                        adsWatchedToday = com.dan1eidtj.data.ShopConstants.AD_WATCH_DAILY_LIMIT
                                        Toast.makeText(context, adLimitReachedText, Toast.LENGTH_SHORT).show()
                                    }
                                    is AdRewardResult.Error -> {
                                        isRewardRequestPending = false
                                        isAdLoading = false
                                        android.util.Log.e("AdsDebug", "Не удалось начислить награду: ${reward.message}")
                                        Toast.makeText(context, creditFailedText, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                isRewardRequestPending = false
                                isAdLoading = false
                                android.util.Log.e("AdsDebug", "Ошибка серверного начисления награды", e)
                                Toast.makeText(context, creditFailedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onError = { message ->
                        isRewardRequestPending = false
                        isAdLoading = false
                        android.util.Log.e("AdsDebug", "Ошибка показа рекламы: $message")
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onClosed = {
                        if (!isRewardRequestPending) isAdLoading = false
                    }
                )
            }
        )
    }
}

@Composable
fun EarnAndShopSection(
    adsWatched: Int,
    isAdLoading: Boolean = false,
    onWatchAd: () -> Unit
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
                    Text(
                        stringResource(R.string.ad_watch_count, adsWatched, com.dan1eidtj.data.ShopConstants.AD_WATCH_DAILY_LIMIT),
                        color = MayasTheme.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(stringResource(R.string.watch_and_earn), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onWatchAd,
                enabled = adsWatched < com.dan1eidtj.data.ShopConstants.AD_WATCH_DAILY_LIMIT && !isAdLoading,
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
                    Text(stringResource(R.string.watch), fontSize = 13.sp)
                }
            }
        }
    }
}




@Composable
private fun rarityLabel(rarity: ItemRarity): String = when (rarity) {
    ItemRarity.COMMON -> stringResource(R.string.rarity_common)
    ItemRarity.RARE -> stringResource(R.string.rarity_rare)
    ItemRarity.EPIC -> stringResource(R.string.rarity_epic)
    ItemRarity.LEGENDARY -> stringResource(R.string.rarity_legendary)
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
            stringResource(R.string.shop_new_badge),
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


private enum class ShopCategory(val labelRes: Int, val itemType: ItemType?, val icon: ImageVector?) {
    DAILY(R.string.promo_label, null, Icons.Default.LocalFireDepartment),
    ALL(R.string.all_label, null, Icons.Default.Apps),
    BUBBLE(R.string.styles_label, ItemType.BUBBLE, Icons.AutoMirrored.Filled.Chat),
    EMOJI(R.string.statuses_label, ItemType.EMOJI_STATUS, Icons.Default.EmojiEmotions),
    FONT(R.string.fonts_label, ItemType.FONT, Icons.Default.TextFields),
    EFFECT(R.string.effects_label, ItemType.EFFECT, Icons.Default.AutoAwesome),
    OTHER(R.string.shop_other_category, null, Icons.Default.Star)
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
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else MayasTheme.Surface,
                animationSpec = tween(200),
                label = "shop_tab_container"
            )
            val contentColor = when {
                !isSelected -> MayasTheme.TextSecondary
                isFire -> Color.Black
                else -> Color.White
            }
            Surface(
                onClick = { onSelect(category) },
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                border = BorderStroke(0.5.dp, if (isSelected) activeColor else MayasTheme.Outline)
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
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        stringResource(category.labelRes),
                        color = contentColor,
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
    isLocked: Boolean,
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
                Text(stringResource(R.string.in_use_label), color = MayasTheme.Accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        isOwned -> Button(
            onClick = onApply,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.apply), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        isLocked -> Surface(
            shape = RoundedCornerShape(10.dp),
            color = MayasTheme.TextSecondary.copy(alpha = 0.12f)
        ) {
            Text(
                stringResource(R.string.soon_locked),
                color = MayasTheme.TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyDealsSection(
    dailyDeals: List<DailyDeal>,
    isLoading: Boolean,
    msUntilReset: Long,
    ownedItems: List<String>,
    equippedIds: Set<String>,
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
            if (isLoading) {
                CircularProgressIndicator(color = MayasTheme.GlowGold, modifier = Modifier.size(28.dp))
            } else {
                Text(
                    stringResource(R.string.daily_deal_over),
                    color = MayasTheme.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
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
            stringResource(R.string.daily_shop_header),
            color = MayasTheme.GlowGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            modifier = Modifier.weight(1f)
        )
        Text(
            stringResource(R.string.deal_refreshes_in, hLeft, mLeft),
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

    ShopGrid(items = dailyDeals, columns = 3, spacing = 10.dp) { deal ->
        DailyDealTile(
            deal = deal,
            isOwned = ownedItems.contains(deal.item.id),
            isEquipped = equippedIds.contains(deal.item.id),
            onBuyItem = onBuyItem,
            onSelectItem = onSelectItem
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyDealTile(
    deal: DailyDeal,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit
) {
    val item = deal.item
    val isAvailable = item.isAvailableNow()
    val previewGradient = when (item.type) {
        ItemType.BUBBLE -> ShopConstants.gradientFor(item)
        else -> listOf(MayasTheme.Surface, MayasTheme.Surface)
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MayasTheme.Surface,
        border = BorderStroke(0.5.dp, MayasTheme.GlowGold.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clickable {
                if (isOwned) onSelectItem(item.id, item.type)
                else if (isAvailable) onBuyItem(item.id, deal.discountedPrice, item.name)
            }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
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
                            stringResource(R.string.sample_text_aa), color = MayasTheme.TextPrimary, fontSize = 18.sp,
                            fontFamily = ShopConstants.getFontFamily(item.id)
                        )
                        ItemType.EFFECT -> Text(item.icon ?: "✨", fontSize = 20.sp)
                        else -> Icon(Icons.Default.Star, null, tint = MayasTheme.GlowGold, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.name, color = MayasTheme.TextPrimary, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                when {
                    isEquipped -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(11.dp))
                        Text(stringResource(R.string.in_use_label), color = MayasTheme.Accent, fontSize = 10.sp)
                    }
                    isOwned -> Text(stringResource(R.string.purchased_label), color = MayasTheme.TextSecondary, fontSize = 10.sp)
                    !isAvailable -> Text(stringResource(R.string.soon_locked), color = MayasTheme.TextSecondary, fontSize = 10.sp)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
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
                }
            }
            if (!isOwned && isAvailable) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 14.dp, bottomStart = 10.dp),
                    color = MayasTheme.GlowGold,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        "-${deal.discountPercent}%",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ShopGrid(
    items: List<T>,
    columns: Int,
    spacing: Dp,
    content: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { content(item) }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun shopItemBorderColor(rarity: ItemRarity, isPreviewing: Boolean): Color {
    if (isPreviewing) return MayasTheme.Accent
    return when (rarity) {
        ItemRarity.COMMON -> MayasTheme.Outline
        ItemRarity.LEGENDARY -> {
            val transition = rememberInfiniteTransition(label = "legendary_border")
            val alpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(tween(1400), repeatMode = RepeatMode.Reverse),
                label = "legendary_border_alpha"
            )
            rarityColor(rarity).copy(alpha = alpha)
        }
        else -> rarityColor(rarity).copy(alpha = 0.35f)
    }
}

private data class PendingPurchase(val id: String, val price: Int, val name: String)

private fun formatCoins(value: Int): String = java.text.NumberFormat.getIntegerInstance().format(value)

@RequiresApi(Build.VERSION_CODES.O)
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
        stringResource(R.string.status_preview_section),
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
                Text(stringResource(R.string.your_profile), color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (previewId.isNotEmpty()) EmojiStatusView(previewId, size = 17.sp)
            }
            if (previewItem != null) {
                ShopPreviewActions(
                    isEquipped = currentEmoji == previewId,
                    isOwned = ownedItems.contains(previewId),
                    isLocked = !previewItem.isAvailableNow(),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.EMOJI_STATUS) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            } else if (previewId.isEmpty()) {
                Text(stringResource(R.string.no_status_label), color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        stringResource(R.string.all_statuses_section),
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))
    ShopGrid(items = emojis, columns = 4, spacing = 8.dp) { item ->
        val emoji = item.id
        val isOwned = ownedItems.contains(emoji)
        val isEquipped = currentEmoji == emoji
        val isPreviewing = previewId == emoji
        val rarity = ShopConstants.rarityOf(item)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
            border = BorderStroke(if (isPreviewing) 1.5.dp else 0.5.dp, shopItemBorderColor(rarity, isPreviewing)),
            modifier = Modifier
                .fillMaxWidth()
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
                        isOwned -> Text(stringResource(R.string.has_it_label), fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                        !item.isAvailableNow() -> Text(stringResource(R.string.soon_locked), fontSize = 9.sp, color = MayasTheme.TextSecondary)
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

@RequiresApi(Build.VERSION_CODES.O)
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
        stringResource(R.string.style_preview_section),
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
                text = stringResource(R.string.message_preview_hint),
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
                    isLocked = !previewItem.isAvailableNow(),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.BUBBLE) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        stringResource(R.string.all_styles_hint_section),
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
            border = BorderStroke(if (isPreviewing) 1.5.dp else 0.5.dp, shopItemBorderColor(rarity, isPreviewing)),
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
                        .background(Brush.linearGradient(ShopConstants.gradientFor(item))),
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
                    isEquipped -> Text(stringResource(R.string.in_use_label), color = MayasTheme.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    isOwned -> Text(stringResource(R.string.purchased_label), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    !item.isAvailableNow() -> Text(stringResource(R.string.soon_locked), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("🪙", fontSize = 12.sp)
                        Text("${item.price}", color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
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
        stringResource(R.string.font_preview_section),
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
            stringResource(R.string.text_preview_hint),
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
                    isLocked = !previewItem.isAvailableNow(),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.FONT) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        stringResource(R.string.all_fonts_section),
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
            border = BorderStroke(if (isPreviewing) 1.5.dp else 0.5.dp, shopItemBorderColor(rarity, isPreviewing)),
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
                    stringResource(R.string.sample_text_aa),
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
                    isEquipped -> Text(stringResource(R.string.in_use_label), color = MayasTheme.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    isOwned -> Text(stringResource(R.string.purchased_label), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    !item.isAvailableNow() -> Text(stringResource(R.string.soon_locked), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("🪙", fontSize = 12.sp)
                        Text("${item.price}", color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
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
        stringResource(R.string.effect_preview_section),
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
                    previewItem?.name ?: stringResource(R.string.no_effect_label),
                    color = MayasTheme.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.message_send_preview_hint),
                    color = MayasTheme.TextSecondary,
                    fontSize = 11.5.sp
                )
            }
            if (previewItem != null && previewId != "none") {
                ShopPreviewActions(
                    isEquipped = currentEffect.ifEmpty { "none" } == previewId,
                    isOwned = ownedItems.contains(previewId),
                    isLocked = !previewItem.isAvailableNow(),
                    price = previewItem.price,
                    onApply = { onSelectItem(previewId, ItemType.EFFECT) },
                    onBuy = { if (previewItem.isAvailableNow()) onBuyItem(previewId, previewItem.price, previewItem.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Text(
        stringResource(R.string.all_effects_section),
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    )
    Spacer(Modifier.height(10.dp))
    ShopGrid(items = effects, columns = 3, spacing = 8.dp) { item ->
        val isOwned = item.id == "none" || ownedItems.contains(item.id)
        val isEquipped = currentEffect.ifEmpty { "none" } == item.id
        val isPreviewing = previewId == item.id
        val rarity = ShopConstants.rarityOf(item)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPreviewing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
            border = BorderStroke(if (isPreviewing) 1.5.dp else 0.5.dp, shopItemBorderColor(rarity, isPreviewing)),
            modifier = Modifier
                .fillMaxWidth()
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
                    isOwned -> Text(stringResource(R.string.has_it_label), fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                    !item.isAvailableNow() -> Text(stringResource(R.string.soon_locked), fontSize = 9.sp, color = MayasTheme.TextSecondary)
                    else -> Text("${item.price} 🪙", fontSize = 9.5.sp, color = MayasTheme.TextSecondary)
                }
            }
        }
    }
}
@Composable
private fun ShopBannersSection(
    banners: List<ShopBanner>,
    onBannerClick: (ShopBanner) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(banners, key = { it.id }) { banner ->
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .height(124.dp)
                    .clickable { onBannerClick(banner) },
                shape = RoundedCornerShape(16.dp),
                color = MayasTheme.Surface,
                border = BorderStroke(0.5.dp, MayasTheme.Outline)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (banner.imageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .width(104.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                .background(MayasTheme.Background)
                        ) {
                            coil.compose.AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(PremiumGradientSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MayasTheme.GlowGold, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = banner.title,
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (banner.subtitle.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = banner.subtitle,
                                color = MayasTheme.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.shop_label),
                            color = MayasTheme.Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotMayasPlusShopSection(balance: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val planCachePrefs = remember(context) {
        context.getSharedPreferences("notmayas_plan_catalog", android.content.Context.MODE_PRIVATE)
    }
    fun readCachedPlans(): List<NotMayasPlan> = runCatching {
        val array = org.json.JSONArray(planCachePrefs.getString("plans", "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("planId").takeIf { it.isNotBlank() } ?: continue
                val months = item.optInt("months", 0)
                val price = item.optInt("price", -1)
                if (months > 0 && price >= 0) add(NotMayasPlan(id, months, price))
            }
        }
    }.getOrDefault(emptyList())
    var plans by remember(planCachePrefs) { mutableStateOf(readCachedPlans()) }
    var status by remember { mutableStateOf<NotMayasStatus?>(null) }
    var loading by remember { mutableStateOf(plans.isEmpty()) }
    var loadingError by remember { mutableStateOf(false) }
    var plansRefreshFailed by remember { mutableStateOf(false) }
    var plansVerifiedForSession by remember { mutableStateOf(false) }
    var statusLoading by remember { mutableStateOf(true) }
    var statusError by remember { mutableStateOf(false) }
    var purchasePlanId by remember { mutableStateOf<String?>(null) }
    val pendingPurchasePrefs = remember(context) {
        context.getSharedPreferences("notmayas_purchase_requests", android.content.Context.MODE_PRIVATE)
    }
    var pendingRequestIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun requestIdFor(planId: String): String {
        pendingRequestIds[planId]?.let { return it }
        pendingPurchasePrefs.getString("request_$planId", null)?.let { stored ->
            pendingRequestIds = pendingRequestIds + (planId to stored)
            return stored
        }
        val generated = java.util.UUID.randomUUID().toString()
        pendingPurchasePrefs.edit().putString("request_$planId", generated).apply()
        pendingRequestIds = pendingRequestIds + (planId to generated)
        return generated
    }
    fun clearRequestId(planId: String) {
        pendingPurchasePrefs.edit().remove("request_$planId").apply()
        pendingRequestIds = pendingRequestIds - planId
    }
    var usernameInput by remember { mutableStateOf("") }
    var usernameCheckBusy by remember { mutableStateOf(false) }
    var usernameStatusText by remember { mutableStateOf<String?>(null) }
    var usernameStatusAvailable by remember { mutableStateOf<Boolean?>(null) }

    suspend fun refreshServerState() = coroutineScope {
        launch {
            statusLoading = status == null
            statusError = false
            try {
                status = BackendApi.notMayasStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusError = true
                android.util.Log.e("NotMayasShop", "Не удалось обновить статус NotMayas+", e)
            } finally {
                statusLoading = false
            }
        }
        launch {
            if (plans.isEmpty()) loading = true
            loadingError = false
            plansRefreshFailed = false
            plansVerifiedForSession = false
            try {
                val nextPlans = BackendApi.notMayasPlans()
                if (nextPlans.isEmpty()) throw IllegalStateException("Каталог NotMayas+ пуст")
                plans = nextPlans
                val cached = org.json.JSONArray()
                nextPlans.forEach { plan ->
                    cached.put(org.json.JSONObject().put("planId", plan.planId).put("months", plan.months).put("price", plan.price))
                }
                planCachePrefs.edit()
                    .putString("plans", cached.toString())
                    .putLong("cachedAt", System.currentTimeMillis())
                    .apply()
                plansVerifiedForSession = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                plansRefreshFailed = true
                loadingError = plans.isEmpty()
                android.util.Log.e("NotMayasShop", "Не удалось обновить каталог NotMayas+", e)
            } finally {
                loading = false
            }
        }
    }

    fun retryServerState() {
        scope.launch { refreshServerState() }
    }

    LaunchedEffect(Unit) {
        loading = plans.isEmpty()
        loadingError = false
        refreshServerState()
    }

    val currentStatus = status
    val active = currentStatus?.active == true
    val aliases = currentStatus?.additionalUsernames.orEmpty()
    val monthlyPrice = plans.firstOrNull { it.months == 1 }?.price

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MayasTheme.Surface,
            border = BorderStroke(1.dp, MayasTheme.GlowGold.copy(alpha = 0.32f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MayasTheme.GlowGold, modifier = Modifier.size(28.dp))
                    Text(stringResource(R.string.notmayas_title), color = MayasTheme.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.notmayas_description), color = MayasTheme.TextSecondary, fontSize = 13.sp)
                when {
                    statusLoading && currentStatus == null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MayasTheme.GlowGold)
                            Text(stringResource(R.string.notmayas_status_loading), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                        }
                    }
                    statusError && currentStatus == null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.notmayas_status_unavailable), color = MayasTheme.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { retryServerState() }) { Text(stringResource(R.string.chat_statistics_retry)) }
                        }
                    }
                    active -> {
                        val date = currentStatus?.expiresAt?.take(10).orEmpty()
                        Text(if (date.isBlank()) stringResource(R.string.notmayas_active_forever) else stringResource(R.string.notmayas_active_until, date), color = MayasTheme.GlowGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    else -> Text(stringResource(R.string.notmayas_not_active), color = MayasTheme.TextSecondary, fontSize = 13.sp)
                }
                val shownBalance = currentStatus?.balance ?: balance
                Text(stringResource(R.string.shop_balance_line, formatCoins(shownBalance)), color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MayasTheme.GlowGold)
            }
            loadingError -> Surface(shape = RoundedCornerShape(16.dp), color = MayasTheme.Surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.notmayas_generic_error), color = MayasTheme.TextSecondary, fontSize = 13.sp)
                    Button(onClick = { retryServerState() }) { Text(stringResource(R.string.chat_statistics_retry)) }
                }
            }
            else -> {
                if (plansRefreshFailed && plans.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MayasTheme.Surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.notmayas_catalog_cache_notice), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                            TextButton(onClick = { retryServerState() }, enabled = purchasePlanId == null) {
                                Text(stringResource(R.string.chat_statistics_retry))
                            }
                        }
                    }
                }
                plans.forEach { plan ->
                    val periodLabel = when (plan.months) {
                        1 -> stringResource(R.string.notmayas_one_month_label)
                        3, 6 -> androidx.compose.ui.res.pluralStringResource(R.plurals.notmayas_month_count, plan.months, plan.months)
                        12, 24 -> androidx.compose.ui.res.pluralStringResource(R.plurals.notmayas_year_count, plan.months / 12, plan.months / 12)
                        else -> androidx.compose.ui.res.pluralStringResource(R.plurals.notmayas_month_count, plan.months, plan.months)
                    }
                    val discount = if (monthlyPrice != null && monthlyPrice > 0) {
                        ((1.0 - plan.price.toDouble() / (monthlyPrice.toDouble() * plan.months)) * 100.0).toInt().coerceAtLeast(0)
                    } else 0
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MayasTheme.Surface,
                        border = BorderStroke(0.5.dp, MayasTheme.Outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(periodLabel, color = MayasTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("${java.text.NumberFormat.getIntegerInstance().format(plan.price)} 🪙", color = MayasTheme.GlowGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (discount > 0) Text(stringResource(R.string.notmayas_discount, discount), color = MayasTheme.Accent, fontSize = 11.sp)
                            }
                            Button(
                                enabled = purchasePlanId == null && plansVerifiedForSession,
                                onClick = {
                                    scope.launch {
                                        purchasePlanId = plan.planId
                                        val requestId = requestIdFor(plan.planId)
                                        try {
                                            val purchase = BackendApi.buyNotMayas(plan.planId, requestId)
                                            clearRequestId(plan.planId)
                                            val spent = java.text.NumberFormat.getIntegerInstance().format(purchase.spent)
                                            Toast.makeText(context, context.getString(R.string.notmayas_purchase_success, purchase.expiresAt?.take(10).orEmpty(), spent), Toast.LENGTH_LONG).show()
                                            runCatching { refreshServerState() }
                                                .onFailure { refreshError ->
                                                    android.util.Log.e("NotMayasShop", "Покупка подтверждена, но обновить экран не удалось", refreshError)
                                                }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NotMayasShop", "Ошибка покупки тарифа ${plan.planId}", e)
                                            val error = e.message?.substringBefore(' ') ?: ""
                                            val message = when (error) {
                                                "LOW_BALANCE" -> context.getString(R.string.notmayas_low_balance)
                                                "USERNAME_TAKEN", "INVALID_PLAN", "INVALID_REQUEST_ID" -> context.getString(R.string.notmayas_generic_error)
                                                else -> context.getString(R.string.notmayas_generic_error)
                                            }
                                            if (error in setOf("LOW_BALANCE", "INVALID_PLAN", "INVALID_REQUEST_ID", "USER_NOT_FOUND", "NOTMAYAS_INACTIVE")) {
                                                clearRequestId(plan.planId)
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        } finally {
                                            purchasePlanId = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowGold, contentColor = Color.Black)
                            ) {
                                if (purchasePlanId == plan.planId) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                else Text(if (active) stringResource(R.string.notmayas_extend) else stringResource(R.string.notmayas_buy), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MayasTheme.Divider)
        Text(stringResource(R.string.notmayas_usernames_title), color = MayasTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.notmayas_username_count, aliases.size, currentStatus?.maxAdditionalUsernames ?: 2), color = MayasTheme.TextSecondary, fontSize = 12.sp)
        if (active) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(32); usernameStatusText = null; usernameStatusAvailable = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.notmayas_username_placeholder)) },
                enabled = !usernameCheckBusy,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MayasTheme.TextPrimary,
                    unfocusedTextColor = MayasTheme.TextPrimary,
                    focusedBorderColor = MayasTheme.GlowGold,
                    unfocusedBorderColor = MayasTheme.Outline
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = usernameInput.length in 5..32 && !usernameCheckBusy,
                    onClick = {
                        scope.launch {
                            usernameCheckBusy = true
                            try {
                                usernameStatusAvailable = BackendApi.checkAdditionalUsername(usernameInput)
                                usernameStatusText = context.getString(if (usernameStatusAvailable == true) R.string.notmayas_username_available else R.string.notmayas_username_taken)
                            } catch (e: Exception) {
                                android.util.Log.e("NotMayasShop", "Проверка дополнительного username не удалась", e)
                                usernameStatusAvailable = null
                                usernameStatusText = context.getString(R.string.notmayas_generic_error)
                            } finally { usernameCheckBusy = false }
                        }
                    }, modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.notmayas_username_check)) }
                Button(
                    enabled = usernameInput.length in 5..32 && aliases.size < (currentStatus?.maxAdditionalUsernames ?: 2) && !usernameCheckBusy && purchasePlanId == null,
                    onClick = {
                        scope.launch {
                            usernameCheckBusy = true
                            try {
                                val names = BackendApi.reserveAdditionalUsername(usernameInput)
                                status = status?.copy(additionalUsernames = names)
                                usernameInput = ""
                                usernameStatusText = null
                                usernameStatusAvailable = null
                                Toast.makeText(context, context.getString(R.string.notmayas_username_available), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("NotMayasShop", "Резервирование дополнительного username не удалось", e)
                                val error = e.message?.substringBefore(' ') ?: ""
                                val message = when (error) {
                                    "USERNAME_TAKEN" -> context.getString(R.string.notmayas_username_taken)
                                    "USERNAME_LIMIT_REACHED" -> context.getString(R.string.notmayas_username_limit_reached)
                                    "NOTMAYAS_INACTIVE" -> context.getString(R.string.notmayas_username_need_subscription)
                                    else -> context.getString(R.string.notmayas_generic_error)
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            } finally { usernameCheckBusy = false }
                        }
                    }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowGold, contentColor = Color.Black)
                ) { Text(stringResource(R.string.notmayas_username_add)) }
            }
            usernameStatusText?.let { Text(it, color = if (usernameStatusAvailable == true) MayasTheme.Accent else MayasTheme.TextSecondary, fontSize = 12.sp) }
        } else {
            Text(stringResource(R.string.notmayas_username_need_subscription), color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        if (aliases.isEmpty()) {
            Text(stringResource(R.string.notmayas_username_empty), color = MayasTheme.TextSecondary, fontSize = 12.sp)
        } else {
            aliases.forEach { alias ->
                Surface(shape = RoundedCornerShape(12.dp), color = MayasTheme.Surface, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("@$alias", color = MayasTheme.TextPrimary, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = {
                            scope.launch {
                                usernameCheckBusy = true
                                try {
                                    val names = BackendApi.releaseAdditionalUsername(alias)
                                    status = status?.copy(additionalUsernames = names)
                                } catch (e: Exception) {
                                    android.util.Log.e("NotMayasShop", "Удаление дополнительного username не удалось", e)
                                    Toast.makeText(context, context.getString(R.string.notmayas_generic_error), Toast.LENGTH_LONG).show()
                                } finally { usernameCheckBusy = false }
                            }
                        }, enabled = !usernameCheckBusy) { Text(stringResource(R.string.notmayas_username_release), color = MayasTheme.ErrorRed) }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ShopBody(
    modifier: Modifier = Modifier,
    contentModifier: Modifier,
    balance: Int,
    ownedItems: List<String>,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit,
    currentEmoji: String,
    messageStyle: String,
    currentFont: String,
    currentEffect: String,
    footer: (@Composable () -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allItems by ShopRepository.items.collectAsState()
    val shopBanners by ShopBannerRepository.banners.collectAsState()
    val activeShopBanners = remember(shopBanners) {
        shopBanners.filter { it.isActive && (it.title.isNotBlank() || it.subtitle.isNotBlank() || it.imageUrl.isNotBlank()) }
    }
    val emojis = remember(allItems) { allItems.filter { it.type == ItemType.EMOJI_STATUS } }
    val styles = remember(allItems) { allItems.filter { it.type == ItemType.BUBBLE } }
    val fonts = remember(allItems) { allItems.filter { it.type == ItemType.FONT } }
    val effects = remember(allItems) { allItems.filter { it.type == ItemType.EFFECT } }
    val equippedIds = remember(currentEmoji, messageStyle, currentFont, currentEffect) {
        setOf(currentEmoji, messageStyle, currentFont, currentEffect).filter { it.isNotEmpty() }.toSet()
    }

    var selectedCategory by remember { mutableStateOf(ShopCategory.DAILY) }
    var dailyDeals by remember { mutableStateOf<List<ShopConstants.DailyDeal>>(emptyList()) }
    var dealsLoading by remember { mutableStateOf(true) }
    var msUntilReset by remember { mutableLongStateOf(ShopConstants.millisUntilNextDailyReset()) }
    var pendingPurchase by remember { mutableStateOf<PendingPurchase?>(null) }
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(600),
        label = "shop_balance"
    )

    val requestBuy: (String, Int, String) -> Unit = { id, price, name ->
        if (price > balance) {
            Toast.makeText(
                context,
                context.getString(R.string.shop_not_enough_coins, formatCoins(price - balance)),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            pendingPurchase = PendingPurchase(id, price, name)
        }
    }

    suspend fun refreshDailyDeals() {
        val result = try {
            com.dan1eidtj.data.getDailyDealsViaBackend()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        when (result) {
            is com.dan1eidtj.data.DailyDealsResult.Success -> {
                dailyDeals = result.items.mapNotNull { remote ->
                    val item = allItems.find { it.id == remote.itemId } ?: return@mapNotNull null
                    ShopConstants.DailyDeal(item, remote.discountPercent)
                }
            }
            else -> {
                if (dailyDeals.isEmpty()) {
                    dailyDeals = ShopConstants.getDailyDeals(pool = allItems)
                }
            }
        }
    }

    LaunchedEffect(allItems) {
        if (allItems.isEmpty()) {
            dealsLoading = true
            return@LaunchedEffect
        }
        dealsLoading = true
        refreshDailyDeals()
        dealsLoading = false
        var previous = ShopConstants.millisUntilNextDailyReset()
        msUntilReset = previous
        while (true) {
            delay(30_000L)
            val current = ShopConstants.millisUntilNextDailyReset()
            msUntilReset = current
            if (current > previous) refreshDailyDeals()
            previous = current
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.shop_label),
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
                        formatCoins(animatedBalance),
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
            modifier = contentModifier,
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
                    ShopCategory.DAILY -> {
                        if (activeShopBanners.isNotEmpty()) {
                            ShopBannersSection(
                                banners = activeShopBanners,
                                onBannerClick = { banner ->
                                    selectedCategory = when (banner.targetType) {
                                        ItemType.BUBBLE -> ShopCategory.BUBBLE
                                        ItemType.EMOJI_STATUS -> ShopCategory.EMOJI
                                        ItemType.FONT -> ShopCategory.FONT
                                        ItemType.EFFECT -> ShopCategory.EFFECT
                                        ItemType.COLOR_SCHEME, ItemType.ANIMATION, null -> ShopCategory.ALL
                                    }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        AdEarnSection(embedded = true)
                        Spacer(Modifier.height(16.dp))
                        DailyDealsSection(
                            dailyDeals = dailyDeals,
                            isLoading = dealsLoading,
                            msUntilReset = msUntilReset,
                            ownedItems = ownedItems,
                            equippedIds = equippedIds,
                            onBuyItem = requestBuy,
                            onSelectItem = onSelectItem
                        )
                    }

                    ShopCategory.ALL -> {
                        EmojiSection(emojis, ownedItems, currentEmoji, requestBuy, onSelectItem)
                        Spacer(Modifier.height(20.dp))
                        BubbleStylesSection(styles, ownedItems, messageStyle, requestBuy, onSelectItem)
                        Spacer(Modifier.height(20.dp))
                        FontSection(fonts, ownedItems, currentFont, requestBuy, onSelectItem)
                        Spacer(Modifier.height(20.dp))
                        EffectSection(effects, ownedItems, currentEffect, requestBuy, onSelectItem)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.more_items_soon),
                            color = MayasTheme.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.4f),
                            textAlign = TextAlign.Center
                        )
                    }

                    ShopCategory.BUBBLE -> BubbleStylesSection(styles, ownedItems, messageStyle, requestBuy, onSelectItem)
                    ShopCategory.EMOJI -> EmojiSection(emojis, ownedItems, currentEmoji, requestBuy, onSelectItem)
                    ShopCategory.FONT -> FontSection(fonts, ownedItems, currentFont, requestBuy, onSelectItem)
                    ShopCategory.EFFECT -> EffectSection(effects, ownedItems, currentEffect, requestBuy, onSelectItem)
                    ShopCategory.OTHER -> NotMayasPlusShopSection(balance = balance)
                }
            }
        }

        if (footer != null) {
            HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Surface)
            footer()
        }
    }

    pendingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            containerColor = MayasTheme.Surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(stringResource(R.string.shop_confirm_title), color = MayasTheme.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.shop_confirm_message, purchase.name, formatCoins(purchase.price)),
                        color = MayasTheme.TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        stringResource(R.string.shop_confirm_balance_after, formatCoins(balance - purchase.price)),
                        color = MayasTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyItem(purchase.id, purchase.price, purchase.name)
                        pendingPurchase = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MayasTheme.GlowGold,
                        contentColor = Color.Black
                    )
                ) {
                    Text(stringResource(R.string.shop_confirm_buy), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) {
                    Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextSecondary)
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
    currentEffect: String = "",
    isPurchaseInProgress: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MayasTheme.Background,
            border = BorderStroke(0.5.dp, MayasTheme.Outline),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ShopBody(
                    contentModifier = Modifier.heightIn(max = 600.dp),
                    balance = balance,
                    ownedItems = ownedItems,
                    onBuyItem = onBuyItem,
                    onSelectItem = onSelectItem,
                    currentEmoji = currentEmoji,
                    messageStyle = messageStyle,
                    currentFont = currentFont,
                    currentEffect = currentEffect,
                    footer = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.close), color = MayasTheme.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                )
                if (isPurchaseInProgress) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MayasTheme.Accent)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    balance: Int,
    ownedItems: List<String>,
    onBack: () -> Unit,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit,
    currentEmoji: String = "",
    messageStyle: String = "",
    currentWallpaper: String = "",
    currentFont: String = "",
    currentEffect: String = "",
    isPurchaseInProgress: Boolean = false
) {
    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ShopBody(
                modifier = Modifier.fillMaxSize(),
                contentModifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                balance = balance,
                ownedItems = ownedItems,
                onBuyItem = onBuyItem,
                onSelectItem = onSelectItem,
                currentEmoji = currentEmoji,
                messageStyle = messageStyle,
                currentFont = currentFont,
                currentEffect = currentEffect
            )
            if (isPurchaseInProgress) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MayasTheme.Accent)
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
        title = { Text(stringResource(R.string.profile_photo_label), color = MayasTheme.TextPrimary) },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.choose_from_gallery), color = MayasTheme.TextPrimary) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = MayasTheme.Accent) },
                    modifier = Modifier.clickable { onGallery() },
                    colors = ListItemDefaults.colors(containerColor = MayasTheme.Surface)
                )
                if (!isGroup) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.system_icon_label), color = MayasTheme.TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Face, null, tint = MayasTheme.Accent) },
                        modifier = Modifier.clickable { onSystemIcon() },
                        colors = ListItemDefaults.colors(containerColor = MayasTheme.Surface)
                    )
                }
                HorizontalDivider(color = MayasTheme.Divider)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.delete_photo), color = MayasTheme.ErrorRed) },
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextSecondary) } },
        title = { Text(stringResource(R.string.profile_icon_content_description), color = MayasTheme.TextPrimary) },
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
private fun ProfileMenuItems(
    isMyProfile: Boolean,
    isGroup: Boolean,
    username: String,
    isBlocked: Boolean,
    isBlockActionLoading: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onCustomizationClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onWriteMessageClick: () -> Unit,
    onMembersClick: () -> Unit,
    onCopyUsernameClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    if (isMyProfile) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_menu_settings)) },
            leadingIcon = { Icon(Icons.Default.Settings, null) },
            onClick = { onDismiss(); onSettingsClick() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_menu_customization)) },
            leadingIcon = { Icon(Icons.Default.Palette, null) },
            onClick = { onDismiss(); onCustomizationClick() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_menu_credits)) },
            leadingIcon = { Icon(Icons.Default.Info, null) },
            onClick = { onDismiss(); onCreditsClick() }
        )
    } else if (isGroup) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_menu_members)) },
            leadingIcon = { Icon(Icons.Default.Group, null) },
            onClick = { onDismiss(); onMembersClick() }
        )
    } else {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_menu_write_message)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
            onClick = { onDismiss(); onWriteMessageClick() }
        )
        if (username.isNotBlank()) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_menu_copy_username)) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = { onDismiss(); onCopyUsernameClick() }
            )
        }
        DropdownMenuItem(
            enabled = !isBlockActionLoading,
            text = {
                Text(
                    stringResource(if (isBlocked) R.string.profile_menu_unblock else R.string.profile_menu_block),
                    color = if (isBlocked) MayasTheme.TextPrimary else MayasTheme.ErrorRed
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isBlocked) MayasTheme.IconPrimary else MayasTheme.ErrorRed
                )
            },
            onClick = { onDismiss(); onBlockClick() }
        )
    }
}

@Composable
private fun FloatingBarChip(
    progress: () -> Float,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .drawBehind {
                drawCircle(Color.Black.copy(alpha = 0.38f * (1f - progress()).coerceIn(0f, 1f)))
            }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
    isMyProfile: Boolean,
    isEditing: Boolean,
    canEdit: Boolean,
    onBack: () -> Unit,
    onShopClick: () -> Unit,
    onEditClick: () -> Unit,
    username: String = "",
    isGroup: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onCustomizationClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onWriteMessageClick: () -> Unit = {},
    onMembersClick: () -> Unit = {},
    onCopyUsernameClick: () -> Unit = {},
    isBlocked: Boolean = false,
    isBlockActionLoading: Boolean = false,
    onBlockClick: () -> Unit = {},
    floatingProgress: (() -> Float)? = null,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    if (floatingProgress != null) {
        val onSolid by remember(floatingProgress) { derivedStateOf { floatingProgress() > 0.86f } }
        val iconTint by animateColorAsState(
            targetValue = if (onSolid) MayasTheme.IconPrimary else Color.White,
            animationSpec = androidx.compose.animation.core.tween(180),
            label = "profileBarTint"
        )
        val accentTint by animateColorAsState(
            targetValue = if (onSolid) MayasTheme.Accent else Color.White,
            animationSpec = androidx.compose.animation.core.tween(180),
            label = "profileBarAccent"
        )
        val barBackground = MayasTheme.Background
        val titleText = if (isMyProfile) stringResource(R.string.me_label) else stringResource(R.string.profile_label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(barBackground.copy(alpha = ((floatingProgress() - 0.82f) / 0.18f).coerceIn(0f, 1f)))
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(ProfileFloatingBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FloatingBarChip(progress = floatingProgress, onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(com.dan1eidtj.mayas.ui.R.string.back),
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = titleText,
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = ((floatingProgress() - 0.9f) / 0.1f).coerceIn(0f, 1f) }
            )
            if (isMyProfile) {
                FloatingBarChip(progress = floatingProgress, onClick = onShopClick) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = stringResource(R.string.shop_label),
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (canEdit) {
                FloatingBarChip(progress = floatingProgress, onClick = onEditClick) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit),
                        tint = accentTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box {
                FloatingBarChip(progress = floatingProgress, onClick = { showMoreMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.profile_more_actions),
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    ProfileMenuItems(
                        isMyProfile = isMyProfile,
                        isGroup = isGroup,
                        username = username,
                        isBlocked = isBlocked,
                        isBlockActionLoading = isBlockActionLoading,
                        onDismiss = { showMoreMenu = false },
                        onSettingsClick = onSettingsClick,
                        onCustomizationClick = onCustomizationClick,
                        onCreditsClick = onCreditsClick,
                        onWriteMessageClick = onWriteMessageClick,
                        onMembersClick = onMembersClick,
                        onCopyUsernameClick = onCopyUsernameClick,
                        onBlockClick = onBlockClick
                    )
                }
            }
        }
        return
    }
    TopAppBar(
        title = {
            Text(
                if (isMyProfile) stringResource(R.string.me_label) else stringResource(R.string.profile_label),
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
                    contentDescription = stringResource(com.dan1eidtj.mayas.ui.R.string.back),
                    tint = MayasTheme.IconPrimary
                )
            }
        },
        actions = {
            if (isMyProfile) {
                IconButton(onClick = onShopClick) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = stringResource(R.string.shop_label),
                        tint = MayasTheme.IconPrimary
                    )
                }
            }

            if (canEdit) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit),
                        tint = MayasTheme.Accent
                    )
                }
            }

            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.profile_more_actions),
                        tint = MayasTheme.IconPrimary
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    ProfileMenuItems(
                        isMyProfile = isMyProfile,
                        isGroup = isGroup,
                        username = username,
                        isBlocked = isBlocked,
                        isBlockActionLoading = isBlockActionLoading,
                        onDismiss = { showMoreMenu = false },
                        onSettingsClick = onSettingsClick,
                        onCustomizationClick = onCustomizationClick,
                        onCreditsClick = onCreditsClick,
                        onWriteMessageClick = onWriteMessageClick,
                        onMembersClick = onMembersClick,
                        onCopyUsernameClick = onCopyUsernameClick,
                        onBlockClick = onBlockClick
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

@Composable
fun ProfileActionsRow(
    isBlocked: Boolean,
    isLoading: Boolean,
    onWriteClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onWriteClick,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(com.dan1eidtj.chats.R.string.write_message), color = Color.White, fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onBlockClick,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBlocked) MayasTheme.Surface else MayasTheme.ErrorRed.copy(0.1f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MayasTheme.ErrorRed
                )
            } else {
                Text(
                    if (isBlocked) stringResource(R.string.unlock) else stringResource(com.dan1eidtj.chat.R.string.block_user),
                    color = MayasTheme.ErrorRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private val ProfileUrlRegex = Regex(com.dan1eidtj.mayas.feature.MESSAGE_URL_REGEX)

fun extractUrls(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return ProfileUrlRegex.findAll(text).map { it.groupValues[1] }.toList()
}

@Composable
fun ProfileTabEmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MayasTheme.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileTabLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MayasTheme.Accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ProfileMediaGrid(messages: List<Message>, onMediaClick: (String) -> Unit) {
    if (messages.isEmpty()) {
        ProfileTabEmptyState(stringResource(R.string.no_photos_in_chat_yet))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        userScrollEnabled = false
    ) {
        items(messages, key = { it.id }) { msg ->
            val key = msg.mediaUrl
            if (key != null) {
                B2Image(
                    key = key,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMediaClick(key) },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun ProfileLinksList(messages: List<Message>) {
    val links = remember(messages) {
        messages.flatMap { msg -> extractUrls(msg.text).map { it to msg } }
    }
    if (links.isEmpty()) {
        ProfileTabEmptyState(stringResource(R.string.no_links_in_chat_yet))
        return
    }
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        links.forEach { (url, msg) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val normalized = if (url.startsWith("http")) url else "https://$url"
                        runCatching { uriHandler.openUri(normalized) }
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp), tint = MayasTheme.Accent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(url, color = MayasTheme.TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (msg.senderName.isNotBlank()) {
                        Text(msg.senderName, color = MayasTheme.TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            HorizontalDivider(color = MayasTheme.TextPrimary.copy(0.06f))
        }
    }
}

@Composable
fun ProfilePinnedList(messages: List<Message>, onMessageClick: (Message) -> Unit) {
    if (messages.isEmpty()) {
        ProfileTabEmptyState(stringResource(R.string.no_pinned_messages_in_chat))
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        messages.forEach { msg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMessageClick(msg) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = MayasTheme.GlowBlue)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (msg.senderName.isNotBlank()) {
                        Text(msg.senderName, color = MayasTheme.GlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        msg.text ?: if (msg.mediaUrl != null) stringResource(com.dan1eidtj.chat.R.string.photo_message) else stringResource(com.dan1eidtj.chats.R.string.message_generic),
                        color = MayasTheme.TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = MayasTheme.TextPrimary.copy(0.06f))
        }
    }
}