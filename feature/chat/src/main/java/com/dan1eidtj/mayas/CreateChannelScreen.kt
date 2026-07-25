package com.dan1eidtj.mayas.feature.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.feature.ChatVM
import kotlinx.coroutines.delay

private enum class UsernameStatus { EMPTY, CHECKING, AVAILABLE, TAKEN, INVALID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    onBack: () -> Unit,
    onChannelCreated: (String) -> Unit
) {
    val chatVM: ChatVM = viewModel()
    val context = LocalContext.current

    var channelTitle by remember { mutableStateOf("") }
    var channelDescription by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var usernameInput by remember { mutableStateOf("") }
    var usernameStatus by remember { mutableStateOf(UsernameStatus.EMPTY) }
    var isCreating by remember { mutableStateOf(false) }


    LaunchedEffect(usernameInput, isPublic) {
        if (!isPublic) { usernameStatus = UsernameStatus.EMPTY; return@LaunchedEffect }
        val clean = usernameInput.trim()
        if (clean.isEmpty()) { usernameStatus = UsernameStatus.EMPTY; return@LaunchedEffect }
        if (clean.length < 5 || !clean.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            usernameStatus = UsernameStatus.INVALID
            return@LaunchedEffect
        }
        usernameStatus = UsernameStatus.CHECKING
        delay(400)
        chatVM.checkChannelUsername(clean) { isAvailable ->
            usernameStatus = if (isAvailable) UsernameStatus.AVAILABLE else UsernameStatus.TAKEN
        }
    }

    val canSubmit = channelTitle.isNotBlank() && !isCreating &&
            (!isPublic || usernameStatus == UsernameStatus.AVAILABLE)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Surface),
                title = {
                    Text(
                        text = "Новый канал",
                        color = MayasTheme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (canSubmit) {
                FloatingActionButton(
                    containerColor = MayasTheme.GlowPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    onClick = {
                        isCreating = true
                        chatVM.createChannel(
                            title = channelTitle,
                            description = channelDescription,
                            isPublic = isPublic,
                            username = if (isPublic) usernameInput.trim() else null,
                            onSuccess = { newChatId ->
                                isCreating = false
                                onChannelCreated(newChatId)
                            },
                            onError = { msg ->
                                isCreating = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MayasTheme.Background)
                .verticalScroll(rememberScrollState())
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.GlowPurple)
                        .clickable { Toast.makeText(context, "Загрузка фото скоро!", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MayasTheme.Surface)
                            .border(2.dp, MayasTheme.Background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MayasTheme.GlowPurple, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    TextField(
                        value = channelTitle,
                        onValueChange = { if (it.length <= 32) channelTitle = it },
                        placeholder = { Text("Название канала", color = MayasTheme.TextSecondary) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        colors = tgTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${channelTitle.length}/32",
                        color = MayasTheme.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            TextField(
                value = channelDescription,
                onValueChange = { channelDescription = it },
                placeholder = { Text("Описание (необязательно)", color = MayasTheme.TextSecondary) },
                maxLines = 3,
                colors = tgTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Text(
                text = "Опишите тему канала — это увидят подписчики в профиле",
                color = MayasTheme.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            )

            SectionLabel("ТИП КАНАЛА")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MayasTheme.Surface)
            ) {
                PrivacyTypeRow(
                    icon = Icons.Outlined.Public,
                    title = "Публичный канал",
                    subtitle = "Виден в поиске, подписаться может любой",
                    selected = isPublic,
                    onClick = { isPublic = true }
                )
                HorizontalDivider(color = MayasTheme.TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(start = 60.dp))
                PrivacyTypeRow(
                    icon = Icons.Outlined.Lock,
                    title = "Приватный канал",
                    subtitle = "Присоединиться можно только по ссылке-приглашению",
                    selected = !isPublic,
                    onClick = { isPublic = false }
                )
            }

            if (isPublic) {
                SectionLabel("ССЫЛКА-ПРИГЛАШЕНИЕ")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MayasTheme.Surface)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("mayas.me/", color = MayasTheme.TextSecondary, fontSize = 16.sp)
                    TextField(
                        value = usernameInput,
                        onValueChange = { input ->
                            usernameInput = input.filter { it.isLetterOrDigit() || it == '_' }.take(32)
                        },
                        placeholder = { Text("username", color = MayasTheme.TextSecondary) },
                        singleLine = true,
                        colors = tgTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    when (usernameStatus) {
                        UsernameStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        UsernameStatus.AVAILABLE -> Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                        else -> {}
                    }
                }

                Text(
                    text = when (usernameStatus) {
                        UsernameStatus.EMPTY -> "Минимум 5 символов: латиница, цифры, _"
                        UsernameStatus.CHECKING -> "Проверяем..."
                        UsernameStatus.AVAILABLE -> "Ссылка свободна ✓"
                        UsernameStatus.TAKEN -> "Этот username уже занят"
                        UsernameStatus.INVALID -> "Минимум 5 символов: латиница, цифры, _"
                    },
                    color = when (usernameStatus) {
                        UsernameStatus.AVAILABLE -> Color(0xFF4CAF50)
                        UsernameStatus.TAKEN, UsernameStatus.INVALID -> MayasTheme.ErrorRed
                        else -> MayasTheme.TextSecondary
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MayasTheme.GlowPurple,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun PrivacyTypeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MayasTheme.GlowPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MayasTheme.GlowPurple, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = MayasTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.width(8.dp))

        RadioDot(selected)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) MayasTheme.GlowPurple else MayasTheme.TextSecondary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MayasTheme.GlowPurple)
            )
        }
    }
}

@Composable
private fun tgTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MayasTheme.GlowPurple,
    focusedTextColor = MayasTheme.TextPrimary,
    unfocusedTextColor = MayasTheme.TextPrimary
)