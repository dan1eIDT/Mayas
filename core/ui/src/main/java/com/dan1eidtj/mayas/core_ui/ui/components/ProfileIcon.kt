package com.dan1eidtj.mayas.core_ui.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProfileIcon(
    icon: String,
    size: Dp = 58.dp
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
        "crown" -> Icons.Default.MilitaryTech
        "diamond" -> Icons.Default.Diamond
        "rocket" -> Icons.Default.RocketLaunch
        "coffee" -> Icons.Default.Coffee
        "pizza" -> Icons.Default.LocalPizza
        "pet" -> Icons.Default.Pets
        "shield" -> Icons.Default.Shield
        "anchor" -> Icons.Default.Anchor
        "beach" -> Icons.Default.BeachAccess
        "brush" -> Icons.Default.Brush
        "party" -> Icons.Default.Celebration
        "cloud" -> Icons.Default.Cloud
        "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "cup" -> Icons.Default.EmojiEvents
        "compass" -> Icons.Default.Explore
        "puzzle" -> Icons.Default.Extension
        "fingerprint" -> Icons.Default.Fingerprint
        "gym" -> Icons.Default.FitnessCenter
        "key" -> Icons.Default.Key
        "web" -> Icons.Default.Language
        "idea" -> Icons.Default.Lightbulb
        "lock" -> Icons.Default.Lock
        "palette" -> Icons.Default.Palette
        "brain" -> Icons.Default.Psychology
        "world" -> Icons.Default.Public
        "school" -> Icons.Default.School
        "lab" -> Icons.Default.Science
        "yoga" -> Icons.Default.SelfImprovement
        "cart" -> Icons.Default.ShoppingCart
        "sun" -> Icons.Default.WbSunny
        "support" -> Icons.Default.SupportAgent
        "theater" -> Icons.Default.TheaterComedy
        "token" -> Icons.Default.Token
        "vpn" -> Icons.Default.VpnKey
        "water" -> Icons.Default.WaterDrop
        "premium" -> Icons.Default.WorkspacePremium
        "verified" -> Icons.Default.CheckCircle
        "diamond_gold" -> Icons.Default.AutoAwesome
        "king" -> Icons.Default.EmojiEvents
        "crystal" -> Icons.Default.Stars
        
        "heart_fire" -> Icons.Default.LocalFireDepartment
        "auto_awesome" -> Icons.Default.AutoAwesome
        "pets" -> Icons.Default.Pets
        "psychology" -> Icons.Default.Psychology

        else -> Icons.Default.Person
    }

    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(size)
    )
}