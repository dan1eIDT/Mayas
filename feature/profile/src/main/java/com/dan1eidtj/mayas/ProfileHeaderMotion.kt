/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme
import com.dan1eidtj.mayas.core_ui.ui.components.UserAvatarView
import com.dan1eidtj.mayas.storage.MediaFileCache
import com.dan1eidtj.profile.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val SmallAvatar = 110.dp
internal val ProfileFloatingBarHeight = 48.dp
private val BannerExpanded = 190.dp
private val BannerCollapsed = 104.dp
private val BannerAvatarBox = 112.dp
private val BannerAvatar = 104.dp
private const val BannerAvatarMinScale = 0.68f
private const val BannerDecodeWidth = 1080
private const val BannerDecodeHeight = 640

internal fun glowNameOf(color: Color): String = when (color) {
    MayasTheme.GlowPink -> "pink"
    MayasTheme.GlowBlue -> "blue"
    MayasTheme.GlowGreen -> "green"
    MayasTheme.GlowGold -> "gold"
    MayasTheme.GlowRed -> "red"
    MayasTheme.GlowOrange -> "orange"
    MayasTheme.GlowCyan -> "cyan"
    MayasTheme.GlowMint -> "mint"
    MayasTheme.GlowIndigo -> "indigo"
    MayasTheme.GlowLime -> "lime"
    MayasTheme.GlowRose -> "rose"
    MayasTheme.GlowAmber -> "amber"
    MayasTheme.GlowSky -> "sky"
    MayasTheme.GlowWhite -> "white"
    else -> "purple"
}

@Stable
class ProfileHeaderState {
    var collapse by mutableFloatStateOf(0f)
        private set
    var stretch by mutableFloatStateOf(0f)
        private set
    var enabled by mutableStateOf(false)
        private set
    var expand by mutableFloatStateOf(0f)
        private set
    private var expandable = false
    private var expandThresholdPx = 0f
    private var rangePx = 0f
    private var maxStretchPx = 0f

    val progress: Float
        get() = if (rangePx <= 0f) 0f else (collapse / rangePx).coerceIn(0f, 1f)

    fun configure(
        enabled: Boolean,
        rangePx: Float,
        maxStretchPx: Float,
        expandable: Boolean = false,
        expandThresholdPx: Float = 0f
    ) {
        this.enabled = enabled
        this.rangePx = rangePx
        this.maxStretchPx = maxStretchPx
        this.expandable = enabled && expandable
        this.expandThresholdPx = expandThresholdPx
        if (!this.expandable && expand != 0f) expand = 0f
        if (!enabled) {
            if (collapse != 0f) collapse = 0f
            if (stretch != 0f) stretch = 0f
        } else if (collapse > rangePx) {
            collapse = rangePx
        }
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!enabled) return Offset.Zero
            val dy = available.y
            if (dy >= 0f) return Offset.Zero
            var left = -dy
            var used = 0f
            if (expand > 0f && expandThresholdPx > 0f) {
                val d = min(expand * expandThresholdPx, left)
                expand = (expand - d / expandThresholdPx).coerceAtLeast(0f)
                if (expand < 0.0001f) expand = 0f
                left -= d
                used += d
            }
            if (stretch > 0f) {
                val d = min(stretch, left)
                stretch -= d
                left -= d
                used += d
            }
            if (left > 0f && collapse < rangePx) {
                val d = min(left, rangePx - collapse)
                collapse += d
                used += d
            }
            return Offset(0f, -used)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (!enabled) return Offset.Zero
            val dy = available.y
            if (dy <= 0f) return Offset.Zero
            var left = dy
            var used = 0f
            if (collapse > 0f) {
                val d = min(collapse, left)
                collapse -= d
                left -= d
                used += d
            }
            if (left > 0f && source == NestedScrollSource.UserInput && maxStretchPx > 0f) {
                val resistance = 1f - (stretch / maxStretchPx).coerceIn(0f, 1f)
                stretch = (stretch + left * 0.5f * resistance).coerceAtMost(maxStretchPx)
                used += left
            } else if (left > 0f && expandable && collapse == 0f && source == NestedScrollSource.UserInput && expandThresholdPx > 0f) {
                expand = (expand + left * 0.6f / expandThresholdPx).coerceAtMost(1f)
                used += left
            }
            return Offset(0f, used)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    suspend fun settle() {
        if (expandable && expand > 0f && expand < 1f) {
            val target = if (expand > 0.55f) 1f else 0f
            animate(
                initialValue = expand,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> expand = value.coerceIn(0f, 1f) }
        }
        if (stretch > 0f) {
            animate(
                initialValue = stretch,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> stretch = value.coerceAtLeast(0f) }
        }
        if (collapse > 0f && collapse < rangePx) {
            val target = if (collapse > rangePx * 0.5f) rangePx else 0f
            animate(
                initialValue = collapse,
                targetValue = target,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) { value, _ -> collapse = value }
        }
    }
}

