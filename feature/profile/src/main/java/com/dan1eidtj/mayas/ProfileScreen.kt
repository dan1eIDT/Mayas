/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@file:Suppress("DEPRECATION")

package com.dan1eidtj.mayas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.dan1eidtj.profile.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dan1eidtj.mayas.feature.ChatVM
import com.dan1eidtj.mayas.feature.formatCompactCount
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.AggregateSource
import com.dan1eidtj.mayas.core_ui.utils.formatLastSeen
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.FullScreenImageViewer
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.data.readBooleanPreference
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.data.Rank
import com.dan1eidtj.data.VerificationInfo
import com.dan1eidtj.mayas.core_ui.Screen
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core_ui.ui.components.AllProfileIcons
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationBadge
import com.dan1eidtj.mayas.core_ui.ui.components.AdminLevelBadge
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationInfoDialog
import com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush
import com.dan1eidtj.mayas.core_ui.utils.nameBrushOf
import com.dan1eidtj.mayas.feature.GroupMemberUi
import com.dan1eidtj.mayas.feature.Message
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.GroupMembersVM
import com.dan1eidtj.mayas.feature.BlockUserConfirmDialog
import com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListViewModel
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.MediaFileCache
import com.dan1eidtj.mayas.storage.ImageCompressor
import com.dan1eidtj.mayas.core.ui.components.ImageCropEditorDialog
import com.dan1eidtj.mayas.core.ui.components.ImageEditAspect
import com.dan1eidtj.mayas.core.ui.components.cleanupEditedImage
import com.dan1eidtj.mayas.storage.MediaKind
import com.google.firebase.auth.FirebaseAuth
import com.dan1eidtj.data.AppScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale


private data class ChannelTagUi(
    val channelId: String = "",
    val channelName: String = "",
    val name: String,
    val label: String,
    val style: String = "violet",
    val tagId: String = ""
)

