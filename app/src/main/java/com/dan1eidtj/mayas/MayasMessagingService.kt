package com.dan1eidtj.mayas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MayasMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // тут позже сохраним токен
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val sender =
            message.data["senderName"] ?: "MAYAS"

        val text =
            message.data["text"] ?: ""

        createChannel()

        val notification = NotificationCompat.Builder(
            this,
            "mayas_messages"
        )
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(sender)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "mayas_messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }
}