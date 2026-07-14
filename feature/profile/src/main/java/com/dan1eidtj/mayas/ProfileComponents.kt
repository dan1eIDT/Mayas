package com.dan1eidtj.mayas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.ChatBubble
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon



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
                style = androidx.compose.ui.text.TextStyle(brush = PremiumGradient),
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

        // Раскрывающийся контент
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
                    // CTA для не-подписчиков
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
                    // Режим невидимки
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

                    // Иконка верификации
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
                            // Сетка по 5 в ряд — с одним рядом уже не помещаются все варианты
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

                    // Обводка аватара
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

                    // Цвет ника
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
                                "rainbow" to MayasTheme.GlowAmber // Условный цвет для радуги в пикере
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

// ─── EarnAndShopSection ──────────────────────────────────────────────────────

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
        // Реклама
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

        // Магазин
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


@Composable
fun ShopDialog(
    balance: Int,
    ownedItems: List<String>,
    onDismiss: () -> Unit,
    onBuyItem: (String, Int, String) -> Unit,
    onSelectItem: (String, ItemType) -> Unit,
    currentEmoji: String = "",
    messageStyle: String = ""
) {
    val emojis = ShopConstants.EMOJI_STATUSES
    val styles = ShopConstants.BUBBLE_STYLES

    var dailyDeals by remember { mutableStateOf(ShopConstants.getDailyDeals()) }
    var msUntilReset by remember { mutableLongStateOf(ShopConstants.millisUntilNextDailyReset()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            msUntilReset = ShopConstants.millisUntilNextDailyReset()
            if (msUntilReset > 23 * 60 * 60 * 1000L) {
                dailyDeals = ShopConstants.getDailyDeals()
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
                // Header
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

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // ── ЕЖЕДНЕВНЫЙ МАГАЗИН ──────────────────────────────
                    if (dailyDeals.isNotEmpty()) {
                        val totalMinutes = (msUntilReset / 60_000L).toInt().coerceAtLeast(0)
                        val hLeft = totalMinutes / 60
                        val mLeft = totalMinutes % 60

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
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            dailyDeals.forEach { deal ->
                                val item = deal.item
                                val isOwned = ownedItems.contains(item.id)
                                val isBubble = item.type == ItemType.BUBBLE
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MayasTheme.Surface,
                                    border = BorderStroke(0.5.dp, MayasTheme.GlowGold.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable {
                                            if (isOwned) onSelectItem(item.id, item.type)
                                            else onBuyItem(item.id, deal.discountedPrice, item.name)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        if (isBubble) ShopConstants.getStyleGradient(item.id)
                                                        else listOf(MayasTheme.Surface, MayasTheme.Surface)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!isBubble) Text(item.id, fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
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
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
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
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        "ЭМОДЗИ-СТАТУСЫ",
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
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false
                    ) {
                        items(emojis, key = { it.id }) { item ->
                            val emoji = item.id
                            val price = item.price
                            val isOwned = ownedItems.contains(emoji)
                            val isSelected = currentEmoji == emoji
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) MayasTheme.Accent else MayasTheme.Outline
                                ),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (isOwned) onSelectItem(emoji, ItemType.EMOJI_STATUS)
                                        else onBuyItem(emoji, price, "Эмодзи $emoji")
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                    Spacer(Modifier.height(3.dp))
                                    if (isOwned) {
                                        Icon(Icons.Default.Check, null, tint = MayasTheme.Accent, modifier = Modifier.size(12.dp))
                                    } else {
                                        Text("$price 🪙", fontSize = 10.sp, color = MayasTheme.TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "ПРЕВЬЮ СТИЛЯ",
                        color = MayasTheme.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.em
                    )
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Surface)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatBubble(
                            text = "Так будут выглядеть твои сообщения ✨",
                            isMe = true,
                            isRead = true,
                            time = "12:00",
                            onLongClick = {},
                            messageStyle = messageStyle.ifEmpty { null }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "СТИЛИ СООБЩЕНИЙ",
                        color = MayasTheme.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.em
                    )
                    Spacer(Modifier.height(10.dp))

                    styles.forEach { item ->
                        val id = item.id
                        val name = item.name
                        val price = item.price
                        val isOwned = ownedItems.contains(id)
                        val isUsing = messageStyle == id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUsing) MayasTheme.Accent.copy(alpha = 0.08f) else MayasTheme.Surface,
                            border = BorderStroke(
                                if (isUsing) 1.5.dp else 0.5.dp,
                                if (isUsing) MayasTheme.Accent else MayasTheme.Outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable {
                                    if (isOwned) onSelectItem(id, ItemType.BUBBLE)
                                    else onBuyItem(id, price, name)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // FIX: раньше тут была плоская однотонная точка —
                                // теперь мини-градиент, реально показывающий стиль
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(ShopConstants.getStyleGradient(id)))
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(name, color = MayasTheme.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (isOwned) {
                                    Text(
                                        if (isUsing) "Используется" else "Куплено",
                                        color = if (isUsing) MayasTheme.Accent else MayasTheme.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("🪙", fontSize = 12.sp)
                                        Text("$price", color = MayasTheme.GlowGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
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

// ─── IconPickerDialog ────────────────────────────────────────────────────────

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