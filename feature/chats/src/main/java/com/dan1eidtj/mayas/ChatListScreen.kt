@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import com.dan1eidtj.mayas.core_ui.Screen
import com.dan1eidtj.mayas.core.ui.theme.MayasAppTheme
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.ui.components.ProfileIcon
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.feature.TypingIndicator
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.chat.CreateGroupScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Serializable
data class AppVersion(
    val latestVersion: String,
    val updateUrl: String,
    val changelog: String? = null
)


enum class ConnectionState { ONLINE, OFFLINE }

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
// УДАЛЕНО: перенесено в UserUtils.kt

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
    // --- OFFLINE-FIRST VM ---
    val chatListVm: ChatListViewModel = viewModel()
    // UI читает чаты из Room — он обновляется VM при каждом снапшоте Firestore
    val roomChats by chatListVm.chats.collectAsState()
    val syncState by chatListVm.syncState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // FIX: httpClient внутри composable — закрывается при выходе с экрана
    val httpClient = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }
    DisposableEffect(Unit) { onDispose { httpClient.close() } }

    // --- ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ ---
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var bannerDismissed by rememberSaveable { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }

    val currentVersionName = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } ?: ""
        } catch (e: Exception) { "" }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val versionInfo: AppVersion = httpClient.get("https://raw.githubusercontent.com/dan1eIDT/Mayas/master/version.json").body()
                withContext(Dispatchers.Main) {
                    updateUrl = versionInfo.updateUrl
                    if (versionInfo.latestVersion.isNotEmpty() && versionInfo.latestVersion != currentVersionName) {
                        isUpdateAvailable = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                vm.db.collection("system").document("config").get().addOnSuccessListener { doc ->
                    val latestVersion = doc.getString("latestVersion") ?: ""
                    updateUrl = doc.getString("updateUrl") ?: ""
                    if (latestVersion.isNotEmpty() && latestVersion != currentVersionName) {
                        isUpdateAvailable = true
                    }
                }
            }
        }
    }

    // --- СОСТОЯНИЕ НАВИГАЦИИ НА ЭКРАН СОЗДАНИЯ ГРУППЫ ---
    var showCreateGroupScreen by remember { mutableStateOf(false) }

    // --- ОСТАЛЬНОЕ СОСТОЯНИЕ ---
    var selectedChats by remember { mutableStateOf(setOf<String>()) }
    var connectionState by remember { mutableStateOf(ConnectionState.ONLINE) }
    var connectionText by remember { mutableStateOf("...") }
    // FIX: chats теперь маппируется из Room ChatEntity в Map<String,Any>
    // чтобы не переписывать весь UI — совместимый формат
    val chats: List<Map<String, Any>> = remember(roomChats) {
        roomChats.map { entity ->
            buildMap {
                put("chatId", entity.chatId)
                put("isGroup", entity.isGroup)
                put("groupName", entity.groupName ?: "")
                put("groupAvatarUrl", entity.groupAvatarUrl ?: "")
                put("groupIcon", entity.groupIcon ?: "groups")
                put("useCustomAvatar", entity.useCustomAvatar)
                put("lastMessage", entity.lastMessage ?: "")
                // unreadCount уже раскрыт по userId в Repository — просто int
                put("unreadCount", entity.unreadCount)
                // updatedAt — Long (миллисекунды), не Timestamp
                put("updatedAt", entity.updatedAt)
                put("description", entity.description ?: "")
                put("ownerId", entity.ownerId ?: "")
                put("isPublic", entity.isPublic)
                // isPinned раскрыт по userId в Repository
                put("isPinned", entity.isPinned)
                // partnerUid нужен для поиска живых данных в userCache
                put("partnerUid", entity.partnerUid ?: "")
                // партнёрские данные для личных чатов
                put("partnerName", entity.partnerName ?: "")
                put("partnerAvatarUrl", entity.partnerAvatarUrl ?: "")
                put("partnerProfileGlow", entity.partnerProfileGlow ?: "purple")
                put("partnerEmoji", entity.partnerEmoji ?: "")
            }
        }
    }
    // isInitialLoading = true пока Room не вернул ни одного элемента И идёт синк
    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(roomChats, syncState) {
        if (syncState != SyncState.IDLE && syncState != SyncState.SYNCING) {
            isInitialLoading = false
        }
        if (roomChats.isNotEmpty()) isInitialLoading = false
    }
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
        // FIX: ключ Unit вместо dots; syncState из VM подсказывает реальный статус Firestore
        LaunchedEffect(Unit) {
            while (isActive) {
                val internetOk = checkInternet(context)
                when {
                    !internetOk -> {
                        connectionState = ConnectionState.OFFLINE
                        connectionText = "Жди инет$dots"
                    }
                    else -> {
                        connectionState = ConnectionState.ONLINE
                        connectionText = "в сети"
                    }
                }
                delay(4000)
            }
        }
        // Синк-статус от VM — показывает реальный статус Firestore без ping-запросов
        LaunchedEffect(syncState) {
            when (syncState) {
                SyncState.OFFLINE -> {
                    connectionState = ConnectionState.OFFLINE
                    connectionText = "Нет связи$dots"
                }
                SyncState.ONLINE -> {
                    connectionState = ConnectionState.ONLINE
                    connectionText = "в сети"
                }
                else -> {}
            }
        }

        val glowColor by animateColorAsState(
            targetValue = when (connectionState) {
                ConnectionState.ONLINE -> MayasTheme.GlowGreen
                ConnectionState.OFFLINE -> Color(0xFFFF4D4D)
            },
            animationSpec = tween(500), label = "glow"
        )

        // --- ЗАГРУЗКА ДАННЫХ СВОЕГО ПРОФИЛЯ ---
        LaunchedEffect(myUid) {
            vm.db.collection("users").document(myUid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    myProfileData = mapOf(
                        "name" to (doc.getString("name") ?: doc.getString("username") ?: "Я"),
                        "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                        "profileIcon" to (doc.getString("profileIcon") ?: "ghost"),
                        "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: false),
                        "activity" to (doc.getString("activity") ?: "в сети"),
                        "isPremium" to (doc.getBoolean("isPremium") ?: false),
                        "nameColor" to (doc.getString("nameColor") ?: "gold"),
                        "isGroup" to false
                    )
                }
            }
        }

        // --- ЗАГРУЗКА ЧАТОВ: теперь через ChatListViewModel (offline-first) ---
        // VM слушает Firestore и пишет в Room. UI читает из roomChats (см. выше).
        // userCache больше не нужен для основных данных — они в ChatEntity.
        // Оставляем его для дополнительных полей (lastSeen, typing, isInvisible)
        // которые не хранятся в Room намеренно (слишком часто меняются).
        DisposableEffect(myUid) {
            val partnerListeners = mutableMapOf<String, ListenerRegistration>()

            // Слушаем только "живые" данные партнёров которые не нужно кэшировать
            val chatsListener = vm.db.collection("chats")
                .whereArrayContains("participants", myUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val currentPartnerUids = snapshot.documents
                        .filter { doc ->
                            val type = doc.getString("type") ?: "DIRECT"
                            val isGroup = type == "GROUP" || (doc.getBoolean("isGroup") ?: false)
                            !isGroup
                        }
                        .flatMap { doc ->
                            (doc.get("participants") as? List<*>)
                                ?.filterIsInstance<String>()
                                ?.filter { it != myUid }
                                ?: emptyList()
                        }.toSet()

                    val toRemove = partnerListeners.keys - currentPartnerUids
                    toRemove.forEach { uid ->
                        partnerListeners[uid]?.remove()
                        partnerListeners.remove(uid)
                    }

                    currentPartnerUids.forEach { partnerUid ->
                        if (!partnerListeners.containsKey(partnerUid)) {
                            partnerListeners[partnerUid] = vm.db.collection("users")
                                .document(partnerUid)
                                .addSnapshotListener { doc, _ ->
                                    if (doc != null && doc.exists()) {
                                        // Только живые данные которые не кэшируем в Room
                                        userCache[partnerUid] = mapOf(
                                            "lastSeen" to doc.getTimestamp("lastSeen"),
                                            "isInvisible" to (doc.getBoolean("isInvisible") ?: false),
                                            "typing" to doc.get("typing"),
                                            "activity" to (doc.getString("activity") ?: ""),
                                            // Остальное уже в Room через ChatEntity
                                            "name" to (doc.getString("name") ?: doc.getString("username") ?: "Аноним"),
                                            "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                                            "profileIcon" to (doc.getString("profileIcon") ?: "ghost"),
                                            "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: false),
                                            "profileGlow" to (doc.getString("profileGlow") ?: "purple"),
                                            "isPremium" to (doc.getBoolean("isPremium") ?: false),
                                            // nameColor — реальный цвет ника из профиля партнёра, тот же
                                            // параметр, что в ProfileScreen/ChatVM. Раньше не читался тут,
                                            // из-за чего список чатов красил премиум-имена жёстко золотым.
                                            "nameColor" to (doc.getString("nameColor") ?: "gold"),
                                            "isGroup" to false,
                                            "emoji" to (doc.getString("emojiStatus") ?: "")
                                        )
                                    }
                                }
                        }
                    }
                }

            onDispose {
                chatsListener.remove()
                partnerListeners.values.forEach { it.remove() }
            }
        }

        // --- СОРТИРОВКА И ФИЛЬТРАЦИЯ ПО ПАПКАМ И ПОИСКУ ---
        val filteredChats = remember(chats, searchQuery, userCache, selectedFolder) {
            chats.filter { chat ->
                val isGroup = chat["isGroup"] as? Boolean ?: false

                // Имя для поиска: для групп — groupName, для личных — из userCache или partnerName
                val name = if (isGroup) {
                    chat["groupName"] as? String ?: ""
                } else {
                    val partnerUid = chat["partnerUid"] as? String ?: ""
                    // Приоритет: живые данные из userCache (имя может обновиться)
                    // иначе — закэшированное в Room partnerName
                    userCache[partnerUid]?.get("name") as? String
                        ?: chat["partnerName"] as? String
                        ?: ""
                }

                val matchesSearch = name.contains(searchQuery, ignoreCase = true)
                if (!matchesSearch) return@filter false

                // isPinned уже раскрыт по userId в Repository и хранится в ChatEntity
                val isPinned = chat["isPinned"] as? Boolean ?: false

                when (selectedFolder) {
                    ChatFolder.ALL -> true
                    ChatFolder.PINNED -> isPinned
                    ChatFolder.GROUPS -> isGroup
                    ChatFolder.CONTACTS -> !isGroup
                }
            }.sortedWith(
                compareByDescending<Map<String, Any>> { it["isPinned"] as? Boolean ?: false }
                    .thenByDescending {
                        // updatedAt — Long (мс), не Timestamp
                        it["updatedAt"] as? Long ?: 0L
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
                                colors = listOf(MayasTheme.Outline, Color.Transparent)
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

                        // --- ПУНКТ ОБНОВЛЕНИЯ (ЕСЛИ ЕСТЬ) ---
                        if (isUpdateAvailable) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MayasTheme.GlowGreen.copy(alpha = 0.15f))
                                    .combinedClickable {
                                        if (updateUrl.isNotEmpty()) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                                        }
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    tint = MayasTheme.GlowGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Обновить Mayas",
                                    color = MayasTheme.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

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
                                        it["isPinned"] as? Boolean ?: false
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
                                                else MayasTheme.Surface.copy(alpha = 0.05f)
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
            // --- ДОБАВЛЕН SCAFFOLD (БЕЗ FAB) ---
            Scaffold(
                containerColor = MayasTheme.Background
            ) { paddingValues ->
                // НАЧАЛО ОСНОВНОГО СТОЛБЦА
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MayasTheme.Background)
                ) {
                    // --- ПЛАШКА ОБНОВЛЕНИЯ ---
                    AnimatedVisibility(
                        visible = isUpdateAvailable && !bannerDismissed,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MayasTheme.GlowPurple.copy(alpha = 0.9f))
                                .clickable {
                                    if (updateUrl.isNotEmpty()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Update, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Доступно обновление Mayas!",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { bannerDismissed = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

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
                                            chatDoc?.get("isPinned") as? Boolean ?: false
                                        vm.db.collection("chats").document(id)
                                            .update("pinned_$myUid", !isCurrentlyPinned)
                                    }
                                    selectedChats = emptySet()
                                },
                                onDeleteChats = {
                                    // FIX: раньше удалялся только сам документ чата в Firestore,
                                    // а подколлекция messages оставалась — утечка данных, и при
                                    // повторном создании чата с тем же chatId старые сообщения
                                    // "воскресали". Теперь сначала чистим сообщения батчем.
                                    selectedChats.forEach { id ->
                                        val chatRef = vm.db.collection("chats").document(id)
                                        chatRef.collection("messages").get()
                                            .addOnSuccessListener { snapshot ->
                                                val batch = vm.db.batch()
                                                snapshot.documents.forEach { batch.delete(it.reference) }
                                                batch.commit().addOnCompleteListener {
                                                    chatRef.delete()
                                                }
                                            }
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
                                    // isPinned уже раскрыт в Repository по userId
                                    val isPinned = chat["isPinned"] as? Boolean ?: false

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
                                            "profileGlow" to "purple",
                                            "isGroup" to true
                                        )

                                        ChatItemNew(
                                            userData = groupData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",
                                            // unreadCount уже Int, раскрытый по userId в Repository
                                            unreadCount = chat["unreadCount"] as? Int ?: 0,
                                            // updatedAt — Long (мс)
                                            updatedAt = chat["updatedAt"] as? Long ?: 0L,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            unreadGlowAlpha = unreadGlowAlpha,
                                            isOnline = false,
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
                                        val partnerUid = chat["partnerUid"] as? String ?: ""
                                        val userData: Map<String, Any?> = userCache[partnerUid]
                                            ?: mapOf(
                                                "name" to chat["partnerName"],
                                                "avatarUrl" to chat["partnerAvatarUrl"],
                                                "profileGlow" to chat["partnerProfileGlow"],
                                                "emoji" to chat["partnerEmoji"],
                                                "profileIcon" to "ghost",
                                                "useCustomAvatar" to false,
                                                "isPremium" to false,
                                                "nameColor" to (chat["partnerNameColor"] ?: "gold"),
                                                "isGroup" to false
                                            )
                                        val isOnline = isUserOnline(userData)
                                        val typingMap = userData["typing"] as? Map<*, *>
                                        val isTyping = typingMap?.get(chatId) == true

                                        ChatItemNew(
                                            userData = userData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",
                                            unreadCount = chat["unreadCount"] as? Int ?: 0,
                                            updatedAt = chat["updatedAt"] as? Long ?: 0L,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            unreadGlowAlpha = unreadGlowAlpha,
                                            isOnline = isOnline,
                                            isTyping = isTyping,
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
            onConfirm = {},
            vm = vm,
            onStartChat = onStartChat,
            onCreateGroup = {
                showUserSearchDialog = false
                showCreateGroupScreen = true
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
                        .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, MayasTheme.Outline, RoundedCornerShape(12.dp))
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
                    .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, MayasTheme.Outline, RoundedCornerShape(12.dp))
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
                focusedContainerColor = MayasTheme.SurfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MayasTheme.SurfaceVariant.copy(alpha = 0.2f),
                focusedBorderColor = MayasTheme.GlowPurple.copy(alpha = 0.35f),
                unfocusedBorderColor = MayasTheme.Outline
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
    updatedAt: Long,
    isSelected: Boolean,
    isPinned: Boolean,
    unreadGlowAlpha: Float,
    isOnline: Boolean = false,
    isTyping: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val avatarGlow = when (userData?.get("profileGlow")) {
        "pink" -> MayasTheme.GlowPink
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "red" -> MayasTheme.GlowRed
        "orange" -> MayasTheme.GlowOrange
        "cyan" -> MayasTheme.GlowCyan
        "mint" -> MayasTheme.GlowMint
        "indigo" -> MayasTheme.GlowIndigo
        "lime" -> MayasTheme.GlowLime
        "rose" -> MayasTheme.GlowRose
        "amber" -> MayasTheme.GlowAmber
        "sky" -> MayasTheme.GlowSky
        "white" -> MayasTheme.GlowWhite
        "gold" -> MayasTheme.GlowGold
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
        Box(contentAlignment = Alignment.Center) {
            MayasAvatar(
                url = if (userData?.get("useCustomAvatar") as? Boolean == true) userData?.get("avatarUrl") as? String else null,
                icon = userData?.get("profileIcon") as? String ?: "ghost",
                glowColor = avatarGlow,
                isPremium = userData?.get("premium") as? Boolean ?: false,
                size = 54.dp,
                useCustomAvatar = userData?.get("useCustomAvatar") as? Boolean ?: false,
                frameType = userData?.get("avatarFrame") as? String ?: "none"
            )

            // Онлайн статус отображается только если человек РЕАЛЬНО в сети
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MayasTheme.GlowGreen)
                        .border(1.5.dp, MayasTheme.Background, CircleShape)
                        .offset(x = 2.dp, y = 2.dp)
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
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val name = userData?.get("name") ?: "..."
                    val isPremium = userData?.get("isPremium") as? Boolean ?: false
                    val isGroupChat =
                        userData?.get("isGroup") as? Boolean ?: userData?.containsKey("groupName")
                        ?: false

                    // Раньше: любой премиум красился жёстко золотым, без учёта
                    // реального nameColor из профиля партнёра. Теперь берём тот
                    // же цвет, что выбран в ProfileScreen (getNameColorBrush — общий источник истины).
                    val nameBrush = if (isPremium && !isGroupChat) {
                        getNameColorBrush(userData?.get("nameColor") as? String ?: "gold")
                    } else {
                        null
                    }

                    Text(
                        text = "${name} ${userData?.get("emoji") ?: ""}".trim(),
                        style = if (nameBrush != null) TextStyle(brush = nameBrush) else TextStyle(
                            color = MayasTheme.TextPrimary
                        ),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isPremium && !isGroupChat) {
                        Spacer(Modifier.width(4.dp))
                        val verifiedIcon = userData?.get("verifiedIcon") as? String ?: "verified"
                        val vIcon = when(verifiedIcon) {
                            "star" -> Icons.Default.Star
                            "diamond" -> Icons.Default.Diamond
                            "auto_awesome" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Verified
                        }
                        Icon(
                            imageVector = vIcon,
                            contentDescription = "Premium",
                            tint = MayasTheme.GlowGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закреплен",
                            tint = MayasTheme.GlowPurple,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (updatedAt > 0L) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(updatedAt),
                        color = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTyping) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TypingIndicator(dotColor = MayasTheme.GlowPurple)
                        Text(
                            text = "печатает...",
                            color = MayasTheme.GlowPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = lastMsg.ifEmpty { "Нет сообщений" },
                        modifier = Modifier.weight(1f),
                        color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                colors = listOf(MayasTheme.Outline, Color.Transparent)
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
                        .background(MayasTheme.SurfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MayasTheme.Outline, CircleShape),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPremium = myProfileData["isPremium"] as? Boolean ?: false
                    // То же самое для своего имени внизу — реальный nameColor вместо
                    // жёсткого золотого по флагу isPremium.
                    val nameBrush = if (isPremium) {
                        getNameColorBrush(myProfileData["nameColor"] as? String ?: "gold")
                    } else {
                        null
                    }

                    Text(
                        text = myProfileData["name"] as? String ?: "Загрузка...",
                        style = if (nameBrush != null) TextStyle(brush = nameBrush) else TextStyle(color = MayasTheme.TextPrimary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val verifiedIcon = myProfileData["verifiedIcon"] as? String ?: "verified"
                        val vIcon = when(verifiedIcon) {
                            "star" -> Icons.Default.Star
                            "diamond" -> Icons.Default.Diamond
                            "auto_awesome" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Verified
                        }
                        Icon(
                            imageVector = vIcon,
                            contentDescription = "Premium",
                            tint = MayasTheme.GlowGold,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Открыть профиль",
                            tint = MayasTheme.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
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
                    .background(MayasTheme.SurfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MayasTheme.Outline, RoundedCornerShape(12.dp)),
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
    onConfirm: () -> Unit,
    vm: AuthVM,
    onStartChat: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    var foundUser by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val chatListVM: ChatListViewModel = viewModel()


    LaunchedEffect(searchInput) {
        val name = searchInput.removePrefix("@").trim()
        if (name.length >= 3) {
            isSearching = true
            vm.resolveUserByUsername(name) { user ->
                foundUser = user
                isSearching = false
            }
        } else {
            foundUser = null
            isSearching = false
        }
    }

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
                // Кнопка создания группы внутри поиска
                Surface(
                    onClick = onCreateGroup,
                    color = MayasTheme.GlowPurple.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Groups, null, tint = MayasTheme.GlowPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Создать группу", color = MayasTheme.GlowPurple, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Поиск по юзернейму",
                    color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = onInputChange,
                    placeholder = { Text("@username", color = MayasTheme.TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = searchError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MayasTheme.Background,
                        unfocusedContainerColor = MayasTheme.SurfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MayasTheme.GlowPurple,
                        unfocusedBorderColor = MayasTheme.Outline
                    )
                )

                Spacer(Modifier.height(16.dp))

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = MayasTheme.GlowPurple)
                } else if (foundUser != null) {
                    val user = foundUser!!
                    val userUid = user["uid"] as? String ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MayasTheme.Surface)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MayasAvatar(
                            url = if (user["useCustomAvatar"] as? Boolean == true) user["avatarUrl"] as? String else null,
                            icon = user["profileIcon"] as? String ?: "ghost",
                            glowColor = MayasTheme.GlowPurple,
                            isPremium = user["isPremium"] as? Boolean ?: false,
                            size = 64.dp,
                            useCustomAvatar = user["useCustomAvatar"] as? Boolean ?: false,
                            frameType = user["frameType"] as? String ?: "rainbow"
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(user["name"] as? String ?: "", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("@${user["username"]}", color = MayasTheme.TextSecondary, fontSize = 12.sp)

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (userUid == myUid) {
                                    // ничего
                                } else {
                                    chatListVM.openOrCreateDirectChat(myUid, userUid) { chatId ->
                                        onStartChat(chatId)
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = userUid != myUid,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowPurple)
                        ) {
                            Text(if (userUid == myUid) "Это вы" else "Написать")
                        }
                    }
                } else if (searchInput.length >= 3) {
                    Text("Никого не нашли :(", color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, MayasTheme.Outline, RoundedCornerShape(20.dp))
    )
}

// УДАЛЕНО: перенесено в UserUtils.kt


fun formatTimestamp(millis: Long): String {
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
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> "вчера"
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
            SimpleDateFormat("d MMM", Locale("ru")).format(date)
        }
        else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}

fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = cm.activeNetwork ?: return false
    val actNw = cm.getNetworkCapabilities(nw) ?: return false
    return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}