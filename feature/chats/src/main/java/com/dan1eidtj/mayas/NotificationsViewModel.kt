package com.dan1eidtj.mayas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Тип уведомления. Строковое значение — это то, что реально лежит в поле "type"
 * в документе Firestore (users/{uid}/notifications/{id}).
 */
enum class NotificationType(val raw: String) {
    MISSED_CALL("missed_call"),
    SYSTEM("system"),
    PROMO("promo");

    companion object {
        fun fromRaw(raw: String?): NotificationType = when (raw) {
            MISSED_CALL.raw -> MISSED_CALL
            PROMO.raw -> PROMO
            else -> SYSTEM
        }
    }
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val isRead: Boolean,
    val payload: Map<String, Any?> = emptyMap()
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myUid: String? get() = auth.currentUser?.uid

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listener: ListenerRegistration? = null

    init {
        startListening()
    }

    private fun collectionRef() = myUid?.let {
        db.collection("users").document(it).collection("notifications")
    }

    fun startListening() {
        listener?.remove()
        val ref = collectionRef() ?: run {
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        listener = ref
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("NotificationsVM", "Ошибка снапшота уведомлений", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                _notifications.value = snapshot.documents.map { doc ->
                    NotificationItem(
                        id = doc.id,
                        type = NotificationType.fromRaw(doc.getString("type")),
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false,
                        payload = doc.data ?: emptyMap()
                    )
                }
            }
    }

    fun markAsRead(id: String) {
        val ref = collectionRef() ?: return
        viewModelScope.launch {
            try {
                ref.document(id).update("isRead", true).await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Ошибка отметки прочитанным $id", e)
            }
        }
    }

    fun markAllAsRead() {
        val ref = collectionRef() ?: return
        val unread = _notifications.value.filter { !it.isRead }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            try {
                val batch = db.batch()
                unread.forEach { item ->
                    batch.update(ref.document(item.id), "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Ошибка массовой отметки прочитанным", e)
            }
        }
    }

    fun delete(id: String) {
        val ref = collectionRef() ?: return
        viewModelScope.launch {
            try {
                ref.document(id).delete().await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Ошибка удаления уведомления $id", e)
            }
        }
    }

    fun clearAll() {
        val ref = collectionRef() ?: return
        val ids = _notifications.value.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val batch = db.batch()
                ids.forEach { id -> batch.delete(ref.document(id)) }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Ошибка очистки уведомлений", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
