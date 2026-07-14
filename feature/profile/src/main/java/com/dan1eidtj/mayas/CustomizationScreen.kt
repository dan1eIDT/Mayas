package com.dan1eidtj.mayas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    vm: AuthVM,
    onBack: () -> Unit
) {
    val ownedItems = vm.ownedItems
    val currentMessageStyle = vm.userData["messageStyle"] ?: "default"
    val currentEmojiStatus = vm.userData["emojiStatus"] ?: ""

    // "default" не является предметом магазина, поэтому добавляется вручную,
    // остальные стили и их цвета берутся из ShopConstants — единого источника правды,
    // который используется и в магазине (ShopDialog), и здесь.
    val styles = listOf(
        Triple("default", "Стандартный", listOf(MayasTheme.Accent, MayasTheme.Accent))
    ) + ShopConstants.BUBBLE_STYLES.map { item ->
        Triple(item.id, item.name, ShopConstants.getStyleGradient(item.id))
    }

    val allEmojiOptions = ShopConstants.EMOJI_STATUSES.map { it.id }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // --- ПРЕВЬЮ СООБЩЕНИЯ ---
            item {
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
                        text = "Так будут выглядеть твои сообщения ✨",
                        isMe = true,
                        isRead = true,
                        time = "12:00",
                        onLongClick = {},
                        messageStyle = if (currentMessageStyle == "default") null else currentMessageStyle
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // --- ЭМОДЗИ СТАТУСЫ ---
            item {
                SectionTitle("ЭМОДЗИ-СТАТУС")
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
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MayasTheme.Surface)
                            .padding(12.dp)
                    ) {
                        // Сетка эмодзи (по 4 в ряд)
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
                                            .clickable { vm.useItem(emoji, ItemType.EMOJI_STATUS) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 26.sp)
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
                                // Пустые ячейки для выравнивания последнего ряда
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        TextButton(
                            onClick = { vm.useItem("", ItemType.EMOJI_STATUS) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Убрать статус", color = MayasTheme.ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // --- СТИЛИ СООБЩЕНИЙ ---
            item {
                SectionTitle("СТИЛЬ СООБЩЕНИЙ")
            }

            items(styles) { (id, name, gradient) ->
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
                        .clickable { if (isOwned) vm.useItem(id, ItemType.BUBBLE) }
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

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Все предметы можно приобрести в магазине",
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}