package com.dan1eidtj.mayas.feature.chats.ChatListScreen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dan1eidtj.mayas.db.ChatEntity
import com.dan1eidtj.mayas.db.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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


    private var chatsListener: ListenerRegistration? = null


    private val partnerListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        startListening()
    }

    fun startListening() {
        val uid = myUid ?: return
        chatsListener?.remove()

        syncState.value = SyncState.SYNCING

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
                    repository.syncChatsFromSnapshot(snapshot, uid)
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
                }


                partnerUids.forEach { partnerUid ->
                    if (!partnerListeners.containsKey(partnerUid)) {
                        partnerListeners[partnerUid] = db.collection("users")
                            .document(partnerUid)
                            .addSnapshotListener { userDoc, _ ->
                                if (userDoc == null || !userDoc.exists()) return@addSnapshotListener


                                val chatId = snapshot.documents
                                    .firstOrNull { doc ->
                                        val participants = doc.get("participants") as? List<*>
                                        participants?.contains(partnerUid) == true &&
                                                participants.contains(uid)
                                    }?.id ?: return@addSnapshotListener


                                viewModelScope.launch {
                                    repository.updatePartnerInfoFromSnapshot(
                                        chatId = chatId,
                                        userDoc = userDoc
                                    )
                                }
                            }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        partnerListeners.values.forEach { it.remove() }
        partnerListeners.clear()
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
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
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
            .addOnFailureListener { onResult(null) }
    }

    fun joinPublicChannel(chatId: String, myUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        db.collection("chats").document(chatId)
            .update(
                "participants", com.google.firebase.firestore.FieldValue.arrayUnion(myUid),
                "members", com.google.firebase.firestore.FieldValue.arrayUnion(myUid),
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
