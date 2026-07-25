@file:OptIn(ExperimentalMaterial3Api::class)
package com.dan1eidtj.mayas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
private fun BoxScope.GlowOrb(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp,
    offsetY: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                ),
                shape = CircleShape
            )
    )
}

@Composable
fun PremiumHeader() {

    val infinite = rememberInfiniteTransition()

    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(contentAlignment = Alignment.Center) {

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MayasTheme.GlowGold.copy(alpha = 0.45f),
                                MayasTheme.GlowGold.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MayasTheme.GlowGold,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "MAYAS+",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MayasTheme.GlowGold,
                        Color(0xFFFFE9B0),
                        MayasTheme.GlowGold
                    )
                )
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Получите максимум возможностей Маяса",
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = MayasTheme.TextSecondary,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


@Composable
fun PremiumScreen(
    vm: MonetizationVM,
    onBack: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isPremium by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var promoStatus by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .addSnapshotListener { snap, _ ->
                    isPremium = snap?.getBoolean("isPremium") ?: false
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Surface(color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0D0D0D).copy(alpha = 0.95f)),
                            )
                        )
                        .padding(16.dp)
                ) {
                    GoldCtaButton(
                        text = if (isPremium) "УПРАВЛЕНИЕ MAYAS+" else "ПОДКЛЮЧИТЬ • 59₽ / МЕС",
                        onClick = {
                            if (!isPremium) {
                                Toast.makeText(context, "Ты угараешь?", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Это пока я добрый , но скоро ", Toast.LENGTH_SHORT).show()
                            }
                        }
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


            GlowOrb(
                color = MayasTheme.GlowGold,
                size = 260.dp,
                alignment = Alignment.TopEnd,
                offsetX = 60.dp,
                offsetY = (-60).dp
            )
            GlowOrb(
                color = MayasTheme.Accent,
                size = 220.dp,
                alignment = Alignment.CenterStart,
                offsetX = (-90).dp
            )

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 120.dp
                )
            ) {

                item {
                    Spacer(Modifier.height(12.dp))
                    PremiumHeader()
                    Spacer(Modifier.height(28.dp))
                    PriceHighlightCard(isPremium = isPremium)
                    Spacer(Modifier.height(28.dp))
                    SectionLabel("ЧТО ВЫ ПОЛУЧАЕТЕ")
                    Spacer(Modifier.height(10.dp))
                }

                itemsIndexed(features) { index, feature ->
                    PremiumFeatureCard(feature = feature, index = index)
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    PromoCodeSection(
                        promoCode = promoCode,
                        promoStatus = promoStatus,
                        onPromoChange = { promoCode = it },
                        onRedeem = {
                            vm.redeemPromoCode(promoCode) { promoStatus = it }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = MayasTheme.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MayasTheme.TextSecondary.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun PriceHighlightCard(isPremium: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MayasTheme.GlowGold.copy(alpha = 0.18f),
                        MayasTheme.GlowGold.copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(MayasTheme.GlowGold.copy(alpha = 0.6f), MayasTheme.GlowGold.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MayasTheme.GlowGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isPremium) "Подписка активна" else "Всего ≈ 2₽ в день",
                        color = MayasTheme.GlowGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isPremium)
                        "Спасибо за поддержку Маяса"
                    else
                        "Дешевле чашки кофе — а профиль сияет весь месяц",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MayasTheme.GlowGold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "ВЫГОДНО",
                        color = MayasTheme.GlowGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MayasTheme.GlowGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun GoldCtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MayasTheme.GlowGold,
                        Color(0xFFFFE08A),
                        MayasTheme.GlowGold
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun PremiumFeatureCard(
    feature: PremiumFeature,
    index: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MayasTheme.Surface)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MayasTheme.GlowGold.copy(alpha = 0.25f),
                                    MayasTheme.GlowGold.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        tint = MayasTheme.GlowGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feature.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        feature.desc,
                        fontSize = 13.sp,
                        color = MayasTheme.TextSecondary
                    )
                }

                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MayasTheme.GlowGold.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
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
    val isSuccess = promoStatus.contains("успешно", true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MayasTheme.Surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayasTheme.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = MayasTheme.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Есть промокод?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = promoCode,
                onValueChange = { onPromoChange(it.uppercase()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("Введите код") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MayasTheme.GlowGold,
                    unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
                trailingIcon = {
                    IconButton(onClick = onRedeem, enabled = promoCode.isNotBlank()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = if (promoCode.isNotBlank()) MayasTheme.GlowGold else MayasTheme.TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            )

            AnimatedVisibility(
                visible = promoStatus.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            (if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C)).copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        promoStatus,
                        color = if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}