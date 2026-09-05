package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan1eidtj.data.FirestoreListenerCoordinator
import com.dan1eidtj.mayas.db.ChatEntity
import com.dan1eidtj.mayas.db.ChatRepository
import com.dan1eidtj.mayas.core_ui.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val myUid: String? get() = auth.currentUser?.uid

    val syncState = MutableStateFlow(SyncState.IDLE)

    private val _chats = MutableStateFlow<List<ChatEntity>>(emptyList())
    val chats: StateFlow<List<ChatEntity>> = _chats.asStateFlow()

    private val chatDocuments = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
    private val chatDocListeners = mutableMapOf<String, ListenerRegistration>()



    private val _myProfile = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val myProfile: StateFlow<Map<String, Any?>> = _myProfile.asStateFlow()







    private val _partnerPresence = MutableStateFlow<Map<String, Map<String, Any?>>>(emptyMap())
    val partnerPresence: StateFlow<Map<String, Map<String, Any?>>> = _partnerPresence.asStateFlow()

    // Счётчик непрочитанных уведомлений (пропущенные звонки, системные, промо/акции)
    // для бейджа на колокольчике в ChatListScreen
    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private var myProfileListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private val partnerListeners = mutableMapOf<String, ListenerRegistration>()

    private var listeningUid: String? = null




    private val teardown: () -> Unit = { stopListening() }




    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != listeningUid) {
            stopListening()
            if (uid != null) startListening()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        FirestoreListenerCoordinator.register(teardown)
        startListening()
    }

    fun startListening() {
        val uid = myUid ?: return
        if (uid == listeningUid && chatsListener != null) return

        stopListening()
        listeningUid = uid
        syncState.value = SyncState.SYNCING

        myProfileListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("ChatListVM", "Ошибка снапшота своего профиля", error)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    _myProfile.value = mapOf(
                        "name" to (doc.getString("name") ?: doc.getString("username") ?: "Я"),
                        "avatarUrl" to (doc.getString("avatarUrl") ?: ""),
                        "profileIcon" to (doc.getString("profileIcon") ?: "ghost"),
                        "profileGlow" to (doc.getString("profileGlow") ?: "purple"),
                        "useCustomAvatar" to (doc.getBoolean("useCustomAvatar") ?: false),
                        "activity" to (doc.getString("activity") ?: "в сети"),
                        "isPremium" to (doc.getBoolean("isPremium") ?: false),
                        "avatarFrame" to (doc.getString("avatarFrame") ?: "none"),
                        "nameColor" to (doc.getString("nameColor") ?: "gold"),
                        "isGroup" to false,
                        "verification" to runCatching { com.dan1eidtj.data.VerificationInfo.fromMap(doc.data) }.getOrDefault(com.dan1eidtj.data.VerificationInfo())
                    )
                }
            }

        // Слушаем коллекцию уведомлений пользователя: /users/{uid}/notifications
        // Ожидаемая структура документа: { type: "missed_call" | "system" | "promo", isRead: Boolean, ... }
        // Подгони путь/поля под свою реальную схему в Firestore, если она отличается
        notificationsListener = db.collection("users").document(uid)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatListVM", "Ошибка снапшота уведомлений", error)
                    return@addSnapshotListener
                }
                _unreadNotificationsCount.value = snapshot?.size() ?: 0
            }

        chatsListener = db.collection("userChats")
            .document(uid)
            .collection("chats")
            .addSnapshotListener { indexSnapshot, error ->
                if (error != null) {
                    Log.e("ChatListVM", "Ошибка снапшота userChats", error)
                    syncState.value = SyncState.OFFLINE
                    return@addSnapshotListener
                }

                if (indexSnapshot == null) return@addSnapshotListener

                syncState.value = SyncState.ONLINE

                val currentChatIds = indexSnapshot.documents
                    .map { it.id }
                    .toSet()

                val removedChatIds = chatDocListeners.keys - currentChatIds

                removedChatIds.forEach { chatId ->
                    chatDocListeners[chatId]?.remove()
                    chatDocListeners.remove(chatId)
                    chatDocuments.remove(chatId)
                }

                if (removedChatIds.isNotEmpty()) {
                    rebuildChatEntities(uid)
                }

                currentChatIds.forEach { chatId ->
                    if (!chatDocListeners.containsKey(chatId)) {
                        chatDocListeners[chatId] = db.collection("chats")
                            .document(chatId)
                            .addSnapshotListener { chatDoc, chatError ->
                                if (chatError != null) {
                                    Log.e(
                                        "ChatListVM",
                                        "Ошибка снапшота чата $chatId",
                                        chatError
                                    )
                                    return@addSnapshotListener
                                }

                                if (chatDoc == null || !chatDoc.exists()) {
                                    chatDocuments.remove(chatId)
                                    rebuildChatEntities(uid)
                                    return@addSnapshotListener
                                }

                                val participants =
                                    (chatDoc.get("participants") as? List<*>)
                                        ?.filterIsInstance<String>()
                                        ?: emptyList()

                                if (!participants.contains(uid)) {
                                    chatDocuments.remove(chatId)
                                    rebuildChatEntities(uid)
                                    return@addSnapshotListener
                                }

                                chatDocuments[chatId] = chatDoc

                                val partnerUids = chatDocuments.values
                                    .mapNotNull { doc ->
                                        val type = doc.getString("type") ?: "DIRECT"
                                        val isGroup =
                                            type == "GROUP" ||
                                                    (doc.getBoolean("isGroup") ?: false) ||
                                                    type == "CHANNEL" ||
                                                    type == "SAVED"

                                        if (isGroup) {
                                            null
                                        } else {
                                            (doc.get("participants") as? List<*>)
                                                ?.filterIsInstance<String>()
                                                ?.firstOrNull { it != uid }
                                        }
                                    }
                                    .toSet()

                                val toRemove = partnerListeners.keys - partnerUids
                                toRemove.forEach { partnerUid ->
                                    partnerListeners[partnerUid]?.remove()
                                    partnerListeners.remove(partnerUid)
                                    _partnerPresence.value =
                                        _partnerPresence.value - partnerUid
                                }

                                partnerUids.forEach { partnerUid ->
                                    if (!partnerListeners.containsKey(partnerUid)) {
                                        partnerListeners[partnerUid] =
                                            db.collection("users")
                                                .document(partnerUid)
                                                .addSnapshotListener { userDoc, userError ->
                                                    if (userError != null) {
                                                        Log.e(
                                                            "ChatListVM",
                                                            "Ошибка снапшота партнёра $partnerUid",
                                                            userError
                                                        )
                                                        return@addSnapshotListener
                                                    }

                                                    if (userDoc == null || !userDoc.exists()) {
                                                        return@addSnapshotListener
                                                    }

                                                    // Настройки приватности этого пользователя.
                                                    // Гейтим прямо тут, в единственной точке
                                                    // сборки presence-кэша — дальше это уходит
                                                    // и в живой список чатов, и в Room (ChatEntity),
                                                    // так что фикс здесь закрывает обе утечки разом.
                                                    val lastSeenAllowed =
                                                        (userDoc.getString("privacy_last_seen") ?: "all") == "all"
                                                    val photoAllowed =
                                                        (userDoc.getString("privacy_photo") ?: "all") == "all"

                                                    val presence = mapOf(
                                                        "lastSeen" to if (lastSeenAllowed) userDoc.getTimestamp("lastSeen") else null,
                                                        "isInvisible" to (userDoc.getBoolean("isInvisible") ?: false),
                                                        "typing" to userDoc.get("typing"),
                                                        "activity" to (userDoc.getString("activity") ?: ""),
                                                        "name" to (
                                                                userDoc.getString("name")
                                                                    ?: userDoc.getString("username")
                                                                    ?: "Аноним"
                                                                ),
                                                        "avatarUrl" to if (photoAllowed) userDoc.getString("avatarUrl") else null,
                                                        "profileIcon" to (userDoc.getString("profileIcon") ?: "ghost"),
                                                        "useCustomAvatar" to if (photoAllowed) (userDoc.getBoolean("useCustomAvatar") ?: false) else false,
                                                        "profileGlow" to (userDoc.getString("profileGlow") ?: "purple"),
                                                        "isPremium" to (userDoc.getBoolean("isPremium") ?: false),
                                                        "avatarFrame" to (userDoc.getString("avatarFrame") ?: "none"),
                                                        "nameColor" to (userDoc.getString("nameColor") ?: "gold"),
                                                        "isGroup" to false,
                                                        "emoji" to userDoc.getString("emojiStatus"),
                                                        "verification" to runCatching { com.dan1eidtj.data.VerificationInfo.fromMap(userDoc.data) }.getOrDefault(com.dan1eidtj.data.VerificationInfo())
                                                    )

                                                    _partnerPresence.value =
                                                        _partnerPresence.value + (partnerUid to presence)

                                                    rebuildChatEntities(uid)
                                                }
                                    }
                                }

                                rebuildChatEntities(uid)
                            }
                    }
                }
            }
    }

    private fun rebuildChatEntities(uid: String) {
        val entities = chatDocuments.values.mapNotNull { doc ->
            try {
                val type = doc.getString("type") ?: "DIRECT"
                val isSavedMessages = type == "SAVED"
                val isGroup = isSavedMessages ||
                        type == "GROUP" ||
                        type == "CHANNEL" ||
                        (doc.getBoolean("isGroup") ?: false)

                val participants = (doc.get("participants") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val partnerUid = if (!isGroup) {
                    participants.firstOrNull { it != uid }
                } else {
                    null
                }

                val partner = partnerUid?.let { _partnerPresence.value[it] }

                val resolvedGroupAvatar =
                    doc.getString("groupAvatarUrl")
                        ?: doc.getString("groupAvatar")

                ChatEntity(
                    chatId = doc.id,
                    isGroup = isGroup,
                    chatType = type,
                    groupName = doc.getString("groupName") ?: doc.getString("title"),
                    groupAvatarUrl = resolvedGroupAvatar,
                    groupIcon = doc.getString("profileIcon") ?: doc.getString("groupIcon"),
                    groupProfileGlow = doc.getString("profileGlow") ?: "purple",
                    useCustomAvatar = if (isGroup) {
                        !resolvedGroupAvatar.isNullOrBlank()
                    } else {
                        doc.getBoolean("useCustomAvatar") ?: false
                    },
                    lastMessage = doc.getString("lastMessage"),
                    unreadCount = (doc.getLong("unreadCount_$uid") ?: 0L).toInt(),
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                    description = doc.getString("description"),
                    ownerId = doc.getString("ownerId"),
                    adminsList = (doc.get("admins") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList(),
                    isPublic = doc.getBoolean("isPublic") ?: false,
                    isPinned = doc.getBoolean("pinned_$uid") ?: false,
                    partnerUid = partnerUid,
                    partnerName = partner?.get("name") as? String,
                    partnerAvatarUrl = partner?.get("avatarUrl") as? String,
                    partnerProfileIcon = partner?.get("profileIcon") as? String ?: "ghost",
                    partnerProfileGlow = partner?.get("profileGlow") as? String ?: "purple",
                    partnerUseCustomAvatar = partner?.get("useCustomAvatar") as? Boolean ?: false,
                    partnerIsPremium = partner?.get("isPremium") as? Boolean ?: false,
                    partnerAvatarFrame = partner?.get("avatarFrame") as? String ?: "none",
                    partnerNameColor = partner?.get("nameColor") as? String ?: "gold",
                    partnerEmoji = partner?.get("emoji") as? String,
                    typingText = null,
                    isSavedMessages = isSavedMessages
                )
            } catch (e: Exception) {
                Log.e("ChatListVM", "Ошибка конвертации чата ${doc.id}", e)
                null
            }
        }

        _chats.value = entities.sortedByDescending { it.updatedAt }
    }

    fun stopListening() {
        chatsListener?.remove()
        chatsListener = null

        chatDocListeners.values.forEach { it.remove() }
        chatDocListeners.clear()
        chatDocuments.clear()
        _chats.value = emptyList()

        myProfileListener?.remove()
        myProfileListener = null
        notificationsListener?.remove()
        notificationsListener = null
        partnerListeners.values.forEach { it.remove() }
        partnerListeners.clear()
        listeningUid = null
        _myProfile.value = emptyMap()
        _partnerPresence.value = emptyMap()
        _unreadNotificationsCount.value = 0
        syncState.value = SyncState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        FirestoreListenerCoordinator.unregister(teardown)
        stopListening()
    }

    fun openOrCreateDirectChat(myUid: String, partnerUid: String, onReady: (String) -> Unit) {
        val chatId = Screen.getChatId(myUid, partnerUid)
        val chatRef = db.collection("chats").document(chatId)
        viewModelScope.launch {
            try {
                val snapshot = chatRef.get().await()
                if (!snapshot.exists()) {
                    chatRef.set(
                        mapOf(
                            "type" to "DIRECT",
                            "participants" to listOf(myUid, partnerUid),
                            "lastMessage" to "",
                            "lastSenderId" to "",
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "typing" to mapOf(myUid to false, partnerUid to false),
                            "unreadCount_$myUid" to 0,
                            "unreadCount_$partnerUid" to 0
                        )
                    ).await()
                }

                db.collection("userChats")
                    .document(myUid)
                    .collection("chats")
                    .document(chatId)
                    .set(mapOf("chatId" to chatId), SetOptions.merge())
                    .await()

                db.collection("userChats")
                    .document(partnerUid)
                    .collection("chats")
                    .document(chatId)
                    .set(mapOf("chatId" to chatId), SetOptions.merge())
                    .await()

                onReady(chatId)
            } catch (e: Exception) {
                Log.e("ChatListVM", "Ошибка создания чата", e)
            }
        }
    }

    fun findPublicChannelByUsername(username: String, onResult: (Map<String, Any?>?) -> Unit) {
        val query = username.lowercase().trim().removePrefix("@")
        if (query.isBlank()) { onResult(null); return }

        db.collection("chats")
            .whereEqualTo("username", query)
            .whereEqualTo("type", "CHANNEL")
            .whereEqualTo("isPublic", true)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) {
                    onResult(null)
                } else {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["chatId"] = doc.id
                    onResult(data)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatListVM", "Ошибка поиска канала", e)
                onResult(null)
            }
    }

    fun joinPublicChannel(chatId: String, myUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        db.collection("chats").document(chatId)
            .update(
                "participants", FieldValue.arrayUnion(myUid),
                "members", FieldValue.arrayUnion(myUid),
                "unreadCount_$myUid", 0
            )
            .addOnSuccessListener { onSuccess(chatId) }
            .addOnFailureListener { e ->
                Log.e("ChatListVM", "Ошибка подписки на канал", e)
                onError(e.localizedMessage ?: "Не удалось подписаться")
            }
    }
}

enum class SyncState { IDLE, SYNCING, ONLINE, OFFLINE }