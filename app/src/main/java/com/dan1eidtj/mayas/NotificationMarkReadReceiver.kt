/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationMarkReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MayasNotifications.ACTION_MARK_READ) return

        val chatId = intent.getStringExtra(MayasNotifications.EXTRA_CHAT_ID)
        val notificationId = intent.getIntExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
        chatId?.let { ChatNotificationStore.clear(context, it) }

        if (chatId == null) return
        markRemoteMessagesAsRead(context, chatId)
    }

    private fun markRemoteMessagesAsRead(context: Context, chatId: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val pendingResult = goAsync()
        val db = FirebaseFirestore.getInstance()

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .whereEqualTo("isRead", false)
            .get()
            .addOnCompleteListener { task ->
                val docs = task.result?.documents.orEmpty().filter {
                    it.getString("senderId") != myUid
                }

                if (docs.isEmpty()) {
                    pendingResult.finish()
                    return@addOnCompleteListener
                }

                val batch = db.batch()
                docs.forEach { doc -> batch.update(doc.reference, "isRead", true) }
                batch.commit().addOnCompleteListener {
                    pendingResult.finish()
                }
            }
    }
}
