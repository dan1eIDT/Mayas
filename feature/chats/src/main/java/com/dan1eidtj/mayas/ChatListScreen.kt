/* Copyright (C) 2026 ProjectIDT */
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import com.dan1eidtj.data.ChatArchive
import com.dan1eidtj.data.ChatSwipeAction
import com.dan1eidtj.data.ChatSwipePrefs
import com.dan1eidtj.data.ShopConstants
import com.dan1eidtj.data.AppUpdatePrefs
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationBadge
import com.dan1eidtj.mayas.core_ui.utils.InstallSource
import com.dan1eidtj.mayas.core_ui.utils.InstallSourceProvider
import com.dan1eidtj.mayas.core_ui.utils.accentColor
import com.dan1eidtj.mayas.core_ui.utils.onAccentColor
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core.ui.theme.HomeScreenLayoutPrefs
import com.dan1eidtj.mayas.core.ui.theme.SidebarLayoutPrefs
import com.dan1eidtj.mayas.core.ui.theme.VerticalSlot
import com.dan1eidtj.mayas.core.ui.theme.HorizontalSlot
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.core_ui.utils.resolveNameBrush
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.feature.StoryCreatorDialog
import com.dan1eidtj.mayas.feature.StoryGroup
import com.dan1eidtj.mayas.feature.StoryOwner
import com.dan1eidtj.mayas.feature.StoryRepository
import com.dan1eidtj.mayas.feature.StorySeenState
import com.dan1eidtj.mayas.feature.StoryViewerDialog
import com.dan1eidtj.mayas.feature.orderStoryGroups
import com.dan1eidtj.mayas.feature.TypingIndicator
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.chat.CreateChannelScreen
import com.dan1eidtj.mayas.feature.chat.CreateGroupScreen
import com.dan1eidtj.mayas.feature.formatCompactCount
import com.dan1eidtj.chats.R
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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


private fun isNewerReleaseVersion(
    latestVersion: String,
    currentVersion: String
): Boolean {
    val latestParts = Regex("\\d+")
        .findAll(latestVersion.trim())
        .mapNotNull { it.value.toLongOrNull() }
        .toList()

    val currentParts = Regex("\\d+")
        .findAll(currentVersion.trim())
        .mapNotNull { it.value.toLongOrNull() }
        .toList()

    if (latestParts.isEmpty() || currentParts.isEmpty()) return false

    val size = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until size) {
        val latest = latestParts.getOrElse(index) { 0L }
        val current = currentParts.getOrElse(index) { 0L }

        when {
            latest > current -> return true
            latest < current -> return false
        }
    }

    return false
}


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

@Composable
fun WhatsNewDialog(
    version: String,
    changelog: String?,
    onDismiss: () -> Unit
) {
    var showChangelog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.whats_new_welcome_title, version),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (showChangelog) {
                Text(
                    text = changelog?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.whats_new_no_changelog)
                )
            } else {
                Text(text = stringResource(R.string.whats_new_welcome_body))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.whats_new_hooray))
            }
        },
        dismissButton = {
            if (!showChangelog) {
                TextButton(onClick = { showChangelog = true }) {
                    Text(text = stringResource(R.string.whats_new_button))
                }
            }
        }
    )
}

enum class ConnectionState { ONLINE, CONNECTING, OFFLINE }

enum class ChatFolder(val labelRes: Int, val icon: ImageVector) {
    ALL(com.dan1eidtj.chat.R.string.all_chats, Icons.Default.ChatBubble),
    PINNED(com.dan1eidtj.chat.R.string.pinned_chats, Icons.Default.PushPin),
    GROUPS(com.dan1eidtj.chat.R.string.groups, Icons.Default.Groups),
    CHANNELS(R.string.channels, Icons.Default.Campaign),
    CONTACTS(R.string.contacts, Icons.Default.People)
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
    onOpenGlobalSearch: () -> Unit = {},
    homeLayoutPrefs: HomeScreenLayoutPrefs = HomeScreenLayoutPrefs(),
    sidebarLayoutPrefs: SidebarLayoutPrefs = SidebarLayoutPrefs(),
    onUpdateSidebarPrefs: (SidebarLayoutPrefs) -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val accountSwitcher = rememberAccountSwitcher(vm, onNavigateToAuth)
    val authUser by rememberFirebaseUser()
    val currentUser = authUser ?: return
    val myUid = currentUser.uid
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatListScrollState = rememberLazyListState()
    val headerState = rememberTelegramHeaderState()

    val chatListVm: ChatListViewModel = viewModel()

    LaunchedEffect(Unit) { StorySeenState.ensure(context) }
    val storyGroups by remember(myUid) { StoryRepository.observeFeed(myUid) }
        .collectAsState(initial = emptyList())
    val seenStoryIds = StorySeenState.seen
    val orderedStories = remember(storyGroups, seenStoryIds) { orderStoryGroups(storyGroups, seenStoryIds) }
    var storyViewerGroups by remember { mutableStateOf<List<StoryGroup>>(emptyList()) }
    var storyViewerStart by remember { mutableStateOf<Int?>(null) }
    var showStoryCreator by remember { mutableStateOf(false) }

    val roomChats by chatListVm.chats.collectAsState()
    val syncState by chatListVm.syncState.collectAsState()


    val muteState by chatListVm.muteState.collectAsState()
    val massMuteOnText = stringResource(R.string.mass_mute_enabled_toast)
    val massMuteOffText = stringResource(R.string.mass_mute_disabled_toast)
    val massMuteNotSyncedText = stringResource(R.string.mass_mute_not_synced_toast)
    val toggleMassMute: () -> Unit = {
        val enable = !muteState.massActive
        val ok = chatListVm.setMassMute(enable)
        Toast.makeText(
            context,
            if (!ok) massMuteNotSyncedText else if (enable) massMuteOnText else massMuteOffText,
            Toast.LENGTH_SHORT
        ).show()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerAccountsExpanded by remember { mutableStateOf(false) }


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
    var updateUrl by remember { mutableStateOf("") }
    var whatsNewChangelog by remember { mutableStateOf<String?>(null) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val installSource = remember(context) { InstallSourceProvider.detect(context) }

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

    fun maybeShowWhatsNew(changelog: String?) {
        if (currentVersionName.isBlank()) return
        val lastSeenVersion = AppUpdatePrefs.lastSeenVersion(context)
        if (lastSeenVersion != null && lastSeenVersion != currentVersionName) {
            whatsNewChangelog = changelog
            showWhatsNewDialog = true
        }
        AppUpdatePrefs.markVersionSeen(context, currentVersionName)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val rawJson = httpClient.get("https://raw.githubusercontent.com/dan1eIDT/Mayas/master/version.json").bodyAsText()
                val versionInfo: AppVersion = Json { ignoreUnknownKeys = true }.decodeFromString(rawJson)
                withContext(Dispatchers.Main) {
                    updateUrl = versionInfo.updateUrl
                    if (isNewerReleaseVersion(versionInfo.latestVersion, currentVersionName)) {
                        isUpdateAvailable = true
                    }
                    maybeShowWhatsNew(versionInfo.changelog)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                vm.db.collection("system").document("config").get().addOnSuccessListener { doc ->
                    val latestVersion = doc.getString("latestVersion") ?: ""
                    updateUrl = doc.getString("updateUrl") ?: ""
                    if (isNewerReleaseVersion(latestVersion, currentVersionName)) {
                        isUpdateAvailable = true
                    }
                    maybeShowWhatsNew(doc.getString("changelog"))
                }.addOnFailureListener {
                    maybeShowWhatsNew(null)
                }
            }
        }
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            version = currentVersionName,
            changelog = whatsNewChangelog,
            onDismiss = { showWhatsNewDialog = false }
        )
    }


