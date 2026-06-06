package com.dan1eidtj.mayas.feature.chat

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

val supabase = createSupabaseClient(
    supabaseUrl = "https://rfkqidshgbioplqyhgca.supabase.co",
    supabaseKey = "sb_publishable_jGI8UyEaXfSyPaMY0l4HVQ_U4rUJZ5_"
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

    var isGroupChat by mutableStateOf(false) // <-- Флаг: группа это или личка
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

    // Переменная для сохранения имени текущего пользователя, чтобы крепить к сообщениям
    private var myName: String = "Вы"

    init {
        // Подгружаем свое имя для отправки в групповые чаты
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
                }
            }

        chatDocListener?.remove()
        chatDocListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    pinnedMessage = doc.getString("pinnedMessage")

                    // Проверяем тип чата (и по type, и по isGroup для надежности)
                    val type = doc.getString("type") ?: "DIRECT"
                    val isGroupField = doc.getBoolean("isGroup") ?: false
                    isGroupChat = type == "GROUP" || isGroupField

                    if (isGroupChat) {
                        // Если это ГРУППА, то "partnerName" — это название группы
                        partnerName = doc.getString("title") ?: doc.getString("groupName") ?: "Группа"
                        partnerAvatarUrl = doc.getString("groupAvatar") ?: doc.getString("groupAvatarUrl")
                        partnerUseCustomAvatar = !partnerAvatarUrl.isNullOrBlank()
                        partnerEmoji = "👥" // Фиксированный эмодзи для групп

                        // Считываем участников из participants (или из members, если они старые)
                        val members = (doc.get("participants") as? List<*>) ?: (doc.get("members") as? List<*>)
                        lastSeenText = "${members?.size ?: 0} участников"
                        typingText = "" // Для групп логику печатания можно расширить позже
                    } else {
                        // Если это ЛИЧНЫЙ чат — запускаем стандартное считывание собеседника
                        setupDirectChatListener(chatId, uid)
                    }
                } else {
                    // Если документа чата еще нет (новый чат), по дефолту считаем DIRECT
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

    fun createGroupChat(
        title: String,
        description: String,               // <-- Добавили описание
        isPublic: Boolean,                  // <-- Добавили приватность (true - публичная, false - приватная)
        selectedUserIds: List<String>,
        onSuccess: (String) -> Unit
    ) {
        val uid = myUid ?: return
        if (title.isBlank() || selectedUserIds.isEmpty()) return

        // 1. Генерируем ID для нового документа группы
        val newChatId = db.collection("chats").document().id

        // 2. Объединяем всех участников и добавляем создателя (тебя)
        val allMembers = selectedUserIds.toMutableList().apply {
            if (!contains(uid)) add(uid)
        }

        // 3. Формируем структуру документа группы уровня Telegram/Discord
        val groupData = mapOf(
            "chatId" to newChatId,
            "type" to "GROUP",
            "isGroup" to true,
            "title" to title.trim(),
            "groupName" to title.trim(),
            "description" to description.trim(), // <-- Сохраняем описание чата
            "ownerId" to uid,                     // <-- Ты создатель! (Пункт 4)
            "admins" to listOf(uid),             // <-- Ты первый админ! (Пункт 5)
            "isPublic" to isPublic,               // <-- Публичный статус чата (Пункт 10)
            "participants" to allMembers,
            "members" to allMembers,
            "pinnedMessage" to null,
            "lastMessage" to "$myName создал(а) группу \"${title.trim()}\"", // <-- Динамический текст
            "lastSenderId" to "system",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // 4. Записываем в Firestore
        db.collection("chats").document(newChatId)
            .set(groupData)
            .addOnSuccessListener {
                // 5. Создаем красивое системное сообщение (Пункт 9)
                val systemMessage = mapOf(
                    "text" to "$myName создал(а) группу \"$title\"",
                    "senderId" to "system",
                    "senderName" to "Система",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid)
                )
                db.collection("chats/$newChatId/messages").add(systemMessage)

                // Переходим в созданный чат
                onSuccess(newChatId)
            }
    }
    private fun sendPushNotification(receiverUid: String, messageText: String) {
        if (receiverUid.isBlank()) return

        // Достаем токен получателя из твоего Firestore
        db.collection("users").document(receiverUid).get().addOnSuccessListener { doc ->
            val token = doc.getString("fcmToken") ?: return@addOnSuccessListener

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Пинаем нашу Edge Function в Supabase
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

                // 5. Закидываем готовое сообщение в твой Firebase
                db.collection("chats").document(chatId)
                    .collection("messages").add(messageData)
                    .addOnSuccessListener {
                        val previewText = if (text.isNotBlank()) text else "📷 Фотография"
                        updateChatMetadata(chatId, previewText, uid)
                    }

            } catch (e: Exception) {
                e.printStackTrace()
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
        if (!isGroupChat) { // В группах пока отключаем, чтобы не спамить в документ юзера
            db.collection("users").document(uid)
                .update("typing.$chatId", isTyping)
        }
    }

    private fun formatLastSeen(ts: Timestamp?): String {
        if (ts == null) return "был(а) недавно"
        val now = System.currentTimeMillis()
        val last = ts.toDate().time
        val diff = now - last

        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val sdfDate = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())

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