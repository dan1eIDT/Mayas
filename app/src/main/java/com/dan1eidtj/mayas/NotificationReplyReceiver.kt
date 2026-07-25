package com.dan1eidtj.mayas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class NotificationReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MayasNotifications.ACTION_REPLY) return

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(MayasNotifications.KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()
        val chatId = intent.getStringExtra(MayasNotifications.EXTRA_CHAT_ID)
        val notificationId = intent.getIntExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, -1)
        val senderName = intent.getStringExtra(MayasNotifications.EXTRA_SENDER_NAME) ?: "MAYAS"

        if (replyText.isNullOrBlank() || chatId == null || notificationId == -1) return

        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val pendingResult = goAsync()






        val messageDoc = hashMapOf(
            "senderId" to myUid,
            "text" to replyText,
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false
        )

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageDoc)
            .addOnCompleteListener {


                appendReplyToNotification(context, chatId, notificationId, senderName, replyText)
                pendingResult.finish()
            }
    }


    private fun appendReplyToNotification(
        context: Context,
        chatId: String,
        notificationId: Int,
        senderName: String,
        replyText: String,
    ) {


        if (!MayasNotifications.canPostNotifications(context)) return

        val history = ChatNotificationStore.appendMessage(
            context = context,
            chatId = chatId,
            text = replyText,
            timestampMs = System.currentTimeMillis(),
            mine = true
        )

        val me = Person.Builder()
            .setName(FirebaseAuth.getInstance().currentUser?.displayName ?: "Я")
            .build()
        val sender = Person.Builder().setName(senderName).build()

        val style = NotificationCompat.MessagingStyle(me)
        style.conversationTitle = senderName
        style.isGroupConversation = false

        history.forEach { stored ->
            if (stored.mine) {
                style.addMessage(stored.text, stored.timestampMs, null as Person?)
            } else {
                style.addMessage(stored.text, stored.timestampMs, sender)
            }
        }

        val updated = NotificationCompat.Builder(context, MayasNotifications.CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setStyle(style)
            .setAutoCancel(true)
            .setGroup(MayasNotifications.GROUP_KEY_MESSAGES)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, updated)
    }
}