    var showCreateGroupScreen by remember { mutableStateOf(false) }
    var showCreateChannelScreen by remember { mutableStateOf(false) }
    var showContactsSyncScreen by remember { mutableStateOf(false) }


    var selectedChats by remember { mutableStateOf(setOf<String>()) }

    remember(context) { ChatSwipePrefs.ensureInit(context) }
    LaunchedEffect(myUid) { ChatArchive.bind(context, myUid) }
    val archivedIds by ChatArchive.ids.collectAsState()
    val swipeAction by ChatSwipePrefs.action.collectAsState()
    var showArchive by remember { mutableStateOf(false) }

    val markChatsRead: (Collection<String>) -> Unit = { ids ->
        ids.forEach { id ->
            vm.db.collection("chats").document(id).update("unread_$myUid", 0)
        }
    }
    val onSwipeAction: (String, Boolean) -> Unit = { id, pinned ->
        when (swipeAction) {
            ChatSwipeAction.READ -> markChatsRead(listOf(id))
            ChatSwipeAction.ARCHIVE ->
                ChatArchive.setArchived(context, myUid, listOf(id), !archivedIds.contains(id))
            ChatSwipeAction.MUTE ->
                chatListVm.setChatsMuted(setOf(id), !muteState.isMuted(id))
            ChatSwipeAction.PIN ->
                vm.db.collection("chats").document(id).update("pinned_$myUid", !pinned)
        }
    }
    var deviceInternetAvailable by remember { mutableStateOf<Boolean?>(null) }


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
                put("draftText", entity.draftText ?: "")
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
                put("partnerVerified", entity.partnerVerified)
                put("partnerVerificationType", entity.partnerVerificationType ?: "")
                put("partnerVerifiedBy", entity.partnerVerifiedBy ?: "")
                put("partnerRank", entity.partnerRank)
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
    var selectedCustomFolderId by remember { mutableStateOf<String?>(null) }
    var folderBeingEdited by remember { mutableStateOf<ChatFolderData?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showBuiltinFolderManager by remember { mutableStateOf(false) }
    val customFolders by chatListVm.customFolders.collectAsState()
    val folderLimitText = stringResource(R.string.folder_limit_reached)

    val userCache by chatListVm.partnerPresence.collectAsState()
    val myProfileData by chatListVm.myProfile.collectAsState()

    val myStoryOwner = remember(myUid, myProfileData) {
        StoryOwner(
            uid = myUid,
            name = myProfileData["name"] as? String ?: "",
            avatarUrl = myProfileData["avatarUrl"] as? String,
            useCustomAvatar = myProfileData["useCustomAvatar"] as? Boolean ?: false,
            profileIcon = myProfileData["profileIcon"] as? String ?: "ghost",
            profileGlow = myProfileData["profileGlow"] as? String ?: "purple",
            isPremium = myProfileData["isPremium"] as? Boolean ?: false,
            avatarFrame = myProfileData["avatarFrame"] as? String ?: "none",
            ringColor = myProfileData["storyRingColor"] as? String
        )
    }

    storyViewerStart?.let { start ->
        StoryViewerDialog(
            groups = storyViewerGroups,
            startIndex = start,
            myUid = myUid,
            myName = myStoryOwner.name,
            onDismiss = { storyViewerStart = null },
            onOpenProfile = { uid ->
                storyViewerStart = null
                onOpenProfile(uid)
            }
        )
    }

    if (showStoryCreator) {
        StoryCreatorDialog(
            myUid = myUid,
            initialRingColor = myStoryOwner.ringColor,
            onDismiss = { showStoryCreator = false },
            onPublished = { showStoryCreator = false }
        )
    }


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
        val dotCountState = infiniteTransition.animateValue(
            initialValue = 1,
            targetValue = 4,
            typeConverter = Int.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dots"
        )
        val connectionState by remember {
            derivedStateOf {
                when {
                    deviceInternetAvailable == false -> ConnectionState.OFFLINE
                    deviceInternetAvailable == null ||
                            syncState == SyncState.IDLE ||
                            syncState == SyncState.SYNCING -> ConnectionState.CONNECTING
                    syncState == SyncState.OFFLINE -> ConnectionState.OFFLINE
                    else -> ConnectionState.ONLINE
                }
            }
        }
        val waitingForNetworkTemplate = stringResource(R.string.waiting_for_network_dots)
        val noConnectionTemplate = stringResource(R.string.no_connection_dots)
        val onlineText = stringResource(R.string.online)
        val meshOnlineText = stringResource(R.string.mesh_online)
        val meshState by com.dan1eidtj.mayas.mesh.MeshManager.state.collectAsState()
        val latestPrivateMeshByPartner = remember(meshState.messages, myUid) {
            meshState.messages.asSequence()
                .filter { message ->
                    message.privateChat && if (message.outgoing) {
                        message.srcUid == myUid && message.dstUid.isNotBlank()
                    } else {
                        message.dstUid == myUid && message.srcUid.isNotBlank()
                    }
                }
                .groupBy { message -> if (message.outgoing) message.dstUid else message.srcUid }
                .mapNotNull { (partnerUid, messages) ->
                    messages.maxByOrNull { it.timestamp }?.let { partnerUid to it }
                }
                .toMap()
        }
        val meshPreviewPrefix = stringResource(R.string.message_transport_mesh)
        val showMeshStatus = meshState.isActive && (deviceInternetAvailable == false || connectionState == ConnectionState.OFFLINE)

        val pulseScaleState = infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        val unreadGlowAlphaState = infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "unreadGlowAlpha"
        )
        val unreadGlowProvider: () -> Float = remember(unreadGlowAlphaState) { { unreadGlowAlphaState.value } }


        LaunchedEffect(context) {
            while (isActive) {
                deviceInternetAvailable = checkInternet(context)
                delay(4000)
            }
        }

        val connectionLabel: () -> String = {
            when {
                showMeshStatus -> meshOnlineText
                connectionState == ConnectionState.ONLINE -> onlineText
                deviceInternetAvailable == true && syncState == SyncState.OFFLINE ->
                    String.format(noConnectionTemplate, ".".repeat(dotCountState.value))
                else -> String.format(waitingForNetworkTemplate, ".".repeat(dotCountState.value))
            }
        }

