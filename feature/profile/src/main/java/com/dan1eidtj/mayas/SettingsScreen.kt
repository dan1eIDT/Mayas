package com.dan1eidtj.mayas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dan1eidtj.data.UserSession
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.feature.auth.AuthVM
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AuthVM,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToCustomization: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    var showChatSettings by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var accountToSwitch by remember { mutableStateOf<UserSession?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var switchPassword by remember { mutableStateOf("") }
    var switchError by remember { mutableStateOf<String?>(null) }

    if (showPasswordDialog && accountToSwitch != null) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                switchPassword = ""
                switchError = null
            },
            title = { Text("Переключить аккаунт", color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    Text("Введите пароль для ${accountToSwitch?.email}", color = MayasTheme.TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = switchPassword,
                        onValueChange = { switchPassword = it; switchError = null },
                        label = { Text("Пароль") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = switchError != null,
                        supportingText = { switchError?.let { Text(it, color = MayasTheme.ErrorRed) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MayasTheme.TextPrimary,
                            unfocusedTextColor = MayasTheme.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.switchAccount(accountToSwitch!!.email, switchPassword) {
                            showPasswordDialog = false
                            switchPassword = ""
                            onBack()
                        }
                    },
                    enabled = switchPassword.isNotBlank() && !vm.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
                ) {
                    if (vm.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Войти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Отмена", color = MayasTheme.TextSecondary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }


    val currentUserName = vm.userData["name"] ?: vm.userData["username"] ?: "Пользователь Mayas"
    val currentUserAvatar = vm.userData["avatarUrl"] ?: vm.userData["photoUrl"] ?: ""

    if (showAccountSheet) {
        AccountSwitchSheet(
            sessions = vm.activeSessions,
            currentUid = vm.user?.uid,
            onSelect = { session ->
                if (session.uid != vm.user?.uid) {
                    accountToSwitch = session
                    showAccountSheet = false
                    showPasswordDialog = true
                }
            },
            onDeleteSession = { uid ->
                vm.removeSession(uid)
            },
            onAddAccount = {
                showAccountSheet = false
                vm.addNewAccount(onNavigateToAuth)
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = MayasTheme.TextPrimary) },
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

            item {
                UserProfileHeader(
                    name = currentUserName,
                    email = vm.user?.email ?: "",
                    avatarUrl = currentUserAvatar,
                    isPremium = vm.isPremium,
                    onHeaderClick = { showAccountSheet = true }
                )
                Spacer(Modifier.height(24.dp))
            }


            item { SettingsSectionTitle("АККАУНТ") }

            item {
                val premiumSubtitle = if (vm.isPremium) {
                    val dateStr = vm.premiumUntil?.let {
                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        " до ${sdf.format(it.toDate())}"
                    } ?: ""
                    "Подписка активна$dateStr"
                } else {
                    "Получить эксклюзивные фишки"
                }

                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "MAYAS+",
                    subtitle = premiumSubtitle,
                    iconColor = MayasTheme.GlowGold,
                    onClick = onNavigateToPremium
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.People,
                    title = "Управление аккаунтами",
                    subtitle = "Активных профилей: ${vm.activeSessions.size}",
                    onClick = { showAccountSheet = true }
                )
            }


            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionTitle("НАСТРОЙКИ") }

            item {
                SettingsItem(
                    icon = Icons.Default.Brush,
                    title = "Кастомизация чата",
                    subtitle = "Фоны, пузыри, эффекты",
                    iconColor = MayasTheme.Accent,
                    onClick = onNavigateToCustomization
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Chat,
                    title = "Настройки чатов",
                    subtitle = "Фон, размер текста, стикеры",
                    onClick = { showChatSettings = !showChatSettings }
                )
            }

            item {
                AnimatedVisibility(
                    visible = showChatSettings,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ChatSettingsSubSection(vm)
                }
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Конфиденциальность",
                    subtitle = "Кто видит мой номер и статус",
                    onClick = { showPrivacy = !showPrivacy }
                )
            }

            item {
                AnimatedVisibility(
                    visible = showPrivacy,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PrivacySubSection(vm, onNavigateToPremium)
                }
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Безопасность",
                    subtitle = "Пароль, сессии, удаление аккаунта",
                    onClick = { showSecurity = !showSecurity }
                )
            }

            item {
                AnimatedVisibility(
                    visible = showSecurity,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SecuritySubSection(vm)
                }
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления и звуки",
                    subtitle = "Настроить пуши",
                    onClick = { /* ты чо , рано)) */ }
                )
            }


            item { Spacer(Modifier.height(24.dp)) }
            item { SettingsSectionTitle("ПРИЛОЖЕНИЕ") }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "О приложении",
                    subtitle = "Версия Mayas $versionName",
                    onClick = onNavigateToCredits
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Выйти",
                    subtitle = "Завершить текущую сессию",
                    iconColor = Color.Red,
                    onClick = {
                        vm.logout()
                        onBack()
                    }
                )
            }

            item {
                Spacer(Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Mayas for Android v$versionName",
                        color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileHeader(
    name: String,
    email: String,
    avatarUrl: String,
    isPremium: Boolean,
    onHeaderClick: () -> Unit
) {
    Surface(
        onClick = onHeaderClick,
        color = MayasTheme.Surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=$name" },
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = if (isPremium) MayasTheme.GlowGold else MayasTheme.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPremium) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.WorkspacePremium, null, tint = MayasTheme.GlowGold, modifier = Modifier.size(20.dp))
                    }
                }
                Text(text = email, color = MayasTheme.TextSecondary, fontSize = 14.sp)
            }
            Icon(Icons.Default.SwapHoriz, null, tint = MayasTheme.Accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitchSheet(
    sessions: List<UserSession>,
    currentUid: String?,
    onSelect: (UserSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MayasTheme.TextSecondary.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Ваши аккаунты",
                color = MayasTheme.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(sessions) { session: UserSession ->
                    val isCurrent = session.uid == currentUid
                    ListItem(
                        headlineContent = { Text(session.name, color = MayasTheme.TextPrimary) },
                        supportingContent = { Text(session.email, color = MayasTheme.TextSecondary) },
                        leadingContent = {
                            AsyncImage(
                                model = session.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${session.name}" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, null, tint = MayasTheme.Accent)
                                } else {
                                    IconButton(onClick = { onDeleteSession(session.uid) }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(session) }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Добавить аккаунт", color = MayasTheme.Accent) },
                        leadingContent = {
                            Icon(Icons.Default.Add, null, tint = MayasTheme.Accent)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onAddAccount() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = MayasTheme.TextSecondary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MayasTheme.Surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    color = MayasTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun ChatSettingsSubSection(vm: AuthVM) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text("Размер текста: ${vm.fontSize.toInt()}", color = MayasTheme.TextPrimary, fontSize = 14.sp)
        Slider(
            value = vm.fontSize,
            onValueChange = {
                vm.updateLocalSettings(
                    description = vm.userData["description"] ?: "",
                    theme = vm.appTheme,
                    fontSize = it
                )
            },
            valueRange = 12f..24f,
            colors = SliderDefaults.colors(
                thumbColor = MayasTheme.Accent,
                activeTrackColor = MayasTheme.Accent
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Тёмная тема", color = MayasTheme.TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = vm.appTheme == "dark",
                onCheckedChange = { isDark ->
                    vm.updateLocalSettings(
                        description = vm.userData["description"] ?: "",
                        theme = if (isDark) "dark" else "light",
                        fontSize = vm.fontSize
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.Accent)
            )
        }
    }
}

@Composable
fun PrivacySubSection(vm: AuthVM, onNavigateToPremium: () -> Unit) {
    var showSelectorFor by remember { mutableStateOf<String?>(null) }

    val privacySettings = listOf(
        Triple("privacy_phone", "Номер телефона", Icons.Default.Phone),
        Triple("privacy_last_seen", "Последняя активность", Icons.Default.AccessTime),
        Triple("privacy_photo", "Фотография профиля", Icons.Default.AccountCircle),
        Triple("privacy_groups", "Группы и каналы", Icons.Default.Group)
    )

    if (showSelectorFor != null) {
        val currentKey = showSelectorFor!!
        val currentValue = vm.userData[currentKey] ?: "all"
        val title = privacySettings.find { it.first == currentKey }?.second ?: "Настройка"

        PrivacySelectorDialog(
            title = title,
            currentValue = currentValue,
            onSelect = { newValue ->
                vm.updateUserData(currentKey, newValue)
                showSelectorFor = null
            },
            onDismiss = { showSelectorFor = null }
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !vm.isPremium) { onNavigateToPremium() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Режим невидимки", color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text("Скрывает статус 'в сети'", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = vm.isInvisible && vm.isPremium,
                onCheckedChange = { vm.updateInvisibleMode(it) },
                enabled = vm.isPremium,
                colors = SwitchDefaults.colors(checkedThumbColor = MayasTheme.GlowPurple)
            )
        }

        if (!vm.isPremium) {
            Text(
                "Доступно только в MAYAS+",
                color = MayasTheme.GlowGold,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        privacySettings.forEach { (key, label, icon) ->
            val value = vm.userData[key] ?: "all"
            val valueText = when (value) {
                "contacts" -> "Мои контакты"
                "none" -> "Никто"
                else -> "Все"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSelectorFor = key },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                    Text(valueText, color = MayasTheme.Accent, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary.copy(0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PrivacySelectorDialog(
    title: String,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MayasTheme.TextPrimary) },
        text = {
            Column {
                val options = listOf(
                    "all" to "Все",
                    "contacts" to "Мои контакты",
                    "none" to "Никто"
                )
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == value,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = MayasTheme.Accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = MayasTheme.TextPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface
    )
}

@Composable
fun SecuritySubSection(vm: AuthVM) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPassDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showEmailDialog) {
        var pass by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Сменить Email", color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Новый Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Текущий пароль") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MayasTheme.ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loading = true
                        vm.updateEmail(pass, newEmail) { err ->
                            loading = false
                            if (err == null) {
                                showEmailDialog = false
                                android.widget.Toast.makeText(context, "Подтвердите новый Email по ссылке", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                error = err
                            }
                        }
                    },
                    enabled = pass.isNotBlank() && newEmail.contains("@") && !loading
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Обновить")
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (showPassDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPassDialog = false },
            title = { Text("Сменить пароль", color = MayasTheme.TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text("Старый пароль") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Новый пароль") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MayasTheme.ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loading = true
                        vm.updatePassword(oldPass, newPass) { err ->
                            loading = false
                            if (err == null) {
                                showPassDialog = false
                                android.widget.Toast.makeText(context, "Пароль успешно изменен", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                error = err
                            }
                        }
                    },
                    enabled = oldPass.isNotBlank() && newPass.length >= 6 && !loading
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Сменить")
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить аккаунт?", color = MayasTheme.ErrorRed) },
            text = { Text("Это действие необратимо. Все ваши данные, чаты и медиа будут удалены навсегда.", color = MayasTheme.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteUserAccount(
                            onSuccess = { showDeleteDialog = false },
                            onError = {  }
                        )
                    }
                ) {
                    Text("Удалить", color = MayasTheme.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = MayasTheme.TextPrimary)
                }
            },
            containerColor = MayasTheme.Surface
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEmailDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Email, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Сменить почту", color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text("Изменить привязанный Email", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPassDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Password, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Сменить пароль", color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text("Обновить текущий пароль", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    vm.sendPasswordReset { error ->
                        val msg = error ?: "Ссылка для сброса пароля отправлена на почту"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LockReset, null, tint = MayasTheme.TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Сбросить пароль", color = MayasTheme.TextPrimary, fontSize = 14.sp)
                Text("Отправить ссылку на почту", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PersonOff, null, tint = MayasTheme.ErrorRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Удалить аккаунт", color = MayasTheme.ErrorRed, fontSize = 14.sp)
                Text("Полное удаление данных", color = MayasTheme.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MayasTheme.Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}