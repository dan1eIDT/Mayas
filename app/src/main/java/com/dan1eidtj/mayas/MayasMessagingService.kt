package com.dan1eidtj.mayas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MayasMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        when (message.data["type"]) {
            "incoming_call" -> handleIncomingCallPush(message)
            else -> handleChatMessagePush(message)
        }
    }

    private fun handleIncomingCallPush(remoteMessage: RemoteMessage) {
        val callId = remoteMessage.data["callId"] ?: return
        val callerId = remoteMessage.data["callerId"] ?: return

        CallConnectionService.startIncoming(applicationContext, callId, callerId)
    }

    private fun handleChatMessagePush(message: RemoteMessage) {
        val data = message.data




        val chatId = data["chatId"] ?: return
        val senderId = data["senderId"].orEmpty()
        val senderName = data["senderName"] ?: "MAYAS"
        val text = data["text"] ?: "Новое сообщение"

        createChatChannel()
        postChatNotification(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestampMs = System.currentTimeMillis()
        )
    }


    private fun postChatNotification(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        timestampMs: Long,
    ) {



        if (!MayasNotifications.canPostNotifications(this)) return

        val notificationId = chatId.hashCode()
        val notificationManager = NotificationManagerCompat.from(this)

        val sender = Person.Builder().setName(senderName).setKey(senderId.ifBlank { senderName }).build()
        val me = Person.Builder()
            .setName(FirebaseAuth.getInstance().currentUser?.displayName ?: "Я")
            .build()

        val history = ChatNotificationStore.appendMessage(
            context = this,
            chatId = chatId,
            text = text,
            timestampMs = timestampMs,
            mine = false
        )

        val messagingStyle = NotificationCompat.MessagingStyle(me)
        messagingStyle.conversationTitle = senderName
        messagingStyle.isGroupConversation = false

        history.forEach { stored ->
            if (stored.mine) {
                messagingStyle.addMessage(stored.text, stored.timestampMs, null as Person?)
            } else {
                messagingStyle.addMessage(stored.text, stored.timestampMs, sender)
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MayasNotifications.EXTRA_CHAT_ID, chatId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MayasNotifications.CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(MayasNotifications.GROUP_KEY_MESSAGES)
            .addAction(buildReplyAction(chatId, notificationId, senderName))
            .addAction(buildMarkReadAction(chatId, notificationId))
            .build()

        notificationManager.notify(notificationId, notification)
        postSummaryNotification(notificationManager)
    }


    private fun buildReplyAction(chatId: String, notificationId: Int, senderName: String): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(MayasNotifications.KEY_REPLY_TEXT)
            .setLabel("Сообщение")
            .build()

        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            action = MayasNotifications.ACTION_REPLY
            putExtra(MayasNotifications.EXTRA_CHAT_ID, chatId)
            putExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MayasNotifications.EXTRA_SENDER_NAME, senderName)
        }


        val replyPendingIntent = PendingIntent.getBroadcast(
            this, notificationId, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Ответить", replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }


    private fun buildMarkReadAction(chatId: String, notificationId: Int): NotificationCompat.Action {
        val markReadIntent = Intent(this, NotificationMarkReadReceiver::class.java).apply {
            action = MayasNotifications.ACTION_MARK_READ
            putExtra(MayasNotifications.EXTRA_CHAT_ID, chatId)
            putExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(


            this, notificationId + 1, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel, "Прочитано", markReadPendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }


    private fun postSummaryNotification(notificationManager: NotificationManagerCompat) {
        if (!MayasNotifications.canPostNotifications(this)) return

        val summary = NotificationCompat.Builder(this, MayasNotifications.CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("Новые сообщения")
            )
            .setGroup(MayasNotifications.GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(MayasNotifications.SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun createChatChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MayasNotifications.CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых сообщениях в Маяс"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}