private fun channelTagFrom(value: Any?, fallbackChannelId: String = "", fallbackChannelName: String = ""): ChannelTagUi? {
    val map = value as? Map<*, *> ?: return null
    val name = map["name"] as? String ?: return null
    val label = map["label"] as? String ?: return null
    if (name.isBlank() || label.isBlank()) return null
    return ChannelTagUi(
        channelId = map["channelId"] as? String ?: fallbackChannelId,
        channelName = map["channelName"] as? String ?: fallbackChannelName,
        name = name,
        label = label,
        style = (map["style"] as? String)?.lowercase(Locale.ROOT)?.takeIf {
            it in setOf("violet", "blue", "green", "gold", "red")
        } ?: "violet",
        tagId = (map["tagId"] as? String) ?: (map["id"] as? String).orEmpty()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(
    targetId: String?,
    isGroup: Boolean,
    vm: AuthVM,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (String, String?) -> Unit,
    onNavigateToProfile: (String, Boolean) -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToGroupMembers: (String) -> Unit = {},
) {
    val currentMyUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val finalId = targetId ?: currentMyUid
    val isMyProfile = !isGroup && (targetId == null || targetId == currentMyUid)
    var steamGameName by remember(finalId) { mutableStateOf<String?>(null) }
    var steamDisplayMode by remember(finalId) { mutableStateOf("HIDDEN") }

    LaunchedEffect(finalId, isGroup, steamDisplayMode) {
        if (isGroup || finalId.isBlank() || steamDisplayMode == "HIDDEN") {
            steamGameName = null
            return@LaunchedEffect
        }
        while (true) {
            steamGameName = try {
                BackendApi.steamCurrentGame(finalId)
            } catch (e: Exception) {
                Log.w("SteamStatus", "steamCurrentGame failed", e)
                null
            }
            delay(25_000L)
        }
    }
    val context = LocalContext.current
    val chatVM: ChatVM = viewModel()
    val chatListVM: ChatListViewModel = viewModel()

    val coroutineScope = rememberCoroutineScope()

    val loadingLabel = stringResource(com.dan1eidtj.mayas.ui.R.string.loading)
    val recentlyLabel = stringResource(com.dan1eidtj.chat.R.string.last_seen_recently)
    val channelFallbackName = stringResource(com.dan1eidtj.chat.R.string.channel_label)
    val groupFallbackName = stringResource(com.dan1eidtj.chat.R.string.group_fallback_name)
    val noNameLabel = stringResource(com.dan1eidtj.chats.R.string.no_name)
    val onlineLabel = stringResource(com.dan1eidtj.chats.R.string.online)
    val avatarUploadFailedText = stringResource(com.dan1eidtj.chat.R.string.error_avatar_upload_failed)
    val usernameTakenShortText = stringResource(R.string.username_taken_short)
    val usernameTooShortText = stringResource(R.string.username_too_short)
    val savedStatusText = stringResource(R.string.saved_status)
    val saveErrorGenericText = stringResource(com.dan1eidtj.chat.R.string.error_save_failed)
    val linkCopiedText = stringResource(R.string.link_copied)
    val mentionedUserNotFoundTemplate = stringResource(R.string.mentioned_user_not_found)
    val openProfileFailedText = stringResource(R.string.error_open_profile_failed)
    val userUnblockedText = stringResource(R.string.user_unblocked)
    val blockActionFailedText = stringResource(com.dan1eidtj.chat.R.string.block_action_failed)
    val genericErrorShortText = stringResource(com.dan1eidtj.auth.R.string.error_generic_short)
    val purchasedItemTemplate = stringResource(R.string.purchased_item)
    var name by remember { mutableStateOf(loadingLabel) }
    var username by remember { mutableStateOf("") }
    var additionalUsernames by remember { mutableStateOf<List<String>>(emptyList()) }
    var avatar by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var bannerStyle by remember { mutableStateOf("none") }
    var bannerPlacement by remember { mutableStateOf("PROFILE_ONLY") }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var profileIcon by remember { mutableStateOf("ghost") }
    var profileGlow by remember { mutableStateOf("purple") }
    var useCustomAvatar by remember { mutableStateOf(true) }
    var desc by remember { mutableStateOf("") }
    var emojiStatus by remember { mutableStateOf("") }
    var isOnline by remember { mutableStateOf(false) }
    var lastSeenText by remember { mutableStateOf(recentlyLabel) }
    var isPremium by remember { mutableStateOf(false) }
    var balance by remember { mutableIntStateOf(0) }
    var verifiedIcon by remember { mutableStateOf("verified") }
    var avatarFrame by remember { mutableStateOf("none") }
    var isInvisible by remember { mutableStateOf(false) }
    var nameColor by remember { mutableStateOf("gold") }
    var phone by remember { mutableStateOf("") }
    var verification by remember { mutableStateOf(VerificationInfo()) }
    var profileRank by remember { mutableStateOf(Rank.USER) }

    var isEditing by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf(true) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var chatType by remember { mutableStateOf("GROUP") }
    val isChannel = isGroup && chatType == "CHANNEL"

    var originalUsername by remember(finalId) { mutableStateOf("") }
    LaunchedEffect(username, isEditing) {
        if (!isEditing) originalUsername = username
    }

    LaunchedEffect(username) {
        if (isEditing && username != originalUsername && username.length >= 3) {
            isCheckingUsername = true
            if (isChannel) {
                chatVM.checkChannelUsername(username, excludeChatId = finalId) { available ->
                    isUsernameAvailable = available
                    isCheckingUsername = false
                }
            } else if (!isGroup) {
                vm.checkUsername(username) { available ->
                    isUsernameAvailable = available
                    isCheckingUsername = false
                }
            } else {
                isUsernameAvailable = true
                isCheckingUsername = false
            }
        } else {
            isUsernameAvailable = true
        }
    }

    var messagesSent by remember(finalId) { mutableStateOf<Int?>(null) }
    var groupCreatedAt by remember { mutableStateOf<Date?>(null) }
    var userCreatedAt by remember { mutableStateOf<Date?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var membersUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var adminIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var ownerId by remember { mutableStateOf("") }
    var channelIsPublic by remember { mutableStateOf(false) }
    var channelTag by remember(finalId) { mutableStateOf<ChannelTagUi?>(null) }
    var profileActiveChannelTag by remember(finalId) { mutableStateOf<ChannelTagUi?>(null) }
    var myActiveChannelTag by remember(currentMyUid) { mutableStateOf<ChannelTagUi?>(null) }
    var canManageChannelTagHere by remember(finalId, currentMyUid) { mutableStateOf(false) }
    var showChannelTagEditor by remember { mutableStateOf(false) }
    var isChannelTagBusy by remember { mutableStateOf(false) }
    var statusTagDisplayMode by remember(finalId) { mutableStateOf("BOTH") }
    var adsViewed by remember(finalId) { mutableLongStateOf(0L) }
    var showStats by remember { mutableStateOf(true) }
    var showRegistrationDate by remember { mutableStateOf(true) }
    var showMessageStats by remember { mutableStateOf(true) }
    var showAdStats by remember { mutableStateOf(true) }
    var showBalance by remember { mutableStateOf(true) }
    var imageToEditUri by remember { mutableStateOf<Uri?>(null) }
    var imageEditPurpose by remember { mutableStateOf("avatar") }
    var isSubscribing by remember { mutableStateOf(false) }
    val chatIdForTabs = remember(finalId, isGroup, currentMyUid) {
        if (finalId.isBlank() || currentMyUid.isBlank()) ""
        else if (isGroup) finalId
        else Screen.getChatId(currentMyUid, finalId)
    }
    var isLoadingProfileTabs by remember { mutableStateOf(false) }
    var profileMediaMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var profileLinkMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var profilePinnedMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    val showProfileTabs = chatIdForTabs.isNotBlank() && (isGroup || !isMyProfile)

    LaunchedEffect(chatIdForTabs, showProfileTabs) {
        if (!showProfileTabs) {
            isLoadingProfileTabs = false
            profileMediaMessages = emptyList()
            profileLinkMessages = emptyList()
            profilePinnedMessages = emptyList()
            return@LaunchedEffect
        }
        isLoadingProfileTabs = true
        profileMediaMessages = emptyList()
        profileLinkMessages = emptyList()
        profilePinnedMessages = emptyList()
        try {
            val (recent, pinned) = coroutineScope {
                val recentTask = async { chatVM.fetchRecentMessages(chatIdForTabs) }
                val pinnedTask = async { chatVM.fetchPinnedMessages(chatIdForTabs) }
                recentTask.await() to pinnedTask.await()
            }
            profileMediaMessages = recent.filter { !it.mediaUrl.isNullOrBlank() }
            profileLinkMessages = recent.filter { extractUrls(it.text).isNotEmpty() }
            profilePinnedMessages = pinned
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("ProfileScreen", "Не удалось загрузить вкладки профиля", error)
        } finally {
            isLoadingProfileTabs = false
        }
    }

    val isGroupAdmin =
        isGroup && currentMyUid.isNotBlank() && (currentMyUid in adminIds || currentMyUid == ownerId)
    val isGroupOwner = isGroup && currentMyUid == ownerId
    val canEdit = isMyProfile || isGroupAdmin

    var showGroupStatistics by remember { mutableStateOf(false) }
    var groupStatisticsRefreshKey by remember { mutableIntStateOf(0) }
    var groupStatisticsLoading by remember { mutableStateOf(false) }
    var groupStatisticsFailed by remember { mutableStateOf(false) }
    var groupStatisticsMessageCount by remember { mutableLongStateOf(0L) }
    var groupStatisticsMemberCount by remember { mutableIntStateOf(0) }
    var groupStatisticsUpdatedAt by remember { mutableStateOf<Date?>(null) }

    LaunchedEffect(finalId, currentMyUid, isGroup, showStats, showMessageStats) {
        if (isGroup || finalId.isBlank() || currentMyUid.isBlank() || !showStats || !showMessageStats) {
            messagesSent = null
            return@LaunchedEffect
        }
        try {
            val count = BackendApi.profileMessageStats(finalId)
            messagesSent = count?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("ProfileScreen", "Не удалось загрузить серверную статистику сообщений", error)
        }
    }

    LaunchedEffect(showGroupStatistics, groupStatisticsRefreshKey, finalId, isGroupAdmin, isGroup) {
        if (!showGroupStatistics || !isGroup || !isGroupAdmin || finalId.isBlank()) return@LaunchedEffect
        groupStatisticsLoading = true
        groupStatisticsFailed = false
        try {
            val chatRef = vm.db.collection("chats").document(finalId)
            val messagesRef = chatRef.collection("messages")
            val (chatSnapshot, counts) = coroutineScope {


                val chatSnapshotTask = async { chatRef.get().await() }
                val allCountTask = async {
                    messagesRef.count().get(AggregateSource.SERVER).await().count
                }
                val scheduledCountTask = async {
                    messagesRef.whereEqualTo("messageState", "SCHEDULED")
                        .count().get(AggregateSource.SERVER).await().count
                }
                val systemCountTask = async {
                    messagesRef.whereEqualTo("type", "SYSTEM")
                        .count().get(AggregateSource.SERVER).await().count
                }
                val scheduledSystemCountTask = async {
                    messagesRef.whereEqualTo("messageState", "SCHEDULED")
                        .whereEqualTo("type", "SYSTEM")
                        .count().get(AggregateSource.SERVER).await().count
                }
                val snapshot = chatSnapshotTask.await()
                val values = longArrayOf(
                    allCountTask.await(),
                    scheduledCountTask.await(),
                    systemCountTask.await(),
                    scheduledSystemCountTask.await()
                )
                snapshot to values
            }
            if (!chatSnapshot.exists()) throw IllegalStateException("CHAT_NOT_FOUND")
            val participants = (chatSnapshot.get("participants") as? List<*>)
                ?: (chatSnapshot.get("members") as? List<*>)
                ?: emptyList<Any>()
            val allCount = counts[0]
            val scheduledCount = counts[1]
            val systemCount = counts[2]
            val scheduledSystemCount = counts[3]
            groupStatisticsMessageCount =
                (allCount - scheduledCount - systemCount + scheduledSystemCount).coerceAtLeast(0L)
            groupStatisticsMemberCount = participants.distinct().size
            groupStatisticsUpdatedAt = (chatSnapshot.getTimestamp("updatedAt")
                ?: chatSnapshot.getTimestamp("createdAt"))?.toDate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ProfileScreen", "Не удалось загрузить статистику чата", e)
            groupStatisticsFailed = true
        } finally {
            groupStatisticsLoading = false
        }
    }


    var isBlockedByMe by remember(finalId) { mutableStateOf(false) }
    var isBlockActionLoading by remember(finalId) { mutableStateOf(false) }
    var showBlockConfirm by remember(finalId) { mutableStateOf(false) }
    DisposableEffect(finalId, currentMyUid, isGroup, isMyProfile) {
        if (isGroup || isMyProfile || finalId.isBlank() || currentMyUid.isBlank()) {
            isBlockedByMe = false
            onDispose { }
        } else {
            val registration = vm.db.collection("users").document(currentMyUid)
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        Log.e("ProfileScreen", "Не удалось обновить состояние блокировки", error)
                        return@addSnapshotListener
                    }
                    isBlockedByMe = (doc?.get("blocked") as? List<*>)?.contains(finalId) == true
                }
            onDispose { registration.remove() }
        }
    }

    fun openDirectChatFromProfile() {
        if (currentMyUid.isBlank() || finalId.isBlank() || currentMyUid == finalId) return
        chatListVM.openOrCreateDirectChat(
            myUid = currentMyUid,
            partnerUid = finalId,
            onError = { error ->
                val message = when (error.message) {
                    "blocked" -> context.getString(com.dan1eidtj.chat.R.string.message_send_blocked)
                    else -> context.getString(com.dan1eidtj.chats.R.string.something_went_wrong)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onReady = { chatId -> onNavigateToChat(chatId, null) }
        )
    }

    val handleBlockToggle: () -> Unit = {
        if (!isGroup && !isMyProfile && !isBlockActionLoading) {
            if (isBlockedByMe) {
                isBlockActionLoading = true
                chatVM.unblockUser(
                    currentMyUid,
                    finalId,
                    onSuccess = {
                        isBlockActionLoading = false
                        isBlockedByMe = false
                        Toast.makeText(context, userUnblockedText, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        isBlockActionLoading = false
                        Toast.makeText(context, blockActionFailedText, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                showBlockConfirm = true
            }
        }
    }

    DisposableEffect(currentMyUid) {
        if (currentMyUid.isBlank()) {
            myActiveChannelTag = null
            onDispose { }
        } else {
            val registration = vm.db.collection("users").document(currentMyUid)
                .addSnapshotListener { snapshot, _ ->
                    myActiveChannelTag = channelTagFrom(snapshot?.get("activeChannelTag"))
                }
            onDispose { registration.remove() }
        }
    }

    var availableChannelTags by remember(currentMyUid) { mutableStateOf<List<ChannelTagUi>>(emptyList()) }
    LaunchedEffect(isMyProfile, currentMyUid, myActiveChannelTag?.channelId) {
        if (!isMyProfile || currentMyUid.isBlank()) {
            availableChannelTags = emptyList()
            return@LaunchedEffect
        }
        val channelIds = runCatching { com.dan1eidtj.mayas.db.ChatRepository(context).loadCachedChats() }
            .getOrDefault(emptyList())
            .filter { it.chatType == "CHANNEL" }
            .map { it.chatId }
        val docs: List<com.google.firebase.firestore.DocumentSnapshot?> = coroutineScope {
            channelIds.map { id ->
                async {
                    runCatching { vm.db.collection("chats").document(id).get().await() }.getOrNull()
                }
            }.awaitAll()
        }
        availableChannelTags = docs.mapNotNull { doc: com.google.firebase.firestore.DocumentSnapshot? ->
            if (doc == null || !doc.exists()) return@mapNotNull null
            channelTagFrom(
                doc.get("channelTag"),
                fallbackChannelId = doc.id,
                fallbackChannelName = doc.getString("groupName").orEmpty()
            )
        }
    }

    LaunchedEffect(profileActiveChannelTag?.channelId, profileActiveChannelTag?.tagId) {
        val activeTag = profileActiveChannelTag
        if (activeTag == null || activeTag.channelId.isBlank()) return@LaunchedEffect
        val isStillCurrent = try {
            val source = vm.db.collection("chats").document(activeTag.channelId).get().await()
            val liveTag = source.get("channelTag") as? Map<*, *>
            if (!source.exists() || liveTag == null) {
                false
            } else if (activeTag.tagId.isNotBlank() && (liveTag["id"] as? String).orEmpty().isNotBlank()) {
                activeTag.tagId == liveTag["id"]
            } else {
                activeTag.name == liveTag["name"] && activeTag.label == liveTag["label"] && activeTag.style == (liveTag["style"] as? String)?.lowercase(Locale.ROOT)
            }
        } catch (_: Exception) {
            false
        }
        if (!isStillCurrent && profileActiveChannelTag?.channelId == activeTag.channelId) {
            profileActiveChannelTag = null
        }
    }

    var showImagePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    val applyNameColor: (String) -> Unit = { newColor ->
        val previous = nameColor
        nameColor = newColor
        coroutineScope.launch {
            try {
                BackendApi.setProfileCustomization(nameColor = newColor)
            } catch (e: Exception) {
                nameColor = previous
                Toast.makeText(context, genericErrorShortText, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val applyAvatarFrame: (String) -> Unit = { newFrame ->
        val previous = avatarFrame
        avatarFrame = newFrame
        coroutineScope.launch {
            try {
                BackendApi.setProfileCustomization(avatarFrame = newFrame)
            } catch (e: Exception) {
                avatarFrame = previous
                Toast.makeText(context, genericErrorShortText, Toast.LENGTH_SHORT).show()
            }
        }
    }
    var showGroupMembers by remember { mutableStateOf(false) }
    var fullScreenAvatarUrl by remember { mutableStateOf<String?>(null) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    val glowColor = getGlowColor(profileGlow)

    DisposableEffect(finalId) {
        val collection = if (isGroup) "chats" else "users"
        val reg =
            vm.db.collection(collection).document(finalId).addSnapshotListener { snapshot, _ ->
                snapshot?.data?.let { data ->
                    if (isGroup) {
                        additionalUsernames = emptyList()
                        bannerUrl = ""
                        bannerStyle = "none"
                        bannerPlacement = "PROFILE_ONLY"
                        val docType = data["type"] as? String ?: "GROUP"
                        name = data["groupName"] as? String
                            ?: if (docType == "CHANNEL") channelFallbackName else groupFallbackName
                        avatar = data["groupAvatar"] as? String ?: ""

                        desc = data["description"] as? String ?: ""
                        ownerId = data["ownerId"] as? String ?: ""
                        profileIcon = data["profileIcon"] as? String ?: "default"
                        profileGlow = data["profileGlow"] as? String ?: "purple"
                        emojiStatus =
                            data["emoji"] as? String ?: if (docType == "CHANNEL") "📢" else "👥"
                        groupCreatedAt = (data["createdAt"] as? Timestamp)?.toDate()
                        chatType = docType
                        username = data["username"] as? String ?: ""
                        adminIds = (data["admins"] as? List<String>)
                            ?: listOfNotNull(data["adminId"] as? String)
                        membersUids = (data["participants"] as? List<String>)
                            ?: (data["members"] as? List<String>) ?: emptyList()
                        channelIsPublic = data["isPublic"] as? Boolean ?: false
                        channelTag = if (docType == "CHANNEL") {
                            channelTagFrom(data["channelTag"], fallbackChannelId = finalId, fallbackChannelName = name)
                        } else null
                        @Suppress("UNCHECKED_CAST")
                        val adminPermissions = data["adminPermissions"] as? Map<String, Map<String, Any?>> ?: emptyMap()
                        canManageChannelTagHere = docType == "CHANNEL" && (
                                currentMyUid == ownerId || adminPermissions[currentMyUid]?.get("canEditInfo") == true
                                )
                        useCustomAvatar = avatar.isNotEmpty()


                        verification = if (docType == "CHANNEL")
                            VerificationInfo.fromMap(data)
                        else VerificationInfo()
                    } else {
                        statusTagDisplayMode = when (data["statusTagDisplayMode"] as? String) {
                            "STATUS_ONLY", "TAG_ONLY" -> data["statusTagDisplayMode"] as String
                            else -> "BOTH"
                        }
                        bannerUrl = data["bannerUrl"] as? String ?: ""
                        bannerStyle = data["bannerStyle"] as? String ?: "none"
                        bannerPlacement = data["bannerPlacement"] as? String ?: "PROFILE_ONLY"
                        val storedCreatedAt = (data["createdAt"] as? Timestamp)?.toDate()
                        val authCreatedAt = if (finalId == currentMyUid) {
                            vm.user?.metadata?.creationTimestamp?.takeIf { it > 0L }?.let { Date(it) }
                        } else null
                        userCreatedAt = storedCreatedAt ?: authCreatedAt
                        name = data["name"] as? String ?: noNameLabel
                        username = data["username"] as? String ?: ""
                        additionalUsernames = (data["additionalUsernames"] as? List<*>)
                            ?.filterIsInstance<String>()?.distinct().orEmpty()
                        profileIcon = data["profileIcon"] as? String ?: "ghost"
                        profileGlow = data["profileGlow"] as? String ?: "purple"
                        desc = data["description"] as? String ?: ""
                        emojiStatus = data["emojiStatus"] as? String ?: ""
                        isPremium = data["isPremium"] as? Boolean ?: false
                        balance = (data["balance"] as? Long)?.toInt() ?: 0
                        verifiedIcon = data["verifiedIcon"] as? String ?: "verified"
                        avatarFrame = data["avatarFrame"] as? String ?: "none"
                        isInvisible = data["isInvisible"] as? Boolean ?: false
                        nameColor = data["nameColor"] as? String ?: "gold"
                        adsViewed = ((data["adsViewed"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
                        showStats = readBooleanPreference(data["showStats"], defaultValue = true)
                        showRegistrationDate = readBooleanPreference(data["showRegistrationDate"], defaultValue = true)
                        showMessageStats = readBooleanPreference(data["showMessageStats"], defaultValue = true)
                        showAdStats = readBooleanPreference(data["showAdStats"], defaultValue = true)
                        steamDisplayMode = when (data["steamStatusDisplayMode"] as? String) {
                            "TEXT", "HIDDEN" -> data["steamStatusDisplayMode"] as String
                            else -> "CARD"
                        }
                        showBalance = readBooleanPreference(data["showBalance"], defaultValue = true)
                        profileActiveChannelTag = channelTagFrom(data["activeChannelTag"])
                        verification = VerificationInfo.fromMap(data)
                        profileRank = Rank.fromMap(data)
                        val lastSeenMode = data["privacy_last_seen"] as? String ?: "all"
                        val hiddenLastSeenFrom = (data["privacy_last_seen_hidden_from"] as? List<*>)
                            ?.filterIsInstance<String>()?.toSet().orEmpty()
                        val lastSeenAllowed = isMyProfile || when (lastSeenMode) {
                            "custom" -> currentMyUid !in hiddenLastSeenFrom
                            "all" -> true
                            else -> false
                        }
                        val photoAllowed = isMyProfile ||
                                (data["privacy_photo"] as? String ?: "all") == "all"

                        avatar = if (photoAllowed) data["avatarUrl"] as? String ?: "" else ""
                        useCustomAvatar = if (photoAllowed) {
                            data["useCustomAvatar"] as? Boolean ?: true
                        } else false

                        isOnline = if (lastSeenAllowed) isUserOnline(data) else false
                        val status = data["status"] as? Map<String, Any>
                        val lastSeen = (data["lastSeen"] as? Timestamp)
                            ?: (status?.get("lastSeen") as? Timestamp)
                        lastSeenText = when {
                            !lastSeenAllowed -> recentlyLabel
                            isOnline -> onlineLabel
                            else -> formatLastSeen(lastSeen)
                        }
                    }
                }
            }
        onDispose { reg.remove() }
    }
    LaunchedEffect(finalId, isGroup, isMyProfile) {
        if (isGroup || !isMyProfile || finalId.isBlank()) return@LaunchedEffect
        vm.getMyPhone { phone = it }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageEditPurpose = "avatar"
            imageToEditUri = it
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && isMyProfile && !isGroup && currentMyUid.isNotBlank()) {
            imageEditPurpose = "banner"
            imageToEditUri = uri
        }
    }

    imageToEditUri?.let { sourceUri ->
        val editingBanner = imageEditPurpose == "banner"
        ImageCropEditorDialog(
            sourceUri = sourceUri,
            aspect = if (editingBanner) ImageEditAspect.BANNER else ImageEditAspect.SQUARE,
            onDismiss = { imageToEditUri = null },
            onApply = { editedUri ->
                imageToEditUri = null
                if (editingBanner) {
                    coroutineScope.launch {
                        if (isUploadingBanner) return@launch
                        isUploadingBanner = true
                        try {
                            val bytes = ImageCompressor.compressProfileBanner(context, editedUri)
                            val key = B2MediaClient().uploadMedia(
                                kind = MediaKind.AVATAR,
                                ownerId = currentMyUid,
                                bytes = bytes,
                                contentType = "image/jpeg",
                                extension = "jpg"
                            )
                            vm.db.collection("users").document(currentMyUid).update(
                                mapOf("bannerUrl" to key, "bannerStyle" to "custom")
                            ).await()
                            bannerUrl = key
                            bannerStyle = "custom"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("ProfileScreen", "Не удалось загрузить баннер профиля", e)
                            Toast.makeText(context, context.getString(R.string.profile_banner_upload_failed), Toast.LENGTH_SHORT).show()
                        } finally {
                            cleanupEditedImage(context, editedUri)
                            isUploadingBanner = false
                        }
                    }
                } else {
                    coroutineScope.launch {
                        try {
                            val bytes = ImageCompressor.compressAvatar(context, editedUri)
                            val key = B2MediaClient().uploadMedia(
                                kind = MediaKind.AVATAR,
                                ownerId = currentMyUid,
                                bytes = bytes,
                                contentType = "image/jpeg",
                                extension = "jpg"
                            )
                            val field = if (isGroup) "groupAvatar" else "avatarUrl"
                            val updates: Map<String, Any> = if (isGroup) {
                                mapOf(field to key)
                            } else {
                                mapOf(field to key, "useCustomAvatar" to true)
                            }
                            vm.db.collection(if (isGroup) "chats" else "users").document(finalId)
                                .update(updates)
                                .addOnFailureListener { e ->
                                    Log.e("ProfileScreen", "Не удалось сохранить ключ аватара", e)
                                }
                            avatar = key
                            useCustomAvatar = true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("ProfileScreen", "Не удалось загрузить аватар", e)
                            Toast.makeText(context, avatarUploadFailedText, Toast.LENGTH_SHORT).show()
                        } finally {
                            cleanupEditedImage(context, editedUri)
                        }
                    }
                }
            }
        )
    }

    fun updateProfileBannerStyle(style: String) {
        if (!isMyProfile || isGroup || currentMyUid.isBlank()) return
        coroutineScope.launch {
            try {
                vm.db.collection("users").document(currentMyUid).update(
                    mapOf("bannerStyle" to style, "bannerUrl" to "")
                ).await()
                bannerStyle = style
                bannerUrl = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Не удалось обновить баннер профиля", e)
                Toast.makeText(context, context.getString(R.string.profile_banner_update_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateProfileBannerPlacement(placement: String) {
        if (!isMyProfile || isGroup || currentMyUid.isBlank()) return
        val allowed = setOf("PROFILE_ONLY", "PROFILE_SIDEBAR", "PROFILE_BOTTOM", "ALL")
        if (placement !in allowed) return
        coroutineScope.launch {
            try {
                vm.db.collection("users").document(currentMyUid)
                    .update("bannerPlacement", placement).await()
                bannerPlacement = placement
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Не удалось сохранить размещение баннера", e)
                Toast.makeText(context, context.getString(R.string.profile_banner_update_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        topBar = {}
    ) { padding ->
        val profileListState = rememberLazyListState()
        val profileHeaderState = rememberProfileHeaderState()
        val topBarHasBanner = !isGroup && (bannerUrl.isNotBlank() || bannerStyle != "none")
        val bannerExpanded by remember(profileHeaderState) { derivedStateOf { profileHeaderState.expand > 0.5f } }
        val floatingTopBar = !isEditing && useCustomAvatar && avatar.isNotBlank() && (!topBarHasBanner || bannerExpanded)
        val barDensity = LocalDensity.current
        val estimatedBarPx = WindowInsets.statusBars.getTop(barDensity) + with(barDensity) { 64.dp.roundToPx() }
        var standardBarPx by remember { mutableIntStateOf(estimatedBarPx) }
        val listTopPad by animateDpAsState(
            targetValue = if (floatingTopBar) 0.dp else with(barDensity) { standardBarPx.toDp() },
            animationSpec = tween(200),
            label = "profileListTop"
        )
        val floatingProgress = remember(profileHeaderState) { { profileHeaderState.progress } }
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            state = profileListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = listTopPad)
                .nestedScroll(profileHeaderState.connection),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ProfileHeader(
                    headerState = profileHeaderState,
                    bannerUrl = bannerUrl,
                    bannerStyle = bannerStyle,
                    bannerPlacement = bannerPlacement,
                    onBannerClick = { bannerLauncher.launch("image/*") },
                    onBannerStyleChange = { style -> updateProfileBannerStyle(style) },
                    onBannerPlacementChange = { placement -> updateProfileBannerPlacement(placement) },
                    isBannerUploading = isUploadingBanner,
                    balance = balance,
                    showBalance = showBalance,
                    name = name,
                    avatar = avatar,
                    profileIcon = profileIcon,
                    glowColor = glowColor,
                    isPremium = isPremium,
                    useCustomAvatar = useCustomAvatar,
                    avatarFrame = avatarFrame,
                    isEditing = isEditing,
                    emojiStatus = if (statusTagDisplayMode == "TAG_ONLY") "" else emojiStatus,
                    activeTag = profileActiveChannelTag,
                    statusTagDisplayMode = statusTagDisplayMode,
                    onStatusTagDisplayModeChange = { statusTagDisplayMode = it },
                    verifiedIcon = verifiedIcon,
                    isGroup = isGroup,
                    isChannel = isChannel,
                    membersCount = membersUids.size,
                    username = username,
                    additionalUsernames = additionalUsernames,
                    lastSeenText = lastSeenText,
                    isOnline = isOnline,
                    nameColor = nameColor,
                    isUsernameAvailable = isUsernameAvailable,
                    isCheckingUsername = isCheckingUsername,
                    onAvatarClick = { showImagePicker = true },
                    onAvatarView = {
                        val resolved = when {
                            avatar.startsWith("http") -> avatar
                            else -> null
                        }
                        if (resolved != null) {
                            fullScreenAvatarUrl = resolved
                        } else {
                            coroutineScope.launch {
                                fullScreenAvatarUrl = MediaFileCache.resolveModel(context, avatar)
                            }
                        }
                    },
                    onNameChange = { name = it },
                    onUsernameChange = { username = it },
                    onDescChange = { desc = it },
                    desc = desc,
                    onMembersClick = { showGroupMembers = true },
                    isMyProfile = isMyProfile,
                    phone = phone,
                    onPhoneChange = { phone = it },
                    verification = verification,
                    onVerificationClick = { showVerificationDialog = true },
                    rank = profileRank
                )
            }

            if (!isEditing && !isGroup && !isMyProfile && statusTagDisplayMode != "STATUS_ONLY") {
                profileActiveChannelTag?.let { activeTag ->
                    item {
                        ActiveChannelTagCard(
                            tag = activeTag,
                            canRemove = isMyProfile,
                            isBusy = isChannelTagBusy,
                            onRemove = {
                                if (!isChannelTagBusy) {
                                    isChannelTagBusy = true
                                    coroutineScope.launch {
                                        try {
                                            BackendApi.releaseChannelTag(activeTag.channelId)
                                            profileActiveChannelTag = null
                                            Toast.makeText(context, context.getString(R.string.channel_tag_removed), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.w("ProfileScreen", "Не удалось убрать тег канала", e)
                                            Toast.makeText(context, context.getString(R.string.channel_tag_action_failed), Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isChannelTagBusy = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (!isEditing) {
                steamGameName?.takeIf { steamDisplayMode != "HIDDEN" }?.let { gameName ->
                    item {
                        if (steamDisplayMode == "TEXT") {
                            Text(
                                stringResource(R.string.steam_playing_now, gameName),
                                color = MayasTheme.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B2838))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SportsEsports, null, tint = Color(0xFF66C0F4), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.steam_playing_now, gameName),
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                item {
                    ProfileInfoSection(
                        isGroup = isGroup, isChannel = isChannel, desc = desc, username = username,
                        membersCount = membersUids.size, messagesSent = messagesSent, adsViewed = adsViewed,
                        groupCreatedAt = groupCreatedAt, userCreatedAt = userCreatedAt, isGroupAdmin = isGroupAdmin,
                        showStats = isGroup || showStats,
                        showRegistrationDate = isGroup || showRegistrationDate,
                        showMessageStats = isGroup || showMessageStats,
                        showAdStats = isGroup || showAdStats,
                        onUsernameClick = {
                            val link = "@$username"
                            val clip = ClipData.newPlainText("MayasUN", link)
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
                        },
                        onMembersClick = { showGroupMembers = true },
                        onMentionClick = { mentionedUsername ->
                            vm.db.collection("users")
                                .whereEqualTo("username", mentionedUsername)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { result ->
                                    val doc = result.documents.firstOrNull()
                                    if (doc != null) {
                                        onNavigateToProfile(doc.id, false)
                                    } else {
                                        vm.db.collection("usernameClaims").document(mentionedUsername.lowercase().removePrefix("@")).get()
                                            .addOnSuccessListener { claim ->
                                                val uid = claim.getString("uid")
                                                val kind = claim.getString("kind")
                                                if (uid.isNullOrBlank() || kind !in setOf("MAIN", "ADDITIONAL")) {
                                                    Toast.makeText(context, String.format(mentionedUserNotFoundTemplate, mentionedUsername), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    onNavigateToProfile(uid, false)
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, openProfileFailedText, Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        openProfileFailedText,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    )
                }

                if (isGroup && isGroupAdmin) {
                    item {
                        OutlinedButton(
                            onClick = { showGroupStatistics = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.chat_statistics_open))
                        }
                    }
                }

                if (isChannel) {
                    item {
                        ChannelTagSection(
                            tag = channelTag,
                            canManage = canManageChannelTagHere,
                            isBusy = isChannelTagBusy,
                            activeTagChannelId = myActiveChannelTag?.channelId,
                            activeTagId = myActiveChannelTag?.tagId,
                            onEdit = { showChannelTagEditor = true },
                            onClaim = {
                                if (!isChannelTagBusy) {
                                    isChannelTagBusy = true
                                    coroutineScope.launch {
                                        try {
                                            BackendApi.claimChannelTag(finalId)
                                            Toast.makeText(context, context.getString(R.string.channel_tag_claimed), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.w("ProfileScreen", "Не удалось получить тег канала", e)
                                            Toast.makeText(context, context.getString(R.string.channel_tag_action_failed), Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isChannelTagBusy = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                if (!isGroup && !isMyProfile) {
                    item {
                        ProfileActionsRow(
                            isBlocked = isBlockedByMe,
                            isLoading = isBlockActionLoading,
                            onWriteClick = { openDirectChatFromProfile() },
                            onBlockClick = { handleBlockToggle() }
                        )
                    }
                }

                if (isChannel && currentMyUid.isNotBlank() && currentMyUid !in membersUids && currentMyUid != ownerId) {
                    if (channelIsPublic) {
                        item {
                            Button(
                                onClick = {
                                    isSubscribing = true
                                    chatVM.subscribeToPublicChannel(
                                        chatId = finalId,
                                        myUid = currentMyUid,
                                        onSuccess = { isSubscribing = false },
                                        onError = { isSubscribing = false }
                                    )
                                },
                                enabled = !isSubscribing,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
                            ) {
                                if (isSubscribing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.subscribe_channel), color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                } else if (isGroup) {
                    item {
                        Button(
                            onClick = {
                                if (isGroupOwner) {
                                    vm.db.collection("chats").document(finalId).delete()
                                        .addOnSuccessListener { onBack() }
                                } else {
                                    chatVM.leaveGroup(finalId, currentMyUid) { onBack() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MayasTheme.ErrorRed.copy(
                                    0.1f
                                )
                            )
                        ) {
                            Text(
                                if (isGroupOwner) {
                                    if (isChannel) stringResource(R.string.delete_channel) else stringResource(R.string.delete_group)
                                } else {
                                    if (isChannel) stringResource(R.string.leave_channel) else stringResource(R.string.leave_group)
                                },
                                color = MayasTheme.ErrorRed, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (showProfileTabs) {
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MayasTheme.Background,
                            contentColor = MayasTheme.Accent,
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(selectedTabIndex = selectedTab),
                                    color = MayasTheme.Accent
                                )
                            },
                            divider = {}
                        ) {
                            listOf(stringResource(R.string.media_label), stringResource(com.dan1eidtj.chat.R.string.links_label), stringResource(R.string.pinned_label)).forEachIndexed { i, label ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                                    Text(
                                        label, modifier = Modifier.padding(16.dp),
                                        color = if (selectedTab == i) MayasTheme.Accent else MayasTheme.TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        if (isLoadingProfileTabs) {
                            ProfileTabLoading()
                        } else {
                            when (selectedTab) {
                                0 -> ProfileMediaGrid(profileMediaMessages) { mediaKey ->
                                    coroutineScope.launch {
                                        fullScreenAvatarUrl = if (mediaKey.startsWith("http")) {
                                            mediaKey
                                        } else {
                                            MediaFileCache.resolveModel(context, mediaKey)
                                        }
                                    }
                                }
                                1 -> ProfileLinksList(profileLinkMessages)
                                else -> ProfilePinnedList(profilePinnedMessages) { msg -> onNavigateToChat(chatIdForTabs, msg.id) }
                            }
                        }
                    }
                }

            } else {

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(stringResource(R.string.color_scheme_section))
                        ColorPicker(
                            profileGlow,
                            isPremium,
                            { profileGlow = it },
                            onNavigateToPremium
                        )
                    }
                }

                if (isGroup) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle(stringResource(R.string.group_icon_section))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MayasTheme.Surface)
                                    .clickable { showIconPicker = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatarView(
                                    avatarUrl = null,
                                    useCustomAvatar = false,
                                    profileIcon = profileIcon,
                                    profileGlow = profileGlow,
                                    isPremium = false,
                                    frameType = "none",
                                    size = 48.dp
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.group_icon_label),
                                        color = MayasTheme.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        stringResource(R.string.tap_to_select),
                                        color = MayasTheme.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MayasTheme.TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                if (!isGroup) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle("MAYAS+")
                            PremiumSectionCollapsible(
                                isPremium = isPremium,
                                avatarFrame = avatarFrame,
                                nameColor = nameColor,
                                onNavigateToPremium = onNavigateToPremium,
                                onFrameSelect = { newFrame ->
                                    if (isPremium) applyAvatarFrame(newFrame) else onNavigateToPremium()
                                },
                                onNameColorSelect = { newColor ->
                                    if (isPremium) applyNameColor(newColor) else onNavigateToPremium()
                                }
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { if (!floatingTopBar && it.height > 0) standardBarPx = it.height }
        ) {
            ProfileTopBar(
                isMyProfile = isMyProfile,
                isEditing = isEditing,
                canEdit = canEdit,
                onBack = onBack,
                onShopClick = { showShop = true },
                username = username,
                isGroup = isGroup,
                onSettingsClick = onNavigateToSettings,
                onCustomizationClick = onNavigateToCustomization,
                onCreditsClick = onNavigateToCredits,
                onWriteMessageClick = { openDirectChatFromProfile() },
                onMembersClick = { onNavigateToGroupMembers(finalId) },
                onCopyUsernameClick = {
                    if (username.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("username", "@$username"))
                        Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
                    }
                },
                isBlocked = isBlockedByMe,
                isBlockActionLoading = isBlockActionLoading,
                onBlockClick = { handleBlockToggle() },
                onEditClick = {
                    if (isEditing) {
                        if (!isUsernameAvailable) {
                            Toast.makeText(context, usernameTakenShortText, Toast.LENGTH_SHORT)
                                .show()
                            return@ProfileTopBar
                        }
                        if (isMyProfile && !isGroup && username.isBlank()) {
                            Toast.makeText(context, usernameTooShortText, Toast.LENGTH_SHORT).show()
                            return@ProfileTopBar
                        }
                        if (username.length < 5 && (!isGroup || isChannel) && username.isNotBlank()) {
                            Toast.makeText(context, usernameTooShortText, Toast.LENGTH_SHORT)
                                .show()
                            return@ProfileTopBar
                        }

                        val cleanChannelUsername = username.lowercase().trim().replace(" ", "").removePrefix("@")
                        val updates = if (isChannel) {
                            mapOf(
                                "groupName" to name,
                                "description" to desc,
                                "profileIcon" to profileIcon,
                                "profileGlow" to profileGlow
                            )
                        } else if (isGroup) {
                            mapOf(
                                "groupName" to name,
                                "description" to desc,
                                "profileIcon" to profileIcon,
                                "profileGlow" to profileGlow
                            )
                        } else {
                            mapOf(
                                "name" to name,
                                "username" to username.lowercase().trim().replace(" ", ""),
                                "description" to desc,
                                "profileIcon" to profileIcon,
                                "profileGlow" to profileGlow,
                                "useCustomAvatar" to useCustomAvatar,
                                "statusTagDisplayMode" to statusTagDisplayMode
                            )
                        }
                        val collection = if (isGroup) "chats" else "users"
                        val cleanOwnUsername = username.lowercase().trim().replace(" ", "")
                        val finalUpdates = if (isMyProfile && !isGroup) updates - "username" else updates
                        val appContext = context.applicationContext
                        AppScope.scope.launch {
                            try {
                                if (isChannel) {
                                    BackendApi.changeChannelUsername(finalId, cleanChannelUsername, cleanChannelUsername.isNotBlank())
                                } else if (isMyProfile && !isGroup) {

                                    BackendApi.changeMainUsername(cleanOwnUsername)
                                }
                                vm.db.collection(collection).document(finalId).update(finalUpdates).await()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(appContext, savedStatusText, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("ProfileScreen", "Не удалось сохранить изменения профиля", e)
                                val userMessage = when (e.message?.substringBefore(' ')) {
                                    "USERNAME_TAKEN" -> usernameTakenShortText
                                    "INVALID_USERNAME" -> "Username: 5–32 символа, латинские буквы, цифры и _"
                                    else -> saveErrorGenericText
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(appContext, userMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }



                        if (isMyProfile) {
                            vm.updatePhoneNumber(
                                rawPhone = phone,
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    isEditing = !isEditing
                },
                floatingProgress = if (floatingTopBar) floatingProgress else null
            )
        }
        }

    }

    if (showShop) {
        ShopDialog(
            balance = balance, ownedItems = vm.ownedItems, onDismiss = { showShop = false },
            onBuyItem = { id, price, itemName ->
                vm.buyItem(
                    id, price,
                    onSuccess = {
                        Toast.makeText(context, String.format(purchasedItemTemplate, itemName), Toast.LENGTH_SHORT).show()
                    },
                    onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                )
            },
            onSelectItem = { id, type ->
                vm.useItem(id, type)
            },
            currentEmoji = emojiStatus, messageStyle = vm.userData["messageStyle"] ?: "",
            currentFont = vm.userData["fontId"] ?: "",
            currentEffect = vm.userData["effectId"] ?: "",
            isPurchaseInProgress = vm.isPurchaseInProgress
        )
    }

    if (showImagePicker) {
        ImagePickerDialog(
            isGroup = isGroup,
            onDismiss = {
                showImagePicker = false
            },
            onGallery = {
                showImagePicker = false
                launcher.launch("image/*")
            },
            onDelete = {
                showImagePicker = false

                val updates = if (isGroup) {
                    mapOf(
                        "groupAvatar" to "",
                        "useCustomAvatar" to false
                    )
                } else {
                    mapOf(
                        "avatarUrl" to "",
                        "useCustomAvatar" to false
                    )
                }

                vm.db.collection(if (isGroup) "chats" else "users")
                    .document(finalId)
                    .update(updates)

                avatar = ""
                useCustomAvatar = false
            },
            onSystemIcon = {
                showImagePicker = false
                showIconPicker = true
            }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            icons = AllProfileIcons,
            onDismiss = { showIconPicker = false },
            onSelect = { selectedIcon ->
                showIconPicker = false
                profileIcon = selectedIcon
                useCustomAvatar = false

                vm.db.collection(if (isGroup) "chats" else "users")
                    .document(finalId)
                    .update(
                        mapOf(
                            "profileIcon" to selectedIcon,
                            "useCustomAvatar" to false
                        )
                    )
            }
        )
    }


    if (showGroupMembers && isGroup) {
        GroupMembersBottomSheet(
            chatId = finalId,
            onDismiss = { showGroupMembers = false },
            onOpenProfile = onNavigateToProfile
        )
    }



    fullScreenAvatarUrl?.let { url ->
        FullScreenImageViewer(
            imageUrl = url,
            onDismiss = { fullScreenAvatarUrl = null }
        )
    }

    if (showGroupStatistics && isGroup && isGroupAdmin) {
        AlertDialog(
            onDismissRequest = { if (!groupStatisticsLoading) showGroupStatistics = false },
            containerColor = MayasTheme.Surface,
            title = { Text(stringResource(R.string.chat_statistics_title), color = MayasTheme.TextPrimary) },
            text = {
                when {
                    groupStatisticsLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MayasTheme.Accent)
                    }
                    groupStatisticsFailed -> Text(
                        stringResource(R.string.chat_statistics_error),
                        color = MayasTheme.TextSecondary
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatisticsValueRow(
                            label = stringResource(R.string.chat_statistics_participants),
                            value = groupStatisticsMemberCount.toString()
                        )
                        StatisticsValueRow(
                            label = stringResource(R.string.chat_statistics_messages),
                            value = groupStatisticsMessageCount.toString()
                        )
                        StatisticsValueRow(
                            label = stringResource(R.string.chat_statistics_updated),
                            value = groupStatisticsUpdatedAt?.let {
                                SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(it)
                            } ?: stringResource(R.string.chat_statistics_no_activity)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (groupStatisticsFailed) groupStatisticsRefreshKey++ else showGroupStatistics = false
                }) {
                    Text(
                        stringResource(if (groupStatisticsFailed) R.string.chat_statistics_retry else R.string.close),
                        color = MayasTheme.Accent
                    )
                }
            }
        )
    }

    if (showChannelTagEditor && canManageChannelTagHere && isChannel) {
        EditChannelTagDialog(
            initial = channelTag,
            isSaving = isChannelTagBusy,
            onDismiss = { if (!isChannelTagBusy) showChannelTagEditor = false },
            onSave = { tagName, tagLabel, tagStyle ->
                if (!isChannelTagBusy) {
                    isChannelTagBusy = true
                    coroutineScope.launch {
                        try {
                            BackendApi.setChannelTag(finalId, tagName, tagLabel, tagStyle)
                            channelTag = if (tagName.isBlank() && tagLabel.isBlank()) null else ChannelTagUi(
                                channelId = finalId,
                                channelName = name,
                                name = tagName.trim(),
                                label = tagLabel.trim(),
                                style = tagStyle
                            )
                            showChannelTagEditor = false
                            Toast.makeText(context, context.getString(R.string.channel_tag_saved), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.w("ProfileScreen", "Не удалось сохранить тег канала", e)
                            Toast.makeText(context, context.getString(R.string.channel_tag_action_failed), Toast.LENGTH_SHORT).show()
                        } finally {
                            isChannelTagBusy = false
                        }
                    }
                }
            }
        )
    }

    if (showVerificationDialog) {
        VerificationInfoDialog(
            info = verification,
            onDismiss = { showVerificationDialog = false }
        )
    }

    if (showBlockConfirm) {
        val userBlockedText = stringResource(com.dan1eidtj.chat.R.string.user_blocked)
        BlockUserConfirmDialog(
            onConfirm = {
                showBlockConfirm = false
                isBlockActionLoading = true
                chatVM.blockUser(
                    currentMyUid,
                    finalId,
                    onSuccess = {
                        isBlockActionLoading = false
                        isBlockedByMe = true
                        Toast.makeText(context, userBlockedText, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        isBlockActionLoading = false
                        Toast.makeText(context, blockActionFailedText, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDismiss = { showBlockConfirm = false }
        )
    }
}

@Composable
private fun StatisticsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MayasTheme.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileHeader(
    name: String, avatar: String, profileIcon: String, glowColor: Color,
    isPremium: Boolean, useCustomAvatar: Boolean, avatarFrame: String,
    isEditing: Boolean, emojiStatus: String,
    statusTagDisplayMode: String = "BOTH",
    onStatusTagDisplayModeChange: (String) -> Unit = {},
    verifiedIcon: String,
    isGroup: Boolean, isChannel: Boolean = false, membersCount: Int, username: String,
    additionalUsernames: List<String> = emptyList(),
    lastSeenText: String, isOnline: Boolean, nameColor: String = "gold",
    isUsernameAvailable: Boolean = true, isCheckingUsername: Boolean = false,
    onAvatarClick: () -> Unit, onAvatarView: () -> Unit = {}, onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit = {},
    onDescChange: (String) -> Unit, desc: String,
    onMembersClick: () -> Unit = {},
    isMyProfile: Boolean = false,
    phone: String = "",
    onPhoneChange: (String) -> Unit = {},
    verification: VerificationInfo = VerificationInfo(),
    onVerificationClick: () -> Unit = {},
    rank: Rank = Rank.USER,
    headerState: ProfileHeaderState? = null,
    bannerUrl: String = "",
    bannerStyle: String = "none",
    bannerPlacement: String = "PROFILE_ONLY",
    onBannerClick: () -> Unit = {},
    onBannerStyleChange: (String) -> Unit = {},
    onBannerPlacementChange: (String) -> Unit = {},
    isBannerUploading: Boolean = false,
    balance: Int = 0,
    showBalance: Boolean = true,
    activeTag: ChannelTagUi? = null
) {
    val nameBrush = nameBrushOf(nameColor)

    val hasVisibleBanner = !isGroup && (bannerUrl.isNotBlank() || bannerStyle != "none")
    val avatarInBanner = hasVisibleBanner && !isEditing
    val showBigPhoto = headerState != null && !isEditing && useCustomAvatar && avatar.isNotBlank() && !hasVisibleBanner
    val glowName = glowNameOf(glowColor)
    if (headerState != null) {
        ConfigureProfileHeaderMotion(
            state = headerState,
            bigPhoto = showBigPhoto,
            banner = avatarInBanner,
            expandable = useCustomAvatar && avatar.isNotBlank()
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(
            top = if (showBigPhoto || avatarInBanner) 0.dp else 20.dp,
            bottom = 8.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isGroup && (bannerUrl.isNotBlank() || bannerStyle != "none" || (isEditing && isMyProfile))) {
            if (avatarInBanner) {
                ProfileBannerWithAvatar(
                    state = headerState,
                    bannerUrl = bannerUrl,
                    bannerStyle = bannerStyle,
                    avatar = avatar,
                    useCustomAvatar = useCustomAvatar,
                    profileIcon = profileIcon,
                    glowName = glowName,
                    isPremium = isPremium,
                    avatarFrame = avatarFrame,
                    onAvatarClick = if (useCustomAvatar && avatar.isNotBlank()) onAvatarView else null,
                    expandable = useCustomAvatar && avatar.isNotBlank()
                )
            } else {
                ProfileBannerEditCard(
                    bannerUrl = bannerUrl,
                    bannerStyle = bannerStyle,
                    showLabel = isEditing && isMyProfile
                )
            }
            if (isEditing && isMyProfile) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = bannerStyle == "aurora" && bannerUrl.isBlank(),
                        onClick = { onBannerStyleChange("aurora") },
                        label = { Text(stringResource(R.string.profile_banner_aurora)) }
                    )
                    FilterChip(
                        selected = bannerStyle == "ocean" && bannerUrl.isBlank(),
                        onClick = { onBannerStyleChange("ocean") },
                        label = { Text(stringResource(R.string.profile_banner_ocean)) }
                    )
                    FilterChip(
                        selected = bannerStyle == "sunset" && bannerUrl.isBlank(),
                        onClick = { onBannerStyleChange("sunset") },
                        label = { Text(stringResource(R.string.profile_banner_sunset)) }
                    )
                    FilterChip(
                        selected = bannerStyle == "forest" && bannerUrl.isBlank(),
                        onClick = { onBannerStyleChange("forest") },
                        label = { Text(stringResource(R.string.profile_banner_forest)) }
                    )
                    FilterChip(
                        selected = bannerStyle == "night" && bannerUrl.isBlank(),
                        onClick = { onBannerStyleChange("night") },
                        label = { Text(stringResource(R.string.profile_banner_night)) }
                    )
                }
                Text(
                    text = stringResource(R.string.profile_banner_placement),
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = bannerPlacement == "PROFILE_ONLY",
                        onClick = { onBannerPlacementChange("PROFILE_ONLY") },
                        label = { Text(stringResource(R.string.profile_banner_profile_only)) }
                    )
                    FilterChip(
                        selected = bannerPlacement == "PROFILE_SIDEBAR",
                        onClick = { onBannerPlacementChange("PROFILE_SIDEBAR") },
                        label = { Text(stringResource(R.string.profile_banner_profile_sidebar)) }
                    )
                    FilterChip(
                        selected = bannerPlacement == "PROFILE_BOTTOM",
                        onClick = { onBannerPlacementChange("PROFILE_BOTTOM") },
                        label = { Text(stringResource(R.string.profile_banner_profile_bottom)) }
                    )
                    FilterChip(
                        selected = bannerPlacement == "ALL",
                        onClick = { onBannerPlacementChange("ALL") },
                        label = { Text(stringResource(R.string.profile_banner_all)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBannerClick,
                        enabled = !isBannerUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBannerUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.profile_banner_uploading))
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.profile_banner_choose_image))
                        }
                    }
                    TextButton(
                        onClick = { onBannerStyleChange("none") },
                        enabled = !isBannerUploading && (bannerStyle != "none" || bannerUrl.isNotBlank())
                    ) {
                        Text(stringResource(R.string.profile_banner_reset))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (showBigPhoto && headerState != null) {
            ProfileBigPhoto(
                state = headerState,
                avatar = avatar,
                useCustomAvatar = useCustomAvatar,
                profileIcon = profileIcon,
                glowName = glowName,
                isPremium = isPremium,
                avatarFrame = avatarFrame,
                onClick = onAvatarView
            )
        } else if (!avatarInBanner) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        if (isEditing) {
                            onAvatarClick()
                        } else if (useCustomAvatar && avatar.isNotBlank()) {
                            onAvatarView()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                UserAvatarView(
                    avatarUrl = avatar,
                    useCustomAvatar = useCustomAvatar,
                    profileIcon = profileIcon,
                    profileGlow = when (glowColor) {
                        MayasTheme.GlowPink -> "pink"
                        MayasTheme.GlowBlue -> "blue"
                        MayasTheme.GlowGreen -> "green"
                        MayasTheme.GlowGold -> "gold"
                        MayasTheme.GlowRed -> "red"
                        MayasTheme.GlowOrange -> "orange"
                        MayasTheme.GlowCyan -> "cyan"
                        MayasTheme.GlowMint -> "mint"
                        MayasTheme.GlowIndigo -> "indigo"
                        MayasTheme.GlowLime -> "lime"
                        MayasTheme.GlowRose -> "rose"
                        MayasTheme.GlowAmber -> "amber"
                        MayasTheme.GlowSky -> "sky"
                        MayasTheme.GlowWhite -> "white"
                        else -> "purple"
                    },
                    isPremium = isPremium,
                    frameType = avatarFrame,
                    size = 120.dp
                )
                if (isEditing) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        val balanceChip: @Composable () -> Unit = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MayasTheme.GlowGold.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, MayasTheme.GlowGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🪙", fontSize = 14.sp)
                    Text(
                        "$balance",
                        color = MayasTheme.GlowGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (isMyProfile && showBalance && !isEditing && !showBigPhoto) {
            balanceChip()
        }

        if (isEditing) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Секция "Имя и юзернейм" — сгруппированы в одной карточке,
                // как в Telegram (там это первый экран редактора профиля).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayasTheme.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = onNameChange,
                        label = { Text(stringResource(com.dan1eidtj.auth.R.string.name_label), fontSize = 12.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MayasTheme.Accent,
                            unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.25f),
                            focusedLabelColor = MayasTheme.Accent,
                            unfocusedLabelColor = MayasTheme.TextSecondary,
                            focusedTextColor = MayasTheme.TextPrimary,
                            unfocusedTextColor = MayasTheme.TextPrimary,
                            cursorColor = MayasTheme.Accent
                        )
                    )
                    if (!isGroup || isChannel) {
                        OutlinedTextField(
                            value = username, onValueChange = onUsernameChange,
                            label = {
                                Text(
                                    if (isChannel) stringResource(R.string.channel_username_label) else stringResource(R.string.username_at_label),
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = !isUsernameAvailable,
                            trailingIcon = {
                                if (isCheckingUsername) {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MayasTheme.Accent
                                    )
                                } else if (username.length >= 3) {
                                    Icon(
                                        if (isUsernameAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                                        null,
                                        tint = if (isUsernameAvailable) MayasTheme.Success else MayasTheme.ErrorRed
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    if (!isUsernameAvailable) stringResource(R.string.username_already_taken_ru)
                                    else stringResource(R.string.username_allowed_chars),
                                    color = if (!isUsernameAvailable) MayasTheme.ErrorRed else MayasTheme.TextSecondary,
                                    fontSize = 11.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isUsernameAvailable) MayasTheme.Accent else MayasTheme.ErrorRed,
                                unfocusedBorderColor = if (isUsernameAvailable) MayasTheme.TextSecondary.copy(
                                    alpha = 0.25f
                                ) else MayasTheme.ErrorRed,
                                focusedLabelColor = if (isUsernameAvailable) MayasTheme.Accent else MayasTheme.ErrorRed,
                                unfocusedLabelColor = MayasTheme.TextSecondary,
                                focusedTextColor = MayasTheme.TextPrimary,
                                unfocusedTextColor = MayasTheme.TextPrimary,
                                cursorColor = MayasTheme.Accent
                            )
                        )
                    }
                }

                if (!isGroup) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MayasTheme.Surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.status_tag_display_mode), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("BOTH", "STATUS_ONLY", "TAG_ONLY").forEach { mode ->
                                FilterChip(
                                    selected = statusTagDisplayMode == mode,
                                    onClick = { onStatusTagDisplayModeChange(mode) },
                                    label = { Text(stringResource(when (mode) {
                                        "STATUS_ONLY" -> R.string.status_tag_status_only
                                        "TAG_ONLY" -> R.string.status_tag_tag_only
                                        else -> R.string.status_tag_both
                                    })) }
                                )
                            }
                        }
                        Text(stringResource(R.string.status_tag_display_note), color = MayasTheme.TextSecondary, fontSize = 11.sp)
                    }
                }

                // Секция "О себе" — своя карточка, как Bio в Telegram (текст-подсказка
                // под полем, а не внутри общего блока с именем).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MayasTheme.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = desc, onValueChange = onDescChange,
                        label = {
                            Text(
                                if (isChannel) stringResource(R.string.channel_description_label) else if (isGroup) stringResource(R.string.group_description_label) else stringResource(R.string.about_me_label),
                                fontSize = 12.sp
                            )
                        },
                        minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MayasTheme.Accent,
                            unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.25f),
                            focusedLabelColor = MayasTheme.Accent,
                            unfocusedLabelColor = MayasTheme.TextSecondary,
                            focusedTextColor = MayasTheme.TextPrimary,
                            unfocusedTextColor = MayasTheme.TextPrimary,
                            cursorColor = MayasTheme.Accent
                        )
                    )
                    Text(
                        if (isGroup) stringResource(R.string.visible_to_all_members)
                        else stringResource(R.string.about_me_hint),
                        color = MayasTheme.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Секция "Номер телефона" — отдельная карточка, только для своего профиля.
                if (isMyProfile && !isGroup) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MayasTheme.Surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone, onValueChange = onPhoneChange,
                            label = { Text(stringResource(R.string.phone_number_label), fontSize = 12.sp) },
                            placeholder = { Text(stringResource(R.string.phone_placeholder_example)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MayasTheme.Accent,
                                unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.25f),
                                focusedLabelColor = MayasTheme.Accent,
                                unfocusedLabelColor = MayasTheme.TextSecondary,
                                focusedTextColor = MayasTheme.TextPrimary,
                                unfocusedTextColor = MayasTheme.TextPrimary,
                                cursorColor = MayasTheme.Accent
                            )
                        )
                        Text(
                            stringResource(R.string.phone_number_hint),
                            color = MayasTheme.TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                if (isGroup) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MayasTheme.Surface)
                            .clickable { onMembersClick() }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Group,
                            null,
                            tint = MayasTheme.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isChannel) "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.subscribers) else "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.participants_label),
                                color = MayasTheme.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (isChannel) stringResource(R.string.manage_channel_subscribers) else stringResource(R.string.manage_group_members),
                                color = MayasTheme.TextSecondary, fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MayasTheme.TextSecondary)
                    }
                }
            }
        } else {
          Column(
            modifier = if (showBigPhoto && headerState != null) Modifier.fillMaxWidth().photoNameOverlay(headerState) else Modifier.fillMaxWidth(),
            horizontalAlignment = if (showBigPhoto) Alignment.Start else Alignment.CenterHorizontally
          ) {
            val overlayState = if (showBigPhoto) headerState else null
            Row(modifier = Modifier.photoNameAlign(overlayState), verticalAlignment = Alignment.CenterVertically) {
                if (isPremium) {
                    Text(
                        name, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        style = TextStyle(brush = nameBrush)
                    )
                } else {
                    Text(
                        name,
                        color = MayasTheme.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeTag != null && !isGroup && statusTagDisplayMode != "STATUS_ONLY",
                    enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                        androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220), initialScale = 0.7f),
                    exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) +
                        androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(150), targetScale = 0.7f)
                ) {
                    var shownTag by remember { mutableStateOf(activeTag) }
                    if (activeTag != null) shownTag = activeTag
                    shownTag?.let { tag ->
                        Row {
                            Spacer(Modifier.width(8.dp))
                            ChannelTagBadge(label = tag.label, style = tag.style)
                        }
                    }
                }
                if (verification.verified) {
                    Spacer(Modifier.width(6.dp))
                    VerificationBadge(
                        info = verification,
                        size = 20.dp,
                        onClick = onVerificationClick
                    )
                }
                // Глобальный ранг (модератор/админ/овнер) — отдельно от верификации,
                // ничего не рисуется для обычного юзера (rank == USER).
                if (rank != Rank.USER) {
                    Spacer(Modifier.width(6.dp))
                    AdminLevelBadge(rank = rank, size = 20.dp)
                }
                if (emojiStatus.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    StatusBadge(value = emojiStatus, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier.photoNameAlign(overlayState),
                text = when {
                    isChannel -> "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.subscribers)
                    isGroup -> "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.participants_label)
                    username.isNotEmpty() -> "@$username"
                    else -> lastSeenText
                },
                color = if (isOnline && !isGroup) MayasTheme.Accent else MayasTheme.TextSecondary,
                fontSize = 14.sp
            )
            val aliasText = additionalUsernames.filter { it.isNotBlank() && !it.equals(username, ignoreCase = true) }
                .distinct().joinToString(", ") { "@$it" }
            if (!isGroup && aliasText.isNotBlank()) {
                Text(
                    stringResource(R.string.profile_additional_usernames, aliasText),
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.photoNameAlign(overlayState).padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
          }
            if (isMyProfile && showBalance && showBigPhoto) {
                Spacer(Modifier.height(8.dp))
                balanceChip()
            }
        }
    }
}

@Composable
private fun ProfileInfoSection(
    isGroup: Boolean,
    isChannel: Boolean = false,
    desc: String,
    username: String,
    membersCount: Int,
    messagesSent: Int?,
    adsViewed: Long,
    groupCreatedAt: Date?,
    userCreatedAt: Date?,
    isGroupAdmin: Boolean,
    showStats: Boolean = true,
    showRegistrationDate: Boolean = true,
    showMessageStats: Boolean = true,
    showAdStats: Boolean = true,
    onUsernameClick: () -> Unit,
    onMembersClick: () -> Unit,
    onMentionClick: (String) -> Unit = {}
) {
    val sdf = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val showAnyUserStat = showStats && (showRegistrationDate || showMessageStats || showAdStats)
        if (showStats && (isGroup || showAnyUserStat)) {
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.statistics_section))
            InfoSection {
                if (!isGroup) {
                    if (showRegistrationDate) {
                        userCreatedAt?.let { createdAt ->
                            TelegramInfoRow(
                                title = sdf.format(createdAt),
                                subtitle = stringResource(R.string.account_created_date_label),
                                icon = Icons.Default.CalendarToday
                            )
                        }
                    }
                    if (showMessageStats) {
                        TelegramInfoRow(
                            title = messagesSent?.toString() ?: "—",
                            subtitle = stringResource(
                                if (messagesSent == null) R.string.messages_sent_history_unavailable_label
                                else R.string.messages_sent_since_tracking_label
                            ),
                            icon = Icons.Default.Chat
                        )
                    }
                    if (showAdStats) {
                        TelegramInfoRow(
                            title = adsViewed.toString(),
                            subtitle = stringResource(R.string.ad_views_since_tracking_label),
                            icon = Icons.Default.PlayArrow
                        )
                    }
                } else {
                    groupCreatedAt?.let {
                        TelegramInfoRow(
                            title = sdf.format(it),
                            subtitle = if (isChannel) stringResource(R.string.channel_created_date_label) else stringResource(R.string.group_created_date_label),
                            icon = Icons.Default.CalendarToday
                        )
                    }
                }
            }
        }

        if (!isGroup) {
            if (desc.isNotEmpty() || username.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.information_section))
                InfoSection {
                    if (desc.isNotEmpty()) {
                        DescriptionInfoRow(
                            title = desc,
                            subtitle = stringResource(R.string.about_me_label),
                            onMentionClick = onMentionClick
                        )
                        if (username.isNotEmpty()) {
                            HorizontalDivider(
                                color = MayasTheme.Background.copy(0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                    if (username.isNotEmpty()) {
                        TelegramInfoRow(
                            title = "@$username",
                            subtitle = stringResource(R.string.username_label),
                            onClick = onUsernameClick
                        )
                    }
                }
            }
        } else {
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.description_section))
                InfoSection {
                    DescriptionInfoRow(
                        title = desc,
                        subtitle = if (isChannel) stringResource(R.string.channel_description_label) else stringResource(R.string.group_description_label),
                        onMentionClick = onMentionClick
                    )
                }
            }

            if (isChannel && username.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.link_section))
                InfoSection {
                    TelegramInfoRow(
                        title = "@$username",
                        subtitle = stringResource(R.string.public_channel_link_label),
                        onClick = onUsernameClick
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(if (isChannel) stringResource(R.string.subscribers_section) else stringResource(com.dan1eidtj.chat.R.string.members_label))
            InfoSection {
                TelegramInfoRow(
                    title = if (isChannel) "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.subscribers) else "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.participants_label),
                    subtitle = if (isGroupAdmin) {
                        if (isChannel) stringResource(R.string.manage_channel) else stringResource(R.string.manage_group)
                    } else stringResource(R.string.view_list),
                    icon = Icons.Default.Group, onClick = onMembersClick
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChannelTagSection(
    tag: ChannelTagUi?,
    canManage: Boolean,
    isBusy: Boolean,
    activeTagChannelId: String?,
    activeTagId: String?,
    onEdit: () -> Unit,
    onClaim: () -> Unit
) {
    val activeTagMatches = tag != null && activeTagChannelId == tag.channelId &&
            (activeTagId.isNullOrBlank() || tag.tagId.isBlank() || activeTagId == tag.tagId)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(MayasTheme.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Label, null, tint = MayasTheme.Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.channel_tag_section), color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
        }
        if (tag == null) {
            Text(stringResource(R.string.channel_tag_not_configured), color = MayasTheme.TextSecondary, fontSize = 13.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChannelTagBadge(label = tag.label, style = tag.style)
                Column(modifier = Modifier.weight(1f)) {
                    Text(tag.name, color = MayasTheme.TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        if (activeTagMatches) stringResource(R.string.channel_tag_active)
                        else stringResource(R.string.channel_tag_claim_hint),
                        color = MayasTheme.TextSecondary, fontSize = 12.sp
                    )
                }
            }
        }
        if (canManage) {
            OutlinedButton(onClick = onEdit, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.channel_tag_configure))
            }
        } else if (tag != null) {
            Button(
                onClick = onClaim,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
            ) {
                if (isBusy) CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (activeTagMatches) stringResource(R.string.channel_tag_refresh) else stringResource(R.string.channel_tag_take))
            }
        }
    }
}

@Composable
private fun ActiveChannelTagCard(
    tag: ChannelTagUi,
    canRemove: Boolean,
    isBusy: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).background(MayasTheme.Surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChannelTagBadge(label = tag.label, style = tag.style)
        Column(modifier = Modifier.weight(1f)) {
            Text(tag.name, color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.channel_tag_from_channel, tag.channelName.ifBlank { stringResource(R.string.channel_label_fallback) }),
                color = MayasTheme.TextSecondary, fontSize = 12.sp
            )
        }
        if (canRemove) {
            TextButton(onClick = onRemove, enabled = !isBusy) {
                Text(stringResource(R.string.channel_tag_remove), color = MayasTheme.ErrorRed)
            }
        }
    }
}

@Composable
private fun ChannelTagBadge(label: String, style: String) {
    val color = when (style.lowercase(Locale.ROOT)) {
        "blue" -> MayasTheme.GlowBlue
        "green" -> MayasTheme.GlowGreen
        "gold" -> MayasTheme.GlowGold
        "red" -> MayasTheme.ErrorRed
        else -> MayasTheme.GlowPurple
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditChannelTagDialog(
    initial: ChannelTagUi?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, label: String, style: String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var label by remember(initial) { mutableStateOf(initial?.label.orEmpty()) }
    var style by remember(initial) { mutableStateOf(initial?.style ?: "violet") }
    val removeTag = name.isBlank() && label.isBlank()
    val valid = removeTag || (name.trim().length in 2..24 && label.trim().length in 1..8)
    val styles = listOf(
        "violet" to R.string.channel_tag_style_purple,
        "blue" to R.string.channel_tag_style_blue,
        "green" to R.string.channel_tag_style_green,
        "gold" to R.string.channel_tag_style_gold,
        "red" to R.string.channel_tag_style_red
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.channel_tag_configure), color = MayasTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.channel_tag_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(8) },
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.channel_tag_badge_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.channel_tag_style_label), color = MayasTheme.TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    styles.forEach { (value, labelId) ->
                        FilterChip(
                            selected = style == value,
                            onClick = { style = value },
                            enabled = !isSaving,
                            label = { Text(stringResource(labelId)) }
                        )
                    }
                }
                Text(stringResource(if (removeTag) R.string.channel_tag_empty_removes else R.string.channel_tag_limit_help), color = MayasTheme.TextSecondary, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), label.trim(), style) },
                enabled = !isSaving && valid,
                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(if (removeTag) R.string.channel_tag_remove else R.string.channel_tag_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.close), color = MayasTheme.TextSecondary)
            }
        }
    )
}

@Composable
fun GroupMembersScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenProfile: (String, Boolean) -> Unit,
) {
    val vm: GroupMembersVM = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(chatId) { vm.observeGroup(chatId) }

    var showAddMembers by remember { mutableStateOf(false) }
    var memberPendingAction by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingTag by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingKick by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingBan by remember { mutableStateOf<GroupMemberUi?>(null) }

    fun showSnack(text: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(text) }
    }

    val membersAddedText = stringResource(R.string.members_added)
    val addMembersFailedText = stringResource(R.string.error_add_members_failed)
    val adminRightsRevokedText = stringResource(R.string.admin_rights_revoked)
    val permissionRevokeFailedText = stringResource(com.dan1eidtj.chat.R.string.error_permission_revoke_failed)
    val promotedToAdminText = stringResource(R.string.promoted_to_admin)
    val promoteFailedText = stringResource(R.string.error_promote_failed)
    val moderatorRightsRevokedText = stringResource(R.string.moderator_rights_revoked)
    val promotedToModeratorText = stringResource(R.string.promoted_to_moderator)
    val memberKickedText = stringResource(R.string.member_kicked)
    val kickFailedText = stringResource(R.string.error_kick_failed)
    val bannedStatusText = stringResource(R.string.banned_status)
    val banFailedText = stringResource(com.dan1eidtj.chats.R.string.error_ban_failed)
    val memberTagSavedText = stringResource(R.string.member_tag_saved)
    val memberTagSaveFailedText = stringResource(R.string.member_tag_save_failed)
    val memberTagForbiddenText = stringResource(R.string.member_tag_forbidden)
    val memberTagNotMemberText = stringResource(R.string.member_tag_not_member)

    Scaffold(
        containerColor = MayasTheme.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.participants_title),
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${vm.memberIds.size}",
                            color = MayasTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = MayasTheme.IconPrimary
                        )
                    }
                },
                actions = {
                    if (vm.isMyAdmin) {
                        IconButton(onClick = {
                            showAddMembers = true
                        }) { Icon(Icons.Default.PersonAdd, null, tint = MayasTheme.Accent) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                vm.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MayasTheme.Accent
                )

                vm.errorMessage != null -> Text(
                    vm.errorMessage.orEmpty(),
                    color = MayasTheme.TextSecondary,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.members, key = { it.uid }) { member ->
                        GroupMemberRow(
                            member = member,
                            isViewerAdmin = vm.isMyAdmin,
                            isSelf = member.uid == vm.myUid,
                            onClick = { onOpenProfile(member.uid, false) },
                            onManageClick = { memberPendingAction = member })
                    }
                }
            }
        }
    }

    if (showAddMembers) {
        AddMembersDialog(
            onDismiss = { showAddMembers = false },
            onSearch = { query, onResult ->
                vm.searchAddableUsers(
                    vm.memberIds,
                    query,
                    onResult
                )
            },
            onConfirm = { selectedUids ->
                showAddMembers = false
                vm.addMembers(
                    chatId,
                    selectedUids
                ) { success -> showSnack(if (success) membersAddedText else addMembersFailedText) }
            }
        )
    }

    memberPendingAction?.let { member ->
        GroupMemberActionsSheet(
            member = member,
            canEditTag = vm.canManageMemberTags,
            onDismiss = { memberPendingAction = null },
            onEditTag = { memberPendingAction = null; memberPendingTag = member },
            onToggleAdmin = {
                memberPendingAction = null
                if (member.isAdmin) vm.demoteAdmin(
                    chatId,
                    member.uid
                ) { success, error ->
                    showSnack(
                        if (success) adminRightsRevokedText else (error
                            ?: permissionRevokeFailedText)
                    )
                }
                else vm.promoteToAdmin(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) promotedToAdminText else promoteFailedText) }
            },
            onToggleModerator = {
                memberPendingAction = null
                if (member.isModerator) vm.demoteModerator(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) moderatorRightsRevokedText else permissionRevokeFailedText) }
                else vm.promoteToModerator(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) promotedToModeratorText else promoteFailedText) }
            },
            onKick = { memberPendingAction = null; memberPendingKick = member },
            onBan = { memberPendingAction = null; memberPendingBan = member }
        )
    }

    memberPendingTag?.let { member ->
        EditMemberTagDialog(
            member = member,
            onDismiss = { memberPendingTag = null },
            onSave = { tag ->
                memberPendingTag = null
                vm.setMemberTag(chatId, member.uid, tag) { success, errorCode ->
                    val message = when {
                        success -> memberTagSavedText
                        errorCode == "MEMBER_TAG_FORBIDDEN" -> memberTagForbiddenText
                        errorCode == "NOT_A_GROUP_MEMBER" -> memberTagNotMemberText
                        else -> memberTagSaveFailedText
                    }
                    showSnack(message)
                }
            }
        )
    }

    memberPendingKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingKick = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.kick_member_confirm, member.name), color = MayasTheme.TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.kick_explanation),
                    color = MayasTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingKick = null; vm.kickMember(
                        chatId,
                        member.uid
                    ) { success, error ->
                        showSnack(
                            if (success) memberKickedText else (error
                                ?: kickFailedText)
                        )
                    }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text(stringResource(R.string.kick)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    memberPendingKick = null
                }) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextSecondary) }
            }
        )
    }

    memberPendingBan?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingBan = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.ban_member_confirm, member.name), color = MayasTheme.TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.ban_explanation_channel),
                    color = MayasTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingBan = null; vm.banMember(
                        chatId,
                        member.uid
                    ) { success, error ->
                        showSnack(
                            if (success) bannedStatusText else (error ?: banFailedText)
                        )
                    }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text(stringResource(R.string.ban)) }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingBan = null }) {
                    Text(
                        stringResource(com.dan1eidtj.mayas.ui.R.string.cancel),
                        color = MayasTheme.TextSecondary
                    )
                }
            }
        )
    }
}

@Composable
fun GroupMembersBottomSheet(
    chatId: String,
    onDismiss: () -> Unit,
    onOpenProfile: (String, Boolean) -> Unit,
) {
    val vm: GroupMembersVM = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(chatId) { vm.observeGroup(chatId) }

    var showAddMembers by remember { mutableStateOf(false) }
    var memberPendingAction by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingTag by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingKick by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingBan by remember { mutableStateOf<GroupMemberUi?>(null) }
    var showBanned by remember { mutableStateOf(false) }

    fun showSnack(text: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(text) }
    }

    val membersAddedText2 = stringResource(R.string.members_added)
    val addMembersFailedText2 = stringResource(R.string.error_add_members_failed)
    val adminRightsRevokedText2 = stringResource(R.string.admin_rights_revoked)
    val permissionRevokeFailedText2 = stringResource(com.dan1eidtj.chat.R.string.error_permission_revoke_failed)
    val promotedToAdminText2 = stringResource(R.string.promoted_to_admin)
    val promoteFailedText2 = stringResource(R.string.error_promote_failed)
    val moderatorRightsRevokedText2 = stringResource(R.string.moderator_rights_revoked)
    val promotedToModeratorText2 = stringResource(R.string.promoted_to_moderator)
    val memberKickedText2 = stringResource(R.string.member_kicked)
    val kickFailedText2 = stringResource(R.string.error_kick_failed)
    val bannedStatusText2 = stringResource(R.string.banned_status)
    val banFailedText2 = stringResource(com.dan1eidtj.chats.R.string.error_ban_failed)
    val unbannedStatusText2 = stringResource(R.string.unbanned_status)
    val unbanFailedText2 = stringResource(com.dan1eidtj.chats.R.string.error_unban_failed)
    val linkCopiedText2 = stringResource(R.string.link_copied)
    val linkCreatedAndCopiedText2 = stringResource(R.string.link_created_and_copied)
    val createLinkFailedText2 = stringResource(R.string.error_create_link_failed)
    val memberTagSavedText2 = stringResource(R.string.member_tag_saved)
    val memberTagSaveFailedText2 = stringResource(R.string.member_tag_save_failed)
    val memberTagForbiddenText2 = stringResource(R.string.member_tag_forbidden)
    val memberTagNotMemberText2 = stringResource(R.string.member_tag_not_member)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MayasTheme.Surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(bottom = 8.dp)
            ) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MayasTheme.TextSecondary.copy(alpha = 0.4f))
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.participants_title),
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${vm.memberIds.size}",
                            color = MayasTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (vm.isMyAdmin) {
                        IconButton(onClick = { showAddMembers = true }) {
                            Icon(Icons.Default.PersonAdd, null, tint = MayasTheme.Accent)
                        }
                    }
                    if (vm.canIInvite) {
                        IconButton(onClick = {
                            if (vm.inviteCode != null) {
                                clipboardManager.setText(AnnotatedString("https://dan1eidt.github.io/mayas-site/join/?code=${vm.inviteCode}"))
                                showSnack(linkCopiedText2)
                            } else {
                                vm.generateInviteLink(chatId) { code ->
                                    if (code != null) {
                                        clipboardManager.setText(AnnotatedString("https://dan1eidt.github.io/mayas-site/join/?code=${vm.inviteCode}"))
                                        showSnack(linkCreatedAndCopiedText2)
                                    } else {
                                        showSnack(createLinkFailedText2)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Link, null, tint = MayasTheme.Accent)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp)) {
                    when {
                        vm.isLoading -> CircularProgressIndicator(
                            modifier = Modifier.align(
                                Alignment.Center
                            ).padding(32.dp), color = MayasTheme.Accent
                        )

                        vm.errorMessage != null -> Text(
                            vm.errorMessage.orEmpty(),
                            color = MayasTheme.TextSecondary,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )

                        else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(vm.members, key = { it.uid }) { member ->
                                GroupMemberRow(
                                    member = member,
                                    isViewerAdmin = vm.isMyAdmin,
                                    isSelf = member.uid == vm.myUid,
                                    onClick = { onOpenProfile(member.uid, false) },
                                    onManageClick = { memberPendingAction = member })
                            }

                            if (vm.bannedMembers.isNotEmpty() && vm.canIBan) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { showBanned = !showBanned }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            stringResource(R.string.banned_count, vm.bannedMembers.size),
                                            color = MayasTheme.ErrorRed,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Icon(
                                            if (showBanned) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            null,
                                            tint = MayasTheme.TextSecondary
                                        )
                                    }
                                }
                                if (showBanned) {
                                    items(
                                        vm.bannedMembers,
                                        key = { "banned_" + it.uid }) { member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MayasAvatar(
                                                url = member.avatarUrl,
                                                icon = member.profileIcon,
                                                glowColor = MayasTheme.TextSecondary,
                                                isPremium = member.isPremium,
                                                useCustomAvatar = member.useCustomAvatar,
                                                size = 40.dp,
                                                frameType = "none"
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                member.name,
                                                color = MayasTheme.TextSecondary,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = {
                                                vm.unbanMember(
                                                    chatId,
                                                    member.uid
                                                ) { success -> showSnack(if (success) unbannedStatusText2 else unbanFailedText2) }
                                            }) {
                                                Text(
                                                    stringResource(R.string.unban),
                                                    color = MayasTheme.Accent,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAddMembers) {
        AddMembersDialog(
            onDismiss = { showAddMembers = false },
            onSearch = { query, onResult ->
                vm.searchAddableUsers(
                    vm.memberIds,
                    query,
                    onResult
                )
            },
            onConfirm = { selectedUids ->
                showAddMembers = false
                vm.addMembers(
                    chatId,
                    selectedUids
                ) { success -> showSnack(if (success) membersAddedText2 else addMembersFailedText2) }
            }
        )
    }

    memberPendingAction?.let { member ->
        GroupMemberActionsSheet(
            member = member,
            canEditTag = vm.canManageMemberTags,
            onDismiss = { memberPendingAction = null },
            onEditTag = { memberPendingAction = null; memberPendingTag = member },
            onToggleAdmin = {
                memberPendingAction = null
                if (member.isAdmin) vm.demoteAdmin(
                    chatId,
                    member.uid
                ) { success, error ->
                    showSnack(
                        if (success) adminRightsRevokedText2 else (error
                            ?: permissionRevokeFailedText2)
                    )
                }
                else vm.promoteToAdmin(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) promotedToAdminText2 else promoteFailedText2) }
            },
            onToggleModerator = {
                memberPendingAction = null
                if (member.isModerator) vm.demoteModerator(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) moderatorRightsRevokedText2 else permissionRevokeFailedText2) }
                else vm.promoteToModerator(
                    chatId,
                    member.uid
                ) { success -> showSnack(if (success) promotedToModeratorText2 else promoteFailedText2) }
            },
            onKick = { memberPendingAction = null; memberPendingKick = member },
            onBan = { memberPendingAction = null; memberPendingBan = member }
        )
    }

    memberPendingTag?.let { member ->
        EditMemberTagDialog(
            member = member,
            onDismiss = { memberPendingTag = null },
            onSave = { tag ->
                memberPendingTag = null
                vm.setMemberTag(chatId, member.uid, tag) { success, errorCode ->
                    val message = when {
                        success -> memberTagSavedText2
                        errorCode == "MEMBER_TAG_FORBIDDEN" -> memberTagForbiddenText2
                        errorCode == "NOT_A_GROUP_MEMBER" -> memberTagNotMemberText2
                        else -> memberTagSaveFailedText2
                    }
                    showSnack(message)
                }
            }
        )
    }

    memberPendingKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingKick = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.kick_member_confirm, member.name), color = MayasTheme.TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.kick_explanation),
                    color = MayasTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingKick = null; vm.kickMember(
                        chatId,
                        member.uid
                    ) { success, error ->
                        showSnack(
                            if (success) memberKickedText2 else (error
                                ?: kickFailedText2)
                        )
                    }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text(stringResource(R.string.kick)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    memberPendingKick = null
                }) { Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextSecondary) }
            }
        )
    }

    memberPendingBan?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingBan = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.ban_member_confirm, member.name), color = MayasTheme.TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.ban_explanation_channel),
                    color = MayasTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingBan = null; vm.banMember(
                        chatId,
                        member.uid
                    ) { success, error ->
                        showSnack(
                            if (success) bannedStatusText2 else (error ?: banFailedText2)
                        )
                    }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text(stringResource(R.string.ban)) }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingBan = null }) {
                    Text(
                        stringResource(com.dan1eidtj.mayas.ui.R.string.cancel),
                        color = MayasTheme.TextSecondary
                    )
                }
            }
        )
    }
}

