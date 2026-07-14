package com.dan1eidtj.mayas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


object CallUiVisibility {
    private val _isAppForeground = MutableStateFlow(false)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground

    fun setAppForeground(foreground: Boolean) {
        _isAppForeground.value = foreground
    }
}
