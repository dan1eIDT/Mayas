package com.dan1eidtj.mayas.core_ui.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.BrunchDining
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme




val AllProfileIcons: List<String> = listOf(

    "skull", "star", "favorite", "bolt", "face", "ghost", "flash", "moon",
    "heartbreak", "smile", "cool",

    "music", "headset", "mic", "piano", "camera", "movie", "book",

    "game", "code", "terminal", "robot", "eye", "controller", "puzzle",
    "fingerprint", "key", "lock", "vpn", "security",

    "crown", "diamond", "premium", "verified", "diamond_gold", "king",
    "crystal", "auto_awesome", "bookmark", "shield", "cup",

    "pet", "pets", "cloud", "sun", "snow", "night", "umbrella", "storm",
    "forest", "flower", "world", "compass", "web", "map", "mountain", "landscape",

    "coffee", "pizza", "wine", "icecream", "cake", "burger", "cookie", "cart",

    "bike", "gym", "yoga", "run", "car", "boat", "plane", "sail", "hike",
    "basketball", "soccer", "tennis", "volleyball", "football", "beach", "pool",
    "spa", "anchor",

    "brush", "palette", "paint", "theater", "party", "casino", "toy", "child",

    "idea", "brain", "psychology", "lab", "school",

    "fire", "heart_fire", "water",

    "money", "piggy",

    "rocket", "support", "token",

    "happy", "sad", "excited", "neutral", "silly",

    "chip", "network", "wifi", "bluetooth", "storage", "bug", "dev",

    "note", "album", "radio", "playlist", "library",

    "luggage", "hotel", "train", "subway", "taxi", "traffic",

    "eco", "waves", "grass", "park", "sunrise",

    "golf", "hockey", "rugby", "cricket", "martial", "mma",

    "liquor", "ramen", "bakery", "lunch", "dinner", "brunch", "seafood",

    "weekend", "nightlife",
)

