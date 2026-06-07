@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dan1eidtj.mayas.*
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.auth.AuthVM.Screen.Chat.create
import com.dan1eidtj.mayas.feature.auth.Screen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    targetId: String?,
    isGroup: Boolean,
    vm: AuthVM,
    onBack: () -> Unit,
    onNavigate: (String, Boolean) -> Unit,
    onNavigateToCredits: () -> Unit
) {
    val currentMyUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val finalId = targetId ?: currentMyUid
    val isMyProfile = !isGroup && (targetId == null || targetId == currentMyUid)

    var name by remember { mutableStateOf("Загрузка...") }
    var username by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    var profileIcon by remember { mutableStateOf("ghost") }
    var profileGlow by remember { mutableStateOf("purple") }
    var useCustomAvatar by remember { mutableStateOf(true) }
    var desc by remember { mutableStateOf("") }
    var emojiStatus by remember { mutableStateOf("") }
    var isOnline by remember { mutableStateOf(false) }
    var lastSeenText by remember { mutableStateOf("был(а) недавно") }

    // Состояния для групп
    var membersUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var groupMembers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var adminId by remember { mutableStateOf("") }

    val isGroupAdmin = isGroup && adminId == currentMyUid
    val canEdit = isMyProfile || isGroupAdmin

    var isEditing by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val glowColor = when (profileGlow) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "gold" -> MayasTheme.GlowGold
        "red" -> MayasTheme.GlowRed
        else -> MayasTheme.GlowPurple
    }

    val iconsList = listOf(
        "face", "ghost", "star", "favorite", "bolt", "flash", "moon", "music",
        "game", "code", "terminal", "fire", "robot", "eye", "heartbreak", "skull"
    )

    // Real-time подписка
    DisposableEffect(finalId, isGroup) {
        if (finalId.isEmpty()) return@DisposableEffect onDispose {}
        val collectionPath = if (isGroup) "groups" else "users"
        val listener = vm.db.collection(collectionPath).document(finalId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    profileIcon = doc.getString("profileIcon") ?: "face"
                    profileGlow = doc.getString("profileGlow") ?: "purple"
                    useCustomAvatar = doc.getBoolean("useCustomAvatar") ?: true
                    name = doc.getString("name") ?: (if (isGroup) "Группа" else "Пользователь")
                    avatar = doc.getString("avatarUrl") ?: ""
                    desc = doc.getString("description") ?: ""

                    if (isGroup) {
                        @Suppress("UNCHECKED_CAST")
                        membersUids = doc.get("members") as? List<String> ?: emptyList()
                        adminId = doc.getString("adminId") ?: doc.getString("ownerId") ?: ""
                    } else {
                        username = doc.getString("username") ?: ""
                        isOnline = doc.getBoolean("isOnline") ?: false
                        emojiStatus = doc.getString("emojiStatus") ?: ""
                    }
                }
            }
        onDispose { listener.remove() }
    }

    // Загрузка участников группы
    LaunchedEffect(membersUids) {
        if (!isGroup || membersUids.isEmpty()) {
            groupMembers = emptyList()
            return@LaunchedEffect
        }
        val tempMembers = mutableListOf<Map<String, Any>>()
        var completedCount = 0

        membersUids.forEach { uid ->
            vm.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        tempMembers.add(
                            mapOf(
                                "uid" to uid,
                                "name" to (doc.getString("name") ?: "Пользователь"),
                                "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                                "profileIcon" to (doc.getString("profileIcon") ?: "face"),
                                "profileGlow" to (doc.getString("profileGlow") ?: "purple"),
                                "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: true)
                            )
                        )
                    }
                    completedCount++
                    if (completedCount == membersUids.size) {
                        groupMembers = tempMembers.toList()
                    }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == membersUids.size) {
                        groupMembers = tempMembers.toList()
                    }
                }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            vm.uploadAvatar(it, onSuccess = { url ->
                useCustomAvatar = true
                avatar = url
                val collectionPath = if (isGroup) "groups" else "users"
                vm.db.collection(collectionPath).document(finalId).update(
                    mapOf("avatarUrl" to url, "useCustomAvatar" to true)
                )
            }, onError = {})
        }
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    val collectionPath = if (isGroup) "groups" else "users"
                                    val updateData = if (isGroup) {
                                        mapOf(
                                            "name" to name,
                                            "description" to desc,
                                            "profileIcon" to profileIcon,
                                            "profileGlow" to profileGlow,
                                            "useCustomAvatar" to useCustomAvatar
                                        )
                                    } else {
                                        mapOf(
                                            "name" to name,
                                            "username" to username,
                                            "description" to desc,
                                            "emojiStatus" to emojiStatus,
                                            "profileIcon" to profileIcon,
                                            "profileGlow" to profileGlow,
                                            "useCustomAvatar" to useCustomAvatar
                                        )
                                    }
                                    vm.db.collection(collectionPath).document(finalId).update(updateData)
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = padding.calculateBottomPadding() + 40.dp
            )
        ) {
            // --- HEADER ---
            item {
                Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(glowColor.copy(alpha = 0.4f), glowColor.copy(alpha = 0.08f), Color.Transparent)
                            )
                        )
                    )

                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Avatar Box
                        Box(modifier = Modifier.size(116.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.5.dp, glowColor, CircleShape)
                                    .clickable(enabled = isEditing) { showImagePicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (useCustomAvatar && avatar.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(avatar).build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.8f), Color(0xFF1E1F22)))
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) { ProfileIcon(profileIcon) }
                                }
                                if (isEditing) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            if (!isGroup) {
                                Box(
                                    modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                                        .border(3.5.dp, MayasTheme.Background, CircleShape)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF23A55A) else Color(0xFF80848E))
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MayasTheme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (emojiStatus.isNotBlank() && !isGroup) {
                                Spacer(Modifier.width(6.dp))
                                Text(emojiStatus, fontSize = 20.sp)
                            }
                        }

                        if (!isGroup) {
                            Text(
                                text = if (isOnline) "в сети" else lastSeenText,
                                fontSize = 14.sp,
                                color = if (isOnline) Color(0xFF23A55A) else MayasTheme.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "${groupMembers.size} участников",
                                fontSize = 14.sp,
                                color = MayasTheme.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- КОНТЕНТ (РАЗДЕЛЕНИЕ) ---
            if (isGroup) {
                // Раздел для групп
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        QuickActionRow {
                            QuickActionItem(icon = Icons.Default.Notifications, label = if (notificationsEnabled) "Звук вкл." else "Без звука") { notificationsEnabled = !notificationsEnabled }
                            if (canEdit) QuickActionItem(icon = Icons.Default.Settings, label = "Настройки") { isEditing = true }
                        }

                        Spacer(Modifier.height(16.dp))

                        InfoSection {
                            if (isEditing) {
                                EditField("Название группы", name) { name = it }
                                EditField("Описание группы", desc) { desc = it }
                            } else {
                                TelegramInfoRow(Icons.Default.Info, desc.ifBlank { "Описание отсутствует" }, "Описание")
                            }
                        }
                    }
                }

                if (!isEditing) {
                    item {
                        SectionTitle("УЧАСТНИКИ")
                        MembersList(groupMembers, adminId) { uid ->
                            onNavigate(uid, false)
                        }
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        ExitButton(isOwner = adminId == currentMyUid) {
                            // Логика выхода
                            vm.db.collection("groups").document(finalId).get().addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val currentMembers = doc.get("members") as? List<String> ?: emptyList()
                                    val updatedMembers = currentMembers.filter { it != currentMyUid }
                                    if (updatedMembers.isEmpty()) {
                                        vm.db.collection("groups").document(finalId).delete().addOnSuccessListener { onBack() }
                                    } else {
                                        val updates = mutableMapOf<String, Any>("members" to updatedMembers)
                                        if (adminId == currentMyUid) updates["adminId"] = updatedMembers.first()
                                        vm.db.collection("groups").document(finalId).update(updates).addOnSuccessListener { onBack() }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Раздел для пользователей
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        QuickActionRow {
                            if (!isMyProfile) {
                                QuickActionItem(icon = Icons.AutoMirrored.Filled.Send, label = "Написать") {
                                    onNavigate(create(Screen.getChatId(currentMyUid, finalId)),
                                        false
                                    )
                                }
                                QuickActionItem(icon = Icons.Default.Call, label = "Звонок") { }
                            }
                            QuickActionItem(icon = Icons.Default.Notifications, label = if (notificationsEnabled) "Звук вкл." else "Без звука") {
                                notificationsEnabled = !notificationsEnabled
                            }
                            if (isMyProfile) QuickActionItem(icon = Icons.Default.Settings, label = "Настройки") { isEditing = true }
                        }

                        Spacer(Modifier.height(16.dp))

                        InfoSection {
                            if (isEditing) {
                                EditField("Имя", name) { name = it }
                                EditField("Имя пользователя", username) { username = it }
                                EditField("О себе", desc) { desc = it }
                                EditField("Emoji-статус", emojiStatus) { emojiStatus = it.take(2) }
                            } else {
                                TelegramInfoRow(Icons.Default.Info, desc.ifBlank { "Информация отсутствует" }, "О себе")
                                HorizontalDivider(color = MayasTheme.Background.copy(alpha = 0.5f), thickness = 0.8.dp, modifier = Modifier.padding(start = 56.dp))
                                TelegramInfoRow(Icons.Default.Person, if (username.isBlank()) "@id$finalId" else "@$username", "Имя пользователя")
                            }
                        }

                        if (isMyProfile) {
                            Spacer(Modifier.height(24.dp))
                            SectionTitle("ПРИЛОЖЕНИЕ")
                            InfoSection {
                                TelegramInfoRow(
                                    icon = Icons.Default.Info,
                                    title = "О приложении",
                                    subtitle = "Mayas"
                                ) {
                                    onNavigateToCredits()
                                }
                                HorizontalDivider(color = MayasTheme.Background.copy(alpha = 0.5f), thickness = 0.8.dp, modifier = Modifier.padding(start = 56.dp))
                                TelegramInfoRow(
                                    icon = Icons.Default.DoorBack,
                                    title = "Выйти из аккаунта",
                                    subtitle = "Завершить сессию"
                                ) {
                                    vm.logout()
                                    onBack()
                                }
                            }
                        }
                    }
                }
            }

            // --- ВЫБОР ЦВЕТА (ТОЛЬКО ПРИ РЕДАКТИРОВАНИИ) ---
            if (isEditing) {
                item {
                    SectionTitle("ЦВЕТОВОЕ ОФОРМЛЕНИЕ")
                    ColorPicker(profileGlow) { profileGlow = it }
                }
            }
        }
    }

    // Диалоги выбора аватара
    if (showImagePicker) {
        ImagePickerDialog(
            isGroup = isGroup,
            onDismiss = { showImagePicker = false },
            onGallery = { launcher.launch("image/*"); showImagePicker = false },
            onSystemIcon = { showIconPicker = true; showImagePicker = false },
            onDelete = { avatar = ""; useCustomAvatar = false; showImagePicker = false }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(iconsList, profileIcon, onDismiss = { showIconPicker = false }) {
            profileIcon = it
            useCustomAvatar = false
            showIconPicker = false
        }
    }
}

