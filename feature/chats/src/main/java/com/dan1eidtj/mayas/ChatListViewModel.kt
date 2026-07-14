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

    // --- UI-состояние ---


    val syncState = MutableStateFlow(SyncState.IDLE)


    val chats: StateFlow<List<ChatEntity>> = repository
        .getChats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // --- Firestore слушатели ---
    private var chatsListener: ListenerRegistration? = null

    // uid → слушатель профиля партнёра
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
                    // Room Flow продолжает отдавать кэш — ничего не делаем
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                syncState.value = SyncState.ONLINE

                // Пишем в Room асинхронно — Room Flow автоматом обновит UI
                viewModelScope.launch {
                    repository.syncChatsFromSnapshot(snapshot, uid)
                }

                // Подписываемся на профили партнёров для личных чатов
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

                // Снимаем слушатели тех кто больше не в списке
                val toRemove = partnerListeners.keys - partnerUids
                toRemove.forEach { partnerUid ->
                    partnerListeners[partnerUid]?.remove()
                    partnerListeners.remove(partnerUid)
                }

                // Добавляем новые слушатели
                partnerUids.forEach { partnerUid ->
                    if (!partnerListeners.containsKey(partnerUid)) {
                        partnerListeners[partnerUid] = db.collection("users")
                            .document(partnerUid)
                            .addSnapshotListener { userDoc, _ ->
                                if (userDoc == null || !userDoc.exists()) return@addSnapshotListener

                                // Находим chatId для этого партнёра
                                val chatId = snapshot.documents
                                    .firstOrNull { doc ->
                                        val participants = doc.get("participants") as? List<*>
                                        participants?.contains(partnerUid) == true &&
                                                participants.contains(uid)
                                    }?.id ?: return@addSnapshotListener

                                // Обновляем партнёрские данные в Room
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
}
enum class SyncState { IDLE, SYNCING, ONLINE, OFFLINE }
