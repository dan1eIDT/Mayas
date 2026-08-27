@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core.ui.theme.HomeScreenLayoutPrefs
import com.dan1eidtj.mayas.core.ui.theme.SidebarLayoutPrefs
import com.dan1eidtj.mayas.core.ui.theme.VerticalSlot
import com.dan1eidtj.mayas.core.ui.theme.HorizontalSlot
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.feature.TypingIndicator
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.chat.CreateChannelScreen
import com.dan1eidtj.mayas.feature.chat.CreateGroupScreen
import com.dan1eidtj.mayas.feature.formatCompactCount
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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


@Composable
fun DrawerActionRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    iconAtEnd: Boolean = false,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 40.dp else 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable { onClick() }
            .padding(horizontal = if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (iconAtEnd) Arrangement.SpaceBetween else Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        val iconEl: @Composable () -> Unit = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (compact) 17.dp else 20.dp)
            )
        }
        val labelEl: @Composable () -> Unit = {
            Text(
                text = label,
                color = MayasTheme.TextPrimary,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (iconAtEnd) {
            labelEl(); iconEl()
        } else {
            iconEl(); labelEl()
        }
    }
}

enum class ConnectionState { ONLINE, OFFLINE }

enum class ChatFolder(val displayName: String, val icon: ImageVector) {
    ALL("Все чаты", Icons.Default.ChatBubble),
    PINNED("Закрепленные", Icons.Default.PushPin),
    GROUPS("Группы", Icons.Default.Groups),
    CHANNELS("Каналы", Icons.Default.Campaign),
    CONTACTS("Контакты", Icons.Default.People)
}

fun getChatId(uid1: String, uid2: String): String {
    return listOf(uid1, uid2).sorted().joinToString("_")
}




