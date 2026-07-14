package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dan1eidtj.mayas.core.ui.theme.* // поправь под свой актуальный package для R, см. предыдущий ответ
import com.dan1eidtj.mayas.ui.R
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun SplashScreen() {
    val eventData = remember {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1

        when {
            day == 26 && month == 5 -> "dan1eYT С ДР!" to Color(0xFFFF0000)
            day == 14 && month == 2 -> "dan1e С ДР!" to Color(0xFFFF4081)
            day == 9 && month == 5 -> "С ПОБЕДОЙ!" to Color(0xFFFF9C06)
            day == 1 && month == 6 -> "ЛЕТО!" to Color(0xFF00FFC2)
            day == 8 && month == 3 -> "8 февраля." to Color(0xFFFF4081)
            day == 31 && month == 12 -> "С НОВЫМ ГОДОМ!" to Color(0xFF00B1FF)
            else -> "м?" to MayasTheme.RedAccent
        }
    }

    val eventText = eventData.first
    val accentColor = eventData.second

    // Цвет самого лого зависит от темы: чёрное на светлой, белое на тёмной
    val isDark = isSystemInDarkTheme()
    val logoColor = if (isDark) Color.White else Color.Black

    val infinite = rememberInfiniteTransition(label = "cyber")

    val neonIntensity by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val logoProgress = remember { Animatable(0f) }
    val appearanceProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoProgress.animateTo(
            1f,
            animationSpec = tween(750, easing = CubicBezierEasing(0.17f, 1.6f, 0.3f, 1f))
        )
        appearanceProgress.animateTo(1f, animationSpec = tween(500, easing = LinearEasing))
    }

    var typedText by remember { mutableStateOf("") }
    LaunchedEffect(eventText) {
        delay(950)
        eventText.forEachIndexed { index, _ ->
            typedText = eventText.take(index + 1)
            delay(60)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MayasTheme.Background),
        contentAlignment = Alignment.Center
    ) {

        StaticParticles(accentColor)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = logoProgress.value.coerceIn(0f, 1f)
                }
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {

                // Неоновое свечение — красится в акцентный цвет события, не зависит от темы
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(24.dp * neonIntensity)
                        .graphicsLayer {
                            val s = logoTransform(logoProgress.value)
                            scaleX = s.scale
                            scaleY = s.scale
                            translationX = s.translationX
                            translationY = s.translationY
                            rotationZ = s.rotation
                        }
                )

                // Основное изображение — красится под тему: чёрное/белое
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(logoColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val s = logoTransform(logoProgress.value)
                            scaleX = s.scale
                            scaleY = s.scale
                            translationX = s.translationX
                            translationY = s.translationY
                            rotationZ = s.rotation
                        }
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "МАЯС",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 12.sp,
                color = MayasTheme.TextPrimary,
                modifier = Modifier.graphicsLayer { alpha = appearanceProgress.value }
            )

            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    text = typedText,
                    color = MayasTheme.TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

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

private data class LogoTransform(
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val rotation: Float
)

private fun logoTransform(progress: Float): LogoTransform {
    val p = progress.coerceIn(0f, 1f)
    return LogoTransform(
        scale = 0.5f + p * 0.5f,
        translationX = (1f - p) * 140f,
        translationY = (1f - p) * -160f,
        rotation = (1f - p) * -35f
    )
}

@Composable
fun StaticParticles(color: Color) {
    val infinite = rememberInfiniteTransition(label = "particles")
    val twinkle by infinite.animateFloat(
        initialValue = 0.1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val particles = remember { List(20) { Offset((0..1000).random().toFloat(), (0..2000).random().toFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize().alpha(twinkle)) {
        particles.forEach { offset ->
            drawCircle(
                color = color,
                radius = (1..3).random().dp.toPx(),
                center = Offset(size.width * (offset.x / 1000f), size.height * (offset.y / 2000f))
            )
        }
    }
}