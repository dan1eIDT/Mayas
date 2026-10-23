/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationMarkReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MayasNotifications.ACTION_MARK_READ) return

        val chatId = intent.getStringExtra(MayasNotifications.EXTRA_CHAT_ID)
            ?.takeIf { it.isNotBlank() } ?: return
        val notificationId = intent.getIntExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, -1)
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid.isNullOrBlank()) {
            showToast(context, R.string.notification_mark_read_requires_login)
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                markRemoteMessagesAsRead(chatId, myUid)
                withContext(Dispatchers.Main) {
                    if (notificationId != -1) {
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }
                    ChatNotificationStore.clear(appContext, chatId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Could not mark chat read from notification", error)
                showToast(appContext, R.string.notification_mark_read_failed)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun markRemoteMessagesAsRead(chatId: String, uid: String) {
        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)
        val chat = chatRef.get().await()
        if (!chat.exists()) throw IllegalStateException("CHAT_NOT_FOUND")

        val participants = ((chat.get("participants") as? List<*>)
            ?: (chat.get("members") as? List<*>))
            ?.filterIsInstance<String>()
            .orEmpty()
        if (uid !in participants) throw IllegalStateException("NOT_A_CHAT_MEMBER")

        val isChannel = chat.getString("type") == "CHANNEL"
        val readField = if (isChannel) "viewedBy" else "readBy"
        val messageCollection = chatRef.collection("messages")
        var query = messageCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
        var scanned = 0

        while (true) {
            val page = query.get().await()
            if (page.isEmpty) break

            val batch = db.batch()
            var writeCount = 0
            for (message in page.documents) {
                if (message.getString("senderId") == uid) continue
                if (message.getString("messageState") == "SCHEDULED") continue
                val readers = message.get(readField) as? List<*> ?: emptyList<Any>()
                if (uid in readers) continue
                batch.update(message.reference, readField, FieldValue.arrayUnion(uid))
                writeCount++
            }
            if (writeCount > 0) batch.commit().await()

            scanned += page.size()
            val last = page.documents.last()
            query = messageCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(last)
                .limit(PAGE_SIZE.toLong())

            if (scanned >= MAX_MESSAGES_TO_SCAN) {
                val nextPage = query.limit(1).get().await()
                if (!nextPage.isEmpty) throw IllegalStateException("CHAT_TOO_LARGE_OPEN_IN_APP")
                break
            }
        }

        chatRef.update("unreadCount_$uid", 0).await()
    }

    private fun showToast(context: Context, stringRes: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context.applicationContext, stringRes, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val TAG = "NotificationMarkRead"
        const val PAGE_SIZE = 400
        const val MAX_MESSAGES_TO_SCAN = 10_000
    }
}
