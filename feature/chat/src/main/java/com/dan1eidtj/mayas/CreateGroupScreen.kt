package com.dan1eidtj.mayas.feature.chat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.MayasAvatar
import com.dan1eidtj.mayas.core_ui.utils.getGlowColor
import com.dan1eidtj.mayas.feature.ChatVM
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.ImageCompressor
import com.dan1eidtj.mayas.storage.MediaKind
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


data class SelectableUser(
    val uid: String,
    val name: String,
    val username: String = "",
    val avatarUrl: String? = null,
    val useCustomAvatar: Boolean = false,
    val profileIcon: String = "ghost",
    val profileGlow: String = "purple",
    val isPremium: Boolean = false,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val chatVM: ChatVM = viewModel()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()


    var step by remember { mutableIntStateOf(1) }
    var contacts by remember { mutableStateOf<List<SelectableUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }


    var groupTitle by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var groupAvatarKey by remember { mutableStateOf<String?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pickedUri ->
            coroutineScope.launch {
                isUploadingAvatar = true
                try {
                    val bytes = ImageCompressor.compressAvatar(context, pickedUri)
                    val myUid = chatVM.myUid ?: ""



                    val key = B2MediaClient().uploadMedia(
                        kind = MediaKind.AVATAR,
                        ownerId = myUid,
                        bytes = bytes,
                        contentType = "image/jpeg",
                        extension = "jpg"
                    )
                    groupAvatarKey = key
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    val selectedUsers = contacts.filter { it.isSelected }


    val filteredContacts = contacts.filter { contact ->
        contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.username.contains(searchQuery, ignoreCase = true)
    }



    LaunchedEffect(Unit) {
        val myUid = chatVM.myUid ?: ""

        if (myUid.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }


        db.collection("chats")
            .whereArrayContains("participants", myUid)
            .get()
            .addOnSuccessListener { chatsSnap ->


                val partnerIds = chatsSnap.documents.mapNotNull { doc ->
                    val isGroup = doc.getBoolean("isGroup") ?: false
                    if (!isGroup) {
                        val participants = doc.get("participants") as? List<*>

                        participants?.firstOrNull { it != myUid } as? String
                    } else {
                        null
                    }
                }.distinct()

                if (partnerIds.isEmpty()) {
                    contacts = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }




                val loadedUsers = mutableListOf<SelectableUser>()
                var processedCount = 0

                partnerIds.forEach { partnerId ->
                    db.collection("users").document(partnerId).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val username = userDoc.getString("username") ?: ""
                                val name = userDoc.getString("name") ?: username.ifEmpty { "User" }
                                val avatarUrl = userDoc.getString("avatarUrl")
                                val useCustomAvatar = userDoc.getBoolean("useCustomAvatar") ?: false
                                val profileIcon = userDoc.getString("profileIcon") ?: "ghost"
                                val profileGlow = userDoc.getString("profileGlow") ?: "purple"
                                val isPremium = userDoc.getBoolean("isPremium") ?: false

                                loadedUsers.add(
                                    SelectableUser(
                                        uid = partnerId,
                                        name = name,
                                        username = username,
                                        avatarUrl = avatarUrl,
                                        useCustomAvatar = useCustomAvatar,
                                        profileIcon = profileIcon,
                                        profileGlow = profileGlow,
                                        isPremium = isPremium
                                    )
                                )
                            }

                            processedCount++

                            if (processedCount == partnerIds.size) {
                                contacts = loadedUsers.sortedBy { it.name }
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == partnerIds.size) {
                                contacts = loadedUsers.sortedBy { it.name }
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayasTheme.Surface),
                title = {
                    Column {
                        Text(
                            text = if (step == 1) "Новая группа" else "Название группы",
                            color = MayasTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (step == 1 && selectedUsers.isNotEmpty()) {
                            Text(
                                text = "${selectedUsers.size} из ${contacts.size} выбрано",
                                color = MayasTheme.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedUsers.isNotEmpty()) {
                FloatingActionButton(
                    containerColor = MayasTheme.GlowPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            if (groupTitle.isBlank()) {
                                Toast.makeText(context, "Введите название группы", Toast.LENGTH_SHORT).show()
                                return@FloatingActionButton
                            }
                            val selectedIds = selectedUsers.map { it.uid }


                            chatVM.createGroupChat(
                                title = groupTitle,
                                description = groupDescription,
                                isPublic = isPublic,
                                selectedUserIds = selectedIds,
                                groupAvatar = groupAvatarKey
                            ) { newChatId ->
                                onGroupCreated(newChatId)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MayasTheme.Background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MayasTheme.GlowPurple)
            } else {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "StepAnimation"
                ) { currentStep ->
                    if (currentStep == 1) {
                        Column(modifier = Modifier.fillMaxSize()) {

                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                placeholder = { Text("Поиск участников...", color = MayasTheme.TextSecondary) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск", tint = MayasTheme.TextSecondary) },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MayasTheme.Surface,
                                    unfocusedContainerColor = MayasTheme.Surface,
                                    disabledContainerColor = MayasTheme.Surface,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = MayasTheme.GlowPurple
                                )
                            )

                            AnimatedVisibility(visible = selectedUsers.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(selectedUsers, key = { it.uid }) { user ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(55.dp)
                                        ) {
                                            Box(modifier = Modifier.size(45.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(MayasTheme.GlowPurple.copy(0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(user.name.take(1).uppercase(), color = MayasTheme.TextPrimary, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                        .align(Alignment.BottomEnd)
                                                        .clickable {
                                                            contacts = contacts.map {
                                                                if (it.uid == user.uid) it.copy(isSelected = false) else it
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = user.name,
                                                fontSize = 11.sp,
                                                color = MayasTheme.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedUsers.isNotEmpty()) {
                                HorizontalDivider(color = MayasTheme.TextSecondary.copy(0.1f))
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                items(filteredContacts) { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (user.isSelected) MayasTheme.GlowPurple.copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable {
                                                contacts = contacts.map {
                                                    if (it.uid == user.uid) it.copy(isSelected = !it.isSelected) else it
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            MayasAvatar(
                                                url = user.avatarUrl,
                                                icon = user.profileIcon,
                                                glowColor = getGlowColor(user.profileGlow),
                                                isPremium = user.isPremium,
                                                size = 42.dp,
                                                useCustomAvatar = user.useCustomAvatar,
                                                frameType = "none"
                                            )
                                            if (user.isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .clip(CircleShape)
                                                        .background(MayasTheme.GlowPurple)
                                                        .border(2.dp, MayasTheme.Background, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(9.dp))
                                                }
                                            }
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        Text(
                                            text = user.name,
                                            color = MayasTheme.TextPrimary,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )

                                        RadioDot(user.isSelected)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
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
                                        .clickable(enabled = !isUploadingAvatar) { avatarLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (groupAvatarKey != null) {
                                        MayasAvatar(
                                            url = groupAvatarKey,
                                            icon = "ghost",
                                            glowColor = MayasTheme.GlowPurple,
                                            isPremium = false,
                                            useCustomAvatar = true,
                                            size = 72.dp,
                                            frameType = "none"
                                        )
                                    } else {
                                        Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    if (isUploadingAvatar) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                        }
                                    }
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
                                        value = groupTitle,
                                        onValueChange = { if (it.length <= 32) groupTitle = it },
                                        placeholder = { Text("Название группы", color = MayasTheme.TextSecondary) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                        colors = groupTgTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "${groupTitle.length}/32",
                                        color = MayasTheme.TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            TextField(
                                value = groupDescription,
                                onValueChange = { groupDescription = it },
                                placeholder = { Text("Описание группы (необязательно)", color = MayasTheme.TextSecondary) },
                                maxLines = 3,
                                colors = groupTgTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            )

                            GroupSectionLabel("ТИП ГРУППЫ")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MayasTheme.Surface)
                            ) {
                                GroupPrivacyRow(
                                    icon = Icons.Outlined.Public,
                                    title = "Публичная группа",
                                    subtitle = "Виден в поиске, вступить может любой",
                                    selected = isPublic,
                                    onClick = { isPublic = true }
                                )
                                HorizontalDivider(color = MayasTheme.TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(start = 60.dp))
                                GroupPrivacyRow(
                                    icon = Icons.Outlined.Lock,
                                    title = "Приватная группа",
                                    subtitle = "Присоединиться можно только по приглашению",
                                    selected = !isPublic,
                                    onClick = { isPublic = false }
                                )
                            }

                            GroupSectionLabel("УЧАСТНИКИ")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MayasTheme.Surface)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.People, null, tint = MayasTheme.GlowPurple, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Участников: ${selectedUsers.size}",
                                    color = MayasTheme.TextPrimary,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSectionLabel(text: String) {
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
private fun GroupPrivacyRow(
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
private fun groupTgTextFieldColors() = TextFieldDefaults.colors(
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