private fun channelTagColor(style: String?): Color = when (style?.lowercase(Locale.ROOT)) {
    "blue" -> MayasTheme.GlowBlue
    "green" -> MayasTheme.GlowGreen
    "gold" -> MayasTheme.GlowGold
    "red" -> MayasTheme.ErrorRed
    else -> MayasTheme.GlowPurple
}

@Composable
private fun GroupMemberRow(
    member: GroupMemberUi, isViewerAdmin: Boolean, isSelf: Boolean,
    onClick: () -> Unit, onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarView(
            avatarUrl = member.avatarUrl,
            useCustomAvatar = member.useCustomAvatar,
            profileIcon = member.profileIcon,
            profileGlow = member.profileGlow,
            isPremium = member.isPremium,
            frameType = "none",
            size = 46.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    member.name + if (isSelf) stringResource(R.string.suffix_you) else "",
                    color = MayasTheme.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (member.isOwner) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_owner), MayasTheme.GlowGold)
                } else if (member.isAdmin) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_admin), MayasTheme.GlowPurple)
                } else if (member.isModerator) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_moderator), MayasTheme.GlowBlue)
                }
                if (member.statusTagDisplayMode != "TAG_ONLY" && !member.emojiStatus.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    StatusBadge(value = member.emojiStatus.orEmpty(), fontSize = 14.sp)
                }
                if (member.statusTagDisplayMode != "STATUS_ONLY" && !member.channelTagLabel.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    RoleChip(member.channelTagLabel.orEmpty(), channelTagColor(member.channelTagStyle))
                }
                if (member.verification.verified) {
                    Spacer(Modifier.width(4.dp)); VerificationBadge(info = member.verification, size = 14.dp)
                }
                if (member.globalRank != Rank.USER) {
                    Spacer(Modifier.width(4.dp)); AdminLevelBadge(rank = member.globalRank, size = 14.dp)
                }
            }
            if (!member.memberTag.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                RoleChip(member.memberTag.orEmpty(), MayasTheme.GlowSky)
            }
            if (member.username.isNotEmpty()) Text(
                "@${member.username}",
                color = MayasTheme.TextSecondary,
                fontSize = 12.sp
            )
        }
        if (isViewerAdmin && !member.isOwner && !isSelf) {
            IconButton(onClick = onManageClick) {
                Icon(
                    Icons.Default.MoreVert,
                    null,
                    tint = MayasTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RoleChip(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GroupMemberActionsSheet(
    member: GroupMemberUi,
    canEditTag: Boolean,
    onDismiss: () -> Unit,
    onEditTag: () -> Unit,
    onToggleAdmin: () -> Unit,
    onToggleModerator: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                member.name,
                color = MayasTheme.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(if (member.isAdmin) stringResource(R.string.revoke_admin_rights) else stringResource(R.string.promote_to_admin)) },
                    leadingContent = {
                        Icon(
                            if (member.isAdmin) Icons.Default.RemoveModerator else Icons.Default.AdminPanelSettings,
                            null,
                            tint = MayasTheme.Accent
                        )
                    },
                    modifier = Modifier.clickable { onToggleAdmin() }
                )
                ListItem(
                    headlineContent = { Text(if (member.isModerator) stringResource(R.string.revoke_moderator_rights) else stringResource(R.string.promote_to_moderator)) },
                    leadingContent = {
                        Icon(
                            if (member.isModerator) Icons.Default.VerifiedUser else Icons.Default.Shield,
                            null,
                            tint = MayasTheme.Accent
                        )
                    },
                    modifier = Modifier.clickable { onToggleModerator() }
                )
                if (canEditTag) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.member_tag_edit)) },
                        supportingContent = {
                            Text(
                                member.memberTag ?: stringResource(R.string.member_tag_empty_removes),
                                color = MayasTheme.TextSecondary
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Label, null, tint = MayasTheme.Accent) },
                        modifier = Modifier.clickable { onEditTag() }
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.kick_from_group)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.PersonRemove,
                            null,
                            tint = MayasTheme.TextSecondary
                        )
                    },
                    modifier = Modifier.clickable { onKick() }
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.ban_forever),
                            color = MayasTheme.ErrorRed
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Block,
                            null,
                            tint = MayasTheme.ErrorRed
                        )
                    },
                    modifier = Modifier.clickable { onBan() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    color = MayasTheme.TextSecondary
                )
            }
        }
    )
}