// --- ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ (ВНУТРЕННИЕ) ---

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        color = MayasTheme.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun QuickActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        content = content
    )
}

@Composable
fun InfoSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MayasTheme.CardRadius)
            .background(MayasTheme.Surface),
        content = content
    )
}

@Composable
fun ColorPicker(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(listOf("purple", "pink", "blue", "green", "gold", "red")) { colorName ->
            val color = when (colorName) {
                "pink" -> MayasTheme.GlowPink
                "blue" -> MayasTheme.GlowBlue
                "green" -> MayasTheme.GlowGreen
                "gold" -> MayasTheme.GlowGold
                "red" -> MayasTheme.GlowRed
                else -> MayasTheme.GlowPurple
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(colorName) }
                    .border(if (selected == colorName) 3.dp else 0.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected == colorName) Icon(Icons.Default.Check, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun MembersList(members: List<Map<String, Any>>, adminId: String, onMemberClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(MayasTheme.CardRadius)
            .background(MayasTheme.Surface)
    ) {
        members.forEachIndexed { index, member ->
            val uid = member["uid"] as String
            val glow = when (member["profileGlow"]) {
                "pink" -> MayasTheme.GlowPink
                "blue" -> MayasTheme.GlowBlue
                "green" -> MayasTheme.GlowGreen
                "gold" -> MayasTheme.GlowGold
                "red" -> MayasTheme.GlowRed
                else -> MayasTheme.GlowPurple
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onMemberClick(uid) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, glow, CircleShape)) {
                    if (member["useCustomAvatar"] == true && (member["avatarUrl"] as String).isNotBlank()) {
                        AsyncImage(model = member["avatarUrl"], contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(glow.copy(0.5f)), Alignment.Center) {
                            ProfileIcon(member["profileIcon"] as String)
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(member["name"] as String, color = MayasTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (uid == adminId) Text("Создатель", color = MayasTheme.Accent, fontSize = 12.sp)
                }
            }
            if (index < members.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MayasTheme.Background.copy(0.5f))
        }
    }
}

@Composable
fun ExitButton(isOwner: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed.copy(0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(if (isOwner) "Удалить группу" else "Выйти из группы", color = MayasTheme.ErrorRed, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ImagePickerDialog(isGroup: Boolean, onDismiss: () -> Unit, onGallery: () -> Unit, onSystemIcon: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        title = { Text(if (isGroup) "Фото группы" else "Фото профиля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGallery, modifier = Modifier.fillMaxWidth()) { Text("Загрузить фото") }
                OutlinedButton(onClick = onSystemIcon, modifier = Modifier.fillMaxWidth()) { Text("Выбрать иконку") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDelete) { Text("Удалить", color = MayasTheme.ErrorRed) } }
    )
}

@Composable
fun IconPickerDialog(icons: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        title = { Text("Выберите иконку") },
        text = {
            Box(modifier = Modifier.height(260.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(icons) { icon ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected == icon) MayasTheme.Accent.copy(0.1f) else MayasTheme.Background)
                                .clickable { onSelect(icon) },
                            contentAlignment = Alignment.Center
                        ) { ProfileIcon(icon) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, tint: Color = MayasTheme.Accent, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(8.dp).width(68.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = MayasTheme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TelegramInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MayasTheme.TextSecondary.copy(0.7f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = MayasTheme.TextPrimary, fontSize = 15.sp)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MayasTheme.Accent)
    )
}