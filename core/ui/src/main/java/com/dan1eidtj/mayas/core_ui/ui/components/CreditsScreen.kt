@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.dan1eidtj.mayas.core.ui.theme.*

@Composable
fun CreditsScreen(onBack: () -> Unit) {

    val infinite = rememberInfiniteTransition(label = "InfiniteTransition")

    // 🌌 Анимация фона
    val bgAlpha by infinite.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgAlphaAnimation"
    )

    // 💜 Реализовал пульс логотипа (раз уж в комментах заявлял!)
    val logoScale by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoPulse"
    )

    // ✍️ Печатающийся текст
    var visibleText by remember { mutableStateOf("") }
    val fullText = "Дошутился."

    LaunchedEffect(Unit) {
        fullText.forEachIndexed { i, _ ->
            visibleText = fullText.substring(0, i + 1)
            delay(150) // 60мс было слишком быстро, "Дошутился" пролетало мгновенно. 150мс — в самый раз!
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 🌌 Мягкий glow фон
        Box(
            Modifier
                .fillMaxSize()
                .background(MayasTheme.RedAccent.copy(alpha = bgAlpha))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {


            // 💜 Логотип с эффектом пульсации
            Text(
                "МАЯС",
                fontSize = 50.sp,
                color = MayasTheme.RedAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
            )

            Spacer(Modifier.height(12.dp))

            // ✍️ Печать текста
            Text(
                visibleText,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(40.dp))

            // 👥 Кредиты (плавное появление)
            AnimatedVisibility(
                visible = visibleText.length == fullText.length,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text("Идея", color = Color.White, fontSize = 18.sp)
                    Text("@VeewCr", color = MayasTheme.TextGrey)

                    Spacer(Modifier.height(16.dp))

                    Text("Дизайн", color = Color.White, fontSize = 18.sp) // Поправил опечатку "Дизаин"
                    Text("@valeriy_dobryy", color = MayasTheme.TextGrey)

                    Spacer(Modifier.height(16.dp))

                    Text("Разработка", color = Color.White, fontSize = 18.sp)
                    Text("@dan1eIDT", color = MayasTheme.TextGrey)

                    Spacer(Modifier.height(16.dp))

                    Text("beta.31.05.26", color = MayasTheme.TextGrey)
                }
            }

            Spacer(Modifier.height(50.dp))

            // 💥 Кнопка с эффектом масштаба
            val buttonScale by animateFloatAsState(
                targetValue = if (visibleText.length == fullText.length) 1f else 0.0f, // Если текст не напечатан — прячем кнопку полностью
                label = "ButtonScaleAnimation"
            )

            Button(
                onClick = onBack,
                modifier = Modifier.graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                    alpha = buttonScale
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MayasTheme.RedAccent
                )
            ) {
                Text("Назад", color = Color.White)
            }
        }
    }
}