package com.dan1eidtj.mayas.feature

import android.content.Context
import com.dan1eidtj.data.FirestoreListenerCoordinator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.dan1eidtj.mayas.core_ui.utils.formatLastSeen
import kotlinx.coroutines.tasks.await
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dan1eidtj.mayas.storage.Configtebeblat
import com.dan1eidtj.mayas.db.ChatRepository
import com.google.firebase.firestore.PropertyName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
data class Message(
    @get:Exclude var id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String? = null,
    val mediaUrl: String? = null,
    val mediaKey: String? = null,
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
    val voiceKey: String? = null,
    val voiceDuration: Int = 0,

    val type: String = MessageType.TEXT,

    val systemAction: String? = null,

    val systemRefMessageId: String? = null,

    val callType: String? = null,
    val callStatus: String? = null,
    val callDurationSec: Int = 0,

    val forwardedFromName: String? = null,

    val viewedBy: List<String> = emptyList(),




    val ttlSeconds: Long = 0,
    val expireAt: Date? = null,


    val isSilent: Boolean = false,


    val scheduledFor: Date? = null,
    val messageState: String = MessageState.SENT,
)

object MessageType {
    const val TEXT = "TEXT"
    const val SYSTEM = "SYSTEM"
    const val CALL = "CALL"
}

object MessageState {
    const val SENT = "SENT"
    const val SCHEDULED = "SCHEDULED"
}


object MessageTimerPreset {
    const val OFF = 0L
    const val MIN_1 = 60L
    const val HOUR_1 = 3600L
    const val HOUR_6 = 6 * 3600L
    const val DAY_1 = 24 * 3600L
    const val WEEK_1 = 7 * 24 * 3600L

    val all = listOf(OFF, MIN_1, HOUR_1, HOUR_6, DAY_1, WEEK_1)
}

fun formatTimerDuration(seconds: Long): String {
    return when {
        seconds <= 0 -> "Выкл"
        seconds < 3600 -> "${seconds / 60} мин"
        seconds < 86400 -> "${seconds / 3600} ч"
        seconds < 604800 -> "${seconds / 86400} дн"
        else -> "${seconds / 604800} нед"
    }
}


fun formatCompactCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> {
            val v = count / 1000.0
            "${String.format(Locale.US, "%.1f", v).removeSuffix(".0")}K"
        }
        else -> {
            val v = count / 1_000_000.0
            "${String.format(Locale.US, "%.1f", v).removeSuffix(".0")}M"
        }
    }
}

object SystemAction {
    const val PINNED = "PINNED"
    const val UNPINNED = "UNPINNED"
    const val GROUP_CREATED = "GROUP_CREATED"
    const val MEMBER_ADDED = "MEMBER_ADDED"
    const val MEMBER_REMOVED = "MEMBER_REMOVED"
    const val MEMBER_LEFT = "MEMBER_LEFT"
    const val PROMOTED_ADMIN = "PROMOTED_ADMIN"
    const val DEMOTED_ADMIN = "DEMOTED_ADMIN"
    const val PROMOTED_MODERATOR = "PROMOTED_MODERATOR"
    const val DEMOTED_MODERATOR = "DEMOTED_MODERATOR"
    const val DISAPPEARING_TIMER_CHANGED = "DISAPPEARING_TIMER_CHANGED"
}

object CallStatus {
    const val MISSED = "MISSED"
    const val DECLINED = "DECLINED"
    const val ANSWERED = "ANSWERED"
}


