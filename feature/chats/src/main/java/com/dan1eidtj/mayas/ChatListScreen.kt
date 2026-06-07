@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import com.dan1eidtj.mayas.core.ui.theme.MayasAppTheme
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.chat.CreateGroupScreen // Импортируем экран создания группы
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ConnectionState { ONLINE, FIREBASE_OFFLINE, OFFLINE }

enum class ChatFolder(val displayName: String, val icon: ImageVector) {
    ALL("Все чаты", Icons.Default.ChatBubble),
    PINNED("Закрепленные", Icons.Default.PushPin),
    GROUPS("Группы", Icons.Default.Groups),
    CONTACTS("Контакты", Icons.Default.People)
}

fun getChatId(uid1: String, uid2: String): String {
    return listOf(uid1, uid2).sorted().joinToString("_")
}

// Хелпер для определения, находится ли пользователь реально в сети в данный момент
fun isUserOnline(userData: Map<String, Any?>?): Boolean {
    if (userData == null) return false

    // 1. Проверяем строковое поле активности
    val activity = userData["activity"] as? String
    if (activity == "в сети" || activity == "online") return true

    // 2. Проверяем Timestamp последнего входа (lastSeen)
    val lastSeen = userData["lastSeen"] as? com.google.firebase.Timestamp
    if (lastSeen != null) {
        // Берем по модулю (abs) на случай, если время на девайсе спешит относительно Firebase
        val diff = kotlin.math.abs(System.currentTimeMillis() - lastSeen.toDate().time)
        return diff < 60_000 // Считаем онлайн, если активность была менее 1 минуты (60 сек) назад
    }
    return false
}

