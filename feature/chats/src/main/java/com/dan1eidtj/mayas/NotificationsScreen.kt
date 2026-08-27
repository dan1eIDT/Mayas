@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit = {},
    onOpenPromoAction: (String) -> Unit = {}
) {
    val vm: NotificationsViewModel = viewModel()
    val notifications by vm.notifications.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Уведомления", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MayasTheme.TextPrimary)
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        IconButton(onClick = { vm.markAllAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Прочитать всё", tint = MayasTheme.TextSecondary)
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { vm.clearAll() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить всё", tint = MayasTheme.TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MayasTheme.Background,
                    titleContentColor = MayasTheme.TextPrimary
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MayasTheme.Background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        color = MayasTheme.GlowPurple
                    )
                }
                notifications.isEmpty() -> {
                    EmptyNotificationsState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationRow(
                                item = item,
                                onClick = {
                                    if (!item.isRead) vm.markAsRead(item.id)
                                    when (item.type) {
                                        NotificationType.MISSED_CALL -> {
                                            val chatId = item.payload["chatId"] as? String
                                            if (!chatId.isNullOrBlank()) onOpenChat(chatId)
                                        }
                                        NotificationType.PROMO -> {
                                            val actionUrl = item.payload["actionUrl"] as? String
                                            if (!actionUrl.isNullOrBlank()) onOpenPromoAction(actionUrl)
                                        }
                                        NotificationType.SYSTEM -> {}
                                    }
                                },
                                onDelete = { vm.delete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = MayasTheme.TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Пока тихо", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Пропущенные звонки, системные штуки и акции\nбудут появляться здесь",
            color = MayasTheme.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconTint) = when (item.type) {
        NotificationType.MISSED_CALL -> {
            val callTypeRaw = (item.payload["callType"] as? String)?.uppercase() ?: ""
            val isVideo = callTypeRaw.contains("VIDEO")
            (if (isVideo) Icons.Default.VideocamOff else Icons.Default.PhoneMissed) to MayasTheme.ErrorRed
        }
        NotificationType.SYSTEM -> Icons.Default.Settings to MayasTheme.TextSecondary
        NotificationType.PROMO -> Icons.Default.Campaign to MayasTheme.GlowGold
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (!item.isRead) MayasTheme.GlowPurple.copy(alpha = 0.06f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MayasTheme.TextPrimary,
                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1
            )
            if (item.body.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.body,
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatNotificationTime(item.createdAtMillis),
                color = MayasTheme.TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        if (!item.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.GlowPurple)
            )
        }
    }
}

private fun formatNotificationTime(millis: Long): String {
    if (millis <= 0L) return ""
    val date = Date(millis)
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DATE) - then.get(Calendar.DATE) == 1 &&
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> "вчера, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
            SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(date)
        }
        else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}