private fun postSystemMessage(
    db: FirebaseFirestore,
    chatId: String,
    text: String,
    action: String,
    refMessageId: String? = null,
) {
    val chatRef = db.collection("chats").document(chatId)
    val msgRef = chatRef.collection("messages").document()

    val messageData = mutableMapOf<String, Any?>(
        "type" to MessageType.SYSTEM,
        "text" to text,
        "senderId" to "system",
        "senderName" to "Система",
        "systemAction" to action,
        "timestamp" to FieldValue.serverTimestamp(),
        "readBy" to emptyList<String>()
    )
    if (refMessageId != null) {
        messageData["systemRefMessageId"] = refMessageId
    }

    val batch = db.batch()
    batch.set(msgRef, messageData)
    batch.update(
        chatRef,
        mapOf(
            "lastMessage" to text,
            "lastSenderId" to "system",
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )
    batch.commit()
        .addOnFailureListener { e -> Log.e("SystemMessage", "Не удалось отправить системное сообщение", e) }
}


private object BackendApi {

    private val BASE_URL: String get() = Configtebeblat.functionUrl.trimEnd('/')

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json".toMediaType()

    data class PresignUploadResult(val uploadUrl: String, val key: String)


    suspend fun presignUpload(idToken: String, key: String, contentType: String): PresignUploadResult =
        withContext(Dispatchers.IO) {
            val requestJson = buildJsonObject {
                put("key", key)
                put("contentType", contentType)
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL/presign-upload")
                .addHeader("Authorization", "Bearer $idToken")
                .post(requestJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw Exception("presign-upload: ${response.code} $bodyText")
                }
                val json = Json.parseToJsonElement(bodyText).jsonObject
                PresignUploadResult(
                    uploadUrl = json.getValue("uploadUrl").jsonPrimitive.content,
                    key = json.getValue("key").jsonPrimitive.content
                )
            }
        }

    suspend fun presignDownload(idToken: String, key: String): String =
        withContext(Dispatchers.IO) {
            val requestJson = buildJsonObject { put("key", key) }.toString()

            val request = Request.Builder()
                .url("$BASE_URL/presign-download")
                .addHeader("Authorization", "Bearer $idToken")
                .post(requestJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw Exception("presign-download: ${response.code} $bodyText")
                }
                Json.parseToJsonElement(bodyText).jsonObject
                    .getValue("downloadUrl").jsonPrimitive.content
            }
        }


    suspend fun uploadBytes(uploadUrl: String, bytes: ByteArray, contentType: String) =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(bytes.toRequestBody(contentType.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("B2 upload: ${response.code} ${response.body?.string()}")
                }
            }
        }


    suspend fun notify(token: String, senderName: String, text: String, silent: Boolean = false) =
        withContext(Dispatchers.IO) {
            val requestJson = buildJsonObject {
                put("token", token)
                put("senderName", senderName)
                put("text", text)
                put("silent", silent)
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL/notify")
                .post(requestJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("notify: ${response.code} ${response.body?.string()}")
                }
            }
        }
}

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
    var chatType by mutableStateOf("DIRECT")
    var chatAdmins by mutableStateOf<List<String>>(emptyList())
    var chatOwnerId by mutableStateOf<String?>(null)
    var chatAdminPermissions by mutableStateOf<Map<String, Map<String, Boolean>>>(emptyMap())

    private fun hasChatPermission(perm: String): Boolean {
        val uid = myUid ?: return false
        if (uid == chatOwnerId) return true
        return chatAdminPermissions[uid]?.get(perm) == true
    }

    val canPostInChat: Boolean
        get() = chatType != "CHANNEL" || hasChatPermission(AdminPermission.CAN_POST)

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




    var chatDisappearingTimerSec by mutableStateOf(0L)
        private set

    private var soundPool: SoundPool? = null
    private var messageSentSoundId: Int = 0
    private var messageReceivedSoundId: Int = 0

    private var messagesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var chatDocListener: ListenerRegistration? = null




    private var currentChatId: String? = null
    private var listeningUid: String? = null





    private val teardown: () -> Unit = { stopAllListeners() }



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

    fun playVoice(rawUrlOrKey: String) {
        if (playingUrl == rawUrlOrKey) {
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
        playingUrl = rawUrlOrKey

        viewModelScope.launch {




            val playableUrl = if (rawUrlOrKey.startsWith("http")) {
                rawUrlOrKey
            } else {
                runCatching { com.dan1eidtj.mayas.storage.B2MediaClient.resolveDownloadUrl(rawUrlOrKey) }
                    .getOrNull()
            }

            if (playableUrl == null) {
                Log.e("ChatVM", "Не удалось получить ссылку на голосовое: $rawUrlOrKey")
                playingUrl = null
                return@launch
            }


            if (playingUrl != rawUrlOrKey) return@launch

            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(playableUrl)
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




    private val downloadUrlCache = mutableMapOf<String, Pair<String, Long>>()
    private val DOWNLOAD_URL_TTL_MS = 14 * 60_000L

    suspend fun resolveDownloadUrl(key: String): String? {
        val now = System.currentTimeMillis()
        downloadUrlCache[key]?.let { (url, expiresAt) ->
            if (expiresAt > now) return url
        }

        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token ?: return null

        return try {
            val url = BackendApi.presignDownload(idToken, key)
            downloadUrlCache[key] = url to (now + DOWNLOAD_URL_TTL_MS)
            url
        } catch (e: Exception) {
            Log.e("ChatVM", "Не удалось получить ссылку на файл: $key", e)
            null
        }
    }






    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != listeningUid) {
            stopAllListeners()
            if (uid != null) {
                listeningUid = uid
                attachMyProfileListener(uid)
                currentChatId?.let { observeChat(it) }
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        FirestoreListenerCoordinator.register(teardown)
        myUid?.let { uid ->
            listeningUid = uid
            attachMyProfileListener(uid)
        }
    }

    private fun attachMyProfileListener(uid: String) {
        myProfileListener?.remove()
        myProfileListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("ChatVM", "Ошибка снапшота своего профиля", error)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    myName = doc.getString("name") ?: doc.getString("username") ?: "Вы"
                    myIsPremium = doc.getBoolean("isPremium") ?: false
                    myVerifiedIcon = doc.getString("verifiedIcon") ?: "verified"
                    myMessageStyle = doc.getString("messageStyle")
                }
            }
    }

    private fun stopAllListeners() {
        messagesListener?.remove(); messagesListener = null
        chatDocListener?.remove(); chatDocListener = null
        userListener?.remove(); userListener = null
        userListenerChatId = null
        myProfileListener?.remove(); myProfileListener = null
        listeningUid = null
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
        currentChatId = chatId

        messagesListener?.remove()
        messagesListener = db.collection("chats/$chatId/messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("ChatVM", "Ошибка снапшота сообщений", error)
                    return@addSnapshotListener
                }
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
                    if (chatType == "CHANNEL") {
                        markAsViewed(chatId, list, uid)
                    } else {
                        markAsRead(chatId, list, uid)
                    }


                }
            }

        chatDocListener?.remove()
        chatDocListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("ChatVM", "Ошибка снапшота чата", error)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    pinnedMessageId = doc.getString("pinnedMessageId")
                    pinnedMessageText = doc.getString("pinnedMessage")
                    chatTheme = doc.getString("theme")
                    chatDisappearingTimerSec = doc.getLong("disappearingTimerSec") ?: 0L

                    val type = doc.getString("type") ?: "DIRECT"
                    val isGroupField = doc.getBoolean("isGroup") ?: false
                    isGroupChat = type == "GROUP" || type == "CHANNEL" || isGroupField
                    chatType = type

                    @Suppress("UNCHECKED_CAST")
                    chatAdmins = (doc.get("admins") as? List<*>)?.map { it.toString() } ?: emptyList()
                    chatOwnerId = doc.getString("ownerId")

                    @Suppress("UNCHECKED_CAST")
                    chatAdminPermissions = (doc.get("adminPermissions") as? Map<String, Map<String, Boolean>>) ?: emptyMap()

                    if (isGroupChat) {
                        partnerName = doc.getString("title") ?: doc.getString("groupName") ?: "Группа"
                        partnerAvatarUrl = doc.getString("groupAvatar") ?: doc.getString("groupAvatarUrl")
                        partnerUseCustomAvatar = !partnerAvatarUrl.isNullOrBlank()
                        partnerEmoji = doc.getString("emoji") ?: if (type == "CHANNEL") "📢" else "👥"
                        partnerProfileIcon = doc.getString("profileIcon") ?: "default"
                        partnerProfileGlow = doc.getString("profileGlow") ?: "purple"
                        partnerNameColor = doc.getString("nameColor") ?: "gold"

                        val members = (doc.get("participants") as? List<*>) ?: (doc.get("members") as? List<*>)
                        val membersCount = members?.size ?: 0
                        lastSeenText = if (type == "CHANNEL") "${formatCompactCount(membersCount)} подписчиков"
                        else "${formatCompactCount(membersCount)} участников"
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
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        Log.e("ChatVM", "Ошибка снапшота партнёра", error)
                        return@addSnapshotListener
                    }
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
        replyName: String?,


        timerOverrideSec: Long? = null,

        silent: Boolean = false,


        scheduledFor: Date? = null
    ) {
        if (text.isBlank()) return
        if (!canPostInChat) return

        val uid = myUid ?: return
        val isScheduled = scheduledFor != null && scheduledFor.after(Date())
        val effectiveTimerSec = timerOverrideSec ?: chatDisappearingTimerSec

        val messageData = mutableMapOf<String, Any?>(
            "text" to text,
            "senderId" to uid,
            "senderName" to myName,
            "readBy" to listOf(uid),
            "isPremium" to myIsPremium,
            "messageStyle" to myMessageStyle,
            "isSilent" to silent
        )

        if (isScheduled) {



            messageData["timestamp"] = scheduledFor
            messageData["scheduledFor"] = scheduledFor
            messageData["messageState"] = MessageState.SCHEDULED
        } else {
            messageData["timestamp"] = FieldValue.serverTimestamp()
            messageData["messageState"] = MessageState.SENT

            if (effectiveTimerSec > 0) {
                messageData["ttlSeconds"] = effectiveTimerSec
                messageData["expireAt"] = Date(System.currentTimeMillis() + effectiveTimerSec * 1000)
            }
        }

        if (replyText != null && replyName != null) {
            messageData["replyToText"] = replyText
            messageData["replyToName"] = replyName
        }

        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val userRef = db.collection("users").document(uid)

        val batch = db.batch()

        batch.set(msgRef, messageData)




        if (!isScheduled) {
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
        }

        batch.commit()
            .addOnSuccessListener {
                if (!isScheduled) {
                    playSound(messageSentSoundId)

                    if (!isGroupChat && partnerUid.isNotBlank()) {
                        sendPushNotification(partnerUid, text, silent)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ChatVM", "Ошибка отправки", it)
            }
    }


    fun cancelScheduledMessage(chatId: String, messageId: String) {
        db.collection("chats/$chatId/messages").document(messageId).delete()
            .addOnFailureListener { e -> Log.e("ChatVM", "Не удалось отменить отложенное сообщение", e) }
    }










    fun setDisappearingTimer(chatId: String, seconds: Long) {
        db.collection("chats").document(chatId)
            .update("disappearingTimerSec", seconds)
            .addOnSuccessListener {
                chatDisappearingTimerSec = seconds
                val text = if (seconds <= 0) "Таймер исчезающих сообщений отключён"
                else "Таймер исчезающих сообщений: ${formatTimerDuration(seconds)}"
                postSystemMessage(db, chatId, text, SystemAction.DISAPPEARING_TIMER_CHANGED)
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Не удалось установить таймер чата", e) }
    }

    fun sendMediaMessage(
        chatId: String,
        text: String,
        fileBytes: ByteArray,
        replyText: String?,
        replyName: String?
    ) {
        val uid = myUid ?: return
        if (!canPostInChat) return
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idToken = currentUser.getIdToken(false).await().token
                    ?: throw Exception("Не удалось получить idToken")

                val fileName = "media_${uid}_${System.currentTimeMillis()}.jpg"
                val key = "media/$uid/$fileName"
                val contentType = "image/jpeg"

                val presign = BackendApi.presignUpload(idToken, key, contentType)
                BackendApi.uploadBytes(presign.uploadUrl, fileBytes, contentType)
                val messageData = mutableMapOf<String, Any?>(
                    "text" to text.ifBlank { null },
                    "senderId" to uid,
                    "senderName" to myName,
                    "mediaUrl" to presign.key,
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


    suspend fun ensureSavedMessagesChat(myUid: String): String {
        val chatId = "saved_$myUid"
        val chatRef = db.collection("chats").document(chatId)
        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            chatRef.set(
                mapOf(
                    "type" to "SAVED",
                    "isGroup" to true,
                    "participants" to listOf(myUid),
                    "ownerId" to myUid,
                    "groupName" to "Избранное",
                    "groupIcon" to "bookmark",
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "pinned_$myUid" to true,
                    "unreadCount_$myUid" to 0
                )
            ).await()
        }

        return chatId
    }


    fun forwardMessage(message: Message, targetChatId: String) {
        val uid = myUid ?: return

        val messageData = mutableMapOf<String, Any?>(
            "senderId" to uid,
            "senderName" to myName,
            "timestamp" to FieldValue.serverTimestamp(),
            "readBy" to listOf(uid),
            "isPremium" to myIsPremium,
            "messageStyle" to myMessageStyle,
            "forwardedFromName" to message.senderName,
            "type" to MessageType.TEXT
        )

        if (!message.text.isNullOrBlank()) messageData["text"] = message.text


        if (message.mediaUrl != null) messageData["mediaUrl"] = message.mediaUrl
        if (message.voiceUrl != null) {
            messageData["voiceUrl"] = message.voiceUrl
            messageData["voiceDuration"] = message.voiceDuration
        }

        val previewText = message.text?.takeIf { it.isNotBlank() }
            ?: if (message.mediaUrl != null) "📷 Фотография"
            else if (message.voiceUrl != null) "🎤 Голосовое сообщение"
            else "Сообщение"

        val chatRef = db.collection("chats").document(targetChatId)
        val msgRef = chatRef.collection("messages").document()

        val batch = db.batch()
        batch.set(msgRef, messageData)
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to previewText,
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit()
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка пересылки сообщения", e) }
    }

    fun createGroupChat(
        title: String,
        description: String,
        isPublic: Boolean,
        selectedUserIds: List<String>,
        groupAvatar: String? = null,
        onSuccess: (String) -> Unit
    ) {
        val uid = myUid ?: return
        if (title.isBlank() || selectedUserIds.isEmpty()) return

        val newChatId = db.collection("chats").document().id
        val allMembers = selectedUserIds.toMutableList().apply {
            if (!contains(uid)) add(uid)
        }

        val groupData = mutableMapOf<String, Any?>(
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
        if (!groupAvatar.isNullOrBlank()) groupData["groupAvatar"] = groupAvatar

        db.collection("chats").document(newChatId)
            .set(groupData)
            .addOnSuccessListener {
                val systemMessage = mapOf(
                    "type" to MessageType.SYSTEM,
                    "text" to "$myName создал(а) группу \"$title\"",
                    "senderId" to "system",
                    "senderName" to "Система",
                    "systemAction" to SystemAction.GROUP_CREATED,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(uid)
                )
                db.collection("chats/$newChatId/messages").add(systemMessage)
                onSuccess(newChatId)
            }
    }

    fun createChannel(
        title: String,
        description: String,
        isPublic: Boolean,
        username: String?,
        channelAvatar: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val uid = myUid ?: return
        if (title.isBlank()) return

        val cleanUsername = username?.lowercase()?.trim()?.takeIf { it.isNotBlank() }
        if (isPublic && cleanUsername.isNullOrBlank()) {
            onError("У публичного канала должен быть @username")
            return
        }

        fun writeChannel() {
            val newChatId = db.collection("chats").document().id

            val channelData = mutableMapOf<String, Any?>(
                "chatId" to newChatId,
                "type" to "CHANNEL",
                "isGroup" to true,
                "groupName" to title.trim(),
                "description" to description.trim(),
                "ownerId" to uid,
                "admins" to listOf(uid),
                "isPublic" to isPublic,
                "participants" to listOf(uid),
                "members" to listOf(uid),
                "pinnedMessage" to null,
                "lastMessage" to "Канал \"${title.trim()}\" создан",
                "lastSenderId" to "system",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanUsername != null) channelData["username"] = cleanUsername
            if (!channelAvatar.isNullOrBlank()) channelData["groupAvatar"] = channelAvatar

            db.collection("chats").document(newChatId)
                .set(channelData)
                .addOnSuccessListener {
                    val systemMessage = mapOf(
                        "type" to MessageType.SYSTEM,
                        "text" to "Канал \"$title\" создан",
                        "senderId" to "system",
                        "senderName" to "Система",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "readBy" to listOf(uid)
                    )
                    db.collection("chats/$newChatId/messages").add(systemMessage)
                    onSuccess(newChatId)
                }
                .addOnFailureListener { e ->
                    Log.e("ChatVM", "Ошибка создания канала", e)
                    onError(e.localizedMessage ?: "Ошибка создания канала")
                }
        }

        if (cleanUsername != null) {
            checkChannelUsername(cleanUsername) { isAvailable ->
                if (isAvailable) writeChannel()
                else onError("Юзернейм @$cleanUsername уже занят")
            }
        } else {
            writeChannel()
        }
    }


    fun checkChannelUsername(username: String, excludeChatId: String? = null, onResult: (Boolean) -> Unit) {
        val clean = username.lowercase().trim()
        var channelDone = false
        var userDone = false
        var isTaken = false

        fun finish() {
            if (channelDone && userDone) onResult(!isTaken)
        }

        db.collection("chats")
            .whereEqualTo("username", clean)
            .limit(2)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.documents.any { it.id != excludeChatId }) isTaken = true
                channelDone = true
                finish()
            }
            .addOnFailureListener { channelDone = true; finish() }

        db.collection("users")
            .whereEqualTo("username", clean)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) isTaken = true
                userDone = true
                finish()
            }
            .addOnFailureListener { userDone = true; finish() }
    }

    fun updateChannelUsername(
        chatId: String,
        newUsername: String,
        makePublic: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanUsername = newUsername.lowercase().trim().takeIf { it.isNotBlank() }
        if (makePublic && cleanUsername.isNullOrBlank()) {
            onError("У публичного канала должен быть @username")
            return
        }

        fun write() {
            val updates = mutableMapOf<String, Any?>("isPublic" to makePublic)
            updates["username"] = cleanUsername
            db.collection("chats").document(chatId)
                .update(updates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка сохранения") }
        }

        if (cleanUsername != null) {
            checkChannelUsername(cleanUsername, excludeChatId = chatId) { isAvailable ->
                if (isAvailable) write() else onError("Юзернейм @$cleanUsername уже занят")
            }
        } else {
            write()
        }
    }


    object AdminPermission {
        const val CAN_POST = "canPost"
        const val CAN_DELETE_MESSAGES = "canDeleteMessages"
        const val CAN_BAN_USERS = "canBanUsers"
        const val CAN_ADD_ADMINS = "canAddAdmins"
        const val CAN_EDIT_INFO = "canEditInfo"
        const val CAN_INVITE_USERS = "canInviteUsers"

        fun default(): Map<String, Boolean> = mapOf(
            CAN_POST to true,
            CAN_DELETE_MESSAGES to true,
            CAN_BAN_USERS to false,
            CAN_ADD_ADMINS to false,
            CAN_EDIT_INFO to false,
            CAN_INVITE_USERS to true
        )
    }

    fun promoteToAdmin(
        chatId: String,
        uid: String,
        permissions: Map<String, Boolean> = AdminPermission.default(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "admins" to FieldValue.arrayUnion(uid),
                    "adminPermissions.$uid" to permissions
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось назначить админа") }
    }

    fun updateAdminPermissions(
        chatId: String,
        uid: String,
        permissions: Map<String, Boolean>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .update("adminPermissions.$uid", permissions)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось обновить права") }
    }

    fun removeAdmin(
        chatId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "admins" to FieldValue.arrayRemove(uid),
                    "adminPermissions.$uid" to FieldValue.delete()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось снять права") }
    }

    fun banUser(
        chatId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "bannedUids" to FieldValue.arrayUnion(uid),
                    "participants" to FieldValue.arrayRemove(uid),
                    "members" to FieldValue.arrayRemove(uid),

                    "admins" to FieldValue.arrayRemove(uid),
                    "adminPermissions.$uid" to FieldValue.delete()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось забанить") }
    }

    fun unbanUser(
        chatId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("chats").document(chatId)
            .update("bannedUids", FieldValue.arrayRemove(uid))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось разбанить") }
    }


    fun generateInviteCode(
        chatId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val code = (1..10).map { chars.random() }.joinToString("")
        db.collection("chats").document(chatId)
            .update("inviteCode", code)
            .addOnSuccessListener { onSuccess(code) }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось создать ссылку") }
    }


    fun getInviteInfo(code: String, onResult: (InvitePreview?) -> Unit) {
        db.collection("chats")
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val type = doc.getString("type") ?: "GROUP"
                @Suppress("UNCHECKED_CAST")
                val participants = (doc.get("participants") as? List<String>)
                    ?: (doc.get("members") as? List<String>) ?: emptyList()

                onResult(
                    InvitePreview(
                        chatId = doc.id,
                        name = doc.getString("title") ?: doc.getString("groupName") ?: "Чат",
                        avatarUrl = doc.getString("groupAvatar") ?: doc.getString("groupAvatarUrl"),
                        icon = doc.getString("groupIcon") ?: "group",
                        glowColor = doc.getString("glowColor") ?: "accent",
                        membersCount = participants.size,
                        isChannel = type == "CHANNEL",
                    )
                )
            }
            .addOnFailureListener { onResult(null) }
    }


    fun joinByInviteCode(
        code: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = myUid ?: return onError("Не авторизован")
        db.collection("chats")
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) {
                    onError("Ссылка недействительна")
                    return@addOnSuccessListener
                }
                @Suppress("UNCHECKED_CAST")
                val banned = (doc.get("bannedUids") as? List<String>) ?: emptyList()
                if (uid in banned) {
                    onError("Вы забанены в этом чате")
                    return@addOnSuccessListener
                }
                doc.reference
                    .update(
                        mapOf(
                            "participants" to FieldValue.arrayUnion(uid),
                            "members" to FieldValue.arrayUnion(uid),
                            "unreadCount_$uid" to 0
                        )
                    )
                    .addOnSuccessListener { onSuccess(doc.id) }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Не удалось вступить") }
            }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Ошибка проверки ссылки") }
    }

    private fun sendPushNotification(receiverUid: String, messageText: String, silent: Boolean = false) {
        if (receiverUid.isBlank()) return

        db.collection("users").document(receiverUid).get().addOnSuccessListener { doc ->
            val token = doc.getString("fcmToken") ?: return@addOnSuccessListener

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    BackendApi.notify(token = token, senderName = myName, text = messageText, silent = silent)
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
                .addOnSuccessListener {
                    // Пишем событие в отдельную коллекцию — Cloud Function
                    // на onDocumentCreated отправит письмо на mayassupp@gmail.com.
                    // Слать почту прямо с клиента нельзя (пришлось бы хранить
                    // SMTP-креды в апк), поэтому триггерим через Firestore.
                    db.collection("reactionEvents").add(
                        mapOf(
                            "chatId" to chatId,
                            "messageId" to messageId,
                            "messageText" to (msg.text?.take(200) ?: ""),
                            "emoji" to emoji,
                            "reactorUid" to uid,
                            "reactorName" to myName,
                            "messageSenderId" to msg.senderId,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    ).addOnFailureListener { e ->
                        Log.e("ChatVM", "Не удалось залогировать реакцию", e)
                    }
                }
        }
    }

    fun pinMessage(chatId: String, message: Message) {
        db.collection("chats").document(chatId)
            .update(
                "pinnedMessageId", message.id,
                "pinnedMessage", message.text ?: if (message.mediaUrl != null) "📷 Фотография" else "Голосовое сообщение"
            )
            .addOnSuccessListener {
                postSystemMessage(
                    db, chatId,
                    text = "$myName закрепил(а) сообщение",
                    action = SystemAction.PINNED,
                    refMessageId = message.id
                )
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка закрепления сообщения", e) }
    }

    fun unpinMessage(chatId: String) {
        db.collection("chats").document(chatId)
            .update("pinnedMessageId", null, "pinnedMessage", null)
            .addOnSuccessListener {
                postSystemMessage(db, chatId, text = "$myName открепил(а) сообщение", action = SystemAction.UNPINNED)
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка открепления сообщения", e) }
    }


    fun logCallMessage(
        chatId: String,
        callType: String,
        status: String,
        durationSec: Int = 0
    ) {
        val uid = myUid ?: return

        val text = when (status) {
            CallStatus.MISSED -> "Пропущенный звонок"
            CallStatus.DECLINED -> "Звонок отклонён"
            else -> {
                val minutes = durationSec / 60
                val seconds = durationSec % 60
                "Звонок, $minutes:${seconds.toString().padStart(2, '0')}"
            }
        }

        val messageData = mutableMapOf<String, Any?>(
            "type" to MessageType.CALL,
            "text" to text,
            "senderId" to uid,
            "senderName" to myName,
            "callType" to callType,
            "callStatus" to status,
            "callDurationSec" to durationSec,
            "timestamp" to FieldValue.serverTimestamp(),
            "readBy" to listOf(uid)
        )

        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val previewIcon = if (callType == "VIDEO") "📹" else "📞"

        val batch = db.batch()
        batch.set(msgRef, messageData)
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to "$previewIcon $text",
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (!isGroupChat && partnerUid.isNotBlank() && status == CallStatus.MISSED) {
            batch.update(chatRef, "unreadCount_$partnerUid", FieldValue.increment(1))
        }

        batch.commit()
            .addOnSuccessListener {
                if (!isGroupChat && partnerUid.isNotBlank() && status == CallStatus.MISSED) {
                    sendPushNotification(partnerUid, text)
                }
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Не удалось сохранить сообщение о звонке", e) }
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
                if (snapshot.isEmpty) {
                    onSuccess()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener {
                        viewModelScope.launch {
                            repository.clearChatHistoryLocally(chatId)
                        }
                        onSuccess()
                    }
            }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка очистки чата", e) }
    }

    fun blockUser(myUid: String, partnerUid: String, onSuccess: () -> Unit) {
        db.collection("users").document(myUid)
            .set(mapOf("blocked" to FieldValue.arrayUnion(partnerUid)), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка блокировки пользователя", e) }
    }

    private fun markAsRead(chatId: String, list: List<Message>, uid: String) {
        val unread = list.filter { !it.readBy.contains(uid) }
        if (unread.isEmpty()) return

        val batch = db.batch()
        unread.forEach { msg ->
            val ref = db.collection("chats/$chatId/messages").document(msg.id)
            batch.update(ref, "readBy", FieldValue.arrayUnion(uid))
        }
        batch.commit()
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка отметки прочтения", e) }
    }


    private fun markAsViewed(chatId: String, list: List<Message>, uid: String) {
        val unviewed = list.filter { it.senderId != uid && !it.viewedBy.contains(uid) }
        if (unviewed.isEmpty()) return

        val batch = db.batch()
        unviewed.forEach { msg ->
            val ref = db.collection("chats/$chatId/messages").document(msg.id)
            batch.update(ref, "viewedBy", FieldValue.arrayUnion(uid))
        }
        batch.commit()
            .addOnFailureListener { e -> Log.e("ChatVM", "Ошибка отметки просмотра", e) }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = myUid ?: return
        if (!isGroupChat) {
            db.collection("users").document(uid)
                .update("typing.$chatId", isTyping)
        }
    }

    fun deleteGroup(chatId: String, onSuccess: () -> Unit) {
        db.collection("chats").document(chatId)
            .collection("messages").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnCompleteListener {
                        db.collection("chats").document(chatId).delete()
                            .addOnSuccessListener {
                                viewModelScope.launch {
                                    repository.clearChatHistoryLocally(chatId)
                                }
                                onSuccess()
                            }
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
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idToken = currentUser.getIdToken(false).await().token
                    ?: throw Exception("Не удалось получить idToken")

                val fileName = "voice_${uid}_${System.currentTimeMillis()}.m4a"
                val key = "voice/$uid/$fileName"
                val contentType = "audio/mp4"

                val presign = BackendApi.presignUpload(idToken, key, contentType)
                BackendApi.uploadBytes(presign.uploadUrl, audioBytes, contentType)


                val messageData = mutableMapOf<String, Any?>(
                    "senderId" to uid,
                    "senderName" to myName,
                    "voiceUrl" to presign.key,
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
        auth.removeAuthStateListener(authStateListener)
        FirestoreListenerCoordinator.unregister(teardown)
        stopAllListeners()
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
    val isPremium: Boolean,
    val profileGlow: String,
    val isOwner: Boolean,
    val isAdmin: Boolean,
    val isModerator: Boolean
)


class GroupMembersVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val myUid: String get() = auth.currentUser?.uid ?: ""

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var members by mutableStateOf<List<GroupMemberUi>>(emptyList())
        private set
    var memberIds by mutableStateOf<List<String>>(emptyList())
        private set
    var bannedMembers by mutableStateOf<List<GroupMemberUi>>(emptyList())
        private set
    var inviteCode by mutableStateOf<String?>(null)
        private set
    var isMyAdmin by mutableStateOf(false)
        private set
    var isMyOwner by mutableStateOf(false)
        private set
    var canIBan by mutableStateOf(false)
        private set
    var canIInvite by mutableStateOf(false)
        private set

    private var chatListener: ListenerRegistration? = null
    private var ownerId: String = ""
    private var adminsList: List<String> = emptyList()
    private var adminPermissions: Map<String, Map<String, Boolean>> = emptyMap()
    private var bannedIds: List<String> = emptyList()

    fun observeGroup(chatId: String) {
        chatListener?.remove()
        isLoading = true
        chatListener = db.collection("chats").document(chatId)
            .addSnapshotListener { doc, err ->
                if (err != null || doc == null || !doc.exists()) {
                    errorMessage = "Не удалось загрузить участников"
                    isLoading = false
                    return@addSnapshotListener
                }
                ownerId = doc.getString("ownerId") ?: ""
                @Suppress("UNCHECKED_CAST")
                adminsList = (doc.get("admins") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                adminPermissions = (doc.get("adminPermissions") as? Map<String, Map<String, Boolean>>) ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val participants = (doc.get("participants") as? List<String>)
                    ?: (doc.get("members") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                bannedIds = (doc.get("bannedUids") as? List<String>) ?: emptyList()
                inviteCode = doc.getString("inviteCode")
                memberIds = participants

                isMyOwner = myUid == ownerId
                val myPerms = adminPermissions[myUid] ?: emptyMap()
                isMyAdmin = isMyOwner || myUid in adminsList
                canIBan = isMyOwner || myPerms[ChatVM.AdminPermission.CAN_BAN_USERS] == true
                canIInvite = isMyOwner || myPerms[ChatVM.AdminPermission.CAN_INVITE_USERS] == true

                loadProfiles(participants, bannedIds)
            }
    }

    private fun loadProfiles(participantIds: List<String>, bannedUids: List<String>) {
        viewModelScope.launch {
            try {
                members = participantIds.mapNotNull { uid -> fetchMemberUi(uid) }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Не удалось загрузить участников"
                isLoading = false
            }
            bannedMembers = bannedUids.mapNotNull { uid -> fetchMemberUi(uid) }
        }
    }

    private suspend fun fetchMemberUi(uid: String): GroupMemberUi? {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            val perms = adminPermissions[uid] ?: emptyMap()
            val fullAdmin = perms[ChatVM.AdminPermission.CAN_ADD_ADMINS] == true
            GroupMemberUi(
                uid = uid,
                name = snap.getString("name") ?: snap.getString("username") ?: "Без имени",
                username = snap.getString("username") ?: "",
                avatarUrl = snap.getString("avatarUrl"),
                useCustomAvatar = snap.getBoolean("useCustomAvatar") ?: false,
                profileIcon = snap.getString("profileIcon") ?: "ghost",
                isPremium = snap.getBoolean("isPremium") ?: false,
                profileGlow = snap.getString("profileGlow") ?: "purple",
                isOwner = uid == ownerId,
                isAdmin = uid != ownerId && uid in adminsList && fullAdmin,
                isModerator = uid != ownerId && uid in adminsList && !fullAdmin
            )
        } catch (e: Exception) {
            null
        }
    }

    fun searchAddableUsers(existingIds: List<String>, query: String, onResult: (List<GroupMemberUi>) -> Unit) {
        val clean = query.trim().lowercase().removePrefix("@")
        if (clean.length < 2) { onResult(emptyList()); return }
        db.collection("users")
            .orderBy("username")
            .startAt(clean)
            .endAt(clean + "\uf8ff")
            .limit(15)
            .get()
            .addOnSuccessListener { snap ->
                val found = snap.documents.mapNotNull { doc ->
                    if (doc.id in existingIds) return@mapNotNull null
                    GroupMemberUi(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("username") ?: "Без имени",
                        username = doc.getString("username") ?: "",
                        avatarUrl = doc.getString("avatarUrl"),
                        useCustomAvatar = doc.getBoolean("useCustomAvatar") ?: false,
                        profileIcon = doc.getString("profileIcon") ?: "ghost",
                        isPremium = doc.getBoolean("isPremium") ?: false,
                        profileGlow = doc.getString("profileGlow") ?: "purple",
                        isOwner = false, isAdmin = false, isModerator = false
                    )
                }
                onResult(found)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun addMembers(chatId: String, uids: List<String>, onResult: (Boolean) -> Unit) {
        if (uids.isEmpty()) { onResult(false); return }
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(*uids.toTypedArray()),
                    "members" to FieldValue.arrayUnion(*uids.toTypedArray())
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun promoteToAdmin(chatId: String, uid: String, onResult: (Boolean) -> Unit) {
        val perms = ChatVM.AdminPermission.default() + mapOf(
            ChatVM.AdminPermission.CAN_ADD_ADMINS to true,
            ChatVM.AdminPermission.CAN_EDIT_INFO to true
        )
        db.collection("chats").document(chatId)
            .update(mapOf("admins" to FieldValue.arrayUnion(uid), "adminPermissions.$uid" to perms))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun promoteToModerator(chatId: String, uid: String, onResult: (Boolean) -> Unit) {
        val perms = mapOf(
            ChatVM.AdminPermission.CAN_POST to true,
            ChatVM.AdminPermission.CAN_DELETE_MESSAGES to true,
            ChatVM.AdminPermission.CAN_BAN_USERS to true,
            ChatVM.AdminPermission.CAN_ADD_ADMINS to false,
            ChatVM.AdminPermission.CAN_EDIT_INFO to false,
            ChatVM.AdminPermission.CAN_INVITE_USERS to true
        )
        db.collection("chats").document(chatId)
            .update(mapOf("admins" to FieldValue.arrayUnion(uid), "adminPermissions.$uid" to perms))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun demoteAdmin(chatId: String, uid: String, onResult: (Boolean, String?) -> Unit) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "admins" to FieldValue.arrayRemove(uid),
                    "adminPermissions.$uid" to FieldValue.delete()
                )
            )
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun demoteModerator(chatId: String, uid: String, onResult: (Boolean) -> Unit) {
        demoteAdmin(chatId, uid) { success, _ -> onResult(success) }
    }

    fun kickMember(chatId: String, uid: String, onResult: (Boolean, String?) -> Unit) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(uid),
                    "members" to FieldValue.arrayRemove(uid),
                    "admins" to FieldValue.arrayRemove(uid),
                    "adminPermissions.$uid" to FieldValue.delete()
                )
            )
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun banMember(chatId: String, uid: String, onResult: (Boolean, String?) -> Unit) {
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(uid),
                    "members" to FieldValue.arrayRemove(uid),
                    "admins" to FieldValue.arrayRemove(uid),
                    "adminPermissions.$uid" to FieldValue.delete(),
                    "bannedUids" to FieldValue.arrayUnion(uid)
                )
            )
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun unbanMember(chatId: String, uid: String, onResult: (Boolean) -> Unit) {
        db.collection("chats").document(chatId)
            .update("bannedUids", FieldValue.arrayRemove(uid))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun generateInviteLink(chatId: String, onResult: (String?) -> Unit) {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val code = (1..10).map { chars.random() }.joinToString("")
        db.collection("chats").document(chatId)
            .update("inviteCode", code)
            .addOnSuccessListener { onResult(code) }
            .addOnFailureListener { onResult(null) }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }
}