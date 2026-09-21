/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.chats.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class GlobalUserResult(
    val uid: String,
    val name: String,
    val username: String?,
    val avatarUrl: String?
)

data class GlobalChatResult(
    val chatId: String,
    val title: String,
    val isGroup: Boolean,
    val isChannel: Boolean,
    val isJoined: Boolean
)

data class GlobalMessageResult(
    val chatId: String,
    val messageId: String,
    val chatTitle: String,
    val senderName: String,
    val text: String,
    val timestamp: Date?
)

class GlobalSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    var isLoading by mutableStateOf(false)
        private set
    var hasSearched by mutableStateOf(false)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    var userResults by mutableStateOf<List<GlobalUserResult>>(emptyList())
        private set
    var channelResults by mutableStateOf<List<GlobalChatResult>>(emptyList())
        private set
    var messageResults by mutableStateOf<List<GlobalMessageResult>>(emptyList())
        private set

    private var searchJob: Job? = null

    fun clear() {
        searchJob?.cancel()
        isLoading = false
        hasSearched = false
        errorText = null
        userResults = emptyList()
        channelResults = emptyList()
        messageResults = emptyList()
    }

    fun search(rawQuery: String, myUid: String) {
        searchJob?.cancel()
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) {
            clear()
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            isLoading = true
            errorText = null
            try {
                val usernameQuery = trimmed.lowercase().removePrefix("@")

                val usersDeferred = async {
                    db.collection("users")
                        .whereGreaterThanOrEqualTo("username", usernameQuery)
                        .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                        .limit(10)
                        .get().await()
                }

                val channelsDeferred = async {
                    db.collection("chats")
                        .whereEqualTo("type", "CHANNEL")
                        .whereEqualTo("isPublic", true)
                        .whereGreaterThanOrEqualTo("username", usernameQuery)
                        .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                        .limit(10)
                        .get().await()
                }

                val messagesDeferred = async {
                    db.collectionGroup("messages")
                        .whereArrayContains("readBy", myUid)
                        .whereGreaterThanOrEqualTo("text", trimmed)
                        .whereLessThanOrEqualTo("text", trimmed + "\uf8ff")
                        .orderBy("text")
                        .limit(20)
                        .get().await()
                }

                val usersSnap = runCatching { usersDeferred.await() }.getOrNull()
                val channelsSnap = runCatching { channelsDeferred.await() }.getOrNull()
                val messagesSnap = runCatching { messagesDeferred.await() }.getOrNull()

                userResults = usersSnap?.documents?.mapNotNull { doc ->
                    if (doc.id == myUid) return@mapNotNull null
                    GlobalUserResult(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("username") ?: "",
                        username = doc.getString("username"),
                        avatarUrl = doc.getString("avatarUrl")
                    )
                } ?: emptyList()

                channelResults = channelsSnap?.documents?.mapNotNull { doc ->
                    GlobalChatResult(
                        chatId = doc.id,
                        title = doc.getString("groupName") ?: doc.getString("username") ?: "",
                        isGroup = true,
                        isChannel = true,
                        isJoined = (doc.get("participants") as? List<*>)?.contains(myUid) == true
                    )
                } ?: emptyList()

                val rawMessages = messagesSnap?.documents.orEmpty()
                val chatIds = rawMessages.mapNotNull { it.reference.parent.parent?.id }.distinct().take(15)
                val chatTitles = mutableMapOf<String, String>()
                if (chatIds.isNotEmpty()) {
                    val chatDocs = chatIds.map { id -> async { id to runCatching { db.collection("chats").document(id).get().await() }.getOrNull() } }
                        .map { it.await() }
                    chatDocs.forEach { (id, doc) ->
                        chatTitles[id] = doc?.getString("groupName") ?: ""
                    }
                }

                messageResults = rawMessages.mapNotNull { doc ->
                    val chatId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    val senderName = doc.getString("senderName") ?: ""
                    GlobalMessageResult(
                        chatId = chatId,
                        messageId = doc.id,
                        chatTitle = chatTitles[chatId]?.takeIf { it.isNotBlank() } ?: senderName,
                        senderName = senderName,
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getDate("timestamp")
                    )
                }

                hasSearched = true
            } catch (e: Exception) {
                Log.e("GlobalSearchVM", "Ошибка глобального поиска", e)
                errorText = "error"
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(
    myUid: String,
    localChats: List<Map<String, Any>>,
    onBack: () -> Unit,
    onOpenChat: (chatId: String, messageId: String?) -> Unit,
    onOpenProfile: (uid: String) -> Unit
) {
    val vm: GlobalSearchViewModel = viewModel()
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val localMatches = remember(query, localChats) {
        val q = query.trim()
        if (q.length < 2) emptyList() else localChats.filter { chat ->
            val isGroup = chat["isGroup"] as? Boolean ?: false
            val title = if (isGroup) chat["groupName"] as? String ?: "" else chat["partnerName"] as? String ?: ""
            title.contains(q, ignoreCase = true)
        }
    }

    LaunchedEffect(query) {
        vm.search(query, myUid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MayasTheme.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MayasTheme.TextPrimary)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint_global), color = MayasTheme.TextSecondary) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MayasTheme.TextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, null, tint = MayasTheme.TextSecondary)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        when {
            query.trim().length < 2 -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.search_start_typing),
                        color = MayasTheme.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            vm.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MayasTheme.GlowPurple)
                }
            }
            vm.errorText != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.search_error), color = MayasTheme.ErrorRed, fontSize = 14.sp)
                }
            }
            localMatches.isEmpty() && vm.userResults.isEmpty() && vm.channelResults.isEmpty() && vm.messageResults.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.search_no_results), color = MayasTheme.TextSecondary, fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (localMatches.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_chats)) }
                        items(localMatches, key = { "chat_${it["chatId"]}" }) { chat ->
                            val chatId = chat["chatId"] as? String ?: return@items
                            val isGroup = chat["isGroup"] as? Boolean ?: false
                            val title = if (isGroup) chat["groupName"] as? String ?: "" else chat["partnerName"] as? String ?: ""
                            SearchResultRow(
                                title = title,
                                subtitle = null,
                                icon = if (isGroup) Icons.Default.Group else Icons.Default.Person,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenChat(chatId, null)
                                }
                            )
                        }
                    }

                    if (vm.userResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_users)) }
                        items(vm.userResults, key = { "user_${it.uid}" }) { user ->
                            SearchResultRow(
                                title = user.name,
                                subtitle = user.username?.let { "@$it" },
                                icon = Icons.Default.Person,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenProfile(user.uid)
                                }
                            )
                        }
                    }

                    if (vm.channelResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_channels)) }
                        items(vm.channelResults, key = { "channel_${it.chatId}" }) { channel ->
                            SearchResultRow(
                                title = channel.title,
                                subtitle = if (channel.isJoined) null else "•",
                                icon = Icons.Default.Group,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenChat(channel.chatId, null)
                                }
                            )
                        }
                    }

                    if (vm.messageResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_messages)) }
                        items(vm.messageResults, key = { "msg_${it.messageId}" }) { message ->
                            SearchResultRow(
                                title = message.chatTitle,
                                subtitle = message.text,
                                icon = Icons.Default.Search,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenChat(message.chatId, message.messageId)
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(text: String) {
    Text(
        text = text,
        color = MayasTheme.GlowPurple,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MayasTheme.SurfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MayasTheme.GlowPurple, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MayasTheme.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MayasTheme.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