@Composable
private fun EditMemberTagDialog(
    member: GroupMemberUi,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tag by remember(member.uid, member.memberTag) { mutableStateOf(member.memberTag.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.member_tag_edit), color = MayasTheme.TextPrimary) },
        text = {
            Column {
                Text(member.name, color = MayasTheme.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tag,
                    onValueChange = { next -> tag = next.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.member_tag_label)) },
                    placeholder = { Text(stringResource(R.string.member_tag_hint)) },
                    supportingText = { Text(stringResource(R.string.member_tag_empty_removes)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MayasTheme.Accent,
                        unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.35f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(tag.trim()) }) {
                Text(stringResource(R.string.channel_tag_save), color = MayasTheme.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.dan1eidtj.mayas.ui.R.string.cancel), color = MayasTheme.TextSecondary)
            }
        }
    )
}

@Composable
private fun AddMembersDialog(
    onDismiss: () -> Unit,
    onSearch: (String, (List<GroupMemberUi>) -> Unit) -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GroupMemberUi>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList(); return@LaunchedEffect
        }
        delay(500)
        isSearching = true
        onSearch(query) { found -> results = found; isSearching = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MayasTheme.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.add_members), color = MayasTheme.TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_by_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MayasTheme.Accent,
                        unfocusedBorderColor = MayasTheme.TextSecondary.copy(0.3f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.height(280.dp)) {
                    when {
                        isSearching -> CircularProgressIndicator(
                            modifier = Modifier.align(
                                Alignment.Center
                            ).size(28.dp), color = MayasTheme.Accent
                        )

                        results.isEmpty() -> Text(
                            stringResource(R.string.nobody_found),
                            color = MayasTheme.TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        else -> LazyColumn {
                            items(results, key = { it.uid }) { user ->
                                val isChecked = selected.contains(user.uid)
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            selected =
                                                if (isChecked) selected - user.uid else selected + user.uid
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            selected =
                                                if (it) selected + user.uid else selected - user.uid
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MayasTheme.Accent
                                        )
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            user.name,
                                            color = MayasTheme.TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        if (user.username.isNotEmpty()) Text(
                                            "@${user.username}",
                                            color = MayasTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)
            ) { Text(stringResource(R.string.add_count, selected.size)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(com.dan1eidtj.mayas.ui.R.string.cancel),
                    color = MayasTheme.TextSecondary
                )
            }
        }
    )
}