@Composable
fun ChatListScreen(
    vm: AuthVM,
    onStartChat: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenUserSearch: () -> Unit,
    onDismissUserSearch: () -> Unit,
    homeLayoutPrefs: HomeScreenLayoutPrefs = HomeScreenLayoutPrefs(),
    sidebarLayoutPrefs: SidebarLayoutPrefs = SidebarLayoutPrefs(),
    onUpdateSidebarPrefs: (SidebarLayoutPrefs) -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val myUid = currentUser.uid
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val chatListVm: ChatListViewModel = viewModel()

    val roomChats by chatListVm.chats.collectAsState()
    val syncState by chatListVm.syncState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


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


    var showCreateGroupScreen by remember { mutableStateOf(false) }
    var showCreateChannelScreen by remember { mutableStateOf(false) }
    var showContactsSyncScreen by remember { mutableStateOf(false) }


    var selectedChats by remember { mutableStateOf(setOf<String>()) }
    var connectionState by remember { mutableStateOf(ConnectionState.ONLINE) }
    var connectionText by remember { mutableStateOf("...") }


    val chats: List<Map<String, Any>> = remember(roomChats) {
        roomChats.map { entity ->
            buildMap {
                put("chatId", entity.chatId)
                put("isGroup", entity.isGroup)
                put("chatType", entity.chatType)
                put("groupName", entity.groupName ?: "")
                put("groupAvatarUrl", entity.groupAvatarUrl ?: "")
                put("groupIcon", entity.groupIcon ?: "groups")
                put("groupProfileGlow", entity.groupProfileGlow ?: "purple")
                put("useCustomAvatar", entity.useCustomAvatar)
                put("lastMessage", entity.lastMessage ?: "")
                put("unreadCount", entity.unreadCount)
                put("updatedAt", entity.updatedAt)
                put("description", entity.description ?: "")
                put("ownerId", entity.ownerId ?: "")
                put("isPublic", entity.isPublic)
                put("isPinned", entity.isPinned)
                put("partnerUid", entity.partnerUid ?: "")
                put("partnerName", entity.partnerName ?: "")
                put("partnerAvatarUrl", entity.partnerAvatarUrl ?: "")
                put("partnerProfileIcon", entity.partnerProfileIcon ?: "ghost")
                put("partnerProfileGlow", entity.partnerProfileGlow ?: "purple")
                put("partnerUseCustomAvatar", entity.partnerUseCustomAvatar)
                put("partnerIsPremium", entity.partnerIsPremium)
                put("partnerAvatarFrame", entity.partnerAvatarFrame ?: "none")
                put("partnerNameColor", entity.partnerNameColor ?: "gold")
                put("partnerEmoji", entity.partnerEmoji ?: "")
                put("isSavedMessages", entity.isSavedMessages)
            }
        }
    }

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(roomChats, syncState) {
        if (syncState != SyncState.IDLE && syncState != SyncState.SYNCING) {
            isInitialLoading = false
        }
        if (roomChats.isNotEmpty()) isInitialLoading = false
    }
    var searchQuery by remember { mutableStateOf("") }
    var forceShowSearch by remember { mutableStateOf(false) }
    LaunchedEffect(myUid) {
        try {
            vm.ensureSavedMessagesChat(myUid)
        } catch (e: Exception) {
            Log.e("SavedChat", "Не удалось создать/проверить Избранное", e)
        }
    }
    var showUserSearchDialog by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf(ChatFolder.ALL) }

    val userCache by chatListVm.partnerPresence.collectAsState()
    val myProfileData by chatListVm.myProfile.collectAsState()


    if (showCreateGroupScreen) {
        CreateGroupScreen(
            onBack = { showCreateGroupScreen = false },
            onGroupCreated = { newChatId ->
                showCreateGroupScreen = false
                onStartChat(newChatId)
            }
        )
    } else if (showCreateChannelScreen) {
        CreateChannelScreen(
            onBack = { showCreateChannelScreen = false },
            onChannelCreated = { newChatId ->
                showCreateChannelScreen = false
                onStartChat(newChatId)
            }
        )
    } else if (showContactsSyncScreen) {
        ContactsSyncScreen(
            myUid = myUid,
            onBack = { showContactsSyncScreen = false },
            onStartChat = { partnerUid ->
                showContactsSyncScreen = false
                chatListVm.openOrCreateDirectChat(myUid, partnerUid, onReady = onStartChat)
            }
        )
    } else {

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

















        val filteredChats = remember(chats, searchQuery, userCache, selectedFolder) {
            chats.filter { chat ->
                val isGroup = chat["isGroup"] as? Boolean ?: false


                val name = if (isGroup) {
                    chat["groupName"] as? String ?: ""
                } else {
                    val partnerUid = chat["partnerUid"] as? String ?: ""


                    userCache[partnerUid]?.get("name") as? String
                        ?: chat["partnerName"] as? String
                        ?: ""
                }

                val matchesSearch = name.contains(searchQuery, ignoreCase = true)
                if (!matchesSearch) return@filter false


                val isPinned = chat["isPinned"] as? Boolean ?: false

                when (selectedFolder) {
                    ChatFolder.ALL -> true
                    ChatFolder.PINNED -> isPinned
                    ChatFolder.GROUPS -> isGroup && chat["chatType"] != "CHANNEL" && !(chat["isSavedMessages"] as? Boolean ?: false)
                    ChatFolder.CHANNELS -> chat["chatType"] == "CHANNEL"
                    ChatFolder.CONTACTS -> !isGroup
                }
            }.sortedWith(
                compareByDescending<Map<String, Any>> { it["isPinned"] as? Boolean ?: false }
                    .thenByDescending {

                        it["updatedAt"] as? Long ?: 0L
                    }
            )
        }


        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MayasTheme.Background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier
                        .width(if (sidebarLayoutPrefs.compactMode) 250.dp else 300.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 24.dp)
                    ) {

                        val profileBlock: @Composable () -> Unit = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 14.dp else 24.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable {
                                        coroutineScope.launch { drawerState.close() }
                                        onOpenProfile(myUid)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                UserAvatarView(
                                    avatarUrl = myProfileData["avatarUrl"] as? String,
                                    useCustomAvatar = myProfileData["useCustomAvatar"] as? Boolean ?: false,
                                    profileIcon = myProfileData["profileIcon"] as? String ?: "ghost",
                                    profileGlow = myProfileData["profileGlow"] as? String ?: "purple",
                                    isPremium = myProfileData["isPremium"] as? Boolean ?: false,
                                    frameType = myProfileData["avatarFrame"] as? String ?: "none",
                                    size = 52.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = myProfileData["name"] as? String ?: "Я",
                                        color = MayasTheme.TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Открыть профиль",
                                        color = MayasTheme.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (sidebarLayoutPrefs.profileBlockPosition == VerticalSlot.TOP) {
                            profileBlock()
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MayasTheme.Outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Column(modifier = Modifier.padding(horizontal = if (sidebarLayoutPrefs.compactMode) 14.dp else 24.dp)) {

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


                            if (sidebarLayoutPrefs.showQuickActions) {
                            DrawerActionRow(
                                icon = Icons.Default.GroupAdd,
                                iconTint = MayasTheme.GlowPurple,
                                label = "Создать группу",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showCreateGroupScreen = true
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            DrawerActionRow(
                                icon = Icons.Default.Campaign,
                                iconTint = MayasTheme.GlowPurple,
                                label = "Создать канал",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showCreateChannelScreen = true
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            DrawerActionRow(
                                icon = Icons.Default.PersonAdd,
                                iconTint = MayasTheme.GlowPurple,
                                label = "Пригласить друга",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Заходи в Маяс , не похалеешь) - https://dan1eidt.github.io/mayas-site/")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Пригласить друга"))
                                    } catch (e: Exception) {}
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            DrawerActionRow(
                                icon = Icons.Default.Contacts,
                                iconTint = MayasTheme.GlowPurple,
                                label = "Найти контакты",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showContactsSyncScreen = true
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            }

                            if (!homeLayoutPrefs.showAddFriendButton) {
                                DrawerActionRow(
                                    icon = Icons.Default.PersonAdd,
                                    iconTint = MayasTheme.GlowPurple,
                                    label = "Новый чат",
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showUserSearchDialog = true; onOpenUserSearch()
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                                )
                            }
                            if (!homeLayoutPrefs.showSearchField) {
                                DrawerActionRow(
                                    icon = Icons.Default.Search,
                                    iconTint = MayasTheme.TextSecondary,
                                    label = "Поиск чатов",
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        forceShowSearch = true
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                                )
                            }

                            if (sidebarLayoutPrefs.customLinks.isNotEmpty()) {
                                sidebarLayoutPrefs.customLinks.forEach { link ->
                                    DrawerActionRow(
                                        icon = Icons.Default.Link,
                                        iconTint = MayasTheme.Accent,
                                        label = link.label,
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            val safeUrl = if (link.url.startsWith("http://") || link.url.startsWith("https://")) {
                                                link.url
                                            } else {
                                                "https://${link.url}"
                                            }
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Не удалось открыть ссылку: ${link.url}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (sidebarLayoutPrefs.showPinnedChats && sidebarLayoutPrefs.pinnedChatIds.isNotEmpty()) {
                                val pinnedChats = sidebarLayoutPrefs.pinnedChatIds.mapNotNull { pinnedId ->
                                    chats.firstOrNull { it["chatId"] == pinnedId }
                                }
                                if (pinnedChats.isNotEmpty()) {
                                    Text(
                                        text = "ЗАКРЕПЛЁННЫЕ ЧАТЫ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    pinnedChats.forEach { pinnedChat ->
                                        val pinnedChatId = pinnedChat["chatId"] as? String ?: return@forEach
                                        val isGroupChat = pinnedChat["isGroup"] as? Boolean ?: false
                                        val displayName = if (isGroupChat) {
                                            pinnedChat["groupName"] as? String ?: "Группа"
                                        } else {
                                            pinnedChat["partnerName"] as? String ?: "Чат"
                                        }
                                        val avatarUrl = if (isGroupChat) {
                                            pinnedChat["groupAvatarUrl"] as? String
                                        } else {
                                            pinnedChat["partnerAvatarUrl"] as? String
                                        }
                                        val profileIcon = if (isGroupChat) {
                                            pinnedChat["groupIcon"] as? String ?: "groups"
                                        } else {
                                            pinnedChat["partnerProfileIcon"] as? String ?: "ghost"
                                        }
                                        val profileGlow = if (isGroupChat) {
                                            pinnedChat["groupProfileGlow"] as? String ?: "purple"
                                        } else {
                                            pinnedChat["partnerProfileGlow"] as? String ?: "purple"
                                        }
                                        val useCustomAvatar = pinnedChat["useCustomAvatar"] as? Boolean ?: false

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (sidebarLayoutPrefs.compactMode) 44.dp else 52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .combinedClickable(
                                                    onClick = {
                                                        coroutineScope.launch { drawerState.close() }
                                                        onStartChat(pinnedChatId)
                                                    },
                                                    onLongClick = {
                                                        onUpdateSidebarPrefs(
                                                            sidebarLayoutPrefs.copy(
                                                                pinnedChatIds = sidebarLayoutPrefs.pinnedChatIds - pinnedChatId
                                                            )
                                                        )
                                                        Toast.makeText(context, "Откреплено из панели", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            UserAvatarView(
                                                avatarUrl = avatarUrl,
                                                useCustomAvatar = useCustomAvatar,
                                                profileIcon = profileIcon,
                                                profileGlow = profileGlow,
                                                isPremium = false,
                                                frameType = "none",
                                                size = if (sidebarLayoutPrefs.compactMode) 26.dp else 32.dp
                                            )
                                            Text(
                                                text = displayName,
                                                color = MayasTheme.TextPrimary,
                                                fontSize = if (sidebarLayoutPrefs.compactMode) 13.sp else 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = MayasTheme.TextSecondary.copy(alpha = 0.4f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            val foldersBlock: @Composable () -> Unit = {
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
                                        .height(if (sidebarLayoutPrefs.compactMode) 40.dp else 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MayasTheme.GlowPurple.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .combinedClickable {
                                            selectedFolder = folder
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp)
                                ) {
                                    Icon(
                                        imageVector = folder.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MayasTheme.GlowPurple else MayasTheme.TextSecondary,
                                        modifier = Modifier.size(if (sidebarLayoutPrefs.compactMode) 17.dp else 20.dp)
                                    )
                                    Text(
                                        text = folder.displayName,
                                        color = if (isSelected) MayasTheme.TextPrimary else MayasTheme.TextSecondary,
                                        fontSize = if (sidebarLayoutPrefs.compactMode) 13.sp else 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    val count = when (folder) {
                                        ChatFolder.ALL -> chats.size
                                        ChatFolder.PINNED -> chats.count {
                                            it["isPinned"] as? Boolean ?: false
                                        }

                                        ChatFolder.GROUPS -> chats.count {
                                            (it["isGroup"] as? Boolean ?: false) &&
                                                    it["chatType"] != "CHANNEL" &&
                                                    !(it["isSavedMessages"] as? Boolean ?: false)
                                        }

                                        ChatFolder.CHANNELS -> chats.count {
                                            it["chatType"] == "CHANNEL"
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
                            }

                            val appBlock: @Composable () -> Unit = {
                            Text(
                                text = "ПРИЛОЖЕНИЕ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DrawerActionRow(
                                icon = Icons.Default.Settings,
                                iconTint = MayasTheme.TextSecondary,
                                label = "Настройки",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onOpenSettings()
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            DrawerActionRow(
                                icon = Icons.Default.Info,
                                iconTint = MayasTheme.TextSecondary,
                                label = "О приложении",
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onOpenCredits()
                                },
                                iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                compact = sidebarLayoutPrefs.compactMode
                            )
                            }

                            val foldersIdx = sidebarLayoutPrefs.itemsOrder.indexOf("Папки чатов")
                            val appIdx = sidebarLayoutPrefs.itemsOrder.indexOf("Настройки")
                            val foldersFirst = if (foldersIdx == -1 || appIdx == -1) true else foldersIdx < appIdx
                            val showFoldersBlock = sidebarLayoutPrefs.showFolders
                            val showAppBlock = sidebarLayoutPrefs.showAppSection

                            if (foldersFirst) {
                                if (showFoldersBlock) foldersBlock()
                                if (showFoldersBlock && showAppBlock) Spacer(modifier = Modifier.height(16.dp))
                                if (showAppBlock) appBlock()
                            } else {
                                if (showAppBlock) appBlock()
                                if (showFoldersBlock && showAppBlock) Spacer(modifier = Modifier.height(16.dp))
                                if (showFoldersBlock) foldersBlock()
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (sidebarLayoutPrefs.profileBlockPosition == VerticalSlot.BOTTOM) {
                            HorizontalDivider(color = MayasTheme.Outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            profileBlock()
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MayasTheme.Outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 14.dp else 24.dp)
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

            Scaffold(
                containerColor = MayasTheme.Background
            ) { paddingValues ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MayasTheme.Background)
                ) {

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
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                fabAtStart = homeLayoutPrefs.fabPosition == HorizontalSlot.START,
                                showSearchField = homeLayoutPrefs.showSearchField &&
                                    (homeLayoutPrefs.searchPosition == VerticalSlot.TOP || forceShowSearch),
                                showAddFriendButton = homeLayoutPrefs.showAddFriendButton
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
                                },
                                onPinToSidebar = {
                                    val newPinned = (sidebarLayoutPrefs.pinnedChatIds + selectedChats).distinct()
                                    onUpdateSidebarPrefs(sidebarLayoutPrefs.copy(pinnedChatIds = newPinned))
                                    Toast.makeText(context, "Закреплено в панели", Toast.LENGTH_SHORT).show()
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

                                    val isPinned = chat["isPinned"] as? Boolean ?: false

                                    if (isGroup) {

                                        val isSavedMessages = chat["isSavedMessages"] as? Boolean ?: false
                                        val isChannel = chat["chatType"] == "CHANNEL"
                                        val groupData = mapOf(
                                            "name" to (
                                                    chat["groupName"] as? String ?: "Группа без названия"
                                                    ),
                                            "avatarUrl" to (
                                                    chat["groupAvatarUrl"] as? String ?: ""
                                                    ),
                                            "profileIcon" to (
                                                    chat["groupIcon"] as? String
                                                        ?: if (isChannel) "campaign" else "groups"
                                                    ),
                                            "useCustomAvatar" to (
                                                    chat["useCustomAvatar"] as? Boolean ?: false
                                                    ),
                                            "profileGlow" to (
                                                    chat["groupProfileGlow"] as? String ?: "purple"
                                                    ),
                                            "isPremium" to false,
                                            "avatarFrame" to "none",
                                            "isGroup" to true
                                        )

                                        ChatItemNew(
                                            userData = groupData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",

                                            unreadCount = chat["unreadCount"] as? Int ?: 0,

                                            updatedAt = chat["updatedAt"] as? Long ?: 0L,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            unreadGlowAlpha = unreadGlowAlpha,
                                            isOnline = false,
                                            onClick = {
                                                if (selectedChats.isNotEmpty()) {
                                                    if (!isSavedMessages) {
                                                        selectedChats =
                                                            if (selectedChats.contains(chatId)) selectedChats - chatId else selectedChats + chatId
                                                    }
                                                } else {
                                                    onStartChat(chatId)
                                                }
                                            },
                                            onLongClick = {


                                                if (!isSavedMessages) {
                                                    selectedChats = selectedChats + chatId
                                                }
                                            },
                                            avatarAtEnd = homeLayoutPrefs.avatarPosition == HorizontalSlot.END,
                                            compact = homeLayoutPrefs.compactList
                                        )
                                    } else {
                                        val partnerUid = chat["partnerUid"] as? String ?: ""
                                        val userData: Map<String, Any?> = userCache[partnerUid]
                                            ?: mapOf(
                                                "name" to chat["partnerName"],
                                                "avatarUrl" to chat["partnerAvatarUrl"],
                                                "profileIcon" to chat["partnerProfileIcon"],
                                                "profileGlow" to chat["partnerProfileGlow"],
                                                "useCustomAvatar" to (
                                                        chat["partnerUseCustomAvatar"] as? Boolean ?: false
                                                        ),
                                                "isPremium" to (
                                                        chat["partnerIsPremium"] as? Boolean ?: false
                                                        ),
                                                "avatarFrame" to (
                                                        chat["partnerAvatarFrame"] as? String ?: "none"
                                                        ),
                                                "emoji" to chat["partnerEmoji"],
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
                                            },
                                            avatarAtEnd = homeLayoutPrefs.avatarPosition == HorizontalSlot.END,
                                            compact = homeLayoutPrefs.compactList
                                        )
                                    }
                                }
                            }
                        }
                    }


                    if (homeLayoutPrefs.searchPosition == VerticalSlot.BOTTOM &&
                        (homeLayoutPrefs.showSearchField || forceShowSearch)) {
                        ChatSearchField(searchQuery = searchQuery, onSearchChange = { searchQuery = it })
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val unreadNotificationsCount by chatListVm.unreadNotificationsCount.collectAsState()

                    BottomProfileBar(
                        myUid = myUid,
                        myProfileData = myProfileData,
                        connectionText = connectionText,
                        glowColor = glowColor,
                        pulseScale = pulseScale,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onOpenNotifications = onOpenNotifications,
                        unreadNotificationsCount = unreadNotificationsCount
                    )
                }
            }
        }
    }

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
            },
            onCreateChannel = {
                showUserSearchDialog = false
                showCreateChannelScreen = true
            }
        )
    }
}



@Composable
fun HeaderSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddFriendClick: () -> Unit,
    onMenuClick: () -> Unit,
    fabAtStart: Boolean = false,
    showSearchField: Boolean = true,
    showAddFriendButton: Boolean = true
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
            val menuGroup: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Открыть меню",
                        tint = MayasTheme.TextPrimary,
                        modifier = Modifier.size(24.dp)
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
            }

            val addFriendButton: @Composable () -> Unit = {
            IconButton(
                onClick = onAddFriendClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Добавить друзей",
                    tint = MayasTheme.GlowPurple
                )
            }
            }

            if (fabAtStart) {
                if (showAddFriendButton) addFriendButton()
                menuGroup()
            } else {
                menuGroup()
                if (showAddFriendButton) addFriendButton()
            }
        }

        if (showSearchField) {
            Spacer(modifier = Modifier.height(14.dp))
            ChatSearchField(searchQuery = searchQuery, onSearchChange = onSearchChange)
        }
    }
}

@Composable
fun ChatSearchField(searchQuery: String, onSearchChange: (String) -> Unit) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Поиск по чатам...", color = MayasTheme.TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = MayasTheme.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MayasTheme.SurfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MayasTheme.SurfaceVariant.copy(alpha = 0.2f),
                focusedBorderColor = MayasTheme.GlowPurple.copy(alpha = 0.35f),
                unfocusedBorderColor = MayasTheme.Outline
            )
        )
}

@Composable
fun HeaderSelection(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteChats: () -> Unit,
    onPinToSidebar: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MayasTheme.Surface.copy(alpha = 0.2f))
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
        IconButton(onClick = onPinToSidebar) {
            Icon(Icons.Default.Dashboard, null, tint = MayasTheme.GlowPurple)
        }
        IconButton(onClick = onTogglePin) {
            Icon(Icons.Default.PushPin, null, tint = MayasTheme.TextPrimary)
        }
        IconButton(onClick = onDeleteChats) {
            Icon(Icons.Default.Delete, null, tint = MayasTheme.ErrorRed)
        }
    }
}


@Composable
private fun StatusBadge(
    value: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    iconSize: Dp = fontSize.value.dp
) {
    if (value.isNullOrBlank()) return
    if (ShopConstants.isIconStatus(value)) {
        val context = LocalContext.current
        val resourceName = ShopConstants.iconStatusResourceName(value)
        val resId = remember(resourceName) {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = modifier.size(iconSize)
            )
        }
    } else {
        Text(text = value, fontSize = fontSize, modifier = modifier)
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
    onLongClick: () -> Unit,
    avatarAtEnd: Boolean = false,
    compact: Boolean = false
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
            .padding(horizontal = 18.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val avatarEl: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            UserAvatarView(
                avatarUrl = userData?.get("avatarUrl") as? String,
                useCustomAvatar = userData?.get("useCustomAvatar") as? Boolean ?: false,
                profileIcon = userData?.get("profileIcon") as? String ?: "ghost",
                profileGlow = userData?.get("profileGlow") as? String ?: "purple",
                isPremium = userData?.get("isPremium") as? Boolean
                    ?: userData?.get("premium") as? Boolean
                    ?: false,
                frameType = userData?.get("avatarFrame") as? String ?: "none",
                size = if (compact) 40.dp else 54.dp
            )


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
        }


        val contentEl: @Composable () -> Unit = {
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




                    val nameBrush = if (isPremium && !isGroupChat) {
                        getNameColorBrush(userData?.get("nameColor") as? String ?: "gold")
                    } else {
                        null
                    }

                    Text(
                        text = name as? String ?: "...",
                        style = if (nameBrush != null) TextStyle(brush = nameBrush) else TextStyle(
                            color = MayasTheme.TextPrimary
                        ),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val partnerStatus = userData?.get("emoji") as? String
                    if (!partnerStatus.isNullOrBlank()) {
                        Spacer(Modifier.width(4.dp))
                        StatusBadge(value = partnerStatus, fontSize = 15.sp)
                    }

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

        if (avatarAtEnd) {
            contentEl()
            avatarEl()
        } else {
            avatarEl()
            contentEl()
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
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    unreadNotificationsCount: Int = 0
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
                UserAvatarView(
                    avatarUrl = myProfileData["avatarUrl"] as? String,
                    useCustomAvatar = myProfileData["useCustomAvatar"] as? Boolean ?: false,
                    profileIcon = myProfileData["profileIcon"] as? String ?: "ghost",
                    profileGlow = myProfileData["profileGlow"] as? String ?: "purple",
                    isPremium = myProfileData["isPremium"] as? Boolean ?: false,
                    frameType = myProfileData["avatarFrame"] as? String ?: "none",
                    size = 50.dp
                )

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


            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MayasTheme.SurfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Уведомления",
                        tint = MayasTheme.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (unreadNotificationsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(if (unreadNotificationsCount > 9) 16.dp else 14.dp)
                            .clip(CircleShape)
                            .background(MayasTheme.GlowPurple)
                            .border(1.5.dp, MayasTheme.Background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (unreadNotificationsCount > 9) {
                            Text(
                                text = "9+",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
    onCreateGroup: () -> Unit,
    onCreateChannel: () -> Unit
) {
    var foundUser by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var foundChannel by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isJoiningChannel by remember { mutableStateOf(false) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val chatListVM: ChatListViewModel = viewModel()


    LaunchedEffect(searchInput) {
        val name = searchInput.removePrefix("@").trim()
        // Если ввод похож на номер телефона (только цифры, +, пробелы, скобки, дефисы,
        // и хотя бы 6 цифр) — ищем по номеру, а не по юзернейму/каналу.
        val digitsOnly = searchInput.filter { it.isDigit() }
        val looksLikePhone = searchInput.isNotBlank() &&
            searchInput.all { it.isDigit() || it in "+ ()-" } &&
            digitsOnly.length >= 6

        if (looksLikePhone) {
            isSearching = true
            foundChannel = null
            vm.resolveUserByPhone(searchInput) { user ->
                foundUser = user
                isSearching = false
            }
        } else if (name.length >= 3) {
            isSearching = true
            var userDone = false
            var channelDone = false
            vm.resolveUserByUsername(name) { user ->
                foundUser = user
                userDone = true
                if (channelDone) isSearching = false
            }
            chatListVM.findPublicChannelByUsername(name) { channel ->
                foundChannel = channel
                channelDone = true
                if (userDone) isSearching = false
            }
        } else {
            foundUser = null
            foundChannel = null
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


                Surface(
                    onClick = onCreateChannel,
                    color = MayasTheme.GlowPurple.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Campaign, null, tint = MayasTheme.GlowPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Создать канал", color = MayasTheme.GlowPurple, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Поиск по юзернейму или номеру телефона",
                    color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = onInputChange,
                    placeholder = { Text("@username или +7 999 123-45-67", color = MayasTheme.TextSecondary.copy(alpha = 0.5f)) },
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
                } else {
                    if (foundUser != null) {
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
                            UserAvatarView(
                                avatarUrl = user["avatarUrl"] as? String,
                                useCustomAvatar = user["useCustomAvatar"] as? Boolean ?: false,
                                profileIcon = user["profileIcon"] as? String ?: "ghost",
                                profileGlow = user["profileGlow"] as? String ?: "purple",
                                isPremium = user["isPremium"] as? Boolean ?: false,
                                frameType = user["frameType"] as? String ?: "rainbow",
                                size = 64.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(user["name"] as? String ?: "", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("@${user["username"]}", color = MayasTheme.TextSecondary, fontSize = 12.sp)

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (userUid == myUid) {

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
                    }

                    if (foundChannel != null) {
                        if (foundUser != null) Spacer(Modifier.height(12.dp))

                        val channel = foundChannel!!
                        val channelId = channel["chatId"] as? String ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val alreadyIn = (channel["participants"] as? List<String>)?.contains(myUid) == true
                        @Suppress("UNCHECKED_CAST")
                        val subsCount = (channel["participants"] as? List<*>)?.size ?: 0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MayasTheme.Surface)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            UserAvatarView(
                                avatarUrl = channel["groupAvatar"] as? String,
                                useCustomAvatar = (channel["useCustomAvatar"] as? Boolean) ?: false,
                                profileIcon = channel["groupIcon"] as? String ?: "campaign",
                                profileGlow = channel["profileGlow"] as? String ?: "purple",
                                isPremium = false,
                                frameType = "rainbow",
                                size = 64.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(channel["groupName"] as? String ?: "", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                "@${channel["username"]} · ${formatCompactCount(subsCount)} подписчиков",
                                color = MayasTheme.TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (alreadyIn) {
                                        onStartChat(channelId)
                                        onDismiss()
                                    } else {
                                        isJoiningChannel = true
                                        chatListVM.joinPublicChannel(
                                            chatId = channelId,
                                            myUid = myUid,
                                            onSuccess = {
                                                isJoiningChannel = false
                                                onStartChat(channelId)
                                                onDismiss()
                                            },
                                            onError = { isJoiningChannel = false }
                                        )
                                    }
                                },
                                enabled = !isJoiningChannel,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowPurple)
                            ) {
                                if (isJoiningChannel) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(if (alreadyIn) "Открыть" else "Подписаться")
                                }
                            }
                        }
                    }

                    if (foundUser == null && foundChannel == null && searchInput.length >= 3) {
                        Text("Никого не нашли :(", color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
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