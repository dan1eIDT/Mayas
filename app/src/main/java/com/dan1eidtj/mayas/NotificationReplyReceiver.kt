/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.data.NotificationPrefs
import com.dan1eidtj.mayas.feature.NotificationReplySender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class NotificationReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MayasNotifications.ACTION_REPLY) return

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(MayasNotifications.KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()
        val chatId = intent.getStringExtra(MayasNotifications.EXTRA_CHAT_ID)
        val notificationId = intent.getIntExtra(MayasNotifications.EXTRA_NOTIFICATION_ID, -1)
        val notificationSenderName = intent.getStringExtra(MayasNotifications.EXTRA_SENDER_NAME) ?: "MAYAS"

        if (replyText.isNullOrBlank() || chatId.isNullOrBlank() || notificationId == -1) return
        if (replyText.length > MAX_REPLY_LENGTH) {
            showToast(context, R.string.notification_reply_too_long)
            return
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showToast(context, R.string.notification_reply_requires_login)
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val result = NotificationReplySender.send(db, chatId, myUid, replyText)
                if (!result.accepted) {
                    val messageId = when (result.failure) {
                        NotificationReplySender.Failure.BLOCKED -> R.string.notification_reply_blocked
                        NotificationReplySender.Failure.NOT_DIRECT_CHAT -> R.string.notification_reply_open_chat
                        NotificationReplySender.Failure.CHAT_NOT_FOUND,
                        NotificationReplySender.Failure.NOT_PARTICIPANT -> R.string.notification_reply_no_access
                        NotificationReplySender.Failure.PROFILE_UNAVAILABLE -> R.string.notification_reply_requires_login
                        NotificationReplySender.Failure.NONE -> R.string.notification_reply_failed
                    }
                    showToast(appContext, messageId)
                    return@launch
                }

                runCatching {
                    withContext(Dispatchers.Main) {
                        if (MayasNotifications.canPostNotifications(appContext)) {
                            appendReplyToNotification(
                                appContext,
                                chatId,
                                notificationId,
                                notificationSenderName,
                                replyText
                            )
                        }
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Message committed but notification UI update failed", error)
                }

                runCatching {
                    result.messageId?.let { BackendApi.recordMessageSent(chatId, it) }
                }.onFailure { error ->
                    Log.w(TAG, "Message committed but trusted statistics update failed", error)
                }

                // The committed message remains valid even if push delivery fails or times out.
                runCatching {
                    withTimeoutOrNull(PUSH_TIMEOUT_MS) {
                        val peerUid = result.peerUid ?: return@withTimeoutOrNull
                        val peer = db.collection("users").document(peerUid).get().await()
                        val token = peer.getString("fcmToken")?.takeIf { it.isNotBlank() }
                            ?: return@withTimeoutOrNull
                        BackendApi.notify(
                            chatId = chatId,
                            senderId = myUid,
                            receiverUid = peerUid,
                            token = token,
                            senderName = result.senderName,
                            text = replyText,
                            silent = false
                        )
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Message committed but push delivery failed", error)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Notification reply failed", error)
                showToast(appContext, R.string.notification_reply_failed)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showToast(context: Context, stringRes: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context.applicationContext, stringRes, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
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
            .setName(FirebaseAuth.getInstance().currentUser?.displayName ?: context.getString(com.dan1eidtj.chats.R.string.me))
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

        val updated = NotificationCompat.Builder(
            context,
            MayasNotifications.channelIdFor(
                soundOn = NotificationPrefs.soundEnabled(context),
                vibrateOn = NotificationPrefs.vibrationEnabled(context)
            )
        )
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setStyle(style)
            .setAutoCancel(true)
            .setGroup(MayasNotifications.GROUP_KEY_MESSAGES)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, updated)
    }

    private companion object {
        const val TAG = "NotificationReply"
        const val MAX_REPLY_LENGTH = 4096
        const val PUSH_TIMEOUT_MS = 2500L
    }
}
