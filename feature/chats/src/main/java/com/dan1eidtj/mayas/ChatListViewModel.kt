package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan1eidtj.data.FirestoreListenerCoordinator
import com.dan1eidtj.mayas.db.ChatEntity
import com.dan1eidtj.mayas.db.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val myUid: String? get() = auth.currentUser?.uid

    val syncState = MutableStateFlow(SyncState.IDLE)

    val chats: StateFlow<List<ChatEntity>> = repository
        .getChats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )



    private val _myProfile = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val myProfile: StateFlow<Map<String, Any?>> = _myProfile.asStateFlow()







    private val _partnerPresence = MutableStateFlow<Map<String, Map<String, Any?>>>(emptyMap())
    val partnerPresence: StateFlow<Map<String, Map<String, Any?>>> = _partnerPresence.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private var myProfileListener: ListenerRegistration? = null
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
                        "isGroup" to false
                    )
                }
            }

        chatsListener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatListVM", "Ошибка снапшота чатов", error)
                    syncState.value = SyncState.OFFLINE
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                syncState.value = SyncState.ONLINE

                viewModelScope.launch {
                    try {
                        repository.syncChatsFromSnapshot(snapshot, uid)
                    } catch (e: Exception) {
                        Log.e("ChatListVM", "Ошибка синхронизации чатов", e)
                    }
                }

                val partnerUids = snapshot.documents
                    .filter { doc ->
                        val type = doc.getString("type") ?: "DIRECT"
                        val isGroup = type == "GROUP" || (doc.getBoolean("isGroup") ?: false)
                        !isGroup
                    }
                    .flatMap { doc ->
                        (doc.get("participants") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.filter { it != uid }
                            ?: emptyList()
                    }
                    .toSet()

                val toRemove = partnerListeners.keys - partnerUids
                toRemove.forEach { partnerUid ->
                    partnerListeners[partnerUid]?.remove()
                    partnerListeners.remove(partnerUid)
                    _partnerPresence.value = _partnerPresence.value - partnerUid
                }

                partnerUids.forEach { partnerUid ->
                    if (!partnerListeners.containsKey(partnerUid)) {
                        partnerListeners[partnerUid] = db.collection("users")
                            .document(partnerUid)
                            .addSnapshotListener { userDoc, error ->
                                if (error != null) {
                                    Log.e("ChatListVM", "Ошибка снапшота партнёра $partnerUid", error)
                                    return@addSnapshotListener
                                }
                                if (userDoc == null || !userDoc.exists()) return@addSnapshotListener

                                _partnerPresence.value = _partnerPresence.value + (partnerUid to mapOf(
                                    "lastSeen" to userDoc.getTimestamp("lastSeen"),
                                    "isInvisible" to (userDoc.getBoolean("isInvisible") ?: false),
                                    "typing" to userDoc.get("typing"),
                                    "activity" to (userDoc.getString("activity") ?: ""),
                                    "name" to (userDoc.getString("name") ?: userDoc.getString("username") ?: "Аноним"),
                                    "avatarUrl" to (userDoc.getString("avatarUrl") ?: ""),
                                    "profileIcon" to (userDoc.getString("profileIcon") ?: "ghost"),
                                    "useCustomAvatar" to (userDoc.getBoolean("useCustomAvatar") ?: false),
                                    "profileGlow" to (userDoc.getString("profileGlow") ?: "purple"),
                                    "isPremium" to (userDoc.getBoolean("isPremium") ?: false),
                                    "avatarFrame" to (userDoc.getString("avatarFrame") ?: "none"),
                                    "nameColor" to (userDoc.getString("nameColor") ?: "gold"),
                                    "isGroup" to false,
                                    "emoji" to (userDoc.getString("emojiStatus") ?: "")
                                ))

                                val chatId = snapshot.documents
                                    .firstOrNull { doc ->
                                        val participants = doc.get("participants") as? List<*>
                                        participants?.contains(partnerUid) == true &&
                                            participants.contains(uid)
                                    }?.id ?: return@addSnapshotListener

                                viewModelScope.launch {
                                    try {
                                        repository.updatePartnerInfoFromSnapshot(
                                            chatId = chatId,
                                            userDoc = userDoc
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ChatListVM", "Ошибка обновления партнёра $partnerUid", e)
                                    }
                                }
                            }
                    }
                }
            }
    }

    fun stopListening() {
        chatsListener?.remove()
        chatsListener = null
        myProfileListener?.remove()
        myProfileListener = null
        partnerListeners.values.forEach { it.remove() }
        partnerListeners.clear()
        listeningUid = null
        _myProfile.value = emptyMap()
        _partnerPresence.value = emptyMap()
        syncState.value = SyncState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        FirestoreListenerCoordinator.unregister(teardown)
        stopListening()
    }

    fun openOrCreateDirectChat(myUid: String, partnerUid: String, onReady: (String) -> Unit) {
        val chatId = listOf(myUid, partnerUid).sorted().joinToString("_")
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