        val glowColor by animateColorAsState(
            targetValue = when (connectionState) {
                ConnectionState.ONLINE -> MayasTheme.GlowGreen
                ConnectionState.CONNECTING -> Color(0xFFFFC107)
                ConnectionState.OFFLINE -> Color(0xFFFF4D4D)
            },
            animationSpec = tween(500), label = "glow"
        )

        val headerStatusActive by androidx.compose.runtime.produceState(false, connectionState, showMeshStatus) {
            if (connectionState == ConnectionState.ONLINE && !showMeshStatus) {
                value = false
            } else {
                delay(700)
                value = true
            }
        }


        val filteredChats = remember(chats, searchQuery, userCache, selectedFolder, selectedCustomFolderId, customFolders, latestPrivateMeshByPartner, archivedIds, showArchive) {
            val activeCustomFolder = selectedCustomFolderId?.let { id -> customFolders.find { it.id == id } }
            chats.filter { chat ->
                val chatIdForArchive = chat["chatId"] as? String ?: ""
                val inArchive = archivedIds.contains(chatIdForArchive)
                if (showArchive) {
                    if (!inArchive) return@filter false
                } else if (inArchive && searchQuery.isEmpty()) {
                    return@filter false
                }
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

                if (activeCustomFolder != null) {
                    activeCustomFolder.chatIds.contains(chat["chatId"] as? String)
                } else {
                    when (selectedFolder) {
                        ChatFolder.ALL -> true
                        ChatFolder.PINNED -> isPinned
                        ChatFolder.GROUPS -> isGroup && chat["chatType"] != "CHANNEL" && !(chat["isSavedMessages"] as? Boolean ?: false)
                        ChatFolder.CHANNELS -> chat["chatType"] == "CHANNEL"
                        ChatFolder.CONTACTS -> !isGroup
                    }
                }
            }.sortedWith(
                compareByDescending<Map<String, Any>> { it["isPinned"] as? Boolean ?: false }
                    .thenByDescending { chat ->
                        val firestoreUpdatedAt = chat["updatedAt"] as? Long ?: 0L
                        val isGroup = chat["isGroup"] as? Boolean ?: false
                        if (isGroup) firestoreUpdatedAt else {
                            val partnerUid = chat["partnerUid"] as? String ?: ""
                            maxOf(firestoreUpdatedAt, latestPrivateMeshByPartner[partnerUid]?.timestamp ?: 0L)
                        }
                    }
            )
        }

        val archivedChats = remember(chats, archivedIds) {
            chats.filter { archivedIds.contains(it["chatId"] as? String ?: "") }
                .sortedByDescending { it["updatedAt"] as? Long ?: 0L }
        }
        val archivePreview = remember(archivedChats, userCache) {
            archivedChats.take(3).joinToString(", ") { chat ->
                if (chat["isGroup"] as? Boolean == true) {
                    chat["groupName"] as? String ?: ""
                } else {
                    val partnerUid = chat["partnerUid"] as? String ?: ""
                    val cachedName = userCache[partnerUid]?.get("name") as? String
                    cachedName ?: chat["partnerName"] as? String ?: ""
                }
            }
        }
        val showArchiveEntry = !showArchive &&
                searchQuery.isEmpty() &&
                selectedFolder == ChatFolder.ALL &&
                selectedCustomFolderId == null &&
                archivedChats.isNotEmpty()

        LaunchedEffect(showArchive, archivedChats.isEmpty()) {
            if (showArchive && archivedChats.isEmpty()) showArchive = false
        }
        BackHandler(enabled = showArchive && selectedChats.isEmpty()) { showArchive = false }

        val unreadByFolder = remember(chats, archivedIds) {
            ChatFolder.values().associateWith { folder ->
                chats.count { chat ->
                    !archivedIds.contains(chat["chatId"] as? String ?: "") &&
                    chat.matchesFolder(folder) && (chat["unreadCount"] as? Int ?: 0) > 0
                }
            }
        }
        val unreadByCustomFolder = remember(chats, customFolders, archivedIds) {
            customFolders.associate { folder ->
                folder.id to chats.count { chat ->
                    !archivedIds.contains(chat["chatId"] as? String ?: "") &&
                    folder.chatIds.contains(chat["chatId"]) && (chat["unreadCount"] as? Int ?: 0) > 0
                }
            }
        }

