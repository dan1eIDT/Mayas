package com.dan1eidtj.mayas

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


object MayasNotifications {
    const val CHANNEL_MESSAGES = "mayas_messages"


    const val GROUP_KEY_MESSAGES = "com.dan1eidtj.mayas.MESSAGES_GROUP"


    const val SUMMARY_NOTIFICATION_ID = 0

    const val KEY_REPLY_TEXT = "key_reply_text"

    const val ACTION_REPLY = "com.dan1eidtj.mayas.ACTION_REPLY"
    const val ACTION_MARK_READ = "com.dan1eidtj.mayas.ACTION_MARK_READ"

    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"


    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun findActiveNotification(context: Context, notificationId: Int): Notification? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.activeNotifications
            ?.firstOrNull { it.id == notificationId }
            ?.notification
    }
}