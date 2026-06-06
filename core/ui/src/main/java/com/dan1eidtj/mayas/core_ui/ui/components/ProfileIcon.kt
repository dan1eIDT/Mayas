package com.dan1eidtj.mayas.core_ui.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProfileIcon(
    icon: String
) {

    val vector = when(icon) {

        "skull" -> Icons.Default.Warning

        "star" -> Icons.Default.Star

        "favorite" -> Icons.Default.Favorite

        "bolt" -> Icons.Default.Bolt

        "face" -> Icons.Default.Face

        "ghost" -> Icons.Default.Person

        "flash" -> Icons.Default.FlashOn

        "moon" -> Icons.Default.DarkMode

        "music" -> Icons.Default.Headphones

        "game" -> Icons.Default.SportsEsports

        "code" -> Icons.Default.Code

        "terminal" -> Icons.Default.Terminal

        "fire" -> Icons.Default.LocalFireDepartment

        "robot" -> Icons.Default.SmartToy

        "eye" -> Icons.Default.Visibility

        "heartbreak" -> Icons.Default.HeartBroken


        else -> Icons.Default.Person
    }

    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(58.dp)
    )
}