        val folderChips = buildList {
            ChatFolder.values()
                .filterNot { it.name in sidebarLayoutPrefs.hiddenStandardFolderIds }
                .forEach { folder ->
                    add(
                        FolderChipItem(
                            key = folder.name,
                            label = stringResource(folder.labelRes),
                            count = unreadByFolder[folder] ?: 0,
                            selected = selectedCustomFolderId == null && selectedFolder == folder,
                            onClick = {
                                selectedFolder = folder
                                selectedCustomFolderId = null
                            }
                        )
                    )
                }
            customFolders.sortedBy { it.order }.forEach { folder ->
                add(
                    FolderChipItem(
                        key = folder.id,
                        label = folder.name,
                        count = unreadByCustomFolder[folder.id] ?: 0,
                        selected = selectedCustomFolderId == folder.id,
                        onClick = {
                            selectedFolder = ChatFolder.ALL
                            selectedCustomFolderId = folder.id
                        }
                    )
                )
            }
        }


        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MayasTheme.Background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .width(if (sidebarLayoutPrefs.compactMode) 250.dp else 300.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {

                        val sidebarBannerPlacement = myProfileData["bannerPlacement"] as? String ?: "PROFILE_ONLY"
                        val showSidebarBanner = sidebarBannerPlacement == "PROFILE_SIDEBAR" || sidebarBannerPlacement == "ALL"
                        val profileBlock: @Composable (Boolean) -> Unit = { insetTop ->
                            DrawerProfileHeader(
                                profile = myProfileData,
                                compact = sidebarLayoutPrefs.compactMode,
                                insetTop = insetTop,
                                showBanner = showSidebarBanner,
                                accountsExpanded = drawerAccountsExpanded,
                                onToggleAccounts = { drawerAccountsExpanded = !drawerAccountsExpanded },
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onOpenProfile(myUid)
                                }
                            )
                            DrawerAccountsList(
                                visible = drawerAccountsExpanded,
                                sessions = accountSwitcher.sessions,
                                currentUid = accountSwitcher.currentUid,
                                onSelect = { session ->
                                    drawerAccountsExpanded = false
                                    coroutineScope.launch { drawerState.close() }
                                    accountSwitcher.select(session)
                                },
                                onAddAccount = {
                                    drawerAccountsExpanded = false
                                    coroutineScope.launch { drawerState.close() }
                                    accountSwitcher.addAccount()
                                }
                            )
                        }

                        if (sidebarLayoutPrefs.profileBlockPosition == VerticalSlot.TOP) {
                            profileBlock(true)
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            Spacer(modifier = Modifier.statusBarsPadding().height(20.dp))
                        }

                        Column(modifier = Modifier.padding(horizontal = if (sidebarLayoutPrefs.compactMode) 14.dp else 24.dp)) {

                            if (isUpdateAvailable) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(installSource.accentColor.copy(alpha = 0.15f))
                                        .combinedClickable {
                                            InstallSourceProvider.openUpdate(context, installSource, updateUrl)
                                        }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = null,
                                        tint = installSource.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.update_mayas),
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
                                    label = stringResource(R.string.create_group),
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
                                    label = stringResource(R.string.create_channel),
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showCreateChannelScreen = true
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                    compact = sidebarLayoutPrefs.compactMode
                                )
                                val inviteFriendLabel = stringResource(R.string.invite_friend_action)
                                val inviteFriendShareText = stringResource(R.string.invite_friend_share_text)
                                DrawerActionRow(
                                    icon = Icons.Default.PersonAdd,
                                    iconTint = MayasTheme.GlowPurple,
                                    label = inviteFriendLabel,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, inviteFriendShareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, inviteFriendLabel))
                                        } catch (e: Exception) {}
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                    compact = sidebarLayoutPrefs.compactMode
                                )
                                DrawerActionRow(
                                    icon = Icons.Default.Contacts,
                                    iconTint = MayasTheme.GlowPurple,
                                    label = stringResource(R.string.find_contacts_action),
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
                                    label = stringResource(R.string.new_chat),
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
                                    label = stringResource(R.string.search_chats_action),
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        forceShowSearch = true
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                    compact = sidebarLayoutPrefs.compactMode
                                )
                            }

                            if (sidebarLayoutPrefs.showMuteAllButton) {
                                DrawerActionRow(
                                    icon = if (muteState.massActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    iconTint = if (muteState.massActive) MayasTheme.GlowPurple else MayasTheme.TextSecondary,
                                    label = stringResource(
                                        if (muteState.massActive) R.string.unmute_all_chats else R.string.mute_all_chats
                                    ),
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        toggleMassMute()
                                    },
                                    iconAtEnd = sidebarLayoutPrefs.actionsIconPosition == HorizontalSlot.END,
                                    compact = sidebarLayoutPrefs.compactMode
                                )
                            }

                            if (sidebarLayoutPrefs.customLinks.isNotEmpty()) {
                                val openLinkFailedTemplate = stringResource(R.string.error_open_link_failed)
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
                                                Toast.makeText(context, String.format(openLinkFailedTemplate, link.url), Toast.LENGTH_SHORT).show()
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
                                        text = stringResource(R.string.pinned_chats_section_label),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val unpinnedFromPanelText = stringResource(R.string.unpinned_from_panel)
                                    pinnedChats.forEach { pinnedChat ->
                                        val pinnedChatId = pinnedChat["chatId"] as? String ?: return@forEach
                                        val isGroupChat = pinnedChat["isGroup"] as? Boolean ?: false
                                        val displayName = if (isGroupChat) {
                                            pinnedChat["groupName"] as? String ?: stringResource(R.string.group_fallback_name)
                                        } else {
                                            pinnedChat["partnerName"] as? String ?: stringResource(R.string.chat_generic)
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
                                                        Toast.makeText(context, unpinnedFromPanelText, Toast.LENGTH_SHORT).show()
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.folders_label),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    IconButton(
                                        onClick = { showBuiltinFolderManager = true },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.manage_sidebar_folders),
                                            tint = MayasTheme.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                ChatFolder.values()
                                    .filterNot { it.name in sidebarLayoutPrefs.hiddenStandardFolderIds }
                                    .forEach { folder ->
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
                                                    selectedCustomFolderId = null
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
                                                text = stringResource(folder.labelRes),
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

                                customFolders.sortedBy { it.order }.forEach { folder ->
                                    val isSelected = selectedCustomFolderId == folder.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (sidebarLayoutPrefs.compactMode) 40.dp else 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MayasTheme.GlowPurple.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    selectedFolder = ChatFolder.ALL
                                                    selectedCustomFolderId = folder.id
                                                    coroutineScope.launch { drawerState.close() }
                                                },
                                                onLongClick = { folderBeingEdited = folder }
                                            )
                                            .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) MayasTheme.GlowPurple else MayasTheme.TextSecondary,
                                            modifier = Modifier.size(if (sidebarLayoutPrefs.compactMode) 17.dp else 20.dp)
                                        )
                                        Text(
                                            text = folder.name,
                                            color = if (isSelected) MayasTheme.TextPrimary else MayasTheme.TextSecondary,
                                            fontSize = if (sidebarLayoutPrefs.compactMode) 13.sp else 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val count = chats.count { folder.chatIds.contains(it["chatId"]) }
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
                                        IconButton(
                                            onClick = { folderBeingEdited = folder },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                null,
                                                tint = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                if (customFolders.size < ChatListViewModel.MAX_CUSTOM_FOLDERS) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (sidebarLayoutPrefs.compactMode) 40.dp else 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showCreateFolderDialog = true }
                                            .padding(horizontal = if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(if (sidebarLayoutPrefs.compactMode) 10.dp else 14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MayasTheme.GlowPurple,
                                            modifier = Modifier.size(if (sidebarLayoutPrefs.compactMode) 17.dp else 20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.create_folder),
                                            color = MayasTheme.GlowPurple,
                                            fontSize = if (sidebarLayoutPrefs.compactMode) 13.sp else 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            val appBlock: @Composable () -> Unit = {
                                Text(
                                    text = stringResource(R.string.app_section_label),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MayasTheme.TextSecondary.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                DrawerActionRow(
                                    icon = Icons.Default.Settings,
                                    iconTint = MayasTheme.TextSecondary,
                                    label = stringResource(R.string.settings_menu_label),
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
                                    label = stringResource(R.string.about_app_menu_label),
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
                            Spacer(modifier = Modifier.height(16.dp))
                            profileBlock(false)
                            Spacer(modifier = Modifier.height(8.dp))
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
                                contentDescription = stringResource(R.string.logout_content_description),
                                tint = MayasTheme.ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(com.dan1eidtj.auth.R.string.logout_from_account_button),
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

                headerState.enabled = searchQuery.isEmpty() && selectedChats.isEmpty() && !showArchive
                LaunchedEffect(searchQuery.isNotEmpty(), selectedChats.isNotEmpty()) {
                    if (searchQuery.isNotEmpty() || selectedChats.isNotEmpty()) headerState.expand()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MayasTheme.Background)
                        .nestedScroll(headerState.connection)
                ) {

                    AnimatedContent(
                        targetState = selectedChats.isEmpty(),
                        label = "header",
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 3 })
                                .togetherWith(fadeOut(tween(140)))
                        }
                    ) { isNormalMode ->
                        if (isNormalMode && showArchive) {
                            ArchiveHeader(onBack = { showArchive = false })
                        } else if (isNormalMode) {
                            HeaderSection(
                                progress = { headerState.progress },
                                statusLabel = if (headerStatusActive) connectionLabel else null,
                                statusColor = if (showMeshStatus) Color(0xFFFFC107) else glowColor,
                                statusSpinner = connectionState == ConnectionState.CONNECTING && !showMeshStatus,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onAddFriendClick = {
                                    showUserSearchDialog = true; onOpenUserSearch()
                                },
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                onGlobalSearchClick = onOpenGlobalSearch,
                                showSearchField = homeLayoutPrefs.showSearchField &&
                                        (homeLayoutPrefs.searchPosition == VerticalSlot.TOP || forceShowSearch),
                                showAddFriendButton = homeLayoutPrefs.showAddFriendButton,
                                showStories = homeLayoutPrefs.showStories && searchQuery.isEmpty(),
                                stories = orderedStories,
                                myOwner = myStoryOwner,
                                seen = seenStoryIds,
                                onStoryAdd = { showStoryCreator = true },
                                onStoryOpen = { index ->
                                    storyViewerGroups = orderedStories
                                    storyViewerStart = index
                                },
                                folderChips = folderChips
                            )
                        } else {
                            val pinnedToPanelText = stringResource(R.string.pinned_to_panel)
                            val allSelectedMuted = selectedChats.isNotEmpty() &&
                                    selectedChats.all { muteState.isMuted(it) }
                            HeaderSelection(
                                selectedCount = selectedChats.size,
                                onClearSelection = { selectedChats = emptySet() },
                                isSelectionMuted = allSelectedMuted,
                                onToggleMute = {
                                    chatListVm.setChatsMuted(selectedChats, !allSelectedMuted)
                                    selectedChats = emptySet()
                                },
                                massMuteActive = muteState.massActive,
                                onToggleMassMute = {
                                    toggleMassMute()
                                    selectedChats = emptySet()
                                },
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
                                onToggleArchive = {
                                    ChatArchive.setArchived(context, myUid, selectedChats.toList(), !showArchive)
                                    selectedChats = emptySet()
                                },
                                isArchiveMode = showArchive,
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
                                    Toast.makeText(context, pinnedToPanelText, Toast.LENGTH_SHORT).show()
                                    selectedChats = emptySet()
                                }
                            )
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
                        } else if (filteredChats.isEmpty() && !showArchiveEntry) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) stringResource(R.string.nothing_found) else stringResource(R.string.no_chats_in_folder),
                                    color = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyColumn(
                                state = chatListScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                if (showArchiveEntry) {
                                    item(key = "archive_entry") {
                                        ArchiveEntryRow(
                                            preview = archivePreview,
                                            unreadChats = archivedChats.count { (it["unreadCount"] as? Int ?: 0) > 0 },
                                            compact = homeLayoutPrefs.compactList,
                                            onClick = { showArchive = true },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
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
                                                    chat["groupName"] as? String ?: stringResource(R.string.unnamed_group)
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

                                        ChatSwipeRow(
                                            action = swipeAction,
                                            enabled = selectedChats.isEmpty() && !isSavedMessages,
                                            isArchived = archivedIds.contains(chatId),
                                            isMuted = muteState.isMuted(chatId),
                                            isPinned = isPinned,
                                            hasUnread = (chat["unreadCount"] as? Int ?: 0) > 0,
                                            onTrigger = { onSwipeAction(chatId, isPinned) },
                                            modifier = Modifier.animateItem()
                                        ) {
                                        ChatItemNew(
                                            userData = groupData,
                                            lastMsg = chat["lastMessage"] as? String ?: "",
                                            draftText = chat["draftText"] as? String ?: "",

                                            unreadCount = chat["unreadCount"] as? Int ?: 0,

                                            updatedAt = chat["updatedAt"] as? Long ?: 0L,
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            isMuted = muteState.isMuted(chatId),
                                            unreadGlowAlpha = unreadGlowProvider,
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
                                            compact = homeLayoutPrefs.compactList,
                                            isPinnedToSidebar = sidebarLayoutPrefs.pinnedChatIds.contains(chatId)
                                        )
                                        }
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
                                                "isGroup" to false,


                                                "verification" to com.dan1eidtj.data.VerificationInfo(
                                                    verified = chat["partnerVerified"] as? Boolean ?: false,
                                                    type = com.dan1eidtj.data.VerificationType.fromId(
                                                        (chat["partnerVerificationType"] as? String)?.ifBlank { null }
                                                    ),
                                                    verifiedBy = (chat["partnerVerifiedBy"] as? String)?.ifBlank { null }
                                                )
                                            )
                                        val isOnline = isUserOnline(userData)
                                        val typingMap = userData["typing"] as? Map<*, *>
                                        val isTyping = typingMap?.get(chatId) == true

                                        val firestoreUpdatedAt = chat["updatedAt"] as? Long ?: 0L
                                        val latestMeshMessage = latestPrivateMeshByPartner[partnerUid]
                                        val showMeshPreview = latestMeshMessage != null && latestMeshMessage.timestamp > firestoreUpdatedAt
                                        val lastMessagePreview = if (showMeshPreview) {
                                            "$meshPreviewPrefix · ${latestMeshMessage!!.text}"
                                        } else {
                                            chat["lastMessage"] as? String ?: ""
                                        }

                                        ChatSwipeRow(
                                            action = swipeAction,
                                            enabled = selectedChats.isEmpty() && !false,
                                            isArchived = archivedIds.contains(chatId),
                                            isMuted = muteState.isMuted(chatId),
                                            isPinned = isPinned,
                                            hasUnread = (chat["unreadCount"] as? Int ?: 0) > 0,
                                            onTrigger = { onSwipeAction(chatId, isPinned) },
                                            modifier = Modifier.animateItem()
                                        ) {
                                        ChatItemNew(
                                            userData = userData,
                                            lastMsg = lastMessagePreview,
                                            draftText = chat["draftText"] as? String ?: "",
                                            unreadCount = chat["unreadCount"] as? Int ?: 0,
                                            updatedAt = maxOf(firestoreUpdatedAt, latestMeshMessage?.timestamp ?: 0L),
                                            isSelected = selectedChats.contains(chatId),
                                            isPinned = isPinned,
                                            isMuted = muteState.isMuted(chatId),
                                            unreadGlowAlpha = unreadGlowProvider,
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
                                            compact = homeLayoutPrefs.compactList,
                                            isPinnedToSidebar = sidebarLayoutPrefs.pinnedChatIds.contains(chatId)
                                        )
                                        }
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
                        connectionLabel = connectionLabel,
                        glowColor = if (showMeshStatus) Color(0xFFFFC107) else glowColor,
                        pulseScale = { pulseScaleState.value },
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onOpenNotifications = onOpenNotifications,
                        onOpenAccounts = { accountSwitcher.openSheet() },
                        unreadNotificationsCount = unreadNotificationsCount,
                        homeLayoutPrefs = homeLayoutPrefs
                    )
                }
            }
        }
    }

    AccountSwitcherHost(accountSwitcher)

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

    if (showCreateFolderDialog) {
        FolderEditorDialog(
            initialName = "",
            allChats = chats,
            userCache = userCache,
            initiallySelectedChatIds = emptySet(),
            onDismiss = { showCreateFolderDialog = false },
            onSave = { name, chatIds ->
                chatListVm.createFolder(
                    name,
                    chatIds.toList(),
                    onError = { reason ->
                        if (reason == "limit") {
                            Toast.makeText(context, folderLimitText, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showCreateFolderDialog = false
            },
            onDelete = null
        )
    }

    if (showBuiltinFolderManager) {
        SidebarBuiltinFolderManagerDialog(
            hiddenFolderIds = sidebarLayoutPrefs.hiddenStandardFolderIds,
            onFolderVisibilityChanged = { folderId, isVisible ->
                val updatedHiddenFolders = if (isVisible) {
                    sidebarLayoutPrefs.hiddenStandardFolderIds - folderId
                } else {
                    sidebarLayoutPrefs.hiddenStandardFolderIds + folderId
                }
                val updatedPrefs = sidebarLayoutPrefs.copy(hiddenStandardFolderIds = updatedHiddenFolders)
                onUpdateSidebarPrefs(updatedPrefs)
                if (!isVisible && selectedFolder.name == folderId) {
                    selectedFolder = ChatFolder.ALL
                    selectedCustomFolderId = null
                }
            },
            onDismiss = { showBuiltinFolderManager = false }
        )
    }

    folderBeingEdited?.let { folder ->
        FolderEditorDialog(
            initialName = folder.name,
            allChats = chats,
            userCache = userCache,
            initiallySelectedChatIds = folder.chatIds.toSet(),
            onDismiss = { folderBeingEdited = null },
            onSave = { name, chatIds ->
                chatListVm.renameFolder(folder.id, name)
                chatListVm.updateFolderChats(folder.id, chatIds.toList())
                folderBeingEdited = null
            },
            onDelete = {
                chatListVm.deleteFolder(folder.id)
                if (selectedCustomFolderId == folder.id) selectedCustomFolderId = null
                folderBeingEdited = null
            }
        )
    }
}


@Composable
private fun SidebarBuiltinFolderManagerDialog(
    hiddenFolderIds: Set<String>,
    onFolderVisibilityChanged: (folderId: String, isVisible: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.sidebar_folder_visibility_title),
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.sidebar_folder_visibility_hint),
                    color = MayasTheme.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                ChatFolder.values().forEach { folder ->
                    val isVisible = folder.name !in hiddenFolderIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { onFolderVisibilityChanged(folder.name, !isVisible) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = folder.icon,
                            contentDescription = null,
                            tint = MayasTheme.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(folder.labelRes),
                            color = MayasTheme.TextPrimary,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Checkbox(
                            checked = isVisible,
                            onCheckedChange = { checked ->
                                onFolderVisibilityChanged(folder.name, checked)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done_action), color = MayasTheme.GlowPurple)
            }
        },
        containerColor = MayasTheme.Surface
    )
}


@Composable
fun FolderEditorDialog(
    initialName: String,
    allChats: List<Map<String, Any>>,
    userCache: Map<String, Map<String, Any?>>,
    initiallySelectedChatIds: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedChatIds by remember { mutableStateOf(initiallySelectedChatIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (onDelete == null) stringResource(R.string.create_folder) else stringResource(R.string.edit_folder),
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.select_chats_for_folder),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MayasTheme.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(allChats, key = { it["chatId"] ?: it.hashCode() }) { chat ->
                        val chatId = chat["chatId"] as? String ?: return@items
                        val isGroup = chat["isGroup"] as? Boolean ?: false
                        val partnerUid = chat["partnerUid"] as? String ?: ""
                        val title = if (isGroup) {
                            chat["groupName"] as? String ?: ""
                        } else {
                            userCache[partnerUid]?.get("name") as? String
                                ?: chat["partnerName"] as? String
                                ?: ""
                        }
                        val isChecked = selectedChatIds.contains(chatId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedChatIds = if (isChecked) selectedChatIds - chatId else selectedChatIds + chatId
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = {
                                selectedChatIds = if (isChecked) selectedChatIds - chatId else selectedChatIds + chatId
                            })
                            Text(
                                text = title,
                                color = MayasTheme.TextPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete_folder), color = MayasTheme.ErrorRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), selectedChatIds) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save), color = MayasTheme.GlowPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface
    )
}


