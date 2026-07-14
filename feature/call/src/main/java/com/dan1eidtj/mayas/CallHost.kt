package com.dan1eidtj.mayas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CallHost(
    callManager: CallManager,
    currentUserId: String,
    content: @Composable () -> Unit
) {
    val viewModel: CallViewModel = viewModel(factory = CallViewModelFactory(callManager, currentUserId))
    val state by viewModel.uiState.collectAsState()

    content()

    // Раньше экран звонка появлялся/пропадал жёстким if — переключение состояния сразу
    // выкидывало из композиции CallScreen целиком, и когда звонок завершался (например,
    // при "Отклонён"), сообщение о причине не успевало быть увиденным: экран пропадал
    // мгновенно вместе со сменой CallScreenState.
    //
    // Держим последний непустой Active-стейт в remember: пока идёт fade-out, рисуем
    // именно ЕГО (там уже isEnded=true и statusText вроде "Отклонён"), а не пустоту —
    // иначе анимация играется по факту над пустым composable и визуально ничего не видно.
    val activeState = state as? CallScreenState.Active
    var lastActiveState by remember { mutableStateOf<CallScreenState.Active?>(null) }
    if (activeState != null) lastActiveState = activeState

    AnimatedVisibility(
        visible = activeState != null,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(300))
    ) {
        lastActiveState?.let { screenState ->
            CallScreen(
                state = screenState,
                onAccept = viewModel::onAcceptClicked,
                onDeclineOrEnd = viewModel::onDeclineOrEndClicked,
                onToggleMute = viewModel::onMuteToggleClicked,
                onToggleSpeaker = viewModel::onSpeakerToggleClicked
            )
        }
    }
}