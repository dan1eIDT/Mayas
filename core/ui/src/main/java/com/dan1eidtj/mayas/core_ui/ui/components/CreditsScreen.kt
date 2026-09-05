/* Copyright (C) 2026 ProjectIDT */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.dan1eidtj.mayas.core_ui.ui.components

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.dan1eidtj.mayas.core.ui.theme.*
import com.dan1eidtj.mayas.core_ui.utils.InstallSourceProvider
import com.dan1eidtj.mayas.core_ui.utils.label

private val IconPurple = Color(0xFF9C6ADE)
private val IconBlue = Color(0xFF5AC8FA)

private data class CreditEntry(val role: String, val handle: String)

private val CREDITS = listOf(
    CreditEntry("Идея", "@VeewCr"),
    CreditEntry("Дизайн", "@valeriy_dobryy"),
    CreditEntry("Разработка", "@dan1eIDT")
)

@Composable
fun CreditsScreen(onBack: () -> Unit, icon: Painter? = null) {
    val context = LocalContext.current

    val appVersion = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "unknown"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    val installSource = remember(context) { InstallSourceProvider.detect(context) }

    val logoResId = remember(context) {
        context.resources.getIdentifier("ic_logo", "drawable", context.packageName)
    }
    val effectiveIcon: Painter? = icon ?: if (logoResId != 0) painterResource(id = logoResId) else null

    val infinite = rememberInfiniteTransition(label = "InfiniteTransition")

    val bgAlpha by infinite.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse),
        label = "BgAlphaAnimation"
    )

    val iconFloat by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "IconFloat"
    )

    var visibleText by remember { mutableStateOf("") }
    val fullText = "Дошутился."

    LaunchedEffect(Unit) {
        fullText.forEachIndexed { i, _ ->
            visibleText = fullText.substring(0, i + 1)
            delay(120)
        }
    }

    val creditsShown = visibleText.length == fullText.length

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            IconPurple.copy(alpha = bgAlpha),
                            IconBlue.copy(alpha = bgAlpha * 0.6f),
                            Color.Black
                        ),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(contentAlignment = Alignment.Center) {

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer { translationY = iconFloat }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (effectiveIcon != null) {
                        Image(
                            painter = effectiveIcon,
                            contentDescription = "Иконка приложения",
                            modifier = Modifier.size(72.dp)
                        )
                    } else {
                        Box(
                            Modifier
                                .size(88.dp)
                                .background(
                                    Brush.linearGradient(listOf(IconPurple, IconBlue)),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "МАЯС",
                fontSize = 32.sp,
                letterSpacing = 6.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    visibleText,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
                if (!creditsShown) {
                    val cursorAlpha by infinite.animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
                        label = "CursorBlink"
                    )
                    Text(
                        "▌",
                        fontSize = 16.sp,
                        color = IconBlue.copy(alpha = cursorAlpha),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CREDITS.forEachIndexed { index, entry ->
                    AnimatedVisibility(
                        visible = creditsShown,
                        enter = fadeIn(tween(400, delayMillis = index * 120)) +
                                slideInVertically(tween(400, delayMillis = index * 120)) { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(
                                            Brush.linearGradient(listOf(IconPurple, IconBlue)),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.role.uppercase(),
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    color = IconBlue
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(entry.handle, color = Color.White, fontSize = 17.sp)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = creditsShown,
                    enter = fadeIn(tween(400, delayMillis = CREDITS.size * 120))
                ) {
                    Text(
                        "v$appVersion • ${installSource.label()}",
                        color = MayasTheme.TextGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(44.dp))

            val buttonScale by animateFloatAsState(
                targetValue = if (creditsShown) 1f else 0f,
                label = "ButtonScaleAnimation"
            )

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                        alpha = buttonScale
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(IconPurple, IconBlue)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Назад", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}