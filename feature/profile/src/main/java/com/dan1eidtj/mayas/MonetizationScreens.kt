@file:OptIn(ExperimentalMaterial3Api::class)
package com.dan1eidtj.mayas

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


data class PremiumFeature(
    val icon: ImageVector,
    val title: String,
    val desc: String
)

enum class SubscriptionPlan(
    val title: String,
    val priceText: String,
    val periodText: String,
    val badge: String? = null,
    val isBestValue: Boolean = false
) {
    MONTHLY("1 Месяц", "59 ₽", "/ мес", null, false),
    ANNUAL("1 Год", "499 ₽", "/ год", "ВЫГОДА 30%", true)
}


fun Modifier.bounceClick(onClick: () -> Unit) = this.then(
    composed {
        var buttonState by remember { mutableStateOf(ButtonState.Idle) }
        val scale by animateFloatAsState(
            targetValue = if (buttonState == ButtonState.Pressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "bounce"
        )

        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(buttonState) {
                detectTapGestures(
                    onPress = {
                        buttonState = ButtonState.Pressed
                        tryAwaitRelease()
                        buttonState = ButtonState.Idle
                    },
                    onTap = { onClick() }
                )
            }
    }
)

private enum class ButtonState { Idle, Pressed }

@Composable
fun PremiumScreen(
    vm: MonetizationVM,
    onBack: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isPremium by remember { mutableStateOf(false) }


    DisposableEffect(uid) {
        if (uid.isNotEmpty()) {
            val listener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .addSnapshotListener { snap, _ ->
                    isPremium = snap?.getBoolean("isPremium") ?: false
                }
            onDispose { listener.remove() }
        } else {
            onDispose { }
        }
    }

    if (isPremium) {

        ManageSubscriptionScreen(
            onBack = onBack,
            onCancelSubscription = {
                Toast.makeText(vm.getApplication(), "Запрос на отмену отправлен", Toast.LENGTH_SHORT).show()
            }
        )
    } else {

        PaywallScreen(
            vm = vm,
            onBack = onBack
        )
    }
}


@Composable
private fun PaywallScreen(
    vm: MonetizationVM,
    onBack: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.ANNUAL) }
    var promoCode by remember { mutableStateOf("") }
    var promoStatus by remember { mutableStateOf("") }
    val context = LocalContext.current

    val features = remember {
        listOf(
            PremiumFeature(Icons.Default.Palette, "Градиентный ник", "Ваш ник переливается уникальным стильным градиентом"),
            PremiumFeature(Icons.Default.Verified, "Золотая галочка", "Особая золотая отметка VIP-подписчика"),
            PremiumFeature(Icons.Default.AutoAwesome, "Эксклюзивные рамки", "Анимированные и редкие рамки вокруг профиля"),
            PremiumFeature(Icons.Default.Bolt, "Приоритетная поддержка", "Ваши обращения обрабатываются в первую очередь")
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("MAYAS+", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0D0D0D).copy(alpha = 0.98f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    GoldCtaButton(
                        text = "ОФОРМИТЬ MAYAS+ • ${selectedPlan.priceText}",
                        onClick = {
                            Toast.makeText(context, "Переход к оплате ${selectedPlan.title}", Toast.LENGTH_SHORT).show()
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
                        listOf(Color(0xFF0C0914), Color(0xFF130E22), Color(0xFF231B0E))
                    )
                )
        ) {
            GlowOrb(color = MayasTheme.GlowGold, size = 280.dp, alignment = Alignment.TopEnd, offsetX = 70.dp, offsetY = (-50).dp)
            GlowOrb(color = MayasTheme.Accent, size = 240.dp, alignment = Alignment.CenterStart, offsetX = (-100).dp)

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    PremiumHeader()
                    Spacer(Modifier.height(24.dp))


                    PlanSelectionSection(
                        selectedPlan = selectedPlan,
                        onPlanSelected = { selectedPlan = it }
                    )

                    Spacer(Modifier.height(24.dp))
                    SectionLabel("ЧТО ВХОДИТ В ПОДПИСКУ")
                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(features) { index, feature ->
                    PremiumFeatureCard(feature = feature, index = index)
                }

                item {
                    Spacer(Modifier.height(20.dp))
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
fun ManageSubscriptionScreen(
    onBack: () -> Unit,
    onCancelSubscription: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Управление MAYAS+", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0C0914), Color(0xFF151026), Color(0xFF0D0D0D))
                    )
                )
        ) {
            GlowOrb(color = MayasTheme.GlowGold, size = 260.dp, alignment = Alignment.TopCenter, offsetY = (-80).dp)

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {

                    ActiveSubscriptionCard()
                    Spacer(Modifier.height(24.dp))

                    SectionLabel("АКТИВНЫЕ ПРИВИЛЕГИИ")
                    Spacer(Modifier.height(12.dp))

                    ActivePerkRow("Градиентный никнейм", "Активен в профиле")
                    ActivePerkRow("Золотой значок VIP", "Отображается у всех")
                    ActivePerkRow("Доступ к премиум рамкам", "Открыты все рамки")

                    Spacer(Modifier.height(32.dp))
                    SectionLabel("НАСТРОЙКИ И ДЕЙСТВИЯ")
                    Spacer(Modifier.height(12.dp))

                    ManageOptionTile(
                        icon = Icons.Default.CreditCard,
                        title = "Способ оплаты",
                        subtitle = "Пока недоступно.",
                        onClick = {}
                    )

                    ManageOptionTile(
                        icon = Icons.Default.HelpOutline,
                        title = "Служба поддержки",
                        subtitle = "Пока недоступно.",
                        onClick = {}
                    )

                    ManageOptionTile(
                        icon = Icons.Default.Cancel,
                        title = "Отменить подписку",
                        subtitle = "Пока недоступно.",
                        isDangerous = true,
                        onClick = { showCancelDialog = true }
                    )
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = Color(0xFF1E1A29),
            title = { Text("Отменить MAYAS+?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Вы потеряете градиентный ник, золотую иконку и эксклюзивные рамки по окончании текущего периода.",
                    color = MayasTheme.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancelSubscription()
                    }
                ) {
                    Text("Да, отменить", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCancelDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MayasTheme.GlowGold)
                ) {
                    Text("Оставить подписку", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}


@Composable
private fun ActiveSubscriptionCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MayasTheme.GlowGold.copy(alpha = 0.2f), Color(0xFF1E162D))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(MayasTheme.GlowGold.copy(alpha = 0.8f), Color.Transparent)),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = MayasTheme.GlowGold, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MAYAS+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayasTheme.GlowGold)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("АКТИВНА", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Следующее списание", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    Text("Никогда", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Тариф", color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    Text("59 ₽ / месяц", color = MayasTheme.GlowGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun PlanSelectionSection(
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubscriptionPlan.values().forEach { plan ->
            val isSelected = plan == selectedPlan
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MayasTheme.GlowGold.copy(alpha = 0.15f) else MayasTheme.Surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MayasTheme.GlowGold else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .bounceClick { onPlanSelected(plan) }
                    .padding(16.dp)
            ) {
                Column {
                    if (plan.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MayasTheme.GlowGold)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(plan.badge, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }

                    Text(plan.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(plan.priceText, color = MayasTheme.GlowGold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(plan.periodText, color = MayasTheme.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun ActivePerkRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MayasTheme.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MayasTheme.GlowGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ManageOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDangerous: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MayasTheme.Surface)
            .bounceClick { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background((if (isDangerous) Color(0xFFE74C3C) else MayasTheme.Accent).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isDangerous) Color(0xFFE74C3C) else MayasTheme.Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isDangerous) Color(0xFFE74C3C) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MayasTheme.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MayasTheme.TextSecondary, modifier = Modifier.size(20.dp))
    }
}


@Composable
private fun BoxScope.GlowOrb(
    color: Color,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                shape = CircleShape
            )
    )
}

@Composable
fun PremiumHeader() {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Brush.radialGradient(listOf(MayasTheme.GlowGold.copy(alpha = 0.4f), Color.Transparent)), CircleShape)
            )
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MayasTheme.GlowGold,
                modifier = Modifier.size(90.dp).scale(scale)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "MAYAS+",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(
                brush = Brush.linearGradient(listOf(MayasTheme.GlowGold, Color(0xFFFFE9B0), MayasTheme.GlowGold))
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Раскройте весь потенциал своего профиля",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MayasTheme.TextSecondary
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = MayasTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(MayasTheme.TextSecondary.copy(alpha = 0.15f)))
    }
}

@Composable
private fun GoldCtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(listOf(MayasTheme.GlowGold, Color(0xFFFFE08A), MayasTheme.GlowGold))
            )
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun PremiumFeatureCard(feature: PremiumFeature, index: Int = 0) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MayasTheme.Surface)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(MayasTheme.GlowGold.copy(0.25f), MayasTheme.GlowGold.copy(0.08f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(feature.icon, contentDescription = null, tint = MayasTheme.GlowGold, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(feature.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(feature.desc, fontSize = 12.sp, color = MayasTheme.TextSecondary)
                }
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
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayasTheme.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MayasTheme.Accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Есть промокод?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = promoCode,
                onValueChange = { onPromoChange(it.uppercase()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Введите промокод", fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MayasTheme.GlowGold,
                    unfocusedBorderColor = MayasTheme.TextSecondary.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
                trailingIcon = {
                    IconButton(onClick = onRedeem, enabled = promoCode.isNotBlank()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (promoCode.isNotBlank()) MayasTheme.GlowGold else MayasTheme.TextSecondary.copy(alpha = 0.3f)
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
                        .background((if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C)).copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        promoStatus,
                        color = if (isSuccess) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}