@Composable
fun ChatSearchField(searchQuery: String, onSearchChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text(stringResource(R.string.search_chats_hint), color = MayasTheme.TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
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
                        contentDescription = stringResource(R.string.clear_search_action),
                        tint = MayasTheme.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp),
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
    onPinToSidebar: () -> Unit = {},
    isSelectionMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    massMuteActive: Boolean = false,
    onToggleMassMute: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    isArchiveMode: Boolean = false
) {
    var showActions by remember { mutableStateOf(false) }
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
            text = stringResource(R.string.selected_count, selectedCount),
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MayasTheme.TextPrimary
        )
        Box {
            IconButton(onClick = { showActions = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.selected_chat_actions),
                    tint = MayasTheme.TextPrimary
                )
            }
            DropdownMenu(
                expanded = showActions,
                onDismissRequest = { showActions = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pin_selected_to_sidebar)) },
                    leadingIcon = { Icon(Icons.Default.Dashboard, null, tint = MayasTheme.GlowPurple) },
                    onClick = { showActions = false; onPinToSidebar() }
                )
                if (onToggleArchive != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isArchiveMode) R.string.unarchive_selected_chats else R.string.archive_selected_chats
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isArchiveMode) Icons.Default.Unarchive else Icons.Default.Archive,
                                null,
                                tint = MayasTheme.GlowPurple
                            )
                        },
                        onClick = { showActions = false; onToggleArchive() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.toggle_pin_selected_chats)) },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) },
                    onClick = { showActions = false; onTogglePin() }
                )
                if (onToggleMute != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isSelectionMuted) R.string.unmute_selected_chats else R.string.mute_selected_chats
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isSelectionMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                null,
                                tint = MayasTheme.GlowPurple
                            )
                        },
                        onClick = { showActions = false; onToggleMute() }
                    )
                }
                if (onToggleMassMute != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (massMuteActive) R.string.unmute_all_chats else R.string.mute_all_chats
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (massMuteActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                null,
                                tint = MayasTheme.GlowPurple
                            )
                        },
                        onClick = { showActions = false; onToggleMassMute() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_selected_chats), color = MayasTheme.ErrorRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MayasTheme.ErrorRed) },
                    onClick = { showActions = false; onDeleteChats() }
                )
            }
        }
    }
}


