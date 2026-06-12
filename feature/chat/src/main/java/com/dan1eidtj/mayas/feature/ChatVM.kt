package com.dan1eidtj.mayas.feature

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import io.github.jan.supabase.functions.functions
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Locale

val supabase = createSupabaseClient(
    supabaseUrl = "supabaseUrlP",
    supabaseKey = "supabaseKeyP"
) {
    install(Storage)
    install(Realtime)
    install(Functions)
}

data class Message(
    @get:Exclude var id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String? = null,
    val mediaUrl: String? = null,
    @ServerTimestamp val timestamp: Date? = null,
    val readBy: List<String> = emptyList(),
    val replyToText: String? = null,
    val replyToName: String? = null,
)

class ChatVM : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set

    var lastSeenText by mutableStateOf("Загрузка...")
        private set

    var typingText by mutableStateOf("")
        private set

    var partnerUid by mutableStateOf("")
        private set

    var isGroupChat by mutableStateOf(false)
        private set

    var partnerName by mutableStateOf("Загрузка...")
        private set

    var partnerAvatarUrl by mutableStateOf<String?>(null)
        private set

    var partnerEmoji by mutableStateOf<String?>(null)
        private set

    var pinnedMessage by mutableStateOf<String?>(null)
        private set

    private var messagesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var chatDocListener: ListenerRegistration? = null

    var partnerUseCustomAvatar by mutableStateOf(true)
        private set

    var partnerProfileIcon by mutableStateOf("face")
        private set

    var partnerProfileGlow by mutableStateOf("purple")
        private set

    val myUid: String?
        get() = auth.currentUser?.uid

    private var myName: String = "Вы"

    init {
        myUid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                myName = doc.getString("name") ?: doc.getString("username") ?: "Вы"
            }
        }
    }

    fun observeChat(chatId: String) {
        val uid = myUid ?: return

        messagesListener?.remove()
        messagesListener = db.collection("chats/$chatId/messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    messages = list
                    markAsRead(chatId, list)

                    // 🔥 Раз мы сидим внутри чата и получаем сообщения — сразу гасим свой счётчик непрочитанных в 0
                    clearUnreadCount(chatId)
                }
            }

        chatDocListener?.remove()
        chatDocListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    pinnedMessage = doc.getString("pinnedMessage")

                    val type = doc.getString("type") ?: "DIRECT"
                    val isGroupField = doc.getBoolean("isGroup") ?: false
                    isGroupChat = type == "GROUP" || isGroupField

                    if (isGroupChat) {
                        partnerName = doc.getString("title") ?: doc.getString("groupName") ?: "Группа"
                        partnerAvatarUrl = doc.getString("groupAvatar") ?: doc.getString("groupAvatarUrl")
                        partnerUseCustomAvatar = !partnerAvatarUrl.isNullOrBlank()
                        partnerEmoji = "👥"

                        val members = (doc.get("participants") as? List<*>) ?: (doc.get("members") as? List<*>)
                        lastSeenText = "${members?.size ?: 0} участников"
                        typingText = ""
                    } else {
                        setupDirectChatListener(chatId, uid)
                    }
                } else {
                    isGroupChat = false
                    setupDirectChatListener(chatId, uid)
                }
            }
    }

    private fun setupDirectChatListener(chatId: String, uid: String) {
        val targetUid = chatId.split("_").firstOrNull { it != uid }
        partnerUid = targetUid ?: ""

        if (targetUid != null && userListener == null) {
            userListener = db.collection("users").document(partnerUid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists() && !isGroupChat) {
                        val lastSeen = doc.getTimestamp("lastSeen")
                        lastSeenText = formatLastSeen(lastSeen)

                        val typingMap = doc.get("typing") as? Map<*, *>
                        val isTypingInThisChat = typingMap?.get(chatId) == true

                        typingText = if (isTypingInThisChat) "печатает..." else ""
                        partnerName = doc.getString("name") ?: doc.getString("username") ?: "User"
                        partnerAvatarUrl = doc.getString("avatarUrl")
                        partnerEmoji = doc.getString("emojiStatus")
                        partnerUseCustomAvatar = doc.getBoolean("useCustomAvatar") ?: true
                        partnerProfileIcon = doc.getString("profileIcon") ?: "face"
                        partnerProfileGlow = doc.getString("profileGlow") ?: "purple"
                    }
                }
        }
    }

    fun clearUnreadCount(chatId: String) {
        val uid = myUid ?: return
        db.collection("chats").document(chatId).update("unreadCount_$uid", 0)
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка сброса счётчика", e) }
    }

    private fun incrementUnreadCount(chatId: String, receiverUid: String) {
        db.collection("chats").document(chatId).update("unreadCount_$receiverUid", FieldValue.increment(1))
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка инкремента счётчика", e) }
    }

    fun sendMessage(chatId: String, text: String, replyText: String?, replyName: String?) {
        if (text.isBlank()) return

        val messageData = mutableMapOf(
            "text" to text,
            "senderId" to myUid,
            "senderName" to myName,
            "timestamp" to FieldValue.serverTimestamp(),
            "readBy" to listOf(myUid)
        )

        if (replyText != null && replyName != null) {
            messageData["replyToText"] = replyText
            messageData["replyToName"] = replyName
        }

        db.collection("chats").document(chatId)
            .collection("messages").add(messageData)
            .addOnSuccessListener {
                updateChatMetadata(chatId, text, myUid ?: "")

                // 🔥 Если это ЛИЧНЫЙ чат, накручиваем счётчик «хуйнюшки» твоему другу в документе чата
                if (!isGroupChat && partnerUid.isNotBlank()) {
                    incrementUnreadCount(chatId, partnerUid)
                }
            }

        if (!isGroupChat && partnerUid.isNotBlank()) {
            sendPushNotification(receiverUid = partnerUid, messageText = text)
        }
    }

    fun sendMediaMessage(
        chatId: String,
        text: String,
        fileBytes: ByteArray,
        replyText: String?,
        replyName: String?
    ) {
        val uid = myUid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = "media_${uid}_${System.currentTimeMillis()}.jpg"
                val bucket = supabase.storage.from("mayas-media")
                bucket.upload(fileName, fileBytes)
                val publicMediaUrl = bucket.publicUrl(fileName)

                val messageData = mutableMapOf(
                    "text" to text.ifBlank { null },
                    "senderId" to uid,
                    "senderName" to myName,
                    "mediaUrl" to publicMediaUrl,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid)
                )

                if (replyText != null && replyName != null) {
                    messageData["replyToText"] = replyText
                    messageData["replyToName"] = replyName
                }

                db.collection("chats").document(chatId)
                    .collection("messages").add(messageData)
                    .addOnSuccessListener {
                        val previewText = if (text.isNotBlank()) text else "📷 Фотография"
                        updateChatMetadata(chatId, previewText, uid)

                        // 🔥 Накручиваем счётчик получателю при отправке ФОТОГРАФИИ
                        if (!isGroupChat && partnerUid.isNotBlank()) {
                            incrementUnreadCount(chatId, partnerUid)
                        }
                    }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createGroupChat(
        title: String,
        description: String,
        isPublic: Boolean,
        selectedUserIds: List<String>,
        onSuccess: (String) -> Unit
    ) {
        val uid = myUid ?: return
        if (title.isBlank() || selectedUserIds.isEmpty()) return

        val newChatId = db.collection("chats").document().id
        val allMembers = selectedUserIds.toMutableList().apply {
            if (!contains(uid)) add(uid)
        }

        val groupData = mapOf(
            "chatId" to newChatId,
            "type" to "GROUP",
            "isGroup" to true,
            "groupName" to title.trim(),
            "description" to description.trim(),
            "ownerId" to uid,
            "admins" to listOf(uid),
            "isPublic" to isPublic,
            "participants" to allMembers,
            "members" to allMembers,
            "pinnedMessage" to null,
            "lastMessage" to "$myName создал(а) группу \"${title.trim()}\"",
            "lastSenderId" to "system",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("chats").document(newChatId)
            .set(groupData)
            .addOnSuccessListener {
                val systemMessage = mapOf(
                    "text" to "$myName создал(а) группу \"$title\"",
                    "senderId" to "system",
                    "senderName" to "Система",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid)
                )
                db.collection("chats/$newChatId/messages").add(systemMessage)
                onSuccess(newChatId)
            }
    }

    private fun sendPushNotification(receiverUid: String, messageText: String) {
        if (receiverUid.isBlank()) return

        db.collection("users").document(receiverUid).get().addOnSuccessListener { doc ->
            val token = doc.getString("fcmToken") ?: return@addOnSuccessListener

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    supabase.functions.invoke("send-fcmpush", buildJsonObject {
                        put("token", token)
                        put("senderName", myName)
                        put("text", messageText)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        if (newText.isBlank()) return
        db.collection("chats/$chatId/messages").document(messageId)
            .update("text", newText)
    }

    fun deleteMessage(chatId: String, messageId: String) {
        db.collection("chats/$chatId/messages").document(messageId).delete()
    }

    fun pinMessage(chatId: String, text: String) {
        db.collection("chats").document(chatId)
            .update("pinnedMessage", text)
    }

    fun unpinMessage(chatId: String) {
        db.collection("chats").document(chatId)
            .update("pinnedMessage", null)
    }

    private fun updateChatMetadata(chatId: String, text: String, uid: String) {
        db.collection("chats").document(chatId).set(
            mapOf(
                "lastMessage" to text,
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    private fun markAsRead(chatId: String, list: List<Message>) {
        val uid = myUid ?: return
        val batch = db.batch()
        list.forEach { msg ->
            if (!msg.readBy.contains(uid)) {
                val ref = db.collection("chats/$chatId/messages").document(msg.id)
                batch.update(ref, "readBy", FieldValue.arrayUnion(uid))
            }
        }
        batch.commit()
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = myUid ?: return
        if (!isGroupChat) {
            db.collection("users").document(uid)
                .update("typing.$chatId", isTyping)
        }
    }

    private fun formatLastSeen(ts: Timestamp?): String {
        if (ts == null) return "был(а) недавно"
        val now = System.currentTimeMillis()
        val last = ts.toDate().time
        val diff = now - last

        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDate = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

        return when {
            diff < 15_000 -> "в сети"
            diff < 60_000 -> "был(а) только что"
            diff < 60 * 60_000 -> "был(а) ${diff / 60_000} мин. назад"
            diff < 24 * 60 * 60_000 -> "был(а) в ${sdfTime.format(ts.toDate())}"
            else -> "был(а) ${sdfDate.format(ts.toDate())}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        userListener?.remove()
        chatDocListener?.remove()
    }
}