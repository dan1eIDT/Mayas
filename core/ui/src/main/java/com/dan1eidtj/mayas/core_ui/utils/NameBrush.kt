package com.dan1eidtj.mayas.core_ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dan1eidtj.mayas.core.ui.theme.nickBrush


@Composable
fun nameBrushOf(key: String?): Brush =
    nickBrush(key)
        ?: getNameColorBrush(key ?: "gold")
        ?: Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFD700)))

@Composable
fun resolveNameBrush(key: String?, isPremium: Boolean): Brush? =
    if (isPremium) nameBrushOf(key) else null