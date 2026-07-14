package com.dan1eidtj.mayas.feature

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import io.github.jan.supabase.functions.functions
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.dan1eidtj.mayas.core_ui.utils.formatLastSeen
import kotlinx.coroutines.tasks.await
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dan1eidtj.mayas.db.ChatRepository
import com.google.firebase.firestore.PropertyName


val supabase = createSupabaseClient(
    supabaseUrl = Configtebeblat.SUPABASE_URL,
    supabaseKey = Configtebeblat.SUPABASE_KEY
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
    @get:PropertyName("isPremium")
    val isPremium: Boolean = false,
    val messageStyle: String? = null,
    val status: Int = 1,
    val reactions: Map<String, String> = emptyMap(),
    val voiceUrl: String? = null,
    val voiceDuration: Int = 0,
)

class ChatVM(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = ChatRepository(application)

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

    var pinnedMessageId by mutableStateOf<String?>(null)
        private set

    var pinnedMessageText by mutableStateOf<String?>(null)
        private set

    var chatTheme by mutableStateOf<String?>(null)
        private set

    private var soundPool: SoundPool? = null
    private var messageSentSoundId: Int = 0
    private var messageReceivedSoundId: Int = 0

    private var messagesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var chatDocListener: ListenerRegistration? = null



    private var userListenerChatId: String? = null

    var partnerUseCustomAvatar by mutableStateOf(true)
        private set

    var partnerProfileIcon by mutableStateOf("face")
        private set

    var partnerProfileGlow by mutableStateOf("purple")
        private set

    var partnerIsPremium by mutableStateOf(false)
        private set

    var partnerVerifiedIcon by mutableStateOf("verified")
        private set

    var partnerAvatarFrame by mutableStateOf("rainbow")
        private set

    var partnerNameColor by mutableStateOf("gold")
        private set

    var myIsPremium by mutableStateOf(false)
        private set

    var myMessageStyle by mutableStateOf<String?>(null)
        private set

    var myVerifiedIcon by mutableStateOf("verified")
        private set

    var isRecording by mutableStateOf(false)
        private set

    private var recordingJob: Job? = null
    var recordingDuration by mutableStateOf(0)
        private set

    var playingUrl by mutableStateOf<String?>(null)
        private set

    var isVoicePlaying by mutableStateOf(false)
        private set

    var voiceProgress by mutableStateOf(0f)
        private set

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var progressJob: Job? = null


    private var myProfileListener: ListenerRegistration? = null

    var searchResults by mutableStateOf<List<Message>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    fun searchMessages(chatId: String, query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        isSearching = true

        db.collection("chats/$chatId/messages")
            .whereGreaterThanOrEqualTo("text", query)
            .whereLessThanOrEqualTo("text", query + "\uf8ff")
            .get()
            .addOnSuccessListener { snap ->
                searchResults = snap.documents.mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) }
                isSearching = false
            }
            .addOnFailureListener {
                isSearching = false
            }
    }

    fun clearSearch() {
        searchResults = emptyList()
        isSearching = false
    }

    fun playVoice(url: String) {
        if (playingUrl == url) {
            if (isVoicePlaying) {
                mediaPlayer?.pause()
                isVoicePlaying = false
                progressJob?.cancel()
            } else {
                mediaPlayer?.start()
                isVoicePlaying = true
                startProgressUpdate()
            }
            return
        }

        stopVoice()
        playingUrl = url
        mediaPlayer = android.media.MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                start()
                isVoicePlaying = true
                startProgressUpdate()
            }
            setOnCompletionListener {
                stopVoice()
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isVoicePlaying) {
                val duration = mediaPlayer?.duration ?: 1
                val current = mediaPlayer?.currentPosition ?: 0
                voiceProgress = current.toFloat() / duration
                delay(100)
            }
        }
    }

    fun stopVoice() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        isVoicePlaying = false
        playingUrl = null
        voiceProgress = 0f
    }

    val myUid: String?
        get() = auth.currentUser?.uid

    private var myName: String = "Вы"

    init {
        myUid?.let { uid ->

            myProfileListener = db.collection("users").document(uid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        myName = doc.getString("name") ?: doc.getString("username") ?: "Вы"
                        myIsPremium = doc.getBoolean("isPremium") ?: false
                        myVerifiedIcon = doc.getString("verifiedIcon") ?: "verified"
                        myMessageStyle = doc.getString("messageStyle")
                    }
                }
        }
    }

    fun initSoundPool(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
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
                        val m = doc.toObject(Message::class.java)?.copy(id = doc.id)
                        if (m != null) {
                            val status = if (m.readBy.size > 1) 2 else 1
                            m.copy(status = status)
                        } else null
                    }
                    if (messages.isNotEmpty() && list.size > messages.size) {
                        val last = list.last()
                        if (last.senderId != uid) {
                            playSound(messageReceivedSoundId)
                        }
                    }
                    messages = list
                    markAsRead(chatId, list, uid)


                }
            }

        chatDocListener?.remove()
        chatDocListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    pinnedMessageId = doc.getString("pinnedMessageId")
                    pinnedMessageText = doc.getString("pinnedMessage")
                    chatTheme = doc.getString("theme")

                    val type = doc.getString("type") ?: "DIRECT"
                    val isGroupField = doc.getBoolean("isGroup") ?: false
                    isGroupChat = type == "GROUP" || isGroupField

                    if (isGroupChat) {
                        partnerName = doc.getString("title") ?: doc.getString("groupName") ?: "Группа"
                        partnerAvatarUrl = doc.getString("groupAvatar") ?: doc.getString("groupAvatarUrl")
                        partnerUseCustomAvatar = !partnerAvatarUrl.isNullOrBlank()
                        partnerEmoji = doc.getString("emoji") ?: "👥"
                        partnerProfileIcon = doc.getString("profileIcon") ?: "default"
                        partnerProfileGlow = doc.getString("profileGlow") ?: "purple"
                        partnerNameColor = doc.getString("nameColor") ?: "gold"

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


        if (targetUid != null && userListenerChatId != chatId) {
            userListener?.remove()
            userListenerChatId = chatId

            userListener = db.collection("users").document(targetUid)
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
                        partnerIsPremium = doc.getBoolean("isPremium") ?: false
                        partnerVerifiedIcon = doc.getString("verifiedIcon") ?: "verified"
                        partnerAvatarFrame = doc.getString("avatarFrame") ?: "rainbow"



                        partnerNameColor = doc.getString("nameColor") ?: "gold"
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
        db.collection("chats").document(chatId)
            .update("unreadCount_$receiverUid", FieldValue.increment(1))
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка инкремента счётчика", e) }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        replyText: String?,
        replyName: String?
    ) {
        if (text.isBlank()) return

        val uid = myUid ?: return

        val messageData = mutableMapOf<String, Any?>(
            "text" to text,
            "senderId" to uid,
            "senderName" to myName,
            "timestamp" to FieldValue.serverTimestamp(),
            "readBy" to listOf(uid),
            "isPremium" to myIsPremium,
            "messageStyle" to myMessageStyle
        )

        if (replyText != null && replyName != null) {
            messageData["replyToText"] = replyText
            messageData["replyToName"] = replyName
        }

        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val userRef = db.collection("users").document(uid)

        val batch = db.batch()

        batch.set(msgRef, messageData)

        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to text,
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.update(
            userRef,
            "messagesSent",
            FieldValue.increment(1)
        )

        if (!isGroupChat && partnerUid.isNotBlank()) {
            batch.update(
                chatRef,
                "unreadCount_$partnerUid",
                FieldValue.increment(1)
            )
        }

        batch.commit()
            .addOnSuccessListener {
                playSound(messageSentSoundId)


                if (!isGroupChat && partnerUid.isNotBlank()) {
                    sendPushNotification(partnerUid, text)
                }
            }
            .addOnFailureListener {
                Log.e("ChatVM", "Ошибка отправки", it)
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

                val messageData = mutableMapOf<String, Any?>(
                    "text" to text.ifBlank { null },
                    "senderId" to uid,
                    "senderName" to myName,
                    "mediaUrl" to publicMediaUrl,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid),
                    "isPremium" to myIsPremium,
                    "messageStyle" to myMessageStyle
                )

                if (replyText != null && replyName != null) {
                    messageData["replyToText"] = replyText
                    messageData["replyToName"] = replyName
                }

                // FIX: используем batch для медиа тоже, как и для текста
                val batch = db.batch()
                val chatRef = db.collection("chats").document(chatId)
                val msgRef = chatRef.collection("messages").document()

                batch.set(msgRef, messageData)

                val previewText = if (text.isNotBlank()) text else "📷 Фотография"
                batch.update(chatRef, mapOf(
                    "lastMessage" to previewText,
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                batch.update(
                    db.collection("users").document(uid),
                    "messagesSent", FieldValue.increment(1)
                )

                if (!isGroupChat && partnerUid.isNotBlank()) {
                    batch.update(chatRef, "unreadCount_$partnerUid", FieldValue.increment(1))
                }

                batch.commit()
                    .addOnSuccessListener {
                        playSound(messageSentSoundId)
                        // FIX: пуш не отправлялся для медиа-сообщений
                        if (!isGroupChat && partnerUid.isNotBlank()) {
                            sendPushNotification(partnerUid, previewText)
                        }
                    }
                    .addOnFailureListener { e -> Log.e("ChatVM", "Failed to send media batch", e) }

            } catch (e: Exception) {
                Log.e("ChatVM", "Ошибка загрузки медиа", e)
            }
        }
    }
    suspend fun createDirectChat(myUid: String, partnerUid: String): String {
        val chatId = listOf(myUid, partnerUid)
            .sorted()
            .joinToString("_")

        val chatRef = db.collection("chats").document(chatId)

        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            chatRef.set(
                mapOf(
                    "type" to "DIRECT",
                    "participants" to listOf(myUid, partnerUid),
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "typing" to mapOf(
                        myUid to false,
                        partnerUid to false
                    ),
                    "unreadCount_$myUid" to 0,
                    "unreadCount_$partnerUid" to 0
                )
            ).await()
        }

        return chatId
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
                    Log.e("ChatVM", "Push notification failed", e)
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
            .addOnSuccessListener {
                viewModelScope.launch {
                    repository.deleteMessageLocally(messageId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatVM", "Ошибка удаления сообщения", e)
            }
    }

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val uid = myUid ?: return
        val msg = messages.find { it.id == messageId } ?: return
        val currentReaction = msg.reactions[uid]

        if (currentReaction == emoji) {
            db.collection("chats/$chatId/messages").document(messageId)
                .update("reactions.$uid", FieldValue.delete())
        } else {
            db.collection("chats/$chatId/messages").document(messageId)
                .update("reactions.$uid", emoji)
        }
    }

    fun pinMessage(chatId: String, message: Message) {
        db.collection("chats").document(chatId)
            .update(
                "pinnedMessageId", message.id,
                "pinnedMessage", message.text ?: if (message.mediaUrl != null) "📷 Фотография" else "Голосовое сообщение"
            )
    }

    fun unpinMessage(chatId: String) {
        db.collection("chats").document(chatId)
            .update("pinnedMessageId", null, "pinnedMessage", null)
    }

    fun setChatTheme(chatId: String, theme: String) {
        db.collection("chats").document(chatId)
            .update("theme", theme)
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка обновления темы", e) }
    }

    fun reportUser(reporterUid: String, targetUid: String, chatId: String, reason: String, onSuccess: () -> Unit) {
        db.collection("reports").add(
            mapOf(
                "reporterUid" to reporterUid,
                "targetUid" to targetUid,
                "chatId" to chatId,
                "reason" to reason,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка отправки жалобы", e) }
    }

    fun clearChat(chatId: String, onSuccess: () -> Unit) {
        db.collection("chats").document(chatId)
            .collection("messages").get()
            .addOnSuccessListener { snapshot ->
                // FIX: guard на пустой батч — commit() на пустом батче не падает,
                // но лишний round-trip к Firestore не нужен
                if (snapshot.isEmpty) {
                    onSuccess()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener { onSuccess() }
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка очистки чата", e) }
    }

    fun blockUser(myUid: String, partnerUid: String, onSuccess: () -> Unit) {
        db.collection("users").document(myUid)
            .set(mapOf("blocked" to FieldValue.arrayUnion(partnerUid)), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка блокировки пользователя", e) }
    }

    // FIX: markAsRead теперь принимает uid явно (не тянет из auth каждый раз)
    // и пропускает commit если нечего обновлять — экономим writes
    private fun markAsRead(chatId: String, list: List<Message>, uid: String) {
        val unread = list.filter { !it.readBy.contains(uid) }
        if (unread.isEmpty()) return

        val batch = db.batch()
        unread.forEach { msg ->
            val ref = db.collection("chats/$chatId/messages").document(msg.id)
            batch.update(ref, "readBy", FieldValue.arrayUnion(uid))
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

    fun deleteGroup(chatId: String, onSuccess: () -> Unit) {
        // FIX: раньше удалялся только сам документ чата, а подколлекция messages
        // оставалась в Firestore навсегда. Если позже кто-то заново создаст чат
        // с этим же chatId, старые сообщения "воскресали". Чистим сообщения сначала.
        db.collection("chats").document(chatId)
            .collection("messages").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnCompleteListener {
                        db.collection("chats").document(chatId).delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка удаления группы", e) }
                    }
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка удаления сообщений группы", e) }
    }

    fun leaveGroup(chatId: String, myUid: String, onSuccess: () -> Unit) {
        db.collection("chats").document(chatId).update(
            "participants", FieldValue.arrayRemove(myUid),
            "members", FieldValue.arrayRemove(myUid),
            "admins", FieldValue.arrayRemove(myUid),
            "moderators", FieldValue.arrayRemove(myUid)
        ).addOnSuccessListener { onSuccess() }
    }

    fun startRecording() {
        isRecording = true
        recordingDuration = 0
        recordingJob = viewModelScope.launch {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    fun stopRecording(chatId: String, audioBytes: ByteArray?, replyText: String?, replyName: String?) {
        isRecording = false
        recordingJob?.cancel()
        val duration = recordingDuration
        recordingDuration = 0

        if (audioBytes == null || duration < 1) return

        val uid = myUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = "voice_${uid}_${System.currentTimeMillis()}.m4a"
                val bucket = supabase.storage.from("mayas-voice")
                bucket.upload(fileName, audioBytes)
                val publicUrl = bucket.publicUrl(fileName)

                val messageData = mutableMapOf<String, Any?>(
                    "senderId" to uid,
                    "senderName" to myName,
                    "voiceUrl" to publicUrl,
                    "voiceDuration" to duration,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid),
                    "isPremium" to myIsPremium,
                    "messageStyle" to myMessageStyle
                )

                if (replyText != null && replyName != null) {
                    messageData["replyToText"] = replyText
                    messageData["replyToName"] = replyName
                }

                val batch = db.batch()
                val chatRef = db.collection("chats").document(chatId)
                val msgRef = chatRef.collection("messages").document()

                batch.set(msgRef, messageData)
                batch.update(chatRef, mapOf(
                    "lastMessage" to "🎤 Голосовое сообщение ($duration сек.)",
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                batch.update(
                    db.collection("users").document(uid),
                    "messagesSent", FieldValue.increment(1)
                )

                if (!isGroupChat && partnerUid.isNotBlank()) {
                    batch.update(chatRef, "unreadCount_$partnerUid", FieldValue.increment(1))
                }

                batch.commit()
                    .addOnSuccessListener {
                        playSound(messageSentSoundId)
                        // FIX: пуш не отправлялся для голосовых сообщений
                        if (!isGroupChat && partnerUid.isNotBlank()) {
                            sendPushNotification(partnerUid, "🎤 Голосовое сообщение ($duration сек.)")
                        }
                    }
                    .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка batch ГС", e) }

            } catch (e: Exception) {
                Log.e("ChatVM", "Ошибка загрузки ГС", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        userListener?.remove()
        chatDocListener?.remove()
        myProfileListener?.remove() // FIX: снимаем слушатель профиля
        soundPool?.release()
        soundPool = null
        stopVoice()
    }
}


data class GroupMemberUi(
    val uid: String,
    val name: String,
    val username: String,
    val avatarUrl: String?,
    val useCustomAvatar: Boolean,
    val profileIcon: String,
    val profileGlow: String,
    val isPremium: Boolean,
    val isModerator: Boolean,
    val isAdmin: Boolean,
    val isOwner: Boolean,
)

class GroupMembersVM : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var groupName by mutableStateOf("Группа")
        private set

    var ownerId by mutableStateOf("")
        private set

    var adminIds by mutableStateOf<List<String>>(emptyList())
        private set

    var moderatorIds by mutableStateOf<List<String>>(emptyList())
        private set

    var memberIds by mutableStateOf<List<String>>(emptyList())
        private set

    var members by mutableStateOf<List<GroupMemberUi>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val myUid: String? get() = auth.currentUser?.uid

    val isMyAdmin: Boolean get() = myUid != null && myUid in adminIds

    private var chatListener: ListenerRegistration? = null
    private var currentChatId: String? = null

    fun observeGroup(chatId: String) {
        if (currentChatId == chatId && chatListener != null) return
        currentChatId = chatId

        chatListener?.remove()
        chatListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    Log.e("GroupMembersVM", "Ошибка слушателя группы", err)
                    errorMessage = "Не удалось загрузить группу"
                    isLoading = false
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    errorMessage = null
                    groupName = doc.getString("groupName") ?: doc.getString("title") ?: "Группа"
                    ownerId = doc.getString("ownerId") ?: ""

                    @Suppress("UNCHECKED_CAST")
                    adminIds = (doc.get("admins") as? List<String>) ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    moderatorIds = (doc.get("moderators") as? List<String>) ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val ids = (doc.get("participants") as? List<String>)
                        ?: (doc.get("members") as? List<String>)
                        ?: emptyList()
                    memberIds = ids
                    loadMemberProfiles(ids)
                } else {
                    isLoading = false
                    errorMessage = "Группа не найдена"
                }
            }
    }

    private fun loadMemberProfiles(ids: List<String>) {
        if (ids.isEmpty()) {
            members = emptyList()
            isLoading = false
            return
        }
        isLoading = true

        // whereIn по documentId() ограничен 30 значениями — бьём на чанки по 10
        val chunks = ids.distinct().chunked(10)
        val collected = mutableMapOf<String, GroupMemberUi>()
        var remaining = chunks.size

        chunks.forEach { chunk ->
            db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { d ->
                        val uid = d.id
                        collected[uid] = GroupMemberUi(
                            uid = uid,
                            name = d.getString("name") ?: d.getString("username") ?: "Без имени",
                            username = d.getString("username") ?: "",
                            avatarUrl = d.getString("avatarUrl"),
                            useCustomAvatar = d.getBoolean("useCustomAvatar") ?: true,
                            profileIcon = d.getString("profileIcon") ?: "face",
                            profileGlow = d.getString("profileGlow") ?: "purple",
                            isPremium = d.getBoolean("isPremium") ?: false,
                            isModerator = uid in moderatorIds,
                            isAdmin = uid in adminIds,
                            isOwner = uid == ownerId,
                        )
                    }
                    remaining--
                    if (remaining <= 0) finishLoadingMembers(collected)
                }
                .addOnFailureListener { e ->
                    Log.e("GroupMembersVM", "Не удалось загрузить участников", e)
                    remaining--
                    if (remaining <= 0) finishLoadingMembers(collected)
                }
        }
    }

    private fun finishLoadingMembers(collected: Map<String, GroupMemberUi>) {
        members = collected.values.sortedWith(
            compareByDescending<GroupMemberUi> { it.isOwner }
                .thenByDescending { it.isAdmin }
                .thenByDescending { it.isModerator }
                .thenBy { it.name.lowercase() }
        )
        isLoading = false
    }

    fun promoteToAdmin(chatId: String, uid: String, onResult: (Boolean) -> Unit = {}) {
        db.collection("chats").document(chatId)
            .update("admins", FieldValue.arrayUnion(uid))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось назначить админа", e)
                onResult(false)
            }
    }

    fun demoteAdmin(chatId: String, uid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (uid == ownerId) {
            onResult(false, "Нельзя снять права у создателя группы")
            return
        }
        db.collection("chats").document(chatId)
            .update("admins", FieldValue.arrayRemove(uid))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось снять права админа", e)
                onResult(false, "Ошибка сети, попробуйте ещё раз")
            }
    }

    fun promoteToModerator(chatId: String, uid: String, onResult: (Boolean) -> Unit = {}) {
        db.collection("chats").document(chatId)
            .update("moderators", FieldValue.arrayUnion(uid))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось назначить модератора", e)
                onResult(false)
            }
    }

    fun demoteModerator(chatId: String, uid: String, onResult: (Boolean) -> Unit = {}) {
        db.collection("chats").document(chatId)
            .update("moderators", FieldValue.arrayRemove(uid))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось снять права модератора", e)
                onResult(false)
            }
    }

    fun kickMember(chatId: String, uid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (uid == ownerId) {
            onResult(false, "Нельзя исключить создателя группы")
            return
        }
        if (uid in adminIds) {
            onResult(false, "Сначала снимите права администратора")
            return
        }
        db.collection("chats").document(chatId)
            .update(mapOf(
                "participants" to FieldValue.arrayRemove(uid),
                "members" to FieldValue.arrayRemove(uid),
                "admins" to FieldValue.arrayRemove(uid)
            ))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось исключить участника", e)
                onResult(false, "Ошибка сети, попробуйте ещё раз")
            }
    }

    fun addMembers(chatId: String, uids: List<String>, onResult: (Boolean) -> Unit = {}) {
        if (uids.isEmpty()) { onResult(true); return }
        db.collection("chats").document(chatId)
            .update(mapOf(
                "participants" to FieldValue.arrayUnion(*uids.toTypedArray()),
                "members" to FieldValue.arrayUnion(*uids.toTypedArray())
            ))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось добавить участников", e)
                onResult(false)
            }
    }

    fun searchAddableUsers(
        excludeIds: List<String>,
        query: String,
        onResult: (List<GroupMemberUi>) -> Unit
    ) {
        val trimmed = query.trim().lowercase()
        val request = if (trimmed.isEmpty()) {
            db.collection("users").limit(30)
        } else {
            db.collection("users")
                .orderBy("username")
                .startAt(trimmed)
                .endAt(trimmed + "\uf8ff")
                .limit(30)
        }

        request.get()
            .addOnSuccessListener { snap ->
                val result = snap.documents
                    .filter { it.id !in excludeIds }
                    .map { d ->
                        val uid = d.id
                        GroupMemberUi(
                            uid = uid,
                            name = d.getString("name") ?: d.getString("username") ?: "Без имени",
                            username = d.getString("username") ?: "",
                            avatarUrl = d.getString("avatarUrl"),
                            useCustomAvatar = d.getBoolean("useCustomAvatar") ?: true,
                            profileIcon = d.getString("profileIcon") ?: "face",
                            profileGlow = d.getString("profileGlow") ?: "purple",
                            isPremium = d.getBoolean("isPremium") ?: false,
                            isModerator = false,
                            isAdmin = false,
                            isOwner = false,
                        )
                    }
                onResult(result)
            }
            .addOnFailureListener { e ->
                Log.e("GroupMembersVM", "Не удалось найти пользователей", e)
                onResult(emptyList())
            }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }
}