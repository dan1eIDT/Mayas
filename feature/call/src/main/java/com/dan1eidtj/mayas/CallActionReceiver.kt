
package com.dan1eidtj.mayas

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {

    @SuppressLint("ServiceCast")
    override fun onReceive(context: Context, intent: Intent) {
        val callManager = (context.applicationContext as CallManagerProvider).callManager

        when (intent.action) {
            ACTION_DECLINE -> {
                callManager.rejectCall()
                cancelCallNotification(context)
            }
            ACTION_TOGGLE_MUTE -> {
                callManager.toggleMute()
                updateServiceNotification(context)
            }
            ACTION_TOGGLE_SPEAKER -> {
                callManager.toggleSpeaker()
                updateServiceNotification(context)
            }
        }
    }

    private fun cancelCallNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CallNotifications.CALL_NOTIFICATION_ID)
    }

    private fun updateServiceNotification(context: Context) {
        val serviceIntent = Intent(context, CallConnectionService::class.java).apply {
            action = CallConnectionService.ACTION_UPDATE_NOTIFICATION
        }
        context.startService(serviceIntent)
    }

    companion object {
        const val ACTION_DECLINE = "com.dan1eidtj.mayas.action.DECLINE_CALL"
        const val ACTION_TOGGLE_MUTE = "com.dan1eidtj.mayas.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.dan1eidtj.mayas.action.TOGGLE_SPEAKER"
    }
}