@Composable
fun ProfileIcon(
    icon: String,
    size: Dp = 58.dp
) {

    val vector = when (icon) {


        "skull" -> Icons.Default.Warning
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        "bolt" -> Icons.Default.Bolt
        "face" -> Icons.Default.Face
        "ghost" -> Icons.Default.Person
        "flash" -> Icons.Default.FlashOn
        "moon" -> Icons.Default.DarkMode
        "heartbreak" -> Icons.Default.HeartBroken
        "smile" -> Icons.Default.EmojiEmotions
        "cool" -> Icons.Default.Mood


        "music" -> Icons.Default.Headphones
        "headset" -> Icons.Default.Headset
        "mic" -> Icons.Default.Mic
        "piano" -> Icons.Default.Piano
        "camera" -> Icons.Default.PhotoCamera
        "movie" -> Icons.Default.Movie
        "book" -> Icons.Default.MenuBook


        "game" -> Icons.Default.SportsEsports
        "code" -> Icons.Default.Code
        "terminal" -> Icons.Default.Terminal
        "robot" -> Icons.Default.SmartToy
        "eye" -> Icons.Default.Visibility
        "controller" -> Icons.Default.VideogameAsset
        "puzzle" -> Icons.Default.Extension
        "fingerprint" -> Icons.Default.Fingerprint
        "key" -> Icons.Default.Key
        "lock" -> Icons.Default.Lock
        "vpn" -> Icons.Default.VpnKey
        "security" -> Icons.Default.Security


        "crown" -> Icons.Default.MilitaryTech
        "diamond" -> Icons.Default.Diamond
        "premium" -> Icons.Default.WorkspacePremium
        "verified" -> Icons.Default.CheckCircle
        "diamond_gold" -> Icons.Default.WbTwilight
        "king" -> Icons.Default.AccountBalance
        "crystal" -> Icons.Default.Stars
        "auto_awesome" -> Icons.Default.AutoAwesome
        "bookmark" -> Icons.Default.Bookmark
        "shield" -> Icons.Default.Shield
        "cup" -> Icons.Default.EmojiEvents


        "pet" -> Icons.Default.Pets
        "pets" -> Icons.Default.Egg
        "cloud" -> Icons.Default.Cloud
        "sun" -> Icons.Default.WbSunny
        "snow" -> Icons.Default.AcUnit
        "night" -> Icons.Default.NightsStay
        "umbrella" -> Icons.Default.Umbrella
        "storm" -> Icons.Default.Thunderstorm
        "forest" -> Icons.Default.Forest
        "flower" -> Icons.Default.LocalFlorist
        "world" -> Icons.Default.Public
        "compass" -> Icons.Default.Explore
        "web" -> Icons.Default.Language
        "map" -> Icons.Default.Map
        "mountain" -> Icons.Default.Terrain
        "landscape" -> Icons.Default.Landscape


        "coffee" -> Icons.Default.Coffee
        "pizza" -> Icons.Default.LocalPizza
        "wine" -> Icons.Default.WineBar
        "icecream" -> Icons.Default.Icecream
        "cake" -> Icons.Default.Cake
        "burger" -> Icons.Default.Fastfood
        "cookie" -> Icons.Default.Cookie
        "cart" -> Icons.Default.ShoppingCart


        "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "gym" -> Icons.Default.FitnessCenter
        "yoga" -> Icons.Default.SelfImprovement
        "run" -> Icons.Default.DirectionsRun
        "car" -> Icons.Default.DirectionsCar
        "boat" -> Icons.Default.DirectionsBoat
        "plane" -> Icons.Default.Flight
        "sail" -> Icons.Default.Sailing
        "hike" -> Icons.Default.Hiking
        "basketball" -> Icons.Default.SportsBasketball
        "soccer" -> Icons.Default.SportsSoccer
        "tennis" -> Icons.Default.SportsTennis
        "volleyball" -> Icons.Default.SportsVolleyball
        "football" -> Icons.Default.SportsFootball
        "beach" -> Icons.Default.BeachAccess
        "pool" -> Icons.Default.Pool
        "spa" -> Icons.Default.Spa
        "anchor" -> Icons.Default.Anchor


        "brush" -> Icons.Default.Brush
        "palette" -> Icons.Default.Palette
        "paint" -> Icons.Default.ColorLens
        "theater" -> Icons.Default.TheaterComedy
        "party" -> Icons.Default.Celebration
        "casino" -> Icons.Default.Casino
        "toy" -> Icons.Default.Toys
        "child" -> Icons.Default.ChildCare


        "idea" -> Icons.Default.Lightbulb
        "brain" -> Icons.Default.Psychology
        "psychology" -> Icons.Default.Biotech
        "lab" -> Icons.Default.Science
        "school" -> Icons.Default.School


        "fire" -> Icons.Default.LocalFireDepartment
        "heart_fire" -> Icons.Default.Whatshot
        "water" -> Icons.Default.WaterDrop


        "money" -> Icons.Default.MonetizationOn
        "piggy" -> Icons.Default.Savings


        "rocket" -> Icons.Default.RocketLaunch
        "support" -> Icons.Default.SupportAgent
        "token" -> Icons.Default.Token


        "happy" -> Icons.Default.SentimentSatisfied
        "sad" -> Icons.Default.SentimentDissatisfied
        "excited" -> Icons.Default.SentimentVerySatisfied
        "neutral" -> Icons.Default.SentimentNeutral
        "silly" -> Icons.Default.TagFaces


        "chip" -> Icons.Default.Memory
        "network" -> Icons.Default.Router
        "wifi" -> Icons.Default.Wifi
        "bluetooth" -> Icons.Default.Bluetooth
        "storage" -> Icons.Default.Storage
        "bug" -> Icons.Default.BugReport
        "dev" -> Icons.Default.DeveloperMode


        "note" -> Icons.Default.MusicNote
        "album" -> Icons.Default.Album
        "radio" -> Icons.Default.Radio
        "playlist" -> Icons.Default.QueueMusic
        "library" -> Icons.Default.LibraryMusic


        "luggage" -> Icons.Default.Luggage
        "hotel" -> Icons.Default.Hotel
        "train" -> Icons.Default.Train
        "subway" -> Icons.Default.Subway
        "taxi" -> Icons.Default.LocalTaxi
        "traffic" -> Icons.Default.Traffic


        "eco" -> Icons.Default.Eco
        "waves" -> Icons.Default.Waves
        "grass" -> Icons.Default.Grass
        "park" -> Icons.Default.Park
        "sunrise" -> Icons.Default.Brightness7


        "golf" -> Icons.Default.SportsGolf
        "hockey" -> Icons.Default.SportsHockey
        "rugby" -> Icons.Default.SportsRugby
        "cricket" -> Icons.Default.SportsCricket
        "martial" -> Icons.Default.SportsMartialArts
        "mma" -> Icons.Default.SportsMma


        "liquor" -> Icons.Default.Liquor
        "ramen" -> Icons.Default.RamenDining
        "bakery" -> Icons.Default.BakeryDining
        "lunch" -> Icons.Default.LunchDining
        "dinner" -> Icons.Default.DinnerDining
        "brunch" -> Icons.Default.BrunchDining
        "seafood" -> Icons.Default.SetMeal


        "weekend" -> Icons.Default.Weekend
        "nightlife" -> Icons.Default.Nightlife

        else -> Icons.Default.Person
    }

    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = MayasTheme.TextPrimary,
        modifier = Modifier.size(size)
    )
}