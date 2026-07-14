
package com.dan1eidtj.mayas

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class CallConnectionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var callStateJob: Job? = null

    private val callManager: CallManager
        get() = (applicationContext as CallManagerProvider).callManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        callManager.startListeningForIncomingCalls()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Если прилетел интент на обновление кнопок в шторке (Mute / Спикер)
        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            refreshCurrentNotification()
            return START_NOT_STICKY
        }

        val peerId = intent?.getStringExtra(EXTRA_PEER_ID)
        val isIncoming = intent?.getBooleanExtra(EXTRA_IS_INCOMING, true) ?: true
        val callId = intent?.getStringExtra(EXTRA_CALL_ID)

        if (peerId.isNullOrEmpty() || callId.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Жёсткий и синхронный запуск старта Foreground, чтобы система не прибила процесс.
        //
        // ВАЖНО: тип microphone на Android 14+ можно запрашивать только когда приложение
        // находится в "eligible"-состоянии (видимо пользователю). Входящий звонок стартует
        // из пуша, когда приложение может быть полностью в фоне — поэтому для него сначала
        // просим только phoneCall (для него есть исключение у полноэкранных call-уведомлений),
        // а microphone "доапгрейживаем" отдельным вызовом startForeground() уже после того,
        // как пользователь принял звонок (см. observeCallState -> CONNECTING/CONNECTED),
        // и на экране уже открыта IncomingCallActivity. Для исходящих звонков это не проблема:
        // пользователь стартует их, уже находясь в приложении, поэтому microphone просим сразу.
        try {
            val placeholderNotification = buildCallNotificationSync(
                isIncoming = isIncoming,
                peerName = peerId,
                peerIcon = loadPersonIcon(null),
                state = if (isIncoming) CallState.INCOMING else CallState.OUTGOING,
                callId = callId
            )
            startForegroundCompat(placeholderNotification, includeMicrophone = !isIncoming)
        } catch (e: Exception) {
            startForegroundCompat(buildFallbackNotification(), includeMicrophone = !isIncoming)
        }

        // Асинхронно подтягиваем имя из Firestore, не блокируя запуск сервиса[cite: 3]
        serviceScope.launch {
            val (peerName, peerPhotoUrl) = fetchPeerInfo(peerId)
            val icon = loadPersonIcon(peerPhotoUrl)
            val updated = buildCallNotificationSync(
                isIncoming, peerName, icon,
                if (isIncoming) CallState.INCOMING else CallState.OUTGOING,
                callId = callId
            )
            getSystemService(NotificationManager::class.java)
                ?.notify(CallNotifications.CALL_NOTIFICATION_ID, updated)
        }

        if (callStateJob == null) observeCallState(isIncoming)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        callStateJob?.cancel()
        callStateJob = null
        microphoneTypeUpgraded = false
        super.onDestroy()
    }


    // Стал ли сервис уже "eligible" и добавили ли мы microphone-тип к foreground-сервису.
    // Нужен, чтобы не дёргать startForeground() повторно на каждое обновление стейта.
    private var microphoneTypeUpgraded = false

    @RequiresApi(Build.VERSION_CODES.R)
    private fun observeCallState(fallbackIsIncoming: Boolean) {
        callStateJob = serviceScope.launch {
            combine(
                callManager.callState,
                CallUiVisibility.isAppForeground
            ) { state, isAppForeground -> state to isAppForeground }
                .distinctUntilChanged()
                .collect { (state, _) ->
                    when (state) {
                        CallState.INCOMING, CallState.OUTGOING, CallState.RINGING,
                        CallState.CONNECTING, CallState.CONNECTED -> {
                            val session = callManager.activeCall.value ?: return@collect
                            val isIncoming = session.receiverId == currentUserIdOrEmpty()
                            val peerId = if (isIncoming) session.callerId else session.receiverId
                            val (peerName, peerPhotoUrl) = fetchPeerInfo(peerId)
                            val notification = buildCallNotificationSync(
                                isIncoming, peerName, loadPersonIcon(peerPhotoUrl), state,
                                callId = session.callId
                            )

                            // Звонок принят (или уже идёт) — пользователь сейчас видит экран звонка,
                            // значит приложение eligible для microphone-типа. Апгрейдим один раз.
                            val needsMicUpgrade = isIncoming &&
                                    !microphoneTypeUpgraded &&
                                    (state == CallState.CONNECTING || state == CallState.CONNECTED)

                            if (needsMicUpgrade) {
                                microphoneTypeUpgraded = true
                                startForegroundCompat(notification, includeMicrophone = true)
                            } else {
                                getSystemService(NotificationManager::class.java)
                                    ?.notify(CallNotifications.CALL_NOTIFICATION_ID, notification)
                            }
                        }
                        CallState.REJECTED, CallState.ENDED, CallState.IDLE -> {
                            stopForegroundCompat()
                            stopSelf()
                        }
                    }
                }
        }
    }


    private fun refreshCurrentNotification() {
        val session = callManager.activeCall.value ?: return
        val state = callManager.callState.value
        val isIncoming = session.receiverId == currentUserIdOrEmpty()
        val peerId = if (isIncoming) session.callerId else session.receiverId

        serviceScope.launch {
            val (peerName, peerPhotoUrl) = fetchPeerInfo(peerId)
            val notification = buildCallNotificationSync(
                isIncoming, peerName, loadPersonIcon(peerPhotoUrl), state,
                callId = session.callId
            )
            getSystemService(NotificationManager::class.java)
                ?.notify(CallNotifications.CALL_NOTIFICATION_ID, notification)
        }
    }

    private fun currentUserIdOrEmpty(): String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()


    private fun buildCallNotificationSync(
        isIncoming: Boolean,
        peerName: String,
        peerIcon: IconCompat,
        state: CallState,
        callId: String
    ): Notification {
        val person = Person.Builder()
            .setName(peerName)
            .setIcon(peerIcon)
            .setImportant(true)
            .build()

        // Интент, который развернет экран звонка при тапе на само уведомление[cite: 3]
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Бродкасты для кликов по кнопкам. ВАЖНО: "Принять" — НЕ бродкаст, а прямой
        // PendingIntent.getActivity() (см. ниже) — иначе на Android 12+ запуск Activity
        // из BroadcastReceiver блокируется системой как notification trampoline.
        val declinePendingIntent = actionPendingIntent(CallActionReceiver.ACTION_DECLINE)
        val mutePendingIntent = actionPendingIntent(CallActionReceiver.ACTION_TOGGLE_MUTE)
        val speakerPendingIntent = actionPendingIntent(CallActionReceiver.ACTION_TOGGLE_SPEAKER)

        val acceptIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(IncomingCallActivity.EXTRA_AUTO_ACCEPT, true)
            // БЕЛЫЙ ЭКРАН БЫЛ ИМЕННО ИЗ-ЗА ОТСУТСТВИЯ ЭТОЙ СТРОКИ: без явного callId
            // acceptCall() внутри CallManager брал callId из _activeCall.value, который
            // на момент нажатия "Принять" в уведомлении иногда ещё не успевал
            // заполниться собственным Firestore-слушателем CallManager (уведомление же
            // строится из push-экстрас сразу, не дожидаясь этого стейта) — acceptCall()
            // тихо ничего не делал, а Activity вечно ждала звонок, которого для неё
            // как будто не существовало.
            putExtra(EXTRA_CALL_ID, callId)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            // requestCode=1, отличный от contentPendingIntent (0) — иначе PendingIntent
            // считались бы "одинаковыми" (Intent.filterEquals игнорирует extras) и
            // затирали бы друг друга.
            this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Тексты кнопок меняются на лету в зависимости от стейта в CallManager
        val muteText = if (callManager.isMuted.value) "Вкл. микр." else "Выкл. микр."
        val speakerText = if (callManager.isSpeakerOn.value) "Динамик" else "Наушник"

        val builder = NotificationCompat.Builder(this, CallNotifications.CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(longArrayOf(0, 800, 800))
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(false)

        val isRingingIncoming = isIncoming && state == CallState.INCOMING
        // Разговор уже идёт (не просто "звонит"), и юзер прямо сейчас смотрит на
        // CallScreen внутри приложения — управление уже есть на самом экране,
        // дублировать его кнопками в уведомлении не нужно. Полноценное Ongoing-
        // уведомление с кнопками mute/speaker должно появляться ИМЕННО когда юзер
        // сворачивает звонок (переключился на другое приложение/домой) — тогда
        // кнопки в шторке остаются единственным способом управлять звонком.
        val isCallScreenLikelyVisible = CallUiVisibility.isAppForeground.value &&
                (state == CallState.OUTGOING || state == CallState.CONNECTING || state == CallState.CONNECTED)

        when {
            // Входящий вызов, который ещё не приняли — всегда полноценная системная
            // шторка "Принять/Отклонить", вне зависимости от foreground: это
            // единственный способ ответить на звонок, пока CallScreen ещё не открыт.
            isRingingIncoming -> {
                builder
                    .setStyle(
                        NotificationCompat.CallStyle.forIncomingCall(
                            person,
                            declinePendingIntent,
                            acceptPendingIntent
                        )
                    )
            }
            // Разговор идёт, и его и так видно на экране — тихая заглушка без кнопок,
            // нужна только чтобы удержать foreground-сервис (Android не даёт сервису
            // с типом MICROPHONE/PHONE_CALL жить вообще без видимого уведомления).
            isCallScreenLikelyVisible -> {
                builder
                    .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, declinePendingIntent))
                    .setSilent(true)
                    .setVibrate(null)
            }
            // Звонок свёрнут (приложение в фоне) либо уже отвечен, но, например,
            // экран ещё не успел прогрузиться — полный набор controls.
            else -> {
                builder
                    .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, declinePendingIntent))
                    .addAction(android.R.drawable.ic_lock_silent_mode, muteText, mutePendingIntent)
                    .addAction(android.R.drawable.stat_sys_speakerphone, speakerText, speakerPendingIntent)
            }
        }

        return builder.build()
    }

    private fun buildFallbackNotification(): Notification =
        NotificationCompat.Builder(this, CallNotifications.CHANNEL_ID_CALLS)
            .setContentTitle("Входящий звонок")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .build()

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, CallActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun fetchPeerInfo(peerId: String): Pair<String, String?> {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(peerId)
                .get()
                .await()
            val name = doc.getString("name") ?: peerId
            val photoUrl = doc.getString("photoUrl")
            name to photoUrl
        } catch (e: Exception) {
            peerId to null
        }
    }

    private fun loadPersonIcon(photoUrl: String?): IconCompat {
        return IconCompat.createWithResource(this, android.R.drawable.ic_menu_call)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startForegroundCompat(notification: Notification, includeMicrophone: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // На Android 14+ microphone-тип можно просить только когда приложение eligible
            // (видимо пользователю). Для входящего звонка на старте сервиса это не так, поэтому
            // includeMicrophone=false и тип добавляется позже через отдельный startForeground().
            val type = if (includeMicrophone) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            }
            startForeground(CallNotifications.CALL_NOTIFICATION_ID, notification, type)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // На API 29-33 ограничения "eligible state" для microphone-типа нет,
            // поэтому просим его сразу же вне зависимости от includeMicrophone.
            startForeground(
                CallNotifications.CALL_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(CallNotifications.CALL_NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CallNotifications.CHANNEL_ID_CALLS,
                "Звонки",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 800)
                setSound(null, null) // Отключаем звук канала, так как играем его сами через Ringtone
            }
        )
    }

    companion object {
        const val EXTRA_PEER_ID = "extra_peer_id"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_IS_INCOMING = "extra_is_incoming"

        // Экшен, чтобы ресивер мог сказать сервису: "Эй, обнови иконки микрофона/динамика!"
        const val ACTION_UPDATE_NOTIFICATION = "com.dan1eidtj.mayas.action.UPDATE_NOTIF"

        fun startIncoming(context: Context, callId: String, callerId: String) {
            start(context, callId = callId, peerId = callerId, isIncoming = true)
        }

        fun startOutgoing(context: Context, callId: String, receiverId: String) {
            start(context, callId = callId, peerId = receiverId, isIncoming = false)
        }

        private fun start(context: Context, callId: String, peerId: String, isIncoming: Boolean) {
            val intent = Intent(context, CallConnectionService::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_ID, peerId)
                putExtra(EXTRA_IS_INCOMING, isIncoming)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallConnectionService::class.java))
        }
    }
}
