@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@file:Suppress("DEPRECATION")

package com.dan1eidtj.mayas

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.dan1eidtj.mayas.ads.AdsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.dan1eidtj.mayas.core_ui.utils.formatLastSeen
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.core_ui.utils.isUserOnline
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.FullScreenImageViewer
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.data.ItemType
import com.dan1eidtj.mayas.feature.GroupMemberUi
import com.dan1eidtj.mayas.feature.GroupMembersVM
import com.dan1eidtj.mayas.feature.auth.AuthVM
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.ImageCompressor
import com.dan1eidtj.mayas.storage.MediaKind
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun ProfileScreen(
    targetId: String?,
    isGroup: Boolean,
    vm: AuthVM,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (String) -> Unit,
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


    val coroutineScope = rememberCoroutineScope()

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
    var isPremium by remember { mutableStateOf(false) }
    var balance by remember { mutableIntStateOf(0) }
    var verifiedIcon by remember { mutableStateOf("verified") }
    var avatarFrame by remember { mutableStateOf("none") }
    var adsWatchedToday by remember { mutableIntStateOf(0) }
    var adsResetAt by remember { mutableStateOf(0L) }
    var isInvisible by remember { mutableStateOf(false) }
    var nameColor by remember { mutableStateOf("gold") }

    var isEditing by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf(true) }
    var isCheckingUsername by remember { mutableStateOf(false) }




    var originalUsername by remember(finalId) { mutableStateOf("") }
    LaunchedEffect(username, isEditing) {
        if (!isEditing) originalUsername = username
    }

    LaunchedEffect(username) {
        if (isEditing && username != originalUsername && username.length >= 3) {
            isCheckingUsername = true
            vm.checkUsername(username) { available ->
                isUsernameAvailable = available
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

    val isGroupAdmin = isGroup && currentMyUid.isNotBlank() && (currentMyUid in adminIds || currentMyUid == ownerId)
    val isGroupOwner = isGroup && currentMyUid == ownerId
    val canEdit = isMyProfile || isGroupAdmin

    var showImagePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showGroupMembers by remember { mutableStateOf(false) }
    var fullScreenAvatarUrl by remember { mutableStateOf<String?>(null) }

    val glowColor = getGlowColor(profileGlow)
    var isAdLoading by remember { mutableStateOf(false) }

    DisposableEffect(finalId) {
        val collection = if (isGroup) "chats" else "users"
        val reg = vm.db.collection(collection).document(finalId).addSnapshotListener { snapshot, _ ->
            snapshot?.data?.let { data ->
                if (isGroup) {
                    name = data["groupName"] as? String ?: "Группа"
                    avatar = data["groupAvatar"] as? String ?: ""
                    desc = data["groupDescription"] as? String ?: ""
                    ownerId = data["ownerId"] as? String ?: ""
                    profileIcon = data["profileIcon"] as? String ?: "default"
                    profileGlow = data["profileGlow"] as? String ?: "purple"
                    emojiStatus = data["emoji"] as? String ?: "👥"
                    groupCreatedAt = (data["createdAt"] as? Timestamp)?.toDate()
                    adminIds = (data["admins"] as? List<String>) ?: listOfNotNull(data["adminId"] as? String)
                    membersUids = (data["participants"] as? List<String>) ?: (data["members"] as? List<String>) ?: emptyList()
                    useCustomAvatar = avatar.isNotEmpty()
                } else {
                    name = data["name"] as? String ?: "Без имени"
                    username = data["username"] as? String ?: ""
                    avatar = data["avatarUrl"] as? String ?: ""
                    profileIcon = data["profileIcon"] as? String ?: "ghost"
                    profileGlow = data["profileGlow"] as? String ?: "purple"
                    useCustomAvatar = data["useCustomAvatar"] as? Boolean ?: true
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
                    isOnline = isUserOnline(data)
                    val status = data["status"] as? Map<String, Any>
                    val lastSeen = (data["lastSeen"] as? Timestamp) ?: (status?.get("lastSeen") as? Timestamp)
                    lastSeenText = if (isOnline) "в сети" else formatLastSeen(lastSeen)
                }
            }
        }
        onDispose { reg.remove() }
    }


    LaunchedEffect(finalId, isGroup, isMyProfile, adsResetAt, adsWatchedToday) {
        if (isGroup || !isMyProfile || finalId.isBlank()) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val needsReset = (adsResetAt == 0L && adsWatchedToday > 0) || (adsResetAt != 0L && now >= adsResetAt)
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
                        uid = finalId,
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
                        .addOnFailureListener { e -> Log.e("ProfileScreen", "Не удалось сохранить ключ аватара", e) }

                    avatar = key
                    useCustomAvatar = true
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Не удалось загрузить аватар", e)
                    Toast.makeText(context, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Этот юзернейм занят", Toast.LENGTH_SHORT).show()
                            return@ProfileTopBar
                        }
                        if (username.length < 3 && !isGroup) {
                            Toast.makeText(context, "Юзернейм слишком короткий", Toast.LENGTH_SHORT).show()
                            return@ProfileTopBar
                        }

                        val updates = if (isGroup) {
                            mapOf(
                                "groupName" to name,
                                "groupDescription" to desc,
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
                            .addOnSuccessListener { Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show() }
                            .addOnFailureListener { Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show() }
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
                    name = name, avatar = avatar, profileIcon = profileIcon, glowColor = glowColor,
                    isPremium = isPremium, useCustomAvatar = useCustomAvatar, avatarFrame = avatarFrame,
                    isEditing = isEditing, emojiStatus = emojiStatus, verifiedIcon = verifiedIcon,
                    isGroup = isGroup, membersCount = membersUids.size, username = username,
                    lastSeenText = lastSeenText, isOnline = isOnline, nameColor = nameColor,
                    isUsernameAvailable = isUsernameAvailable, isCheckingUsername = isCheckingUsername,
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
                                fullScreenAvatarUrl = B2MediaClient.resolveDownloadUrl(avatar)
                            }
                        }
                    },
                    onNameChange = { name = it },
                    onUsernameChange = { username = it },
                    onDescChange = { desc = it }, desc = desc,
                    onMembersClick = { showGroupMembers = true }
                )
            }

            if (!isEditing) {
                item {
                    ProfileInfoSection(
                        isGroup = isGroup, desc = desc, username = username,
                        membersCount = membersUids.size, messagesSent = messagesSent,
                        groupCreatedAt = groupCreatedAt, isGroupAdmin = isGroupAdmin,
                        onUsernameClick = {
                            val link = "@$username"
                            val clip = ClipData.newPlainText("MayasUN", link)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                        },
                        onMembersClick = { showGroupMembers = true }
                    )
                }

                if (isGroup) {
                    item {
                        Button(
                            onClick = {
                                if (isGroupOwner) {
                                    vm.db.collection("chats").document(finalId).delete().addOnSuccessListener { onBack() }
                                } else {
                                    vm.db.collection("chats").document(finalId).update(
                                        "participants", FieldValue.arrayRemove(currentMyUid),
                                        "members", FieldValue.arrayRemove(currentMyUid),
                                        "admins", FieldValue.arrayRemove(currentMyUid)
                                    ).addOnSuccessListener { onBack() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed.copy(0.1f))
                        ) {
                            Text(
                                if (isGroupOwner) "Удалить группу" else "Покинуть группу",
                                color = MayasTheme.ErrorRed, fontWeight = FontWeight.Medium
                            )
                        }
                    }

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
                            listOf("Медиа", "Файлы", "Ссылки").forEachIndexed { i, label ->
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
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            Text(
                                when (selectedTab) {
                                    0 -> "Здесь будут фото и видео"
                                    1 -> "Здесь будут файлы"
                                    else -> "Здесь будут ссылки"
                                },
                                color = MayasTheme.TextSecondary, fontSize = 14.sp
                            )
                        }
                    }
                }


                if (isMyProfile) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SectionTitle("ЗАРАБОТОК И МАГАЗИН")
                            EarnAndShopSection(
                                adsWatched = adsWatchedToday,
                                isAdLoading = isAdLoading,
                                onWatchAd = {
                                    if (adsWatchedToday >= 5) {
                                        Toast.makeText(context, "Лимит рекламы на сегодня исчерпан", Toast.LENGTH_SHORT).show()
                                        return@EarnAndShopSection
                                    }

                                    if (!AdsManager.isRewardedAvailable()) {
                                        Log.d("AdsDebug", "Реклама ещё не загружена, пробуем загрузить и подождать")
                                        Toast.makeText(context, "Реклама ещё грузится, попробуй через пару секунд", Toast.LENGTH_SHORT).show()
                                        AdsManager.loadRewarded()
                                        return@EarnAndShopSection
                                    }

                                    isAdLoading = true
                                    Log.d("AdsDebug", "Показываем rewarded, состояние доступности: ${AdsManager.isRewardedAvailable()}")

                                    AdsManager.showRewarded(
                                        activity = activity,
                                        onReward = {
                                            val isFirstOfCycle = adsWatchedToday == 0
                                            val newCount = adsWatchedToday + 1
                                            val newBalance = balance + 250
                                            val newResetAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L

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
                                                    Toast.makeText(context, "Эта реклама была демо, но ладно держи 250.", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    isAdLoading = false
                                                    Log.e("AdsDebug", "Ошибка начисления баланса", e)
                                                    Toast.makeText(context, "Ошибка начисления", Toast.LENGTH_SHORT).show()
                                                }
                                        },
                                        onError = { message ->
                                            isAdLoading = false
                                            Log.e("AdsDebug", "Ошибка показа рекламы: $message")
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    )


                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(10_000L)
                                        isAdLoading = false
                                    }
                                },
                                onOpenShop = { showShop = true }
                            )
                        }
                    }
                }
            } else {

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("ЦВЕТОВОЕ ОФОРМЛЕНИЕ")
                        ColorPicker(profileGlow, isPremium, { profileGlow = it }, onNavigateToPremium)
                    }
                }


                if (isGroup) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle("ИКОНКА ГРУППЫ")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf(
                                    "ghost", "face", "star", "heart", "bolt", "fire", "diamond",
                                    "rocket", "crown", "medal", "gamepad", "music", "camera", "brush"
                                ).forEach { iconName ->
                                    val isSelected = !useCustomAvatar && profileIcon == iconName
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) MayasTheme.Accent else Color.Transparent, CircleShape)
                                            .clickable {
                                                profileIcon = iconName
                                                useCustomAvatar = false
                                            }
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MayasAvatar(
                                            url = "", icon = iconName, glowColor = glowColor,
                                            isPremium = false, useCustomAvatar = false, size = 48.dp, frameType = "none"
                                        )
                                    }
                                }
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
                                verifiedIcon = verifiedIcon,
                                avatarFrame = avatarFrame,
                                isInvisible = isInvisible,
                                nameColor = nameColor,
                                onNavigateToPremium = onNavigateToPremium,
                                onInvisibleChange = { checked ->
                                    if (isPremium) {
                                        isInvisible = checked
                                        vm.db.collection("users").document(finalId).update("isInvisible", checked)
                                            .addOnFailureListener { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
                                    } else onNavigateToPremium()
                                },
                                onIconSelect = { newIcon ->
                                    if (isPremium) {
                                        verifiedIcon = newIcon
                                        vm.db.collection("users").document(finalId).update("verifiedIcon", newIcon)
                                            .addOnFailureListener { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
                                    } else onNavigateToPremium()
                                },
                                onFrameSelect = { newFrame ->
                                    if (isPremium) {
                                        avatarFrame = newFrame
                                        vm.db.collection("users").document(finalId).update("avatarFrame", newFrame)
                                            .addOnFailureListener { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
                                    } else onNavigateToPremium()
                                },
                                onNameColorSelect = { newColor ->
                                    if (isPremium) {
                                        nameColor = newColor
                                        vm.db.collection("users").document(finalId).update("nameColor", newColor)
                                            .addOnFailureListener { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
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
                vm.buyItem(id, price,
                    onSuccess = { Toast.makeText(context, "Куплено: $itemName", Toast.LENGTH_SHORT).show() },
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
            isGroup = isGroup, onDismiss = { showImagePicker = false },
            onGallery = { launcher.launch("image/*"); showImagePicker = false },
            onSystemIcon = { showIconPicker = true; showImagePicker = false },
            onDelete = {
                avatar = ""; useCustomAvatar = false; showImagePicker = false
                val field = if (isGroup) "groupAvatar" else "avatarUrl"
                vm.db.collection(if (isGroup) "chats" else "users").document(finalId).update(field, "")
            }
        )
    }

    fullScreenAvatarUrl?.let { url ->
        FullScreenImageViewer(
            imageUrl = url,
            onDismiss = { fullScreenAvatarUrl = null }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            icons = listOf("ghost", "face", "star", "heart", "bolt", "fire", "diamond", "rocket", "crown", "medal", "gamepad", "music", "camera", "brush"),
            onDismiss = { showIconPicker = false },
            onSelect = { icon -> profileIcon = icon; useCustomAvatar = false; showIconPicker = false }
        )
    }

    if (showGroupMembers) {
        GroupMembersBottomSheet(
            chatId = finalId,
            onDismiss = { showGroupMembers = false },
            onOpenProfile = { uid, isGroupProfile ->
                showGroupMembers = false
                onNavigateToProfile(uid, isGroupProfile)
            }
        )
    }
}



@Composable
fun ProfileTopBar(
    isMyProfile: Boolean,
    isEditing: Boolean,
    canEdit: Boolean,
    balance: Int,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onShopClick: () -> Unit,
    onEditClick: () -> Unit
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.IconPrimary)
            }
        },
        actions = {
            if (isMyProfile && !isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, null, tint = MayasTheme.IconPrimary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.GlowGold.copy(alpha = 0.1f))
                            .clickable { onShopClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🪙 $balance", color = MayasTheme.GlowGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onNavigateToPremium) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MayasTheme.GlowGold)
                    }
                }
            }
            if (canEdit) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        null,
                        tint = if (isEditing) MayasTheme.Accent else MayasTheme.IconPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
    )
}