@Composable
fun rememberProfileHeaderState(): ProfileHeaderState = remember { ProfileHeaderState() }

@Composable
internal fun ConfigureProfileHeaderMotion(
    state: ProfileHeaderState,
    bigPhoto: Boolean,
    banner: Boolean,
    expandable: Boolean = false
) {
    val density = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val range = with(density) {
        when {
            bigPhoto -> (widthDp.dp - SmallAvatar).toPx()
            banner -> (BannerExpanded - BannerCollapsed).toPx()
            else -> 0f
        }
    }
    val maxStretch = with(density) { if (bigPhoto) (widthDp.dp * 0.32f).toPx() else 0f }
    val enabled = bigPhoto || banner
    val expandThreshold = with(density) { 140.dp.toPx() }
    SideEffect { state.configure(enabled, range, maxStretch, expandable && banner, expandThreshold) }
}

internal fun Modifier.photoNameOverlay(state: ProfileHeaderState): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val p = state.progress
        val inset = 20.dp.roundToPx()
        val topGap = 12.dp.roundToPx()
        val height = ((placeable.height + topGap) * p).roundToInt()
        val y = lerp(-(placeable.height + inset).toFloat(), topGap.toFloat(), p).roundToInt()
        layout(placeable.width, height) { placeable.place(0, y) }
    }

internal fun Modifier.photoNameAlign(state: ProfileHeaderState?): Modifier {
    if (state == null) return this
    return layout { measurable, constraints ->
        val startPx = 20.dp.roundToPx()
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = (constraints.maxWidth - startPx * 2).coerceAtLeast(0)
            )
        )
        val p = state.progress
        val x = lerp(startPx.toFloat(), (constraints.maxWidth - placeable.width) / 2f, p).roundToInt()
        layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
    }
}

@Composable
internal fun ProfileBigPhoto(
    state: ProfileHeaderState,
    avatar: String,
    useCustomAvatar: Boolean,
    profileIcon: String,
    glowName: String,
    isPremium: Boolean,
    avatarFrame: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resolvedUrl by produceState<String?>(initialValue = null, avatar) {
        value = runCatching { MediaFileCache.resolveModel(context, avatar) }.getOrNull()
    }
    val request = remember(resolvedUrl) {
        resolvedUrl?.let {
            ImageRequest.Builder(context).data(it).crossfade(true).build()
        }
    }
    val smallPx = with(LocalDensity.current) { SmallAvatar.toPx() }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topGapPx = with(LocalDensity.current) { (8.dp + ProfileFloatingBarHeight + statusTop).toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val p = state.progress
                val side = lerp(width.toFloat(), smallPx, p)
                val height = (side + state.stretch + topGapPx * p).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (topGapPx * state.progress).roundToInt()) }
                .layout { measurable, constraints ->
                    val p = state.progress
                    val side = lerp(constraints.maxWidth.toFloat(), smallPx, p).roundToInt()
                    val height = side + state.stretch.roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(side, height))
                    layout(side, height) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    val p = state.progress
                    val eased = FastOutSlowInEasing.transform(((p - 0.12f) / 0.88f).coerceIn(0f, 1f))
                    shape = RoundedCornerShape(size = eased * min(size.width, size.height) / 2f)
                    clip = true
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - state.progress * 1.6f).coerceIn(0f, 1f) }
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.35f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (topGapPx * state.progress).roundToInt()) }
                .size(SmallAvatar)
                .graphicsLayer { alpha = ((state.progress - 0.78f) / 0.22f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center
        ) {
            UserAvatarView(
                avatarUrl = avatar,
                useCustomAvatar = useCustomAvatar,
                profileIcon = profileIcon,
                profileGlow = glowName,
                isPremium = isPremium,
                frameType = avatarFrame,
                size = SmallAvatar
            )
        }
    }
}

