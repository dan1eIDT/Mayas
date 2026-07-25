package com.dan1eidtj.mayas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat


class NotificationMarkReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MayasNotifications.ACTION_MARK_READ) return

        val chatId = intent.getStringExtra(MayasNotifications.EXTRA_CHAT_ID)
        val notificationId = intent.getIntExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
        chatId?.let { ChatNotificationStore.clear(context, it) }





    }
}