@Composable
fun ProfileHeader(
    name: String, avatar: String, profileIcon: String, glowColor: Color,
    isPremium: Boolean, useCustomAvatar: Boolean, avatarFrame: String,
    isEditing: Boolean, emojiStatus: String, verifiedIcon: String,
    isGroup: Boolean, membersCount: Int, username: String,
    lastSeenText: String, isOnline: Boolean, nameColor: String = "gold",
    isUsernameAvailable: Boolean = true, isCheckingUsername: Boolean = false,
    onAvatarClick: () -> Unit, onAvatarView: () -> Unit = {}, onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit = {},
    onDescChange: (String) -> Unit, desc: String,
    onMembersClick: () -> Unit = {}
) {
    val nameBrush = com.dan1eidtj.mayas.core_ui.utils.getNameColorBrush(nameColor)

    // FIX: бакет приватный -> avatar теперь чаще всего КЛЮЧ файла в B2, а не готовая ссылка.
    // Резолвим его в свежую presigned-ссылку прямо перед показом (с кэшем внутри B2MediaClient).
    // Старые аватарки, залитые ещё через Supabase, — это настоящие постоянные http(s)-ссылки,
    // их резолвить не нужно, показываем как есть.
    val resolvedAvatarUrl by produceState<String?>(initialValue = null, avatar, useCustomAvatar) {
        android.util.Log.d("ProfileAvatarDebug", "produceState вошёл: avatar='$avatar' useCustomAvatar=$useCustomAvatar")
        value = when {
            !useCustomAvatar || avatar.isBlank() -> null
            avatar.startsWith("http") -> avatar
            else -> B2MediaClient.resolveDownloadUrl(avatar)
        }
        android.util.Log.d("ProfileAvatarDebug", "produceState результат: value=$value")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).clickable {
                if (isEditing) {
                    onAvatarClick()
                } else if (useCustomAvatar && !resolvedAvatarUrl.isNullOrBlank()) {
                    onAvatarView()
                }
            },
            contentAlignment = Alignment.Center
        ) {
            MayasAvatar(
                url = resolvedAvatarUrl, icon = profileIcon, glowColor = glowColor,
                isPremium = isPremium, size = 120.dp, useCustomAvatar = useCustomAvatar, frameType = avatarFrame
            )
            if (isEditing) {
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MayasTheme.Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = onNameChange,
                    label = { Text("Имя", fontSize = 12.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MayasTheme.Accent,
                        unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.25f),
                        focusedLabelColor = MayasTheme.Accent, unfocusedLabelColor = MayasTheme.TextSecondary,
                        focusedTextColor = MayasTheme.TextPrimary, unfocusedTextColor = MayasTheme.TextPrimary,
                        cursorColor = MayasTheme.Accent
                    )
                )
                if (!isGroup) {
                    OutlinedTextField(
                        value = username, onValueChange = onUsernameChange,
                        label = { Text("Имя пользователя (@)", fontSize = 12.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = !isUsernameAvailable,
                        trailingIcon = {
                            if (isCheckingUsername) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MayasTheme.Accent)
                            } else if (username.length >= 3) {
                                Icon(
                                    if (isUsernameAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                                    null,
                                    tint = if (isUsernameAvailable) MayasTheme.Success else MayasTheme.ErrorRed
                                )
                            }
                        },
                        supportingText = {
                            if (!isUsernameAvailable) Text("Этот юзернейм уже занят", color = MayasTheme.ErrorRed)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isUsernameAvailable) MayasTheme.Accent else MayasTheme.ErrorRed,
                            unfocusedBorderColor = if (isUsernameAvailable) MayasTheme.TextSecondary.copy(alpha = 0.25f) else MayasTheme.ErrorRed,
                            focusedLabelColor = if (isUsernameAvailable) MayasTheme.Accent else MayasTheme.ErrorRed,
                            unfocusedLabelColor = MayasTheme.TextSecondary,
                            focusedTextColor = MayasTheme.TextPrimary, unfocusedTextColor = MayasTheme.TextPrimary,
                            cursorColor = MayasTheme.Accent
                        )
                    )
                }


                OutlinedTextField(
                    value = desc, onValueChange = onDescChange,
                    label = { Text(if (isGroup) "Описание группы" else "О себе", fontSize = 12.sp) },
                    minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MayasTheme.Accent,
                        unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.25f),
                        focusedLabelColor = MayasTheme.Accent, unfocusedLabelColor = MayasTheme.TextSecondary,
                        focusedTextColor = MayasTheme.TextPrimary, unfocusedTextColor = MayasTheme.TextPrimary,
                        cursorColor = MayasTheme.Accent
                    )
                )
                if (isGroup) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MayasTheme.Background)
                            .clickable { onMembersClick() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Group, null, tint = MayasTheme.Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$membersCount участников", color = MayasTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Управление участниками группы", color = MayasTheme.TextSecondary, fontSize = 12.sp)
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
                    Text(name, color = MayasTheme.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                if (emojiStatus.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(emojiStatus, fontSize = 20.sp)
                } else if (isPremium) {
                    Spacer(Modifier.width(6.dp))
                    val vIcon = when (verifiedIcon) {
                        "star" -> Icons.Default.Star; "diamond" -> Icons.Default.Diamond
                        "auto_awesome" -> Icons.Default.AutoAwesome
                        "crown" -> Icons.Default.WorkspacePremium; "bolt" -> Icons.Default.Bolt
                        "fire" -> Icons.Default.LocalFireDepartment; "trophy" -> Icons.Default.EmojiEvents
                        "heart" -> Icons.Default.Favorite; "shield" -> Icons.Default.Shield
                        else -> Icons.Default.Verified
                    }
                    Icon(vIcon, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when { isGroup -> "$membersCount участников"; username.isNotEmpty() -> "@$username"; else -> lastSeenText },
                color = if (isOnline && !isGroup) MayasTheme.Accent else MayasTheme.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}



@Composable
fun ProfileInfoSection(
    isGroup: Boolean, desc: String, username: String, membersCount: Int,
    messagesSent: Int, groupCreatedAt: Date?, isGroupAdmin: Boolean,
    onUsernameClick: () -> Unit, onMembersClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("d MMMM yyyy", Locale("ru")) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        SectionTitle("СТАТИСТИКА")
        InfoSection {
            if (!isGroup) {
                TelegramInfoRow(title = "$messagesSent", subtitle = "Сообщений отправлено", icon = Icons.Default.Chat)
            } else {
                groupCreatedAt?.let {
                    TelegramInfoRow(title = sdf.format(it), subtitle = "Дата создания группы", icon = Icons.Default.CalendarToday)
                }
            }
        }

        if (!isGroup) {
            if (desc.isNotEmpty() || username.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("ИНФОРМАЦИЯ")
                InfoSection {
                    if (desc.isNotEmpty()) {
                        TelegramInfoRow(title = desc, subtitle = "О себе")
                        if (username.isNotEmpty()) {
                            HorizontalDivider(color = MayasTheme.Background.copy(0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                    if (username.isNotEmpty()) {
                        TelegramInfoRow(title = "@$username", subtitle = "Имя пользователя", onClick = onUsernameClick)
                    }
                }
            }
        } else {
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("ОПИСАНИЕ")
                InfoSection {
                    TelegramInfoRow(title = desc, subtitle = "Описание группы")
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("УЧАСТНИКИ")
            InfoSection {
                TelegramInfoRow(
                    title = "$membersCount участников",
                    subtitle = if (isGroupAdmin) "Управление группой" else "Посмотреть список",
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

    fun showSnack(text: String) { coroutineScope.launch { snackbarHostState.showSnackbar(text) } }

    Scaffold(
        containerColor = MayasTheme.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Участники", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${vm.memberIds.size}", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.IconPrimary) } },
                actions = { if (vm.isMyAdmin) { IconButton(onClick = { showAddMembers = true }) { Icon(Icons.Default.PersonAdd, null, tint = MayasTheme.Accent) } } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                vm.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MayasTheme.Accent)
                vm.errorMessage != null -> Text(vm.errorMessage.orEmpty(), color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.members, key = { it.uid }) { member ->
                        GroupMemberRow(member = member, isViewerAdmin = vm.isMyAdmin, isSelf = member.uid == vm.myUid,
                            onClick = { onOpenProfile(member.uid, false) }, onManageClick = { memberPendingAction = member })
                    }
                }
            }
        }
    }

    if (showAddMembers) {
        AddMembersDialog(
            onDismiss = { showAddMembers = false },
            onSearch = { query, onResult -> vm.searchAddableUsers(vm.memberIds, query, onResult) },
            onConfirm = { selectedUids ->
                showAddMembers = false
                vm.addMembers(chatId, selectedUids) { success -> showSnack(if (success) "Участники добавлены" else "Не удалось добавить участников") }
            }
        )
    }

    memberPendingAction?.let { member ->
        GroupMemberActionsSheet(
            member = member, onDismiss = { memberPendingAction = null },
            onToggleAdmin = {
                memberPendingAction = null
                if (member.isAdmin) vm.demoteAdmin(chatId, member.uid) { success, error -> showSnack(if (success) "Права администратора сняты" else (error ?: "Не удалось снять права")) }
                else vm.promoteToAdmin(chatId, member.uid) { success -> showSnack(if (success) "Назначен администратором" else "Не удалось назначить") }
            },
            onToggleModerator = {
                memberPendingAction = null
                if (member.isModerator) vm.demoteModerator(chatId, member.uid) { success -> showSnack(if (success) "Права модератора сняты" else "Не удалось снять права") }
                else vm.promoteToModerator(chatId, member.uid) { success -> showSnack(if (success) "Назначен модератором" else "Не удалось назначить") }
            },
            onKick = { memberPendingAction = null; memberPendingKick = member }
        )
    }

    memberPendingKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingKick = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text("Исключить ${member.name}?", color = MayasTheme.TextPrimary) },
            text = { Text("Участник больше не сможет писать в этой группе.", color = MayasTheme.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { memberPendingKick = null; vm.kickMember(chatId, member.uid) { success, error -> showSnack(if (success) "Участник исключён" else (error ?: "Не удалось исключить")) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text("Исключить") }
            },
            dismissButton = { TextButton(onClick = { memberPendingKick = null }) { Text("Отмена", color = MayasTheme.TextSecondary) } }
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
    LaunchedEffect(chatId) { vm.observeGroup(chatId) }

    var showAddMembers by remember { mutableStateOf(false) }
    var memberPendingAction by remember { mutableStateOf<GroupMemberUi?>(null) }
    var memberPendingKick by remember { mutableStateOf<GroupMemberUi?>(null) }

    fun showSnack(text: String) { coroutineScope.launch { snackbarHostState.showSnackbar(text) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MayasTheme.Surface)
                    // Поглощаем клики внутри карточки, чтобы не закрывать окошко
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
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
                        Text("Участники", color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${vm.memberIds.size}", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    }
                    if (vm.isMyAdmin) {
                        IconButton(onClick = { showAddMembers = true }) {
                            Icon(Icons.Default.PersonAdd, null, tint = MayasTheme.Accent)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp)) {
                    when {
                        vm.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(32.dp), color = MayasTheme.Accent)
                        vm.errorMessage != null -> Text(vm.errorMessage.orEmpty(), color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                        else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(vm.members, key = { it.uid }) { member ->
                                GroupMemberRow(member = member, isViewerAdmin = vm.isMyAdmin, isSelf = member.uid == vm.myUid,
                                    onClick = { onOpenProfile(member.uid, false) }, onManageClick = { memberPendingAction = member })
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
            onSearch = { query, onResult -> vm.searchAddableUsers(vm.memberIds, query, onResult) },
            onConfirm = { selectedUids ->
                showAddMembers = false
                vm.addMembers(chatId, selectedUids) { success -> showSnack(if (success) "Участники добавлены" else "Не удалось добавить участников") }
            }
        )
    }

    memberPendingAction?.let { member ->
        GroupMemberActionsSheet(
            member = member, onDismiss = { memberPendingAction = null },
            onToggleAdmin = {
                memberPendingAction = null
                if (member.isAdmin) vm.demoteAdmin(chatId, member.uid) { success, error -> showSnack(if (success) "Права администратора сняты" else (error ?: "Не удалось снять права")) }
                else vm.promoteToAdmin(chatId, member.uid) { success -> showSnack(if (success) "Назначен администратором" else "Не удалось назначить") }
            },
            onToggleModerator = {
                memberPendingAction = null
                if (member.isModerator) vm.demoteModerator(chatId, member.uid) { success -> showSnack(if (success) "Права модератора сняты" else "Не удалось снять права") }
                else vm.promoteToModerator(chatId, member.uid) { success -> showSnack(if (success) "Назначен модератором" else "Не удалось назначить") }
            },
            onKick = { memberPendingAction = null; memberPendingKick = member }
        )
    }

    memberPendingKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingKick = null },
            containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text("Исключить ${member.name}?", color = MayasTheme.TextPrimary) },
            text = { Text("Участник больше не сможет писать в этой группе.", color = MayasTheme.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { memberPendingKick = null; vm.kickMember(chatId, member.uid) { success, error -> showSnack(if (success) "Участник исключён" else (error ?: "Не удалось исключить")) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.ErrorRed)
                ) { Text("Исключить") }
            },
            dismissButton = { TextButton(onClick = { memberPendingKick = null }) { Text("Отмена", color = MayasTheme.TextSecondary) } }
        )
    }
}

// ── Приватные компоненты ─────────────────────────────────────────────────────

@Composable
private fun GroupMemberRow(
    member: GroupMemberUi, isViewerAdmin: Boolean, isSelf: Boolean,
    onClick: () -> Unit, onManageClick: () -> Unit
) {
    val glowColor = getGlowColor(member.profileGlow)

    val resolvedAvatarUrl by produceState<String?>(initialValue = null, member.avatarUrl, member.useCustomAvatar) {
        value = when {
            !member.useCustomAvatar || member.avatarUrl.isNullOrBlank() -> null
            member.avatarUrl!!.startsWith("http") -> member.avatarUrl
            else -> B2MediaClient.resolveDownloadUrl(member.avatarUrl!!)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MayasAvatar(url = resolvedAvatarUrl, icon = member.profileIcon, glowColor = glowColor,
            isPremium = member.isPremium, useCustomAvatar = member.useCustomAvatar, size = 46.dp, frameType = "none")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name + if (isSelf) " (вы)" else "", color = MayasTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (member.isOwner) { Spacer(Modifier.width(6.dp)); RoleChip("создатель", MayasTheme.GlowGold) }
                else if (member.isAdmin) { Spacer(Modifier.width(6.dp)); RoleChip("админ", MayasTheme.GlowPurple) }
                else if (member.isModerator) { Spacer(Modifier.width(6.dp)); RoleChip("модератор", MayasTheme.GlowBlue) }
            }
            if (member.username.isNotEmpty()) Text("@${member.username}", color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        if (isViewerAdmin && !member.isOwner && !isSelf) {
            IconButton(onClick = onManageClick) { Icon(Icons.Default.MoreVert, null, tint = MayasTheme.TextSecondary) }
        }
    }
}

@Composable
private fun RoleChip(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 7.dp, vertical = 2.dp)
    ) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GroupMemberActionsSheet(
    member: GroupMemberUi, onDismiss: () -> Unit,
    onToggleAdmin: () -> Unit, onToggleModerator: () -> Unit, onKick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(member.name, color = MayasTheme.TextPrimary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(if (member.isAdmin) "Снять права администратора" else "Назначить администратором") },
                    leadingContent = { Icon(if (member.isAdmin) Icons.Default.RemoveModerator else Icons.Default.AdminPanelSettings, null, tint = MayasTheme.Accent) },
                    modifier = Modifier.clickable { onToggleAdmin() }
                )
                ListItem(
                    headlineContent = { Text(if (member.isModerator) "Снять права модератора" else "Назначить модератором") },
                    leadingContent = { Icon(if (member.isModerator) Icons.Default.VerifiedUser else Icons.Default.Shield, null, tint = MayasTheme.Accent) },
                    modifier = Modifier.clickable { onToggleModerator() }
                )
                ListItem(
                    headlineContent = { Text("Исключить из группы", color = MayasTheme.ErrorRed) },
                    leadingContent = { Icon(Icons.Default.PersonRemove, null, tint = MayasTheme.ErrorRed) },
                    modifier = Modifier.clickable { onKick() }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = MayasTheme.TextSecondary) } }
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
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(500)
        isSearching = true
        onSearch(query) { found -> results = found; isSearching = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MayasTheme.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text("Добавить участников", color = MayasTheme.TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Поиск по @username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MayasTheme.Accent, unfocusedBorderColor = MayasTheme.TextSecondary.copy(0.3f))
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.height(280.dp)) {
                    when {
                        isSearching -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(28.dp), color = MayasTheme.Accent)
                        results.isEmpty() -> Text("Никого не нашли", color = MayasTheme.TextSecondary, modifier = Modifier.align(Alignment.Center))
                        else -> LazyColumn {
                            items(results, key = { it.uid }) { user ->
                                val isChecked = selected.contains(user.uid)
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { selected = if (isChecked) selected - user.uid else selected + user.uid }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = isChecked, onCheckedChange = { selected = if (it) selected + user.uid else selected - user.uid }, colors = CheckboxDefaults.colors(checkedColor = MayasTheme.Accent))
                                    Spacer(Modifier.width(4.dp))
                                    Column {
                                        Text(user.name, color = MayasTheme.TextPrimary, fontSize = 14.sp)
                                        if (user.username.isNotEmpty()) Text("@${user.username}", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected.toList()) }, enabled = selected.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.Accent)) { Text("Добавить (${selected.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = MayasTheme.TextSecondary) } }
    )
}