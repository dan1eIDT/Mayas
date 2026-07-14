@file:OptIn(ExperimentalMaterial3Api::class)
package com.dan1eidtj.mayas

import androidx.compose.animation.core.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


data class PremiumFeature(
    val icon: ImageVector,
    val title: String,
    val desc: String
)

@Composable
fun PremiumHeader() {

    val infinite =
        rememberInfiniteTransition()

    val scale by infinite.animateFloat(

        initialValue = 1f,
        targetValue = 1.08f,

        animationSpec =
            infiniteRepeatable(

                animation = tween(
                    durationMillis = 1400
                ),

                repeatMode =
                    RepeatMode.Reverse
            )
    )

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(

            Icons.Default.AutoAwesome,

            contentDescription = null,

            tint = MayasTheme.GlowGold,

            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {

                    scaleX = scale
                    scaleY = scale
                }
        )

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "MAYAS+",
            fontSize = 38.sp,
            fontWeight =
                FontWeight.ExtraBold,
            color = MayasTheme.GlowGold
        )
        Spacer(
            Modifier.height(6.dp)
        )
        Text(
            "Получите максимум возможностей Маяса",
            textAlign = TextAlign.Center,

            color =
                MayasTheme.TextSecondary
        )
    }
}


@Composable
fun PremiumScreen(
    vm: MonetizationVM,
    onBack: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isPremium by remember {
        mutableStateOf(false)
    }
    var promoCode by remember {
        mutableStateOf("")
    }
    var promoStatus by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .addSnapshotListener { snap, _ ->

                    isPremium =
                        snap?.getBoolean("isPremium")
                            ?: false
                }
        }
    }

    val features = remember {

        listOf(

            PremiumFeature(
                Icons.Default.Palette,
                "Градиентный ник",
                "Ваш ник переливается градиентом"
            ),

            PremiumFeature(
                Icons.Default.Verified,
                "Золотая иконка",
                "Особая отметка подписчика"
            ),

            PremiumFeature(
                Icons.Default.AutoAwesome,
                "Эксклюзивные рамки",
                "Красивые рамки вокруг профиля"
            ),

        )
    }

    Scaffold(

        containerColor = Color.Transparent,

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "MAYAS+",
                        color = Color.White
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },

        bottomBar = {

            Surface(
                color = Color.Transparent
            ) {

                Button(

                    onClick = {
                        if (!isPremium) {
                            Toast.makeText(context, "Ты угараешь?", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Это пока я добрый , но скоро ", Toast.LENGTH_SHORT).show()
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(58.dp),

                    shape = RoundedCornerShape(18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = MayasTheme.GlowGold
                    )

                ) {

                    Text(

                        if (isPremium)
                            "УПРАВЛЕНИЕ MAYAS+"
                        else
                            "ПОДКЛЮЧИТЬ • 59₽ / МЕС",

                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

    ) { padding ->

        Box(

            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D0D0D),
                            Color(0xFF171320),
                            Color(0xFF2B200F)
                        )
                    )
                )

        ) {

            LazyColumn(

                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),

                horizontalAlignment = Alignment.CenterHorizontally,

                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp
                )

            ) {

                item {

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    PremiumHeader()

                    Spacer(
                        Modifier.height(32.dp)
                    )
                }

                items(features) { feature ->

                    PremiumFeatureCard(
                        feature = feature
                    )
                }

                item {

                    Spacer(
                        Modifier.height(24.dp)
                    )

                    PromoCodeSection(
                        promoCode = promoCode,
                        promoStatus = promoStatus,

                        onPromoChange = {
                            promoCode = it
                        },

                        onRedeem = {

                            vm.redeemPromoCode(
                                promoCode
                            ) {
                                promoStatus = it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureCard(
    feature: PremiumFeature
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MayasTheme.Surface
        )

    ) {

        Row(

            modifier =
                Modifier.padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(

                feature.icon,

                contentDescription = null,

                tint =
                    MayasTheme.GlowGold,

                modifier =
                    Modifier.size(30.dp)
            )

            Spacer(
                Modifier.width(16.dp)
            )

            Column {

                Text(

                    feature.title,

                    color = Color.White,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    feature.desc,

                    fontSize = 13.sp,

                    color =
                        MayasTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
fun PromoCodeSection(
    promoCode: String,
    promoStatus: String,
    onPromoChange: (String) -> Unit,
    onRedeem: () -> Unit
) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MayasTheme.Surface
        )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Text(
                "Промокод",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(12.dp)
            )

            OutlinedTextField(

                value = promoCode,

                onValueChange = {
                    onPromoChange(
                        it.uppercase()
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text("Введите код")
                },

                trailingIcon = {

                    IconButton(
                        onClick = onRedeem
                    ) {

                        Icon(
                            Icons.Default.ConfirmationNumber,
                            null,
                            tint =
                                MayasTheme.GlowGold
                        )
                    }
                }
            )

            if (promoStatus.isNotEmpty()) {

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(

                    promoStatus,

                    color =
                        if (
                            promoStatus.contains(
                                "успешно",
                                true
                            )
                        )
                            Color.Green
                        else
                            Color.Red
                )
            }
        }
    }
}