@Composable
private fun ChannelTagInline(label: String, style: String) {
    val tint = when (style.lowercase(Locale.ROOT)) {
        "blue" -> Color(0xFF3994E8)
        "green" -> Color(0xFF20A46B)
        "gold" -> Color(0xFFC08A16)
        "red" -> Color(0xFFD64A58)
        else -> MayasTheme.GlowPurple
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        com.dan1eidtj.mayas.core_ui.emoji.EmojiGlyph(emoji = value, fontSize = fontSize, modifier = modifier)
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
    unreadGlowAlpha: () -> Float,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    isTyping: Boolean = false,
    draftText: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    avatarAtEnd: Boolean = false,
    compact: Boolean = false,
    isPinnedToSidebar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val itemBgColor = if (isSelected) {
        MayasTheme.Accent.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
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


                        val nameBrush = if (!isGroupChat) {
                            resolveNameBrush(userData?.get("nameColor") as? String ?: "gold", isPremium)
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

                        val partnerDisplayMode = when (userData?.get("statusTagDisplayMode") as? String) {
                            "STATUS_ONLY", "TAG_ONLY" -> userData["statusTagDisplayMode"] as String
                            else -> "BOTH"
                        }
                        val partnerStatus = userData?.get("emoji") as? String
                        if (partnerDisplayMode != "TAG_ONLY" && !partnerStatus.isNullOrBlank()) {
                            Spacer(Modifier.width(4.dp))
                            StatusBadge(value = partnerStatus, fontSize = 15.sp)
                        }
                        if (partnerDisplayMode != "STATUS_ONLY") {
                            val channelTag = userData?.get("activeChannelTag") as? Map<*, *>
                            val channelTagLabel = channelTag?.get("label") as? String
                            val channelTagName = channelTag?.get("name") as? String
                            if (!channelTagLabel.isNullOrBlank() && !channelTagName.isNullOrBlank()) {
                                Spacer(Modifier.width(4.dp))
                                ChannelTagInline(
                                    label = channelTagLabel,
                                    style = channelTag["style"] as? String ?: "violet"
                                )
                            }
                        }



                        val partnerVerification = userData?.get("verification")
                                as? com.dan1eidtj.data.VerificationInfo
                        if (partnerVerification?.verified == true) {
                            Spacer(Modifier.width(4.dp))
                            VerificationBadge(info = partnerVerification, size = 14.dp)
                        }

                        if (isPinned) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(R.string.pinned_indicator),
                                tint = MayasTheme.GlowPurple,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        if (isMuted) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = stringResource(R.string.muted_indicator),
                                tint = MayasTheme.TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (updatedAt > 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(updatedAt, stringResource(R.string.yesterday)),
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
                                text = stringResource(R.string.typing),
                                color = MayasTheme.GlowPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (draftText.isNotBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.draft_prefix),
                                color = MayasTheme.ErrorRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = draftText,
                                color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        com.dan1eidtj.mayas.core_ui.emoji.MayasText(
                            text = lastMsg.ifEmpty { stringResource(R.string.no_messages) },
                            modifier = Modifier.weight(1f),
                            color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedVisibility(
                        visible = unreadCount > 0,
                        enter = scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = scaleOut(targetScale = 0.4f) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MayasTheme.Accent)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            AnimatedContent(
                                targetState = unreadCount,
                                transitionSpec = {
                                    (slideInVertically { h -> h } + fadeIn())
                                        .togetherWith(slideOutVertically { h -> -h } + fadeOut())
                                },
                                label = "unreadCountBadge"
                            ) { count ->
                                Text(
                                    text = "$count",
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

        if (avatarAtEnd) {
            contentEl()
            avatarEl()
        } else {
            avatarEl()
            contentEl()
        }

    }
}

private const val BANNER_DECODE_WIDTH = 720
private const val BANNER_DECODE_HEIGHT = 360

private enum class BannerScrim { Bar, Drawer }

@Composable
private fun ProfileBanner(
    bannerUrl: String,
    bannerStyle: String,
    scrim: BannerScrim,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedUrl by androidx.compose.runtime.produceState<String?>(initialValue = null, bannerUrl) {
        value = if (bannerUrl.isBlank()) null else runCatching {
            com.dan1eidtj.mayas.storage.MediaFileCache.resolveModel(context, bannerUrl)
        }.getOrNull()
    }
    val surfaceColor = MayasTheme.Surface
    val baseBrush = remember(bannerStyle, surfaceColor) {
        when (bannerStyle) {
            "ocean" -> Brush.linearGradient(listOf(Color(0xFF075985), Color(0xFF06B6D4)))
            "sunset" -> Brush.linearGradient(listOf(Color(0xFF9D174D), Color(0xFFF97316)))
            "forest" -> Brush.linearGradient(listOf(Color(0xFF14532D), Color(0xFF10B981)))
            "night" -> Brush.linearGradient(listOf(Color(0xFF111827), Color(0xFF5B21B6)))
            "custom" -> Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF0F172A)))
            "none" -> Brush.linearGradient(listOf(surfaceColor, surfaceColor))
            else -> Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFFDB2777)))
        }
    }
    val scrimBrush = remember(scrim) {
        when (scrim) {
            BannerScrim.Bar -> Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.62f), Color.Black.copy(alpha = 0.22f))
            )
            BannerScrim.Drawer -> Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.62f))
            )
        }
    }
    Box(modifier = modifier.background(baseBrush)) {
        val url = resolvedUrl
        if (!url.isNullOrBlank()) {
            val request = remember(url) {
                coil.request.ImageRequest.Builder(context)
                    .data(url)
                    .size(BANNER_DECODE_WIDTH, BANNER_DECODE_HEIGHT)
                    .crossfade(true)
                    .build()
            }
            coil.compose.AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(scrimBrush))
    }
}

