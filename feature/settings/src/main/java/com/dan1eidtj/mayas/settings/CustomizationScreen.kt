package com.dan1eidtj.mayas.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.ChatBubble
import com.dan1eidtj.mayas.feature.auth.AuthVM
import kotlin.collections.chunked
import com.dan1eidtj.mayas.EmojiStatusView
import com.dan1eidtj.mayas.SectionTitle


private enum class CustomizationCategory(val label: String) {
    EMOJI("Эмодзи"),
    BUBBLE("Стили"),
    FONT("Шрифт"),
    EFFECT("Эффект")
}

@Composable
private fun CustomizationCategoryTabs(
    selected: CustomizationCategory,
    onSelect: (CustomizationCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomizationCategory.values().forEach { category ->
            val isSelected = category == selected
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MayasTheme.Accent else MayasTheme.Surface,
                border = BorderStroke(0.5.dp, if (isSelected) MayasTheme.Accent else MayasTheme.Outline),
                modifier = Modifier.clickable { onSelect(category) }
            ) {
                Text(
                    category.label,
                    color = if (isSelected) Color.White else MayasTheme.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun EmojiCategoryContent(
    allEmojiOptions: List<String>,
    ownedItems: List<String>,
    currentEmojiStatus: String,
    onUse: (String) -> Unit
) {
    val myOwnedEmojis = allEmojiOptions.filter { ownedItems.contains(it) }

    if (myOwnedEmojis.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MayasTheme.Surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "У вас нет купленных статусов.\nИх можно найти в магазине.",
                modifier = Modifier.padding(20.dp),
                color = MayasTheme.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(12.dp)
    ) {
        myOwnedEmojis.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    val isSelected = currentEmojiStatus == emoji
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MayasTheme.Accent.copy(0.1f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MayasTheme.Accent else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUse(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        EmojiStatusView(emoji, size = 26.sp)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = MayasTheme.Accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        TextButton(
            onClick = { onUse("") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Убрать статус", color = MayasTheme.ErrorRed, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BubbleCategoryContent(
    styles: List<Triple<String, String, List<Color>>>,
    ownedItems: List<String>,
    currentMessageStyle: String,
    onUse: (String) -> Unit
) {
    styles.forEach { (id, name, gradient) ->
        val isOwned = id == "default" || ownedItems.contains(id)
        val isUsing = currentMessageStyle == id

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
                .clickable { if (isOwned) onUse(id) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradient))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    name,
                    color = if (isOwned) MayasTheme.TextPrimary else MayasTheme.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isOwned) {
                    if (isUsing) {
                        Text("Используется", color = MayasTheme.Accent, fontSize = 12.sp)
                    }
                } else {
                    Icon(
                        Icons.Default.Palette,
                        null,
                        tint = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Больше стилей можно купить в магазине",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun FontCategoryContent(
    ownedItems: List<String>,
    currentFont: String,
    onUse: (String) -> Unit
) {
    val myOwnedFonts = ShopConstants.FONT_STYLES
        .filter { it.id == "default" || ownedItems.contains(it.id) }

    myOwnedFonts.forEach { font ->
        val isUsing = currentFont == font.id
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
                .clickable { onUse(font.id) }
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
                    fontFamily = ShopConstants.getFontFamily(font.id),
                    modifier = Modifier.width(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    font.name,
                    color = MayasTheme.TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = ShopConstants.getFontFamily(font.id),
                    modifier = Modifier.weight(1f)
                )
                if (isUsing) {
                    Text("Используется", color = MayasTheme.Accent, fontSize = 12.sp)
                }
            }
        }
    }
    Text(
        "Больше шрифтов можно купить в магазине",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EffectCategoryContent(
    ownedItems: List<String>,
    currentEffect: String,
    onUse: (String) -> Unit
) {
    val myOwnedEffects = ShopConstants.EFFECT_STYLES
        .filter { it.id == "none" || ownedItems.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .padding(12.dp)
    ) {
        myOwnedEffects.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { effect ->
                    val isSelected = currentEffect == effect.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MayasTheme.Accent.copy(0.1f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MayasTheme.Accent else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUse(effect.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(effect.icon ?: "✨", fontSize = 22.sp)
                            Text(effect.name, fontSize = 8.5.sp, color = MayasTheme.TextSecondary, maxLines = 1)
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = MayasTheme.Accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Ну ты и так знаешь",
        color = MayasTheme.TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CustomizationTabsAndContent(
    vm: AuthVM,
    modifier: Modifier = Modifier,
    initialCategory: CustomizationCategory = CustomizationCategory.EMOJI
) {
    val ownedItems = vm.ownedItems
    val currentMessageStyle = vm.userData["messageStyle"] ?: "default"
    val currentEmojiStatus = vm.userData["emojiStatus"] ?: ""
    val currentFont = vm.userData["fontFamily"] ?: "default"
    val currentEffect = vm.userData["sendEffect"] ?: "none"

    val styles = listOf(
        Triple("default", "Стандартный", listOf(MayasTheme.Accent, MayasTheme.Accent))
    ) + ShopConstants.BUBBLE_STYLES.map { item ->
        Triple(item.id, item.name, ShopConstants.getStyleGradient(item.id))
    }

    val allEmojiOptions = (ShopConstants.EMOJI_STATUSES + ShopConstants.ICON_STATUSES).map { it.id }

    var selectedCategory by remember { mutableStateOf(initialCategory) }

    Column(modifier = modifier) {
        CustomizationCategoryTabs(
            selected = selectedCategory,
            onSelect = { selectedCategory = it }
        )

        HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Divider)

        AnimatedContent(
            targetState = selectedCategory,
            modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                (fadeIn(tween(150)) togetherWith fadeOut(tween(150)))
            },
            label = "customization_category"
        ) { category ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (category) {
                    CustomizationCategory.EMOJI -> EmojiCategoryContent(
                        allEmojiOptions = allEmojiOptions,
                        ownedItems = ownedItems,
                        currentEmojiStatus = currentEmojiStatus,
                        onUse = { vm.useItem(it, ItemType.EMOJI_STATUS) }
                    )

                    CustomizationCategory.BUBBLE -> BubbleCategoryContent(
                        styles = styles,
                        ownedItems = ownedItems,
                        currentMessageStyle = currentMessageStyle,
                        onUse = { vm.useItem(it, ItemType.BUBBLE) }
                    )

                    CustomizationCategory.FONT -> FontCategoryContent(
                        ownedItems = ownedItems,
                        currentFont = currentFont,
                        onUse = { vm.useItem(it, ItemType.FONT) }
                    )

                    CustomizationCategory.EFFECT -> EffectCategoryContent(
                        ownedItems = ownedItems,
                        currentEffect = currentEffect,
                        onUse = { vm.useItem(it, ItemType.EFFECT) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    vm: AuthVM,
    onBack: () -> Unit
) {
    val currentMessageStyle = vm.userData["messageStyle"] ?: "default"

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text("Внешний вид", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column(modifier = Modifier.padding(16.dp)) {
                SectionTitle("ПРЕВЬЮ")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MayasTheme.Surface)
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChatBubble(
                        text = "Простись с прошлым и начни сначала.",
                        isMe = true,
                        isRead = true,
                        time = "0:05",
                        onLongClick = {},
                        messageStyle = if (currentMessageStyle == "default") null else currentMessageStyle
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Divider)

            CustomizationTabsAndContent(
                vm = vm,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCustomizeSheet(
    vm: AuthVM,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Оформление сообщений",
                color = MayasTheme.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(thickness = 0.5.dp, color = MayasTheme.Divider)

            CustomizationTabsAndContent(
                vm = vm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
