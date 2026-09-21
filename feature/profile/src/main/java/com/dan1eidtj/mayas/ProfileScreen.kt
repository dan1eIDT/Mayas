/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@file:Suppress("DEPRECATION")

package com.dan1eidtj.mayas

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.dan1eidtj.mayas.ads.AdsManager
import androidx.compose.ui.res.stringResource
import com.dan1eidtj.profile.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.feature.ChatVM
import com.dan1eidtj.mayas.feature.formatCompactCount
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.dan1eidtj.mayas.core_ui.utils.formatLastSeen
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.FullScreenImageViewer
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.core_ui.ui.components.AllProfileIcons
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationBadge
import com.dan1eidtj.mayas.core_ui.ui.components.AdminLevelBadge
import com.dan1eidtj.mayas.core_ui.ui.components.VerificationInfoDialog
import com.dan1eidtj.mayas.feature.GroupMemberUi
import com.dan1eidtj.mayas.feature.Message
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.feature.GroupMembersVM
import com.dan1eidtj.mayas.feature.BlockUserConfirmDialog
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.MediaFileCache
import com.dan1eidtj.mayas.storage.ImageCompressor
import com.dan1eidtj.mayas.storage.MediaKind
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale


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
    val context = LocalContext.current
    val activity = context as Activity
    val chatVM: ChatVM = viewModel()
    val chatListVM: com.dan1eidtj.mayas.feature.chats.ChatListScreen.ChatListViewModel = viewModel()

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
    val adLimitReachedText = stringResource(R.string.ad_limit_reached)
    val adStillLoadingText = stringResource(R.string.ad_still_loading)
    val demoAdRewardText = stringResource(R.string.demo_ad_reward_message)
    val creditFailedText = stringResource(R.string.error_credit_failed)
    val genericErrorShortText = stringResource(com.dan1eidtj.auth.R.string.error_generic_short)
    val purchasedItemTemplate = stringResource(R.string.purchased_item)
    var name by remember { mutableStateOf(loadingLabel) }
    var username by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
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
    var adsWatchedToday by remember { mutableIntStateOf(0) }
    var adsResetAt by remember { mutableStateOf(0L) }
    var isInvisible by remember { mutableStateOf(false) }
    var nameColor by remember { mutableStateOf("gold") }
    var phone by remember { mutableStateOf("") }
    var verification by remember { mutableStateOf(com.dan1eidtj.data.VerificationInfo()) }
    var profileRank by remember { mutableStateOf(com.dan1eidtj.data.Rank.USER) }

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

    var messagesSent by remember { mutableIntStateOf(0) }
    var groupCreatedAt by remember { mutableStateOf<Date?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var membersUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var adminIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var ownerId by remember { mutableStateOf("") }

    // chatId для вкладок Медиа/Ссылки/Закреплённые: у группы/канала это сам finalId
    // (chats/{id}), у обычного юзера — детерминированный id личного чата. Переиспользую
    // Screen.getChatId — ровно ту же функцию, которую уже использует ChatListVM для
    // личных чатов, чтобы не считать sorted(uid1, uid2) второй раз своей копией.
    // Чат может ещё не существовать (переписки не было) — тогда запросы просто вернут
    // пустые списки, без крашей.
    val chatIdForTabs = remember(finalId, isGroup, currentMyUid) {
        if (finalId.isBlank() || currentMyUid.isBlank()) ""
        else if (isGroup) finalId
        else com.dan1eidtj.mayas.core_ui.Screen.getChatId(currentMyUid, finalId)
    }
    var isLoadingProfileTabs by remember { mutableStateOf(false) }
    var profileMediaMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var profileLinkMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var profilePinnedMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    val showProfileTabs = chatIdForTabs.isNotBlank() && (isGroup || !isMyProfile)

    LaunchedEffect(chatIdForTabs, showProfileTabs) {
        if (!showProfileTabs) return@LaunchedEffect
        isLoadingProfileTabs = true
        val recent = chatVM.fetchRecentMessages(chatIdForTabs)
        profileMediaMessages = recent.filter { !it.mediaUrl.isNullOrBlank() }
        profileLinkMessages = recent.filter { extractUrls(it.text).isNotEmpty() }
        profilePinnedMessages = chatVM.fetchPinnedMessages(chatIdForTabs)
        isLoadingProfileTabs = false
    }

    val isGroupAdmin =
        isGroup && currentMyUid.isNotBlank() && (currentMyUid in adminIds || currentMyUid == ownerId)
    val isGroupOwner = isGroup && currentMyUid == ownerId
    val canEdit = isMyProfile || isGroupAdmin

    // Статус блокировки — только для чужого обычного профиля (Этап 2).
    var isBlockedByMe by remember { mutableStateOf(false) }
    var isBlockActionLoading by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(finalId, currentMyUid, isGroup, isMyProfile) {
        if (isGroup || isMyProfile || finalId.isBlank() || currentMyUid.isBlank()) {
            isBlockedByMe = false
            return@LaunchedEffect
        }
        vm.db.collection("users").document(currentMyUid).get()
            .addOnSuccessListener { doc ->
                val blocked = doc.get("blocked") as? List<*>
                isBlockedByMe = blocked?.contains(finalId) == true
            }
    }

    var showImagePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showGroupMembers by remember { mutableStateOf(false) }
    var fullScreenAvatarUrl by remember { mutableStateOf<String?>(null) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    val glowColor = getGlowColor(profileGlow)
    var isAdLoading by remember { mutableStateOf(false) }

    DisposableEffect(finalId) {
        val collection = if (isGroup) "chats" else "users"
        val reg =
            vm.db.collection(collection).document(finalId).addSnapshotListener { snapshot, _ ->
                snapshot?.data?.let { data ->
                    if (isGroup) {
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
                        useCustomAvatar = avatar.isNotEmpty()
                        // Верификация канала — только для type == CHANNEL, у обычных
                        // групп её не бывает (VerificationInfo.fromMap безопасен и для них).
                        verification = if (docType == "CHANNEL")
                            com.dan1eidtj.data.VerificationInfo.fromMap(data)
                        else com.dan1eidtj.data.VerificationInfo()
                    } else {
                        name = data["name"] as? String ?: noNameLabel
                        username = data["username"] as? String ?: ""
                        profileIcon = data["profileIcon"] as? String ?: "ghost"
                        profileGlow = data["profileGlow"] as? String ?: "purple"
                        desc = data["description"] as? String ?: ""
                        emojiStatus = data["emojiStatus"] as? String ?: ""
                        isPremium = data["isPremium"] as? Boolean ?: false
                        balance = (data["balance"] as? Long)?.toInt() ?: 0
                        verifiedIcon = data["verifiedIcon"] as? String ?: "verified"
                        avatarFrame = data["avatarFrame"] as? String ?: "none"
                        adsWatchedToday = (data["adsWatchedToday"] as? Long)?.toInt() ?: 0
                        adsResetAt = (data["adsResetAt"] as? Timestamp)?.toDate()?.time ?: 0L
                        isInvisible = data["isInvisible"] as? Boolean ?: false
                        nameColor = data["nameColor"] as? String ?: "gold"
                        messagesSent = (data["messagesSent"] as? Long)?.toInt() ?: 0
                        verification = com.dan1eidtj.data.VerificationInfo.fromMap(data)
                        profileRank = com.dan1eidtj.data.Rank.fromMap(data)
                        val lastSeenAllowed = isMyProfile ||
                                (data["privacy_last_seen"] as? String ?: "all") == "all"
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

    // Номер телефона больше не приходит в общем снапшоте users/{uid} (его там больше нет —
    // вынесен в приватную подколлекцию, чтобы никто, кроме владельца, не мог его прочитать).
    // Подгружаем отдельно, только для своего профиля.
    LaunchedEffect(finalId, isGroup, isMyProfile) {
        if (isGroup || !isMyProfile || finalId.isBlank()) return@LaunchedEffect
        vm.getMyPhone { phone = it }
    }

    LaunchedEffect(finalId, isGroup, isMyProfile, adsResetAt, adsWatchedToday) {
        if (isGroup || !isMyProfile || finalId.isBlank()) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val needsReset =
            (adsResetAt == 0L && adsWatchedToday > 0) || (adsResetAt != 0L && now >= adsResetAt)
        if (needsReset) {
            val newResetAt = now + dayMillis
            vm.db.collection("users").document(finalId)
                .update(
                    mapOf(
                        "adsWatchedToday" to 0,
                        "adsResetAt" to Timestamp(Date(newResetAt))
                    )
                )
                .addOnSuccessListener {
                    adsWatchedToday = 0
                    adsResetAt = newResetAt
                }
                .addOnFailureListener { e -> Log.e("AdsDebug", "Ошибка сброса таймера рекламы", e) }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pickedUri ->
            coroutineScope.launch {
                try {
                    val bytes = ImageCompressor.compressAvatar(context, pickedUri)





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
                            Log.e(
                                "ProfileScreen",
                                "Не удалось сохранить ключ аватара",
                                e
                            )
                        }

                    avatar = key
                    useCustomAvatar = true
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Не удалось загрузить аватар", e)
                    Toast.makeText(context, avatarUploadFailedText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = MayasTheme.Background,
        topBar = {
            ProfileTopBar(
                isMyProfile = isMyProfile,
                isEditing = isEditing,
                canEdit = canEdit,
                balance = balance,
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPremium = onNavigateToPremium,
                onShopClick = { showShop = true },
                onEditClick = {
                    if (isEditing) {
                        if (!isUsernameAvailable) {
                            Toast.makeText(context, usernameTakenShortText, Toast.LENGTH_SHORT)
                                .show()
                            return@ProfileTopBar
                        }
                        if (username.length < 3 && (!isGroup || isChannel) && username.isNotBlank()) {
                            Toast.makeText(context, usernameTooShortText, Toast.LENGTH_SHORT)
                                .show()
                            return@ProfileTopBar
                        }

                        val cleanChannelUsername = username.lowercase().trim().replace(" ", "")
                        val updates = if (isChannel) {
                            mapOf(
                                "groupName" to name,
                                "description" to desc,
                                "profileIcon" to profileIcon,
                                "profileGlow" to profileGlow,
                                "username" to cleanChannelUsername,

                                "isPublic" to cleanChannelUsername.isNotBlank()
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
                                "useCustomAvatar" to useCustomAvatar
                            )
                        }
                        val collection = if (isGroup) "chats" else "users"
                        vm.db.collection(collection).document(finalId).update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    savedStatusText,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    saveErrorGenericText,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        // Номер телефона сохраняем отдельным вызовом: там своя нормализация
                        // (E.164) и валидация, поэтому его не пихаем в общий updates-мап.
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ProfileHeader(
                    name = name,
                    avatar = avatar,
                    profileIcon = profileIcon,
                    glowColor = glowColor,
                    isPremium = isPremium,
                    useCustomAvatar = useCustomAvatar,
                    avatarFrame = avatarFrame,
                    isEditing = isEditing,
                    emojiStatus = emojiStatus,
                    verifiedIcon = verifiedIcon,
                    isGroup = isGroup,
                    isChannel = isChannel,
                    membersCount = membersUids.size,
                    username = username,
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

            if (!isEditing) {
                item {
                    ProfileInfoSection(
                        isGroup = isGroup, isChannel = isChannel, desc = desc, username = username,
                        membersCount = membersUids.size, messagesSent = messagesSent,
                        groupCreatedAt = groupCreatedAt, isGroupAdmin = isGroupAdmin,
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
                                        Toast.makeText(
                                            context,
                                            String.format(mentionedUserNotFoundTemplate, mentionedUsername),
                                            Toast.LENGTH_SHORT
                                        ).show()
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

                if (!isGroup && !isMyProfile) {
                    item {
                        ProfileActionsRow(
                            isBlocked = isBlockedByMe,
                            isLoading = isBlockActionLoading,
                            onWriteClick = {
                                chatListVM.openOrCreateDirectChat(currentMyUid, finalId) { chatId ->
                                    onNavigateToChat(chatId, null)
                                }
                            },
                            onBlockClick = {
                                if (isBlockedByMe) {
                                    isBlockActionLoading = true
                                    chatVM.unblockUser(currentMyUid, finalId) {
                                        isBlockActionLoading = false
                                        isBlockedByMe = false
                                        Toast.makeText(context, userUnblockedText, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    showBlockConfirm = true
                                }
                            }
                        )
                    }
                }

                if (isGroup) {
                    item {
                        Button(
                            onClick = {
                                if (isGroupOwner) {
                                    vm.db.collection("chats").document(finalId).delete()
                                        .addOnSuccessListener { onBack() }
                                } else {
                                    vm.db.collection("chats").document(finalId).update(
                                        "participants", FieldValue.arrayRemove(currentMyUid),
                                        "members", FieldValue.arrayRemove(currentMyUid),
                                        "admins", FieldValue.arrayRemove(currentMyUid)
                                    ).addOnSuccessListener { onBack() }
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

                if (isMyProfile) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SectionTitle(stringResource(R.string.earnings_section))
                            EarnAndShopSection(
                                adsWatched = adsWatchedToday,
                                isAdLoading = isAdLoading,
                                onWatchAd = {
                                    if (adsWatchedToday >= 5) {
                                        Toast.makeText(
                                            context,
                                            adLimitReachedText,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@EarnAndShopSection
                                    }

                                    if (!AdsManager.isRewardedAvailable()) {
                                        Log.d(
                                            "AdsDebug",
                                            "Реклама ещё не загружена, пробуем загрузить и подождать"
                                        )
                                        Toast.makeText(
                                            context,
                                            adStillLoadingText,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        AdsManager.loadRewarded()
                                        return@EarnAndShopSection
                                    }

                                    isAdLoading = true
                                    Log.d(
                                        "AdsDebug",
                                        "Показываем rewarded, состояние доступности: ${AdsManager.isRewardedAvailable()}"
                                    )

                                    AdsManager.showRewarded(
                                        activity = activity,
                                        onReward = {
                                            val isFirstOfCycle = adsWatchedToday == 0
                                            val newCount = adsWatchedToday + 1
                                            val newBalance = balance + 250
                                            val newResetAt =
                                                System.currentTimeMillis() + 24 * 60 * 60 * 1000L

                                            val updates = mutableMapOf<String, Any>(
                                                "adsWatchedToday" to newCount,
                                                "balance" to newBalance
                                            )
                                            if (isFirstOfCycle) {
                                                updates["adsResetAt"] = Timestamp(Date(newResetAt))
                                            }

                                            vm.db.collection("users").document(finalId)
                                                .update(updates)
                                                .addOnSuccessListener {
                                                    isAdLoading = false
                                                    adsWatchedToday = newCount
                                                    balance = newBalance
                                                    if (isFirstOfCycle) adsResetAt = newResetAt
                                                    Toast.makeText(
                                                        context,
                                                        demoAdRewardText,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    isAdLoading = false
                                                    Log.e(
                                                        "AdsDebug",
                                                        "Ошибка начисления баланса",
                                                        e
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        creditFailedText,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        },
                                        onError = { message ->
                                            isAdLoading = false
                                            Log.e("AdsDebug", "Ошибка показа рекламы: $message")
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    )

                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(10_000L)
                                        isAdLoading = false
                                    }
                                }
                            )
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
                                    if (isPremium) {
                                        avatarFrame = newFrame
                                        vm.db.collection("users").document(finalId)
                                            .update("avatarFrame", newFrame)
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    context,
                                                    genericErrorShortText,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else onNavigateToPremium()
                                },
                                onNameColorSelect = { newColor ->
                                    if (isPremium) {
                                        nameColor = newColor
                                        vm.db.collection("users").document(finalId)
                                            .update("nameColor", newColor)
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    context,
                                                    genericErrorShortText,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else onNavigateToPremium()
                                }
                            )
                        }
                    }
                }
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
                val key = when (type) {
                    ItemType.EMOJI_STATUS -> "emojiStatus"
                    ItemType.BUBBLE -> "messageStyle"
                    else -> null
                }
                key?.let { vm.updateUserData(it, id) }
            },
            currentEmoji = emojiStatus, messageStyle = vm.userData["messageStyle"] ?: ""
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
                chatVM.blockUser(currentMyUid, finalId) {
                    isBlockActionLoading = false
                    isBlockedByMe = true
                    Toast.makeText(context, userBlockedText, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showBlockConfirm = false }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String, avatar: String, profileIcon: String, glowColor: Color,
    isPremium: Boolean, useCustomAvatar: Boolean, avatarFrame: String,
    isEditing: Boolean, emojiStatus: String, verifiedIcon: String,
    isGroup: Boolean, isChannel: Boolean = false, membersCount: Int, username: String,
    lastSeenText: String, isOnline: Boolean, nameColor: String = "gold",
    isUsernameAvailable: Boolean = true, isCheckingUsername: Boolean = false,
    onAvatarClick: () -> Unit, onAvatarView: () -> Unit = {}, onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit = {},
    onDescChange: (String) -> Unit, desc: String,
    onMembersClick: () -> Unit = {},
    isMyProfile: Boolean = false,
    phone: String = "",
    onPhoneChange: (String) -> Unit = {},
    verification: com.dan1eidtj.data.VerificationInfo = com.dan1eidtj.data.VerificationInfo(),
    onVerificationClick: () -> Unit = {},
    rank: com.dan1eidtj.data.Rank = com.dan1eidtj.data.Rank.USER
) {
    val nameBrush = com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush(nameColor)


    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                // Значок верификации — независим от Mayas+/emojiStatus, показывается
                // только если verification.verified == true (см. VerificationInfo.fromMap).
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
                if (rank != com.dan1eidtj.data.Rank.USER) {
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
                when {
                    isChannel -> "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.subscribers)
                    isGroup -> "${formatCompactCount(membersCount)} " + stringResource(com.dan1eidtj.chat.R.string.participants_label)
                    username.isNotEmpty() -> "@$username"
                    else -> lastSeenText
                },
                color = if (isOnline && !isGroup) MayasTheme.Accent else MayasTheme.TextSecondary,
                fontSize = 14.sp
            )
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
    messagesSent: Int,
    groupCreatedAt: Date?,
    isGroupAdmin: Boolean,
    onUsernameClick: () -> Unit,
    onMembersClick: () -> Unit,
    onMentionClick: (String) -> Unit = {}
) {
    val sdf = remember { SimpleDateFormat("d MMMM yyyy", Locale("ru")) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.statistics_section))
        InfoSection {
            if (!isGroup) {
                TelegramInfoRow(
                    title = "$messagesSent",
                    subtitle = stringResource(R.string.messages_sent_label),
                    icon = Icons.Default.Chat
                )
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
            member = member, onDismiss = { memberPendingAction = null },
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
            member = member, onDismiss = { memberPendingAction = null },
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
                    fontWeight = FontWeight.Medium
                )
                if (member.isOwner) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_owner), MayasTheme.GlowGold)
                } else if (member.isAdmin) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_admin), MayasTheme.GlowPurple)
                } else if (member.isModerator) {
                    Spacer(Modifier.width(6.dp)); RoleChip(stringResource(R.string.role_moderator), MayasTheme.GlowBlue)
                }
                if (member.verification.verified) {
                    Spacer(Modifier.width(4.dp)); VerificationBadge(info = member.verification, size = 14.dp)
                }
                if (member.globalRank != com.dan1eidtj.data.Rank.USER) {
                    Spacer(Modifier.width(4.dp)); AdminLevelBadge(rank = member.globalRank, size = 14.dp)
                }
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
    onDismiss: () -> Unit,
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
        kotlinx.coroutines.delay(500)
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