@Composable
fun ChatListScreen(
    vm: AuthVM,
    onStartChat: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenUserSearch: () -> Unit,
    onDismissUserSearch: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val myUid = currentUser.uid
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // --- СОСТОЯНИЕ НАВИГАЦИИ НА ЭКРАН СОЗДАНИЯ ГРУППЫ ---
    var showCreateGroupScreen by remember { mutableStateOf(false) }

    // --- ОСТАЛЬНОЕ СОСТОЯНИЕ ---
    var selectedChats by remember { mutableStateOf(setOf<String>()) }
    var connectionState by remember { mutableStateOf(ConnectionState.ONLINE) }
    var connectionText by remember { mutableStateOf("...") }
    var chats by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showUserSearchDialog by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf(ChatFolder.ALL) }

    val userCache = remember { mutableStateMapOf<String, Map<String, Any?>>() }
    var myProfileData by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }

    // --- ЭКРАН СОЗДАНИЯ ГРУППЫ ---
    if (showCreateGroupScreen) {
        CreateGroupScreen(
            onBack = { showCreateGroupScreen = false },
            onGroupCreated = { newChatId ->
                showCreateGroupScreen = false
                onStartChat(newChatId) // Сразу открываем созданную группу
            }
        )
    } else {
        // --- АНИМАЦИИ ---
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val dotCount by infiniteTransition.animateValue(
            initialValue = 1,
            targetValue = 4,
            typeConverter = Int.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Restart
            ),
            label = "dots"
        )
        val dots = ".".repeat(dotCount)

        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        val unreadGlowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "unreadGlowAlpha"
        )

        // --- МОНИТОРИНГ СОЕДИНЕНИЯ ---
        LaunchedEffect(dots) {
            while (isActive) {
                val internetOk = checkInternet(context)
                var firebaseOk = false

                if (internetOk) {
                    runCatching {
                        vm.db.collection("system").document("ping").get().await()
                        firebaseOk = true
                    }.onFailure {
                        firebaseOk = false
                    }
                }

                when {
                    !internetOk -> {
                        connectionState = ConnectionState.OFFLINE
                        connectionText = "Жди инет$dots"
                    }

                    !firebaseOk -> {
                        connectionState = ConnectionState.FIREBASE_OFFLINE
                        connectionText = "База приём$dots"
                    }

                    else -> {
                        connectionState = ConnectionState.ONLINE
                        connectionText = "в сети"
                    }
                }
                delay(4000)
            }
        }

        val glowColor by animateColorAsState(
            targetValue = when (connectionState) {
                ConnectionState.ONLINE -> MayasTheme.GlowGreen
                ConnectionState.FIREBASE_OFFLINE -> Color(0xFFBEFF26)
                ConnectionState.OFFLINE -> Color(0xFFFF4D4D)
            },
            animationSpec = tween(500), label = "glow"
        )

        // --- ЗАГРУЗКА ДАННЫХ СВОЕГО ПРОФИЛЯ ---
        LaunchedEffect(myUid) {
            vm.db.collection("users").document(myUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        myProfileData = mapOf(
                            "name" to (doc.getString("name") ?: doc.getString("username") ?: "Я"),
                            "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                            "profileIcon" to (doc.getString("profileIcon") ?: "ghost"),
                            "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: false),
                            "activity" to (doc.getString("activity") ?: "в сети")
                        )
                    }
                }
        }

        // --- ЗАГРУЗКА ЧАТОВ И КЭШИРОВАНИЕ СОБЕСЕДНИКОВ ---
        DisposableEffect(myUid) {
            var listener: ListenerRegistration? = null

            listener = vm.db.collection("chats")
                .whereArrayContains("participants", myUid)
                .addSnapshotListener { snapshot, error ->
                    isInitialLoading = false
                    if (error != null || snapshot == null) return@addSnapshotListener

                    chats = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                        data["chatId"] = doc.id
                        data
                    }

                    chats.flatMap { data ->
                        (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    }.filter { id -> id != myUid && !userCache.containsKey(id) }
                        .distinct()
                        .forEach { partnerUid ->
                            vm.db.collection("users").document(partnerUid).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        userCache[partnerUid] = mapOf(
                                            "name" to (doc.getString("name") ?: doc.getString("username") ?: "Аноним"),
                                            "emoji" to (doc.getString("emojiStatus") ?: ""),
                                            "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                                            "profileIcon" to (doc.getString("profileIcon") ?: "ghost"),
                                            "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: false),
                                            "profileGlow" to (doc.getString("profileGlow") ?: "purple"),
                                            "activity" to (doc.getString("activity") ?: ""),
                                            "lastSeen" to doc.getTimestamp("lastSeen") // <-- Сохраняем таймстамп активности в кэш!
                                        )
                                    }
                                }
                        }
                }

            onDispose {
                listener?.remove()
            }
        }

        // --- СОРТИРОВКА И ФИЛЬТРАЦИЯ ПО ПАПКАМ И ПОИСКУ ---
        val filteredChats = remember(chats, searchQuery, userCache, selectedFolder) {
            chats.filter { chat ->
                val isGroup = chat["isGroup"] as? Boolean ?: false

                // Определяем имя для фильтра поиска
                val name = if (isGroup) {
                    chat["groupName"] as? String ?: ""
                } else {
                    val participants = chat["participants"] as? List<*> ?: emptyList<Any>()
                    val partnerUid = participants.filterIsInstance<String>().firstOrNull { it != myUid }
                    userCache[partnerUid]?.get("name") as? String ?: ""
                }

                val matchesSearch = name.contains(searchQuery, ignoreCase = true)
                if (!matchesSearch) return@filter false

                val isPinned = chat["pinned_$myUid"] as? Boolean ?: false

                when (selectedFolder) {
                    ChatFolder.ALL -> true
                    ChatFolder.PINNED -> isPinned
                    ChatFolder.GROUPS -> isGroup
                    ChatFolder.CONTACTS -> !isGroup
                }
            }.sortedWith(
                compareByDescending<Map<String, Any>> { (it["pinned_$myUid"] as? Boolean) ?: false }
                    .thenByDescending {
                        (it["updatedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L
                    }
            )
        }

        // --- ВЫДВИЖНОЙ DRAWER (БОКОВАЯ ПАНЕЛЬ) ---
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MayasTheme.Background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Навигация",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MayasTheme.TextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- ДОБАВЛЕНА КНОПКА В БОКОВОМ МЕНЮ ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MayasTheme.GlowPurple.copy(alpha = 0.08f))
                                .combinedClickable {
                                    coroutineScope.launch { drawerState.close() }
                                    showCreateGroupScreen = true
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = MayasTheme.GlowPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Создать группу",
                                color = MayasTheme.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ПАПКИ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ChatFolder.values().forEach { folder ->
                            val isSelected = selectedFolder == folder
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MayasTheme.GlowPurple.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .combinedClickable {
                                        selectedFolder = folder
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = folder.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MayasTheme.GlowPurple else MayasTheme.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = folder.displayName,
                                    color = if (isSelected) MayasTheme.TextPrimary else MayasTheme.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                val count = when (folder) {
                                    ChatFolder.ALL -> chats.size
                                    ChatFolder.PINNED -> chats.count {
                                        it["pinned_$myUid"] as? Boolean ?: false
                                    }

                                    ChatFolder.GROUPS -> chats.count {
                                        it["isGroup"] as? Boolean ?: false
                                    }

                                    ChatFolder.CONTACTS -> chats.count {
                                        !(it["isGroup"] as? Boolean ?: false)
                                    }
                                }
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MayasTheme.GlowPurple.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            color = if (isSelected) MayasTheme.GlowPurple else MayasTheme.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable { onLogout() }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Выход",
                                tint = MayasTheme.ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Выйти из аккаунта",
                                color = MayasTheme.ErrorRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        ) {
            // --- ДОБАВЛЕН SCAFFOLD С ПЛАВАЮЩЕЙ КНОПКОЙ (FAB) ---
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showCreateGroupScreen = true },
                        containerColor = MayasTheme.GlowPurple,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Создать группу"
                        )
                    }
                },
                containerColor = MayasTheme.Background
            ) { paddingValues ->
                // НАЧАЛО ОСНОВНОГО СТОЛБЦА
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MayasTheme.Background)
                ) {
                    AnimatedContent(
                        targetState = selectedChats.isEmpty(),
                        label = "header"
                    ) { isNormalMode ->
                        if (isNormalMode) {
                            HeaderSection(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onAddFriendClick = {
                                    showUserSearchDialog = true; onOpenUserSearch()
                                },
                                onMenuClick = { coroutineScope.launch { drawerState.open() } }
                            )
                        } else {
                            HeaderSelection(
                                selectedCount = selectedChats.size,
                                onClearSelection = { selectedChats = emptySet() },
                                onTogglePin = {
                                    selectedChats.forEach { id ->
                                        val chatDoc = chats.firstOrNull { it["chatId"] == id }
                                        val isCurrentlyPinned =
                                            chatDoc?.get("pinned_$myUid") as? Boolean ?: false
                                        vm.db.collection("chats").document(id)
                                            .update("pinned_$myUid", !isCurrentlyPinned)
                                    }
                                    selectedChats = emptySet()
                                },
                                onDeleteChats = {
                                    selectedChats.forEach { id ->
                                        vm.db.collection("chats").document(id).delete()
                                    }
                                    selectedChats = emptySet()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedFolder.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MayasTheme.GlowPurple,
                            letterSpacing = 0.5.sp
                        )
                        if (selectedFolder != ChatFolder.ALL) {
                            TextButton(
                                onClick = { selectedFolder = ChatFolder.ALL },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Сбросить", color = MayasTheme.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isInitialLoading) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(5) { ShimmerChatItem() }
                            }
                        } else if (filteredChats.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Ничего не найдено" else "В этой папке нет чатов",
                                    color = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(
                                    filteredChats,
                                    key = { it["chatId"] ?: it.hashCode() }) { chat ->
                                    val isGroup = chat["isGroup"] as? Boolean ?: false
                                    val chatId = chat["chatId"] as? String ?: ""
                                    val isPinned = chat["pinned_$myUid"] as? Boolean ?: false

                                    if (isGroup) {
                                        // Логика для ГРУППЫ:
                                        val groupData = mapOf(
                                            "name" to (chat["groupName"] as? String
                                                ?: "Группа без названия"),
                                            "avatarUrl" to (chat["groupAvatarUrl"] as? String
                                                ?: ""),
                                            "profileIcon" to (chat["groupIcon"] as? String
                                                ?: "groups"),
                                            "useCustomAvatar" to (chat["useCustomAvatar"] as? Boolean
                                                ?: false),
                                            "profileGlow" to "purple"
                                        )

                                        ChatItemNew(
                                            userData = groupData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",
                                            unreadCount = (chat["unreadCount_$myUid"] as? Long
                                                ?: 0L).toInt(),
                                            updatedAt = chat["updatedAt"] as? com.google.firebase.Timestamp,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            unreadGlowAlpha = unreadGlowAlpha,
                                            isOnline = false, // Для групп статус сети не выводим
                                            onClick = {
                                                if (selectedChats.isNotEmpty()) {
                                                    selectedChats =
                                                        if (selectedChats.contains(chatId)) selectedChats - chatId else selectedChats + chatId
                                                } else {
                                                    onStartChat(chatId)
                                                }
                                            },
                                            onLongClick = { selectedChats = selectedChats + chatId }
                                        )
                                    } else {
                                        // Логика для ОБЫЧНОГО ЧАТА:
                                        val participants =
                                            chat["participants"] as? List<*> ?: emptyList<Any>()
                                        val partnerUid = participants.filterIsInstance<String>()
                                            .firstOrNull { it != myUid } ?: return@items
                                        val userData = userCache[partnerUid]
                                        val isOnline = isUserOnline(userData) // Проверяем статус в сети

                                        ChatItemNew(
                                            userData = userData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",
                                            unreadCount = (chat["unreadCount_$myUid"] as? Long
                                                ?: 0L).toInt(),
                                            updatedAt = chat["updatedAt"] as? com.google.firebase.Timestamp,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            unreadGlowAlpha = unreadGlowAlpha,
                                            isOnline = isOnline, // Передаем вычисленный статус
                                            onClick = {
                                                if (selectedChats.isNotEmpty()) {
                                                    selectedChats =
                                                        if (selectedChats.contains(chatId)) selectedChats - chatId else selectedChats + chatId
                                                } else {
                                                    onStartChat(chatId)
                                                }
                                            },
                                            onLongClick = {
                                                selectedChats = selectedChats + chatId
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Нижняя панель аккуратно стоит в самом низу вертикального Column
                    BottomProfileBar(
                        myUid = myUid,
                        myProfileData = myProfileData,
                        connectionText = connectionText,
                        glowColor = glowColor,
                        pulseScale = pulseScale,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings
                    )
                } // Конец основного столбца Column
            }
        }
    }
    // --- ДИАЛОГ ПОИСКА ПОЛЬЗОВАТЕЛЕЙ ---
    if (showUserSearchDialog) {
        UserSearchDialog(
            searchInput = searchInput,
            searchError = searchError,
            onInputChange = { searchInput = it; searchError = null },
            onDismiss = { showUserSearchDialog = false; searchInput = ""; searchError = null; onDismissUserSearch() },
            onConfirm = {
                val username = searchInput.removePrefix("@").lowercase().trim()

                if (username.isEmpty()) {
                    searchError = "Введите никнейм"
                } else {
                    vm.resolveUserByUsername(username) { targetUid ->
                        if (targetUid != null) {
                            if (targetUid == myUid) {
                                searchError = "Нельзя создать чат с самим собой"
                            } else {
                                onStartChat(getChatId(myUid, targetUid))
                                showUserSearchDialog = false
                                searchInput = ""
                            }
                        } else {
                            searchError = "Пользователь не найден"
                        }
                    }
                }
            }
        )
    }
}

// ================= КОМПОНЕНТЫ ИНТЕРФЕЙСА =================

@Composable
fun HeaderSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddFriendClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопка открытия Drawer
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MayasTheme.Surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Открыть меню",
                        tint = MayasTheme.TextPrimary
                    )
                }

                Text(
                    text = "маяс.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MayasTheme.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            // Добавить друзей
            IconButton(
                onClick = onAddFriendClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MayasTheme.Surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Добавить друзей",
                    tint = MayasTheme.GlowPurple
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Строка поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Поиск по чатам...", color = MayasTheme.TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = MayasTheme.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MayasTheme.Surface.copy(alpha = 0.4f),
                unfocusedContainerColor = MayasTheme.Surface.copy(alpha = 0.2f),
                focusedBorderColor = MayasTheme.GlowPurple.copy(alpha = 0.35f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
            )
        )
    }
}

@Composable
fun HeaderSelection(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteChats: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MayasTheme.Surface.copy(alpha = 0.2f))
            .border(1.dp, MayasTheme.GlowPurple.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(Icons.Default.Close, null, tint = MayasTheme.TextPrimary)
        }
        Text(
            text = "Выбрано: $selectedCount",
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MayasTheme.TextPrimary
        )
        IconButton(onClick = onTogglePin) {
            Icon(Icons.Default.PushPin, null, tint = MayasTheme.TextPrimary)
        }
        IconButton(onClick = onDeleteChats) {
            Icon(Icons.Default.Delete, null, tint = MayasTheme.ErrorRed)
        }
    }
}

