package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun SplashScreen() {
    // 🧠 Мозги: Твоя фича с датами (оставил как просил)
    val eventData = remember {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1

        when {
            day == 26 && month == 5 -> "dan1eYT С ДР!" to Color(0xFFFFFFFF)
            day == 14 && month == 2 -> "dan1e С ДР!" to Color(0xFFFF4081)
            day == 9 && month == 5 -> "С ПОБЕДОЙ!" to Color(0xFFFF9C06)
            day == 1 && month == 6 -> "ЛЕТО В МАЯС." to Color(0xFF00FFC2)
            day == 31 && month == 12 -> "С НОВЫМ ГОДОМ!" to Color(0xFF00B1FF)
            else -> "м?" to MayasTheme.RedAccent
        }
    }

    val eventText = eventData.first
    val accentColor = eventData.second

    // --- Анимации ---
    val infinite = rememberInfiniteTransition(label = "cyber")

    // Вращение фона для динамики
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing))
    )

    // Пульсация неона
    val neonIntensity by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // Появление (Scale + Alpha)
    val appearanceProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appearanceProgress.animateTo(1f, animationSpec = tween(1200, easing = EaseOutBack))
    }

    // Печатная машинка
    var typedText by remember { mutableStateOf("") }
    LaunchedEffect(eventText) {
        delay(600)
        eventText.forEachIndexed { index, _ ->
            typedText = eventText.take(index + 1)
            delay(60) // Чуть помедленнее, чтобы читалось солидно
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040405)),
        contentAlignment = Alignment.Center
    ) {


        // 2. ✨ ЧАСТИЦЫ (Звездная пыль)
        StaticParticles(accentColor)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = appearanceProgress.value
                    scaleX = 0.8f + (appearanceProgress.value * 0.2f)
                    scaleY = 0.8f + (appearanceProgress.value * 0.2f)
                }
        ) {
            // 3. 🔥 LOGO (Neon Layering)
            Box(contentAlignment = Alignment.Center) {
                // Внешнее размытое свечение
                Text(
                    "!M",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor.copy(alpha = 0.3f),
                    modifier = Modifier.blur(20.dp * neonIntensity)
                )
                // Основной текст лого
                Text(
                    "!M",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    style = TextStyle(
                        shadow = Shadow(accentColor, offset = Offset(0f, 0f), blurRadius = 30f * neonIntensity)
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // 4. 🧠 BRAND NAME
            Text(
                "МАЯС",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 12.sp,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            // 5. 📟 EVENT TEXT
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    text = typedText,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. ⏳ ЛИНИЯ ЗАГРУЗКИ (Bottom Progress)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .width(150.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(appearanceProgress.value)
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun StaticParticles(color: Color) {
    val particles = remember { List(20) { Offset((0..1000).random().toFloat(), (0..2000).random().toFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
        particles.forEach { offset ->
            drawCircle(
                color = color,
                radius = (1..3).random().dp.toPx(),
                center = Offset(size.width * (offset.x / 1000f), size.height * (offset.y / 2000f))
            )
        }
    }
}