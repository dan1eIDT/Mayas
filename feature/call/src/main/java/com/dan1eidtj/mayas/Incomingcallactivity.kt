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