private fun bannerPalette(style: String): List<Color>? = when (style) {
    "ocean" -> listOf(Color(0xFF075985), Color(0xFF06B6D4), Color(0xFF38BDF8))
    "sunset" -> listOf(Color(0xFF9D174D), Color(0xFFF97316), Color(0xFFFBBF24))
    "forest" -> listOf(Color(0xFF14532D), Color(0xFF10B981), Color(0xFF84CC16))
    "night" -> listOf(Color(0xFF111827), Color(0xFF5B21B6), Color(0xFF3B82F6))
    "custom" -> listOf(Color(0xFF312E81), Color(0xFF0F172A), Color(0xFF6366F1))
    "none" -> null
    else -> listOf(Color(0xFF4F46E5), Color(0xFFDB2777), Color(0xFF06B6D4))
}

@Composable
private fun AuroraBackdrop(palette: List<Color>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "profileAurora")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "auroraPhase"
    )
    val base = remember(palette) { Brush.linearGradient(listOf(palette[0], palette[1])) }
    Box(
        modifier = modifier.drawBehind {
            drawRect(base)
            val t = phase.value * 2f * PI.toFloat()
            val radius = size.width * 0.6f
            for (i in 0 until 3) {
                val angle = t + i * 2.1f
                val center = Offset(
                    x = size.width * (0.5f + 0.38f * cos(angle)),
                    y = size.height * (0.5f + 0.32f * sin(angle * 1.3f))
                )
                val color = palette[(i + 1) % palette.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.62f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    )
}

@Composable
internal fun ProfileBannerBackdrop(
    bannerUrl: String,
    bannerStyle: String,
    progress: () -> Float,
    fadeColor: Color,
    parallax: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedUrl by produceState<String?>(initialValue = null, bannerUrl) {
        value = if (bannerUrl.isBlank()) null else runCatching {
            MediaFileCache.resolveModel(context, bannerUrl)
        }.getOrNull()
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val palette = remember(bannerStyle) { bannerPalette(bannerStyle) }
    val surfaceColor = MayasTheme.Surface
    val fadeBrush = remember(fadeColor) {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.28f),
            0.28f to Color.Transparent,
            0.56f to Color.Transparent,
            1f to fadeColor
        )
    }
    val plainFade = remember {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.12f),
            1f to Color.Black.copy(alpha = 0.3f)
        )
    }
    Box(modifier = modifier.clipToBounds().background(surfaceColor)) {
        val url = resolvedUrl
        if (!url.isNullOrBlank()) {
            val request = remember(url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(BannerDecodeWidth, BannerDecodeHeight)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (parallax) Modifier.requiredHeight(BannerExpanded) else Modifier.fillMaxSize())
                    .graphicsLayer {
                        val settle = 1.14f - 0.08f * appear.value
                        scaleX = settle
                        scaleY = settle
                        alpha = appear.value
                    }
            )
        } else if (palette != null) {
            AuroraBackdrop(palette = palette, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (parallax) fadeBrush else plainFade)
        )
    }
}

