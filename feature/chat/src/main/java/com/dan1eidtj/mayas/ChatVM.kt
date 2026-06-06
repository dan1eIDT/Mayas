package com.dan1eidtj.mayas.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

// --- СТРУКТУРА СООБЩЕНИЯ С ПОДДЕРЖКОЙ ИМЕНИ (ДЛЯ ГРУПП) ---
data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "User", // <-- Добавлено для отображения в группах
    val timestamp: Timestamp? = null,
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

    fun sendMessage(chatId: String, text: String, replyText: String?, replyName: String?) {
        if (text.isBlank()) return

        val messageData = mutableMapOf(
            "text" to text,
            "senderId" to myUid,
            "senderName" to myName, // Передаем имя, чтобы в группах оно отображалось без лишних запросов
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