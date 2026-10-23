/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

data class GlobalUserResult(
    val uid: String,
    val name: String,
    val username: String?,
    val avatarUrl: String?,
    val description: String? = null
)

data class GlobalChatResult(
    val chatId: String,
    val title: String,
    val isGroup: Boolean,
    val isChannel: Boolean,
    val isJoined: Boolean,
    val username: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null
)

data class GlobalMessageResult(
    val chatId: String,
    val messageId: String,
    val chatTitle: String,
    val senderName: String,
    val text: String,
    val timestamp: Date?
)

private data class SearchableChat(
    val chatId: String,
    val title: String
)

private data class RecentMessageFetch(
    val startedAtMillis: Long,
    val request: Deferred<List<QueryDocumentSnapshot>?>
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
    var groupResults by mutableStateOf<List<GlobalChatResult>>(emptyList())
        private set
    var messageResults by mutableStateOf<List<GlobalMessageResult>>(emptyList())
        private set

    private var searchJob: Job? = null
    private val recentMessageFetches = ConcurrentHashMap<String, RecentMessageFetch>()
    private val recentMessageCacheTtlMillis = 15_000L

    private fun fetchRecentMessages(chatId: String): Deferred<List<QueryDocumentSnapshot>?> {
        val now = System.currentTimeMillis()
        val existing = recentMessageFetches[chatId]
        if (existing != null && !existing.request.isCancelled &&
            (!existing.request.isCompleted || now - existing.startedAtMillis < recentMessageCacheTtlMillis)
        ) {
            return existing.request
        }

        val request = viewModelScope.async {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(40)
                    .get().await().documents
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("GlobalSearchVM", "Не удалось загрузить сообщения чата $chatId", e)
                null
            }
        }
        recentMessageFetches[chatId] = RecentMessageFetch(now,
            request as Deferred<List<QueryDocumentSnapshot>?>
        )
        return request
    }

    fun clear() {
        searchJob?.cancel()
        isLoading = false
        hasSearched = false
        errorText = null
        userResults = emptyList()
        channelResults = emptyList()
        groupResults = emptyList()
        messageResults = emptyList()
    }

    fun search(rawQuery: String, myUid: String, localChats: List<Map<String, Any>>) {
        searchJob?.cancel()
        val now = System.currentTimeMillis()
        recentMessageFetches.entries.removeIf { (_, cached) ->
            cached.request.isCompleted && now - cached.startedAtMillis >= recentMessageCacheTtlMillis
        }
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) {
            clear()
            return
        }

        isLoading = true
        hasSearched = false
        errorText = null
        searchJob = viewModelScope.launch {
            try {
                delay(350)
                val usernameQuery = trimmed.lowercase().removePrefix("@")

                val usersDeferred = async {
                    runCatching {
                        db.collection("users")
                            .whereGreaterThanOrEqualTo("username", usernameQuery)
                            .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                            .limit(10)
                            .get().await()
                    }.getOrNull()
                }

                val channelsDeferred = async {
                    runCatching {
                        db.collection("chats")
                            .whereEqualTo("type", "CHANNEL")
                            .whereEqualTo("isPublic", true)
                            .whereGreaterThanOrEqualTo("username", usernameQuery)
                            .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                            .limit(10)
                            .get().await()
                    }.getOrNull()
                }

                val groupsByUsernameDeferred = async {
                    runCatching {
                        db.collection("chats")
                            .whereEqualTo("type", "GROUP")
                            .whereEqualTo("isPublic", true)
                            .whereGreaterThanOrEqualTo("username", usernameQuery)
                            .whereLessThanOrEqualTo("username", usernameQuery + "\uf8ff")
                            .limit(10)
                            .get().await()
                    }.getOrNull()
                }


                val claimedUserDeferred = async {
                    if (!trimmed.startsWith("@") || !usernameQuery.matches(Regex("^[a-z0-9_]{5,32}$"))) return@async null
                    runCatching {
                        val claim = db.collection("usernameClaims").document(usernameQuery).get().await()
                        val kind = claim.getString("kind")
                        if (!claim.exists() || kind !in setOf("MAIN", "ADDITIONAL")) return@runCatching null
                        val uid = claim.getString("uid") ?: return@runCatching null
                        db.collection("users").document(uid).get().await().takeIf { it.exists() }
                    }.getOrNull()
                }

                val searchableChats = localChats.mapNotNull { chat ->
                    val chatId = chat["chatId"] as? String ?: return@mapNotNull null
                    if (chatId.isBlank()) return@mapNotNull null
                    val isGroup = chat["isGroup"] as? Boolean ?: false
                    val title = if (isGroup) {
                        (chat["groupName"] as? String).orEmpty()
                    } else {
                        (chat["partnerName"] as? String).orEmpty()
                    }
                    SearchableChat(chatId, title)
                }.distinctBy { it.chatId }.take(30)



                val messageSearches = searchableChats.map { chat ->
                    async {
                        val request = fetchRecentMessages(chat.chatId)
                        val documents = request.await()
                        if (documents == null) {
                            recentMessageFetches.computeIfPresent(chat.chatId) { _, cached ->
                                if (cached.request === request) null else cached
                            }
                        }
                        chat to documents.orEmpty().filter { doc ->
                            doc.getString("text")?.contains(trimmed, ignoreCase = true) == true
                        }
                    }
                }

                val usersSnap = usersDeferred.await()
                val claimedUser = claimedUserDeferred.await()
                val channelsSnap = channelsDeferred.await()
                val groupsByUsernameSnap = groupsByUsernameDeferred.await()
                val searchedMessages = messageSearches.map { it.await() }

                if (usersSnap == null && claimedUser == null && channelsSnap == null &&
                    groupsByUsernameSnap == null && searchedMessages.none { it.second.isNotEmpty() }
                ) {
                    errorText = "error"
                    hasSearched = true
                    return@launch
                }

                val matchingUserDocs = (usersSnap?.documents.orEmpty() + listOfNotNull(claimedUser))
                    .distinctBy { it.id }
                userResults = matchingUserDocs.mapNotNull { doc ->
                    if (doc.id == myUid) return@mapNotNull null
                    val canShowPhoto = (doc.getString("privacy_photo") ?: "all") == "all"
                    GlobalUserResult(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("username") ?: "",
                        username = doc.getString("username"),
                        avatarUrl = doc.getString("avatarUrl").takeIf { canShowPhoto },
                        description = doc.getString("description")
                    )
                }.take(10)

                channelResults = channelsSnap?.documents.orEmpty().map { doc ->
                    GlobalChatResult(
                        chatId = doc.id,
                        title = doc.getString("groupName") ?: doc.getString("username") ?: "",
                        isGroup = true,
                        isChannel = true,
                        isJoined = (doc.get("participants") as? List<*>)?.contains(myUid) == true,
                        username = doc.getString("username"),
                        avatarUrl = doc.getString("groupAvatarUrl") ?: doc.getString("groupAvatar"),
                        description = doc.getString("description")
                    )
                }.take(10)

                groupResults = groupsByUsernameSnap?.documents.orEmpty().map { doc ->
                        GlobalChatResult(
                            chatId = doc.id,
                            title = doc.getString("groupName") ?: doc.getString("username") ?: "",
                            isGroup = true,
                            isChannel = false,
                            isJoined = (doc.get("participants") as? List<*>)?.contains(myUid) == true,
                            username = doc.getString("username"),
                            avatarUrl = doc.getString("groupAvatarUrl") ?: doc.getString("groupAvatar"),
                            description = doc.getString("description")
                        )
                    }.take(10)

                messageResults = searchedMessages.flatMap { (chat, documents) ->
                    documents.map { doc ->
                        GlobalMessageResult(
                            chatId = chat.chatId,
                            messageId = doc.id,
                            chatTitle = chat.title.ifBlank { doc.getString("senderName") ?: "" },
                            senderName = doc.getString("senderName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getDate("timestamp")
                        )
                    }
                }.sortedByDescending { it.timestamp?.time ?: 0L }.take(20)

                hasSearched = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GlobalSearchVM", "Ошибка глобального поиска", e)
                errorText = "error"
            } finally {
                if (searchJob === coroutineContext[Job]) isLoading = false
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
        vm.search(query, myUid, localChats)
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
            localMatches.isEmpty() && vm.userResults.isEmpty() && vm.channelResults.isEmpty() &&
                vm.groupResults.isEmpty() && vm.messageResults.isEmpty() -> {
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
                            val imageUrl = if (isGroup) {
                                (chat["groupAvatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                                    ?: (chat["groupAvatar"] as? String)?.takeIf { it.isNotBlank() }
                            } else {
                                (chat["partnerAvatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                            }
                            val subtitle = (chat["description"] as? String)
                                ?.takeIf { isGroup && it.isNotBlank() }
                            SearchResultRow(
                                title = title,
                                subtitle = subtitle,
                                imageUrl = imageUrl,
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
                            val subtitle = listOfNotNull(
                                user.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
                                user.description?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").takeIf { it.isNotBlank() }
                            SearchResultRow(
                                title = user.name,
                                subtitle = subtitle,
                                imageUrl = user.avatarUrl,
                                icon = Icons.Default.Person,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenProfile(user.uid)
                                }
                            )
                        }
                    }

                    if (vm.groupResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_groups)) }
                        items(vm.groupResults, key = { "group_${it.chatId}" }) { group ->
                            val subtitle = listOfNotNull(
                                group.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
                                group.description?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").takeIf { it.isNotBlank() }
                            SearchResultRow(
                                title = group.title,
                                subtitle = subtitle,
                                imageUrl = group.avatarUrl,
                                icon = Icons.Default.Group,
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenChat(group.chatId, null)
                                }
                            )
                        }
                    }

                    if (vm.channelResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.search_section_channels)) }
                        items(vm.channelResults, key = { "channel_${it.chatId}" }) { channel ->
                            val subtitle = listOfNotNull(
                                channel.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
                                channel.description?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").takeIf { it.isNotBlank() }
                            SearchResultRow(
                                title = channel.title,
                                subtitle = subtitle,
                                imageUrl = channel.avatarUrl,
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
    imageUrl: String? = null,
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
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(icon, null, tint = MayasTheme.GlowPurple, modifier = Modifier.size(20.dp))
            }
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