@Composable
internal fun ProfileBannerWithAvatar(
    state: ProfileHeaderState?,
    bannerUrl: String,
    bannerStyle: String,
    avatar: String,
    useCustomAvatar: Boolean,
    profileIcon: String,
    glowName: String,
    isPremium: Boolean,
    avatarFrame: String,
    onAvatarClick: (() -> Unit)?,
    expandable: Boolean = false
) {
    val progress: () -> Float = { state?.progress ?: 0f }
    val expandNow: () -> Float = { state?.expand ?: 0f }
    val context = LocalContext.current
    val photoUrl by produceState<String?>(initialValue = null, avatar, expandable) {
        value = if (!expandable || avatar.isBlank()) null else runCatching {
            MediaFileCache.resolveModel(context, avatar)
        }.getOrNull()
    }
    val background = MayasTheme.Background
    val expandedPx = with(LocalDensity.current) { BannerExpanded.roundToPx() }
    val collapsedPx = with(LocalDensity.current) { BannerCollapsed.roundToPx() }
    val boxPx = with(LocalDensity.current) { BannerAvatarBox.roundToPx() }

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            ProfileBannerBackdrop(
                bannerUrl = bannerUrl,
                bannerStyle = bannerStyle,
                progress = progress,
                fadeColor = background,
                parallax = true
            )
            Box(
                modifier = Modifier
                    .size(BannerAvatarBox)
                    .clip(CircleShape)
                    .background(background)
                    .then(
                        if (onAvatarClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAvatarClick
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                UserAvatarView(
                    avatarUrl = avatar,
                    useCustomAvatar = useCustomAvatar,
                    profileIcon = profileIcon,
                    profileGlow = glowName,
                    isPremium = isPremium,
                    frameType = avatarFrame,
                    size = BannerAvatar
                )
            }
            val bigUrl = photoUrl
            if (expandable && !bigUrl.isNullOrBlank()) {
                val bigRequest = remember(bigUrl) {
                    ImageRequest.Builder(context).data(bigUrl).crossfade(true).build()
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = expandNow().coerceIn(0f, 1f) }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (expandNow() > 0.5f) onAvatarClick?.invoke()
                        }
                ) {
                    AsyncImage(
                        model = bigRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.22f),
                                    0.3f to Color.Transparent,
                                    1f to Color.Transparent
                                )
                            )
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val p = progress()
        val e = expandNow().coerceIn(0f, 1f)
        val width = constraints.maxWidth
        val baseHeight = lerp(expandedPx.toFloat(), collapsedPx.toFloat(), p)
        val bannerHeight = lerp(baseHeight, width.toFloat(), e).roundToInt()
        val scale = lerp(1f, BannerAvatarMinScale, p)
        val bannerPlaceable = measurables[0].measure(Constraints.fixed(width, bannerHeight))
        val avatarPlaceable = measurables[1].measure(Constraints.fixed(boxPx, boxPx))
        val photoPlaceable = measurables.getOrNull(2)?.measure(Constraints.fixed(width, bannerHeight))
        val halfScaled = (boxPx * scale / 2f * (1f - e)).roundToInt()
        layout(width, bannerHeight + halfScaled) {
            bannerPlaceable.place(0, 0)
            avatarPlaceable.placeWithLayer((width - boxPx) / 2, bannerHeight - boxPx / 2) {
                scaleX = scale
                scaleY = scale
                alpha = (1f - e * 2f).coerceIn(0f, 1f)
            }
            photoPlaceable?.place(0, 0)
        }
    }
}

@Composable
internal fun ProfileBannerEditCard(
    bannerUrl: String,
    bannerStyle: String,
    showLabel: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        ProfileBannerBackdrop(
            bannerUrl = bannerUrl,
            bannerStyle = bannerStyle,
            progress = { 0f },
            fadeColor = Color.Transparent,
            parallax = false,
            modifier = Modifier.fillMaxSize()
        )
        if (showLabel) {
            Text(
                text = stringResource(R.string.profile_banner_title),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }
    }
}
