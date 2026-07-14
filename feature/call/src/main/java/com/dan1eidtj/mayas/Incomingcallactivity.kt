package com.dan1eidtj.mayas

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth


class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenFlags()

        val callManager = (application as CallManagerProvider).callManager
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        // Кнопка "Принять" в уведомлении ведёт СЮДА напрямую через
        // PendingIntent.getActivity(...) с extra=true (см. CallConnectionService),
        // а не через BroadcastReceiver — на Android 12+ система блокирует запуск
        // Activity из BroadcastReceiver как notification trampoline, и "Принять" просто
        // не открыло бы экран. Дёргаем acceptCall(callId) ровно один раз здесь, в
        // onCreate — НЕ внутри setContent/композиции, иначе он вызывался бы повторно
        // на каждой рекомпозиции.
        //
        // callId берём ЯВНО из EXTRA_CALL_ID интента, а не из callManager.activeCall —
        // именно это раньше вызывало белый экран навсегда: CallConnectionService строит
        // уведомление прямо из push-экстрас, не дожидаясь, пока CallManager.attachToCall()
        // сам заполнит activeCall своим Firestore-снапшотом. Если "Принять" жали раньше,
        // чем этот снапшот успевал прийти, acceptCall() без явного callId просто не
        // находил, какой звонок принимать, и тихо ничего не делал.
        //
        // savedInstanceState == null гарантирует, что это действительно первое создание
        // Activity, а не пересоздание при повороте экрана — иначе тот же intent с тем
        // же extra=true пересоздал бы соединение (повторный webRtcClient.init() поверх
        // уже идущего звонка) при каждом повороте телефона во время разговора.
        if (savedInstanceState == null && intent?.getBooleanExtra(EXTRA_AUTO_ACCEPT, false) == true) {
            val callId = intent?.getStringExtra(CallConnectionService.EXTRA_CALL_ID)
            if (callId != null) {
                callManager.acceptCall(callId)
            }
        }

        setContent {
            val viewModel: CallViewModel = viewModel(
                factory = CallViewModelFactory(callManager, currentUserId)
            )
            val state by viewModel.uiState.collectAsState()

            // БАГ, который вырубал экран сразу после открытия: uiState — это StateFlow
            // из stateIn(initialValue = NoCall). Для этой Activity создаётся НОВЫЙ
            // CallViewModel, и в первый же кадр композиции state синхронно ещё равен
            // NoCall — реальное "звонок активен" прилетает на кадр позже, асинхронно,
            // когда комбинирующий Flow внутри CallViewModel успевает подписаться и
            // получить значения из CallManager. Старый код делал finish() прямо по
            // этому первому NoCall — то есть закрывал Activity ДО того, как она вообще
            // успевала увидеть, что звонок есть. Снаружи это выглядело как "открылось
            // и тут же само выключилось".
            //
            // Фикс: не закрываемся на самый первый NoCall. Закрываемся только если
            // реально уже видели Active хотя бы раз, а потом он пропал — то есть звонок
            // действительно закончился (принят и основная Activity уже поверх, либо
            // отклонён/завершён с другой стороны), а не просто "ещё не подгрузилось".
            var hasSeenActiveCall by remember { mutableStateOf(false) }

            LaunchedEffect(state) {
                when {
                    state is CallScreenState.Active -> hasSeenActiveCall = true
                    state is CallScreenState.NoCall && hasSeenActiveCall -> finish()
                }
            }

            if (state is CallScreenState.Active) {
                CallScreen(
                    state = state,
                    onAccept = viewModel::onAcceptClicked,
                    onDeclineOrEnd = viewModel::onDeclineOrEndClicked,
                    onToggleMute = viewModel::onMuteToggleClicked,
                    onToggleSpeaker = viewModel::onSpeakerToggleClicked
                )
            }
            // Пока state == NoCall и hasSeenActiveCall == false — просто ничего не
            // рисуем и ничего не закрываем, ждём первую реальную эмиссию из
            // CallManager. Это доли секунды, отдельный лоадер тут избыточен.
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_AUTO_ACCEPT = "extra_auto_accept"
    }
}