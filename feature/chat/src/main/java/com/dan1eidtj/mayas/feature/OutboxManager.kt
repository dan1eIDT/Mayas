/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.mayas.db.OutboxEntity
import com.dan1eidtj.mayas.db.OutboxStore
import com.dan1eidtj.mayas.storage.B2MediaClient
import com.dan1eidtj.mayas.storage.MediaFileCache
import com.dan1eidtj.mayas.storage.MediaKind
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

object OutboxManager {

    private const val TAG = "OutboxManager"
    private const val MAX_ATTEMPTS = 5
    private const val COMMIT_TIMEOUT_MS = 15_000L
    private const val DIR_NAME = "outbox"

    class SourceFile(
        val file: File,
        val contentType: String,
        val extension: String,
        val storageKind: MediaKind,
        val namePrefix: String?
    )

    class Spec(
        val chatId: String,
        val kind: String,
        val files: List<SourceFile>,
        val targetField: String,
        val fields: Map<String, Any?>,
        val previewText: String,
        val caption: String?,
        val durationSec: Int,
        val partnerUid: String,
        val isGroup: Boolean
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var started = false
    private var appContext: Context? = null

    fun start(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (started) return
        started = true

        try {
            val manager = app.getSystemService(ConnectivityManager::class.java)
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    process()
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось подписаться на состояние сети", e)
        }

        scope.launch {
            OutboxStore(app).resetSending()
            runQueue(app)
        }
    }

    fun process() {
        val context = appContext ?: return
        scope.launch { runQueue(context) }
    }

    suspend fun enqueue(context: Context, spec: Spec) {
        val app = context.applicationContext
        start(app)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Нет авторизованного пользователя")
        val id = UUID.randomUUID().toString()
        val dir = File(app.noBackupFilesDir, DIR_NAME).apply { mkdirs() }

        val files = JSONArray()
        spec.files.forEachIndexed { index, source ->
            val target = File(dir, "${id}_$index.${source.extension}")
            if (!source.file.renameTo(target)) {
                source.file.copyTo(target, overwrite = true)
                source.file.delete()
            }
            files.put(
                JSONObject()
                    .put("path", target.absolutePath)
                    .put("contentType", source.contentType)
                    .put("extension", source.extension)
                    .put("storageKind", source.storageKind.name)
                    .put("namePrefix", source.namePrefix ?: JSONObject.NULL)
            )
        }

        val fields = JSONObject()
        spec.fields.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is List<*> -> fields.put(key, JSONArray(value))
                else -> fields.put(key, value)
            }
        }

        OutboxStore(app).enqueue(
            OutboxEntity(
                id = id,
                ownerUid = uid,
                chatId = spec.chatId,
                kind = spec.kind,
                createdAt = System.currentTimeMillis(),
                state = OutboxEntity.STATE_PENDING,
                attempts = 0,
                filesJson = files.toString(),
                fieldsJson = fields.toString(),
                targetField = spec.targetField,
                previewText = spec.previewText,
                partnerUid = spec.partnerUid,
                isGroup = spec.isGroup,
                caption = spec.caption,
                durationSec = spec.durationSec
            )
        )
        process()
    }

    suspend fun retryFailed(context: Context, chatId: String) {
        OutboxStore(context).retryFailed(chatId)
        process()
    }

    suspend fun cancel(context: Context, id: String) {
        val store = OutboxStore(context)
        val item = store.get(id) ?: return
        store.delete(id)
        deleteFiles(item)
    }

    private fun deleteFiles(item: OutboxEntity) {
        try {
            val array = JSONArray(item.filesJson)
            for (i in 0 until array.length()) {
                File(array.getJSONObject(i).getString("path")).delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось удалить файлы очереди", e)
        }
    }

    private suspend fun runQueue(context: Context) {
        mutex.withLock {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val store = OutboxStore(context)
            for (item in store.active(uid)) {
                store.setState(item.id, OutboxEntity.STATE_SENDING, item.attempts)
                try {
                    Log.d(TAG, "Отправка ${item.kind} ${item.id}, чат ${item.chatId}")
                    deliver(context, uid, item)
                    Log.d(TAG, "Отправлено ${item.kind} ${item.id}")
                    store.delete(item.id)
                    deleteFiles(item)
                } catch (e: CancellationException) {
                    store.setState(item.id, OutboxEntity.STATE_PENDING, item.attempts)
                    throw e
                } catch (e: Exception) {
                    val offline = e is IOException || e is FirebaseNetworkException || e.cause is IOException
                    Log.e(TAG, "Не удалось отправить ${item.kind} ${item.id}, offline=$offline", e)
                    store.setError(item.id, "${e.javaClass.simpleName}: ${e.message.orEmpty().take(200)}")
                    if (offline) {
                        store.setState(item.id, OutboxEntity.STATE_PENDING, item.attempts)
                        return
                    }
                    val attempts = item.attempts + 1
                    store.setState(
                        item.id,
                        if (attempts >= MAX_ATTEMPTS) OutboxEntity.STATE_FAILED else OutboxEntity.STATE_PENDING,
                        attempts
                    )
                }
            }
        }
    }

    private fun jsonToValue(value: Any?): Any? = when (value) {
        is JSONArray -> List(value.length()) { jsonToValue(value.get(it)) }
        JSONObject.NULL -> null
        else -> value
    }

    private suspend fun deliver(context: Context, uid: String, item: OutboxEntity) {
        val client = B2MediaClient()
        val filesArray = JSONArray(item.filesJson)
        val keys = mutableListOf<String>()

        for (i in 0 until filesArray.length()) {
            val entry = filesArray.getJSONObject(i)
            val file = File(entry.getString("path"))
            if (!file.exists()) throw IllegalStateException("Файл очереди не найден: ${file.name}")
            val prefix = if (entry.isNull("namePrefix")) null else entry.getString("namePrefix")
            val key = client.uploadFile(
                kind = MediaKind.valueOf(entry.getString("storageKind")),
                file = file,
                contentType = entry.getString("contentType"),
                extension = entry.getString("extension"),
                namePrefix = prefix
            )
            MediaFileCache.storeLocalCopy(context, key, file)
            keys.add(key)
        }

        val fields = JSONObject(item.fieldsJson)
        val data = mutableMapOf<String, Any?>()
        fields.keys().forEach { key -> data[key] = jsonToValue(fields.get(key)) }

        if (item.targetField == "mediaUrls") {
            data["mediaUrls"] = keys
        } else {
            data[item.targetField] = keys.first()
        }
        data["senderId"] = uid
        data["timestamp"] = FieldValue.serverTimestamp()
        data["readBy"] = listOf(uid)

        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(item.chatId)
        val messageRef = chatRef.collection("messages").document()

        val batch = db.batch()
        batch.set(messageRef, data)
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to item.previewText,
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(db.collection("users").document(uid), "messagesSent", FieldValue.increment(1))
        if (!item.isGroup && item.partnerUid.isNotBlank()) {
            batch.update(chatRef, "unreadCount_${item.partnerUid}", FieldValue.increment(1))
        }

        withTimeoutOrNull(COMMIT_TIMEOUT_MS) { batch.commit().await() }

        if (!item.isGroup && item.partnerUid.isNotBlank()) {
            try {
                val token = db.collection("users").document(item.partnerUid).get().await().getString("fcmToken")
                if (token != null) {
                    BackendApi.notify(
                        chatId = item.chatId,
                        senderId = uid,
                        token = token,
                        senderName = data["senderName"] as? String ?: "",
                        text = item.previewText,
                        silent = false
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Не удалось отправить пуш", e)
            }
        }
    }
}