@Composable
fun ChatItemNew(
    userData: Map<String, Any?>?,
    lastMsg: String,
    unreadCount: Int,
    updatedAt: com.google.firebase.Timestamp?,
    isSelected: Boolean,
    isPinned: Boolean,
    unreadGlowAlpha: Float,
    isOnline: Boolean = false, // Добавлен флаг для отображения статуса сети
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val avatarGlow = when (userData?.get("profileGlow")) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "red" -> MayasTheme.GlowRed
        else -> MayasTheme.GlowPurple
    }

    val itemBgColor = if (isSelected) {
        MayasTheme.Accent.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    // Рамка аватара пульсирует неоновым светом, если есть непрочитанные сообщения
    val glowBorderBrush = if (unreadCount > 0) {
        Brush.sweepGradient(
            listOf(
                avatarGlow,
                avatarGlow.copy(alpha = unreadGlowAlpha),
                avatarGlow
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                avatarGlow.copy(alpha = 0.35f),
                avatarGlow.copy(alpha = 0.10f)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(itemBgColor)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // АВАТАРКА ПОЛЬЗОВАТЕЛЯ
        Box(modifier = Modifier.size(54.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(avatarGlow.copy(alpha = 0.12f))
                    .border(
                        width = if (unreadCount > 0) 2.dp else 1.dp,
                        brush = glowBorderBrush,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val useAvatar = userData?.get("useCustomAvatar") as? Boolean ?: false
                val url = userData?.get("avatarUrl") as? String ?: ""
                if (useAvatar && url.isNotEmpty()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    ProfileIcon(userData?.get("profileIcon") as? String ?: "ghost")
                }
            }

            // Онлайн статус отображается только если человек РЕАЛЬНО в сети
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.GlowGreen)
                        .align(Alignment.BottomEnd)
                        .border(1.5.dp, MayasTheme.Background, CircleShape)
                )
            }
        }

        // ИНФОРМАЦИЯ О СООБЩЕНИИ
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${userData?.get("name") ?: "..."} ${userData?.get("emoji") ?: ""}".trim(),
                        color = MayasTheme.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закреплен",
                            tint = MayasTheme.GlowPurple,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                updatedAt?.let {
                    Text(
                        text = formatTime(it),
                        color = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMsg.ifEmpty { "Нет сообщений" },
                    modifier = Modifier.weight(1f),
                    color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MayasTheme.Accent)
                            .padding(horizontal = 4.dp, vertical = 1.dp)

                    ) {
                        Text(
                            text = "$unreadCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomProfileBar(
    myUid: String,
    myProfileData: Map<String, Any?>,
    connectionText: String,
    glowColor: Color,
    pulseScale: Float,
    onOpenProfile: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = MayasTheme.Surface.copy(alpha = 0.25f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .combinedClickable { onOpenProfile(myUid) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val useAvatar = myProfileData["useCustomAvatar"] as? Boolean ?: false
                    val url = myProfileData["avatarUrl"] as? String ?: ""
                    if (useAvatar && url.isNotEmpty()) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        ProfileIcon(myProfileData["profileIcon"] as? String ?: "ghost")
                    }
                }

                // Индикатор состояния сети
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(glowColor)
                        .align(Alignment.BottomEnd)
                        .border(1.5.dp, MayasTheme.Background, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable { onOpenProfile(myUid) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = myProfileData["name"] as? String ?: "Загрузка...",
                        color = MayasTheme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Открыть профиль",
                        tint = MayasTheme.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = connectionText,
                    color = glowColor.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Настройки уведомлений
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Уведомления",
                        tint = MayasTheme.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerChatItem() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color(0xFF1E1E26),
        Color(0xFF2C2C38),
        Color(0xFF1E1E26),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(brush)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun UserSearchDialog(
    searchInput: String,
    searchError: String?,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Новый чат",
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Введите юзернейм собеседника без символа @",
                    color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = onInputChange,
                    placeholder = { Text("username", color = MayasTheme.TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = searchError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                        focusedBorderColor = MayasTheme.GlowPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                if (searchError != null) {
                    Text(
                        text = searchError,
                        color = MayasTheme.ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MayasTheme.GlowPurple,
                    contentColor = Color.White
                )
            ) {
                Text("Найти", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MayasTheme.TextSecondary.copy(alpha = 0.8f))
            }
        },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    )
}

fun formatTime(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = java.util.Calendar.getInstance().time
    val diff = now.time - date.time
    return when {
        diff < 24 * 60 * 60 * 1000 ->
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
        diff < 7 * 24 * 60 * 60 * 1000 ->
            java.text.SimpleDateFormat("EEE", java.util.Locale("ru")).format(date)
        else ->
            java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault()).format(date)
    }
}

fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = cm.activeNetwork ?: return false
    val actNw = cm.getNetworkCapabilities(nw) ?: return false
    return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}