@Composable
private fun DrawerProfileHeader(
    profile: Map<String, Any?>,
    compact: Boolean,
    insetTop: Boolean,
    showBanner: Boolean,
    accountsExpanded: Boolean,
    onToggleAccounts: () -> Unit,
    onClick: () -> Unit
) {
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (accountsExpanded) 180f else 0f,
        label = "drawerAccountsArrow"
    )
    val bannerUrl = profile["bannerUrl"] as? String ?: ""
    val bannerStyle = profile["bannerStyle"] as? String ?: "none"
    val hasBanner = showBanner && (bannerUrl.isNotBlank() || bannerStyle != "none")
    val avatarSize = if (compact) 56.dp else 68.dp
    val statusEmoji = profile["emoji"] as? String
    val isPremium = profile["isPremium"] as? Boolean ?: false
    val nameBrush = resolveNameBrush(profile["nameColor"] as? String ?: "gold", isPremium)
    val verification = profile["verification"] as? com.dan1eidtj.data.VerificationInfo

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MayasTheme.Surface)
            .clickable(onClick = onClick)
    ) {
        if (hasBanner) {
            ProfileBanner(
                bannerUrl = bannerUrl,
                bannerStyle = bannerStyle,
                scrim = BannerScrim.Drawer,
                modifier = Modifier.matchParentSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (insetTop) Modifier.statusBarsPadding() else Modifier)
                .padding(
                    start = 20.dp,
                    end = 12.dp,
                    top = if (compact) 14.dp else 20.dp,
                    bottom = if (compact) 12.dp else 14.dp
                )
        ) {
            UserAvatarView(
                avatarUrl = profile["avatarUrl"] as? String,
                useCustomAvatar = profile["useCustomAvatar"] as? Boolean ?: false,
                profileIcon = profile["profileIcon"] as? String ?: "ghost",
                profileGlow = profile["profileGlow"] as? String ?: "purple",
                isPremium = isPremium,
                frameType = profile["avatarFrame"] as? String ?: "none",
                size = avatarSize
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile["name"] as? String ?: stringResource(R.string.me),
                            style = if (nameBrush != null) TextStyle(brush = nameBrush) else TextStyle(color = MayasTheme.TextPrimary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!statusEmoji.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            StatusBadge(value = statusEmoji, fontSize = 16.sp)
                        }
                        if (verification?.verified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(info = verification, size = 15.dp)
                        }
                    }
                    Text(
                        text = stringResource(
                            if (statusEmoji.isNullOrBlank()) R.string.set_emoji_status else R.string.open_profile
                        ),
                        color = MayasTheme.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleAccounts),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.chatlist_acc_title),
                        tint = MayasTheme.TextSecondary,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { rotationZ = arrowRotation }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomProfileBar(
    myUid: String,
    myProfileData: Map<String, Any?>,
    connectionLabel: () -> String,
    glowColor: Color,
    pulseScale: () -> Float,
    onOpenProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccounts: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    homeLayoutPrefs: HomeScreenLayoutPrefs = HomeScreenLayoutPrefs()
) {
    val isCompact = homeLayoutPrefs.compactBottomBar
    val bannerUrl = myProfileData["bannerUrl"] as? String ?: ""
    val bannerStyle = myProfileData["bannerStyle"] as? String ?: "none"
    val bannerPlacement = myProfileData["bannerPlacement"] as? String ?: "PROFILE_ONLY"
    val hasBanner = (bannerPlacement == "PROFILE_BOTTOM" || bannerPlacement == "ALL") &&
            (bannerUrl.isNotBlank() || bannerStyle != "none")
    val barHeight = if (isCompact) 60.dp else 82.dp
    val avatarSize = if (isCompact) 38.dp else 50.dp
    val horizontalPadding = if (isCompact) 10.dp else 16.dp
    val shape = remember { RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) }
    val outline = MayasTheme.Outline
    val borderBrush = remember(outline) {
        Brush.verticalGradient(colors = listOf(outline, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(shape)
            .background(MayasTheme.Surface.copy(alpha = 0.32f))
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        if (hasBanner) {
            ProfileBanner(
                bannerUrl = bannerUrl,
                bannerStyle = bannerStyle,
                scrim = BannerScrim.Bar,
                modifier = Modifier.matchParentSize()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .combinedClickable { onOpenProfile(myUid) }
            ) {
                UserAvatarView(
                    avatarUrl = myProfileData["avatarUrl"] as? String,
                    useCustomAvatar = myProfileData["useCustomAvatar"] as? Boolean ?: false,
                    profileIcon = myProfileData["profileIcon"] as? String ?: "ghost",
                    profileGlow = myProfileData["profileGlow"] as? String ?: "purple",
                    isPremium = myProfileData["isPremium"] as? Boolean ?: false,
                    frameType = myProfileData["avatarFrame"] as? String ?: "none",
                    size = avatarSize
                )

                Box(
                    modifier = Modifier
                        .size(if (isCompact) 10.dp else 13.dp)
                        .graphicsLayer {
                            val pulse = pulseScale()
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .clip(CircleShape)
                        .background(glowColor)
                        .align(Alignment.BottomEnd)
                        .border(1.5.dp, MayasTheme.Background, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))

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

                    val nameBrush = resolveNameBrush(myProfileData["nameColor"] as? String ?: "gold", isPremium)

                    Text(
                        text = myProfileData["name"] as? String ?: stringResource(com.dan1eidtj.mayas.ui.R.string.loading),
                        style = if (nameBrush != null) TextStyle(brush = nameBrush) else TextStyle(color = MayasTheme.TextPrimary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val myVerification = myProfileData["verification"]
                            as? com.dan1eidtj.data.VerificationInfo
                    if (myVerification?.verified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(info = myVerification, size = 14.dp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.chatlist_acc_title),
                        tint = MayasTheme.TextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenAccounts)
                            .padding(6.dp)
                            .size(14.dp)
                    )
                }
                if (homeLayoutPrefs.showBottomBarStatus) {
                    Spacer(modifier = Modifier.height(2.dp))
                    ConnectionLabel(
                        label = connectionLabel,
                        color = glowColor.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (homeLayoutPrefs.showBottomBarNotifications) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 34.dp else 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasBanner) Color.Black.copy(alpha = 0.35f)
                            else MayasTheme.SurfaceVariant.copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
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
                text = stringResource(R.string.new_chat),
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
                        Text(stringResource(R.string.create_group), color = MayasTheme.GlowPurple, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.create_channel), color = MayasTheme.GlowPurple, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = stringResource(R.string.search_by_username_or_phone),
                    color = MayasTheme.TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = onInputChange,
                    placeholder = { Text(stringResource(R.string.username_or_phone_placeholder), color = MayasTheme.TextSecondary.copy(alpha = 0.5f)) },
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
                        val user = foundUser ?: emptyMap()
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
                                Text(if (userUid == myUid) stringResource(R.string.it_is_you) else stringResource(R.string.write_message))
                            }
                        }
                    }

                    if (foundChannel != null) {
                        if (foundUser != null) Spacer(Modifier.height(12.dp))

                        val channel = foundChannel ?: emptyMap()
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
                                stringResource(R.string.channel_username_subscribers, channel["username"].toString(), formatCompactCount(subsCount)),
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
                                    Text(if (alreadyIn) stringResource(R.string.open) else stringResource(R.string.subscribe))
                                }
                            }
                        }
                    }

                    if (foundUser == null && foundChannel == null && searchInput.length >= 3) {
                        Text(stringResource(R.string.no_one_found), color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_action), color = MayasTheme.TextSecondary)
            }
        },
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, MayasTheme.Outline, RoundedCornerShape(20.dp))
    )
}


fun formatTimestamp(millis: Long, yesterdayLabel: String): String {
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
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> yesterdayLabel
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
            SimpleDateFormat("d MMM", Locale("ru")).format(date)
        }
        else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}

fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}