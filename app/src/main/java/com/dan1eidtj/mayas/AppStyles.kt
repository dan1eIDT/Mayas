package com.dan1eidtj.mayas.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MayasColors {

    val Bg = Color(0xFF0F1115)

    val Card = Color(0xFF171A20)

    val Accent = Color(0xFF7C4DFF)

    val Accent2 = Color(0xFF00D1FF)

    val Text = Color(0xFFFFFFFF)

    val SubText = Color(0xFF9AA0AA)
}

fun Modifier.mayasCard(): Modifier {
    return this
        .clip(RoundedCornerShape(26.dp))
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF1B1E24),
                    Color(0xFF13161B)
                )
            )
        )
}