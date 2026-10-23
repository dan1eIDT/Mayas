/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.core.ui.components


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.RadialGradient
import android.graphics.RectF as AndroidRectF
import android.graphics.Shader
import com.dan1eidtj.mayas.ui.R
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ───────────────────────────── Публичное API (без изменений) ─────────────────────────────

enum class ImageEditAspect(val ratio: Float) {
    SQUARE(1f), BANNER(2.45f), STORY(9f / 16f), LANDSCAPE(16f / 9f), ORIGINAL(1f)
}

fun cleanupEditedImage(context: Context, uri: Uri) {
    if (uri.scheme != "file") return
    val file = runCatching { File(uri.path ?: return).canonicalFile }.getOrNull() ?: return
    val cacheDirectory = runCatching { context.cacheDir.canonicalFile }.getOrNull() ?: return
    if (file.parentFile == cacheDirectory && file.name.startsWith("mayas_edit_")) {
        file.delete()
    }
}

/**
 * Редактор в стиле Telegram.
 *
 * Если [aspect] != ORIGINAL, пропорция рамки жёстко задана (баннер, сторис и т.п.).
 */
@Composable
fun ImageCropEditorDialog(
    sourceUri: Uri,
    aspect: ImageEditAspect,
    onDismiss: () -> Unit,
    onApply: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var saveFailed by remember(sourceUri) { mutableStateOf(false) }
    var busy by remember(sourceUri) { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        loadFailed = false
        sourceBitmap = withContext(Dispatchers.IO) {
            try {
                decodeOrientedBitmap(context, sourceUri)
            } catch (t: Throwable) { // Throwable, чтобы поймать и OutOfMemoryError
                Log.e(TAG, "decode failed, uri=$sourceUri, mime=${runCatching { context.contentResolver.getType(sourceUri) }.getOrNull()}", t)
                null
            }
        }
        loadFailed = sourceBitmap == null
    }

    DisposableEffect(sourceBitmap) {
        val bitmapToRecycle = sourceBitmap
        onDispose { bitmapToRecycle?.recycle() }
    }

    val callerRatio = if (aspect == ImageEditAspect.ORIGINAL) null else aspect.ratio
    val bmp = sourceBitmap
    val state = remember(bmp) { bmp?.let { EditorState(it, callerRatio) } }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0F))
                .systemBarsPadding()
        ) {
            when {
                state == null && !loadFailed -> CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                state == null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.image_editor_load_failed), color = Color.White)
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.image_editor_cancel), color = AccentLight)
                    }
                }
                else -> EditorContent(
                    state = state,
                    busy = busy,
                    saveFailed = saveFailed,
                    onCancel = { if (!busy) onDismiss() },
                    onSave = { snapshot ->
                        val source = state.bitmap
                        busy = true
                        saveFailed = false
                        scope.launch {
                            val output = withContext(Dispatchers.IO) {
                                try {
                                    renderAndSave(context, source, snapshot)
                                } catch (t: Throwable) {
                                    Log.e(TAG, "renderAndSave failed", t)
                                    null
                                }
                            }
                            busy = false
                            if (output != null) onApply(output) else saveFailed = true
                        }
                    }
                )
            }
        }
    }
}

// ───────────────────────────── Константы и тексты ─────────────────────────────

private const val TAG = "ImageCropEditor"
private const val MAX_DECODE_SIDE = 2048
private const val MAX_OUTPUT_SIDE = 2200f
private const val MAX_VIEW_SCALE = 6f

private val Accent = Color(0xFF6500FF)
private val AccentLight = Color(0xFFC8A9FF)
private val PanelColor = Color(0xFF141418)
private val SliderThumbRadius = 9.dp

// Эти строки можно вынести в strings.xml, если нужна локализация.
private object Txt {
    const val FREE = "Свободно"
    const val ORIGINAL = "Оригинал"
    const val FIXED_FORMAT = "Формат кадра задан"
    const val STRAIGHTEN = "Наклон"
    const val ROTATE = "↻ 90°"
    const val FLIP = "⇋ Отразить"
    const val RESET = "Сброс"
    const val BRIGHTNESS = "Яркость"
    const val CONTRAST = "Контрастность"
    const val SATURATION = "Насыщенность"
    const val SHADOWS = "Тени"
    const val HIGHLIGHTS = "Свет"
    const val WARMTH = "Теплота"
    const val VIGNETTE = "Виньетка"
    const val STRENGTH = "Сила"
}

private enum class EditorTab { CROP, ADJUST, COLOR, EFFECTS }

private enum class Handle(val left: Boolean, val top: Boolean, val right: Boolean, val bottom: Boolean) {
    TOP_LEFT(true, true, false, false),
    TOP_RIGHT(false, true, true, false),
    BOTTOM_LEFT(true, false, false, true),
    BOTTOM_RIGHT(false, false, true, true),
    LEFT(true, false, false, false),
    RIGHT(false, false, true, false),
    TOP(false, true, false, false),
    BOTTOM(false, false, false, true);

    val isCorner: Boolean get() = (left || right) && (top || bottom)
}

private class AspectChoice(val key: String, val label: String, val ratio: Float?)

private val AspectChoices = listOf(
    AspectChoice("free", Txt.FREE, null),
    AspectChoice("orig", Txt.ORIGINAL, null),
    AspectChoice("1:1", "1:1", 1f),
    AspectChoice("4:3", "4:3", 4f / 3f),
    AspectChoice("3:4", "3:4", 3f / 4f),
    AspectChoice("16:9", "16:9", 16f / 9f),
    AspectChoice("9:16", "9:16", 9f / 16f)
)

private fun swapKey(key: String): String = when (key) {
    "4:3" -> "3:4"
    "3:4" -> "4:3"
    "16:9" -> "9:16"
    "9:16" -> "16:9"
    else -> key
}

// ───────────────────────────── Фильтры и цветокоррекция ─────────────────────────────

private enum class ImageEditFilter { ORIGINAL, WARM, COOL, VIVID, FADE, SEPIA, MONO, NOIR }

private val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

private fun scaleCm(r: Float, g: Float, b: Float) = AndroidColorMatrix(
    floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
)

private fun satCm(s: Float) = AndroidColorMatrix().apply { setSaturation(s) }

private fun contrastCm(c: Float, offset: Float = 0f): AndroidColorMatrix {
    val t = (1f - c) * 127.5f + offset
    return AndroidColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/** Применяет матрицы по порядку: первая — первой. */
private fun concatCm(vararg ms: AndroidColorMatrix): AndroidColorMatrix {
    val result = AndroidColorMatrix()
    for (m in ms) result.postConcat(m)
    return result
}

private fun ImageEditFilter.matrixArray(): FloatArray = when (this) {
    ImageEditFilter.ORIGINAL -> IDENTITY.copyOf()
    ImageEditFilter.WARM -> scaleCm(1.08f, 1.02f, 0.91f).array.copyOf()
    ImageEditFilter.COOL -> scaleCm(0.92f, 1.0f, 1.10f).array.copyOf()
    ImageEditFilter.VIVID -> concatCm(satCm(1.35f), contrastCm(1.08f)).array.copyOf()
    ImageEditFilter.FADE -> concatCm(satCm(0.88f), contrastCm(0.86f, 14f)).array.copyOf()
    ImageEditFilter.SEPIA -> AndroidColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ).array.copyOf()
    ImageEditFilter.MONO -> satCm(0f).array.copyOf()
    ImageEditFilter.NOIR -> concatCm(satCm(0f), contrastCm(1.25f)).array.copyOf()
}

private fun lerpArray(a: FloatArray, b: FloatArray, t: Float): FloatArray =
    FloatArray(a.size) { a[it] + (b[it] - a[it]) * t }

private fun toneCm(shadows: Float, highlights: Float): AndroidColorMatrix {
    val a = shadows / 100f * 0.25f
    val h = highlights / 100f * 0.25f
    val k = (1f - a) * (1f + h)
    val t = (1f + h) * a * 255f
    return AndroidColorMatrix(
        floatArrayOf(
            k, 0f, 0f, 0f, t,
            0f, k, 0f, 0f, t,
            0f, 0f, k, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun adjustMatrix(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    shadows: Float,
    highlights: Float
): AndroidColorMatrix {
    val sat = satCm(1f + saturation / 100f)
    val con = contrastCm(1f + contrast / 100f * 0.5f, brightness / 100f * 70f)
    val tone = toneCm(shadows, highlights)
    val k = warmth / 100f * 0.15f
    val warm = scaleCm(1f + k, 1f, 1f - k)
    return concatCm(sat, con, tone, warm)
}

private fun buildColorMatrix(
    filter: ImageEditFilter,
    strength: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    shadows: Float,
    highlights: Float
): AndroidColorMatrix? {
    val neutral = filter == ImageEditFilter.ORIGINAL &&
            brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f &&
            shadows == 0f && highlights == 0f
    if (neutral) return null
    val base = AndroidColorMatrix(lerpArray(IDENTITY, filter.matrixArray(), strength))
    base.postConcat(adjustMatrix(brightness, contrast, saturation, warmth, shadows, highlights))
    return base
}

private fun newBitmapPaint(colorArray: FloatArray?): AndroidPaint {
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG or AndroidPaint.DITHER_FLAG)
    if (colorArray != null) paint.colorFilter = ColorMatrixColorFilter(AndroidColorMatrix(colorArray))
    return paint
}

private fun newVignettePaint(width: Float, height: Float, amount: Float): AndroidPaint {
    val cx = width / 2f
    val cy = height / 2f
    val radius = max(sqrt(cx * cx + cy * cy), 1f)
    val strength = (amount / 100f).coerceIn(0f, 1f)
    val edge = (strength * 0.85f * 255f).roundToInt()
    val mid = (edge * 0.3f).roundToInt()
    val colors = intArrayOf(0x00000000, 0x00000000, mid shl 24, edge shl 24)
    val stops = floatArrayOf(0f, 0.4f, 0.72f, 1f)
    return AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP)
    }
}

// ───────────────────────────── Геометрия ─────────────────────────────

private class Fit(val scale: Float, val center: Offset)

/** Минимальный масштаб, при котором повернутая картинка полностью покрывает рамку. */
private fun minScaleFor(iw: Float, ih: Float, thetaDeg: Float, crop: Rect): Float {
    val th = thetaDeg * PI / 180.0
    val c = abs(cos(th)).toFloat()
    val s = abs(sin(th)).toFloat()
    val a = crop.width / 2f
    val b = crop.height / 2f
    val ex = a * c + b * s
    val ey = a * s + b * c
    return max(2f * ex / iw, 2f * ey / ih)
}

/**
 * Подгоняет масштаб и положение картинки так, чтобы рамка целиком лежала внутри повернутой картинки.
 * Выпуклая фигура лежит внутри прямоугольника, если внутри её ограничивающий бокс в системе
 * координат картинки — поэтому проверка точная.
 */
private fun constrainTransform(
    iw: Float,
    ih: Float,
    thetaDeg: Float,
    crop: Rect,
    scale: Float,
    center: Offset
): Fit {
    val th = thetaDeg * PI / 180.0
    val cs = cos(th).toFloat()
    val sn = sin(th).toFloat()
    val a = crop.width / 2f
    val b = crop.height / 2f
    val ex = a * abs(cs) + b * abs(sn)
    val ey = a * abs(sn) + b * abs(cs)
    val minS = max(2f * ex / iw, 2f * ey / ih)
    val s = max(scale, minS)
    val hw = iw * s / 2f
    val hh = ih * s / 2f
    val p = crop.center
    val dx = p.x - center.x
    val dy = p.y - center.y
    var qx = dx * cs + dy * sn
    var qy = -dx * sn + dy * cs
    val lx = max(hw - ex, 0f)
    val ly = max(hh - ey, 0f)
    qx = qx.coerceIn(-lx, lx)
    qy = qy.coerceIn(-ly, ly)
    val cx = p.x - (qx * cs - qy * sn)
    val cy = p.y - (qx * sn + qy * cs)
    return Fit(s, Offset(cx, cy))
}

private fun buildViewMatrix(iw: Int, ih: Int, scale: Float, center: Offset, thetaDeg: Float, flip: Boolean): AndroidMatrix =
    AndroidMatrix().apply {
        postTranslate(-iw / 2f, -ih / 2f)
        if (flip) postScale(-1f, 1f)
        postRotate(thetaDeg)
        postScale(scale, scale)
        postTranslate(center.x, center.y)
    }

// ───────────────────────────── Состояние редактора ─────────────────────────────

private class EditSnapshot(
    val crop: Rect,
    val scale: Float,
    val center: Offset,
    val theta: Float,
    val flip: Boolean,
    val colorArray: FloatArray?,
    val vignette: Float
)

@Stable
private class EditorState(val bitmap: Bitmap, private val callerRatio: Float?) {
    val iw = bitmap.width.toFloat()
    val ih = bitmap.height.toFloat()

    var ready by mutableStateOf(false)
    var bounds by mutableStateOf(Rect.Zero)
    var crop by mutableStateOf(Rect.Zero)
    var scale by mutableFloatStateOf(1f)
    var center by mutableStateOf(Offset.Zero)
    var straighten by mutableFloatStateOf(0f)
    var rot90 by mutableIntStateOf(0)
    var flipH by mutableStateOf(false)
    var lockedRatio by mutableStateOf<Float?>(callerRatio)
    var ratioKey by mutableStateOf(if (callerRatio != null) "fixed" else "free")
    var interacting by mutableStateOf(false)

    var brightness by mutableFloatStateOf(0f)
    var contrast by mutableFloatStateOf(0f)
    var saturation by mutableFloatStateOf(0f)
    var warmth by mutableFloatStateOf(0f)
    var shadows by mutableFloatStateOf(0f)
    var highlights by mutableFloatStateOf(0f)
    var vignette by mutableFloatStateOf(0f)
    var filter by mutableStateOf(ImageEditFilter.ORIGINAL)
    var filterStrength by mutableFloatStateOf(1f)

    private var containerSize = IntSize.Zero
    private var minFrame = 0f
    private var dragHandle: Handle? = null
    private var grab = Offset.Zero
    private var paintCache: AndroidPaint? = null
    private var paintKey: FloatArray? = null
    private var vignetteCache: AndroidPaint? = null
    private var vignetteW = 0f
    private var vignetteH = 0f
    private var vignetteAmount = 0f

    val theta: Float get() = rot90 * 90f + straighten
    val fixedByCaller: Boolean get() = callerRatio != null
    val isFree: Boolean get() = lockedRatio == null

    fun onContainer(size: IntSize, density: Float) {
        if (size.width <= 0 || size.height <= 0) return
        if (ready && size == containerSize) return
        containerSize = size
        val pad = 20f * density
        minFrame = 72f * density
        bounds = Rect(pad, pad, size.width - pad, size.height - pad)
        if (bounds.width <= minFrame || bounds.height <= minFrame) return
        val ratio = if (ready) crop.width / crop.height else (callerRatio ?: (iw / ih))
        resetFit(ratio)
        ready = true
    }

    private fun fitRect(ratio: Float): Rect {
        var w = bounds.width
        var h = w / ratio
        if (h > bounds.height) {
            h = bounds.height
            w = h * ratio
        }
        val c = bounds.center
        return Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)
    }

    /** Рамка максимального размера с заданной пропорцией, картинка по центру с минимальным зумом. */
    fun resetFit(ratio: Float) {
        crop = fitRect(ratio)
        val fit = constrainTransform(iw, ih, theta, crop, 0f, crop.center)
        scale = fit.scale
        center = fit.center
    }

    private fun reapply() {
        val fit = constrainTransform(iw, ih, theta, crop, scale, center)
        scale = fit.scale
        center = fit.center
    }

    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val minS = minScaleFor(iw, ih, theta, crop)
        val newScale = (scale * zoom).coerceIn(minS, max(minS, MAX_VIEW_SCALE))
        val f = newScale / scale
        val newCenter = centroid + (center - centroid) * f + pan
        val fit = constrainTransform(iw, ih, theta, crop, newScale, newCenter)
        scale = fit.scale
        center = fit.center
    }

    // ── рамка ──

    fun hitTest(p: Offset, slop: Float): Handle? {
        val c = crop
        val nl = abs(p.x - c.left) <= slop
        val nr = abs(p.x - c.right) <= slop
        val nt = abs(p.y - c.top) <= slop
        val nb = abs(p.y - c.bottom) <= slop
        val inX = p.x >= c.left - slop && p.x <= c.right + slop
        val inY = p.y >= c.top - slop && p.y <= c.bottom + slop
        return when {
            nl && nt -> Handle.TOP_LEFT
            nr && nt -> Handle.TOP_RIGHT
            nl && nb -> Handle.BOTTOM_LEFT
            nr && nb -> Handle.BOTTOM_RIGHT
            !isFree -> null // при фиксированной пропорции тянем только за углы
            nl && inY -> Handle.LEFT
            nr && inY -> Handle.RIGHT
            nt && inX -> Handle.TOP
            nb && inX -> Handle.BOTTOM
            else -> null
        }
    }

    fun beginDrag(handle: Handle, p: Offset) {
        dragHandle = handle
        grab = Offset(
            when {
                handle.left -> p.x - crop.left
                handle.right -> p.x - crop.right
                else -> 0f
            },
            when {
                handle.top -> p.y - crop.top
                handle.bottom -> p.y - crop.bottom
                else -> 0f
            }
        )
    }

    fun dragTo(p: Offset) {
        val h = dragHandle ?: return
        val ratio = lockedRatio
        if (ratio == null) {
            var l = crop.left
            var t = crop.top
            var r = crop.right
            var b = crop.bottom
            if (h.left) l = (p.x - grab.x).coerceIn(bounds.left, r - minFrame)
            if (h.right) r = (p.x - grab.x).coerceIn(l + minFrame, bounds.right)
            if (h.top) t = (p.y - grab.y).coerceIn(bounds.top, b - minFrame)
            if (h.bottom) b = (p.y - grab.y).coerceIn(t + minFrame, bounds.bottom)
            crop = Rect(l, t, r, b)
        } else if (h.isCorner) {
            val anchor = Offset(
                if (h.left) crop.right else crop.left,
                if (h.top) crop.bottom else crop.top
            )
            val target = p - grab
            val sx = if (h.right) 1f else -1f
            val sy = if (h.bottom) 1f else -1f
            val availW = if (sx > 0) bounds.right - anchor.x else anchor.x - bounds.left
            val availH = if (sy > 0) bounds.bottom - anchor.y else anchor.y - bounds.top
            var w = max(abs(target.x - anchor.x), abs(target.y - anchor.y) * ratio)
            w = min(w, min(availW, availH * ratio))
            w = max(w, minFrame * max(1f, ratio))
            val hh = w / ratio
            val corner = Offset(anchor.x + sx * w, anchor.y + sy * hh)
            crop = Rect(
                min(anchor.x, corner.x), min(anchor.y, corner.y),
                max(anchor.x, corner.x), max(anchor.y, corner.y)
            )
        }
        reapply()
    }

    fun endDrag() {
        dragHandle = null
        settle()
    }

    /** После отпускания рамки растягиваем её на всё доступное место вместе с картинкой. */
    private fun settle() {
        if (!ready || crop.width <= 0f || crop.height <= 0f) return
        val f = min(bounds.width / crop.width, bounds.height / crop.height)
        if (f.isNaN() || abs(f - 1f) < 0.002f) {
            reapply()
            return
        }
        val bc = bounds.center
        val newW = crop.width * f
        val newH = crop.height * f
        val newCenter = bc + (center - crop.center) * f
        val newCrop = Rect(bc.x - newW / 2f, bc.y - newH / 2f, bc.x + newW / 2f, bc.y + newH / 2f)
        crop = newCrop
        val fit = constrainTransform(iw, ih, theta, newCrop, scale * f, newCenter)
        scale = fit.scale
        center = fit.center
    }

    // ── действия ──

    fun selectChoice(choice: AspectChoice) {
        if (!ready) return
        ratioKey = choice.key
        when (choice.key) {
            "free" -> lockedRatio = null
            "orig" -> {
                val r = if (rot90 % 2 == 1) ih / iw else iw / ih
                lockedRatio = r
                resetFit(r)
            }
            else -> {
                val r = choice.ratio ?: return
                lockedRatio = r
                resetFit(r)
            }
        }
    }

    fun rotate90() {
        if (!ready) return
        rot90 = (rot90 + 1) % 4
        val fixed = callerRatio
        val newRatio: Float
        if (fixed != null) {
            newRatio = fixed
        } else {
            newRatio = crop.height / crop.width
            val locked = lockedRatio
            if (locked != null) {
                lockedRatio = 1f / locked
                ratioKey = swapKey(ratioKey)
            }
        }
        resetFit(newRatio)
    }

    /** Зеркало по горизонтали на экране: зеркалим картинку и инвертируем угол. */
    fun flip() {
        if (!ready) return
        flipH = !flipH
        rot90 = (4 - rot90) % 4
        straighten = -straighten
        reapply()
    }

    fun updateStraighten(value: Float) {
        straighten = if (abs(value) < 1.2f) 0f else value
        reapply()
    }

    fun resetCrop() {
        if (!ready) return
        rot90 = 0
        flipH = false
        straighten = 0f
        lockedRatio = callerRatio
        ratioKey = if (callerRatio != null) "fixed" else "free"
        resetFit(callerRatio ?: (iw / ih))
    }

    // ── рендер ──

    fun colorMatrix(): AndroidColorMatrix? =
        buildColorMatrix(filter, filterStrength, brightness, contrast, saturation, warmth, shadows, highlights)

    fun viewMatrix(): AndroidMatrix = buildViewMatrix(bitmap.width, bitmap.height, scale, center, theta, flipH)

    fun makePaint(): AndroidPaint {
        val array = colorMatrix()?.array?.copyOf()
        val cached = paintCache
        val key = paintKey
        val same = (array == null && key == null) || (array != null && key != null && array.contentEquals(key))
        if (cached != null && same) return cached
        val fresh = newBitmapPaint(array)
        paintCache = fresh
        paintKey = array
        return fresh
    }

    fun vignettePaint(w: Float, h: Float): AndroidPaint? {
        if (vignette <= 0f || w <= 0f || h <= 0f) return null
        val cached = vignetteCache
        if (cached != null && vignetteW == w && vignetteH == h && vignetteAmount == vignette) return cached
        val fresh = newVignettePaint(w, h, vignette)
        vignetteCache = fresh
        vignetteW = w
        vignetteH = h
        vignetteAmount = vignette
        return fresh
    }

    fun snapshot() = EditSnapshot(crop, scale, center, theta, flipH, colorMatrix()?.array?.copyOf(), vignette)
}

// ───────────────────────────── UI ─────────────────────────────

@Composable
private fun EditorContent(
    state: EditorState,
    busy: Boolean,
    saveFailed: Boolean,
    onCancel: () -> Unit,
    onSave: (EditSnapshot) -> Unit
) {
    var tab by remember { mutableStateOf(EditorTab.CROP) }
    Column(Modifier.fillMaxSize()) {
        if (saveFailed) {
            Text(
                text = stringResource(R.string.image_editor_save_failed),
                color = Color(0xFFFF7777),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
        }
        PreviewArea(state, cropMode = tab == EditorTab.CROP, modifier = Modifier.weight(1f).fillMaxWidth())
        Column(Modifier.fillMaxWidth().background(PanelColor)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (tab) {
                        EditorTab.CROP -> CropControls(state)
                        EditorTab.ADJUST -> GeneralControls(state)
                        EditorTab.COLOR -> ColorControls(state)
                        EditorTab.EFFECTS -> EffectsControls(state)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, enabled = !busy) {
                    Text(
                        text = stringResource(R.string.image_editor_cancel).uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorTab.values().forEach { t ->
                        TabIcon(t, selected = t == tab) { tab = t }
                    }
                }
                TextButton(onClick = { onSave(state.snapshot()) }, enabled = state.ready && !busy) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = AccentLight, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = stringResource(R.string.image_editor_apply).uppercase(),
                            color = AccentLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: EditorTab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) AccentLight else Color.White.copy(alpha = 0.75f)
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(24.dp)) { drawTabIcon(tab, tint, PanelColor) }
    }
}

private fun DrawScope.drawTabIcon(tab: EditorTab, tint: Color, fill: Color) {
    val u = size.width / 24f
    val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (tab) {
        EditorTab.CROP -> {
            val path = Path().apply {
                moveTo(6f * u, 2f * u)
                lineTo(6f * u, 18f * u)
                lineTo(22f * u, 18f * u)
                moveTo(2f * u, 6f * u)
                lineTo(18f * u, 6f * u)
                lineTo(18f * u, 22f * u)
            }
            drawPath(path, tint, style = stroke)
        }
        EditorTab.ADJUST -> {
            val ys = floatArrayOf(5f, 12f, 19f)
            val xs = floatArrayOf(8f, 16f, 10f)
            for (i in 0..2) {
                drawLine(tint, Offset(3f * u, ys[i] * u), Offset(21f * u, ys[i] * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
                drawCircle(fill, radius = 2.6f * u, center = Offset(xs[i] * u, ys[i] * u))
                drawCircle(tint, radius = 2.6f * u, center = Offset(xs[i] * u, ys[i] * u), style = stroke)
            }
        }
        EditorTab.COLOR -> {
            val path = Path().apply {
                moveTo(12f * u, 3f * u)
                cubicTo(12f * u, 3f * u, 5f * u, 10f * u, 5f * u, 15f * u)
                cubicTo(5f * u, 18.9f * u, 8.1f * u, 22f * u, 12f * u, 22f * u)
                cubicTo(15.9f * u, 22f * u, 19f * u, 18.9f * u, 19f * u, 15f * u)
                cubicTo(19f * u, 10f * u, 12f * u, 3f * u, 12f * u, 3f * u)
                close()
            }
            drawPath(path, tint, style = stroke)
        }
        EditorTab.EFFECTS -> {
            val path = Path().apply {
                moveTo(2f * u, 12f * u)
                cubicTo(5f * u, 4f * u, 8f * u, 4f * u, 12f * u, 12f * u)
                cubicTo(16f * u, 20f * u, 19f * u, 20f * u, 22f * u, 12f * u)
            }
            drawPath(path, tint, style = stroke)
        }
    }
}

@Composable
private fun PreviewArea(state: EditorState, cropMode: Boolean, modifier: Modifier) {
    val density = LocalDensity.current.density
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.onContainer(it, density) }
            .pointerInput(state, cropMode) {
                if (!cropMode) return@pointerInput
                val slop = 28.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!state.ready) return@awaitEachGesture
                    state.interacting = true
                    val handle = state.hitTest(down.position, slop)
                    if (handle != null) {
                        state.beginDrag(handle, down.position)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break
                            if (change.pressed) {
                                state.dragTo(change.position)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        state.endDrag()
                    } else {
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoom != 1f || pan != Offset.Zero) state.transform(centroid, pan, zoom)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                    state.interacting = false
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) { drawEditor(state, cropMode) }
    }
}

private fun DrawScope.drawEditor(state: EditorState, cropMode: Boolean) {
    if (!state.ready) return
    val matrix = state.viewMatrix()
    val paint = state.makePaint()
    drawIntoCanvas { it.nativeCanvas.drawBitmap(state.bitmap, matrix, paint) }
    state.vignettePaint(state.crop.width, state.crop.height)?.let { vp ->
        drawIntoCanvas {
            val nc = it.nativeCanvas
            nc.save()
            nc.translate(state.crop.left, state.crop.top)
            nc.drawRect(0f, 0f, state.crop.width, state.crop.height, vp)
            nc.restore()
        }
    }

    val c = state.crop
    val dim = Color.Black.copy(alpha = 0.62f)
    drawRect(dim, Offset.Zero, Size(size.width, c.top))
    drawRect(dim, Offset(0f, c.bottom), Size(size.width, size.height - c.bottom))
    drawRect(dim, Offset(0f, c.top), Size(c.left, c.height))
    drawRect(dim, Offset(c.right, c.top), Size(size.width - c.right, c.height))

    drawRect(Color.White, c.topLeft, c.size, style = Stroke(width = 1.dp.toPx()))

    if (!cropMode) return

    if (state.interacting) {
        val grid = Color.White.copy(alpha = 0.45f)
        val gw = 1.dp.toPx()
        for (i in 1..2) {
            val x = c.left + c.width * i / 3f
            val y = c.top + c.height * i / 3f
            drawLine(grid, Offset(x, c.top), Offset(x, c.bottom), strokeWidth = gw)
            drawLine(grid, Offset(c.left, y), Offset(c.right, y), strokeWidth = gw)
        }
    }

    val sw = 3.dp.toPx()
    val len = 22.dp.toPx()
    val half = sw / 2f
    val col = Color.White
    // угловые скобки
    drawLine(col, Offset(c.left - half, c.top), Offset(c.left + len, c.top), strokeWidth = sw)
    drawLine(col, Offset(c.left, c.top - half), Offset(c.left, c.top + len), strokeWidth = sw)
    drawLine(col, Offset(c.right + half, c.top), Offset(c.right - len, c.top), strokeWidth = sw)
    drawLine(col, Offset(c.right, c.top - half), Offset(c.right, c.top + len), strokeWidth = sw)
    drawLine(col, Offset(c.left - half, c.bottom), Offset(c.left + len, c.bottom), strokeWidth = sw)
    drawLine(col, Offset(c.left, c.bottom + half), Offset(c.left, c.bottom - len), strokeWidth = sw)
    drawLine(col, Offset(c.right + half, c.bottom), Offset(c.right - len, c.bottom), strokeWidth = sw)
    drawLine(col, Offset(c.right, c.bottom + half), Offset(c.right, c.bottom - len), strokeWidth = sw)
    if (state.isFree) {
        val mid = len * 0.7f
        val cx = c.center.x
        val cy = c.center.y
        drawLine(col, Offset(cx - mid, c.top), Offset(cx + mid, c.top), strokeWidth = sw)
        drawLine(col, Offset(cx - mid, c.bottom), Offset(cx + mid, c.bottom), strokeWidth = sw)
        drawLine(col, Offset(c.left, cy - mid), Offset(c.left, cy + mid), strokeWidth = sw)
        drawLine(col, Offset(c.right, cy - mid), Offset(c.right, cy + mid), strokeWidth = sw)
    }
}

@Composable
private fun CropControls(state: EditorState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.fixedByCaller) {
            Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.CenterStart) {
                Text(Txt.FIXED_FORMAT, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            LazyRow(
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(AspectChoices) { choice ->
                    Chip(choice.label, selected = state.ratioKey == choice.key) { state.selectChoice(choice) }
                }
            }
        }
        SliderRow(
            label = Txt.STRAIGHTEN,
            value = state.straighten,
            range = -45f..45f,
            defaultValue = 0f,
            snap = 0f,
            valueText = "${state.straighten.roundToInt()}°",
            onChange = { state.updateStraighten(it) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(Txt.ROTATE) { state.rotate90() }
            PillButton(Txt.FLIP) { state.flip() }
            PillButton(Txt.RESET) { state.resetCrop() }
        }
    }
}

@Composable
private fun GeneralControls(state: EditorState) {
    Column {
        AdjustRow(Txt.BRIGHTNESS, state.brightness) { state.brightness = it }
        AdjustRow(Txt.CONTRAST, state.contrast) { state.contrast = it }
        AdjustRow(Txt.SHADOWS, state.shadows) { state.shadows = it }
        AdjustRow(Txt.HIGHLIGHTS, state.highlights) { state.highlights = it }
    }
}

@Composable
private fun ColorControls(state: EditorState) {
    Column {
        AdjustRow(Txt.SATURATION, state.saturation) { state.saturation = it }
        AdjustRow(Txt.WARMTH, state.warmth) { state.warmth = it }
    }
}

@Composable
private fun EffectsControls(state: EditorState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterControls(state)
        SliderRow(
            label = Txt.VIGNETTE,
            value = state.vignette,
            range = 0f..100f,
            defaultValue = 0f,
            snap = 2f,
            valueText = state.vignette.roundToInt().toString(),
            onChange = { state.vignette = it }
        )
    }
}

@Composable
private fun AdjustRow(label: String, value: Float, onChange: (Float) -> Unit) {
    SliderRow(label, value, -100f..100f, 0f, 3f, value.roundToInt().toString(), onChange)
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    snap: Float,
    valueText: String,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier
                .width(104.dp)
                .clickable { onChange(defaultValue) }
        )
        TgSlider(value, range, defaultValue, snap, onChange, Modifier.weight(1f).fillMaxHeight())
        Text(
            text = valueText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun TgSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    snap: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)
    val span = range.endInclusive - range.start
    Canvas(
        modifier
            .pointerInput(defaultValue) {
                detectTapGestures(
                    onDoubleTap = { currentOnChange(defaultValue) },
                    onLongPress = { currentOnChange(defaultValue) }
                )
            }
            .pointerInput(range, snap, defaultValue) {
                val pad = SliderThumbRadius.toPx()
                val grabRadius = 28.dp.toPx()
                val usable = (size.width - 2f * pad).coerceAtLeast(1f)
                var grab = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val thumbX = pad + (currentValue - range.start) / span * usable
                        grab = if (abs(offset.x - thumbX) <= grabRadius) thumbX - offset.x else 0f
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = ((change.position.x + grab - pad) / usable).coerceIn(0f, 1f)
                        val raw = range.start + fraction * span
                        currentOnChange(if (abs(raw - defaultValue) < snap) defaultValue else raw)
                    }
                )
            }
    ) {
        val pad = SliderThumbRadius.toPx()
        val usable = (size.width - 2f * pad).coerceAtLeast(1f)
        val cy = size.height / 2f
        val trackWidth = 2.dp.toPx()
        fun xAt(v: Float) = pad + ((v - range.start) / span).coerceIn(0f, 1f) * usable
        drawLine(
            Color.White.copy(alpha = 0.25f),
            Offset(pad, cy),
            Offset(pad + usable, cy),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            Accent,
            Offset(xAt(defaultValue), cy),
            Offset(xAt(value), cy),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawCircle(Color.White, radius = pad, center = Offset(xAt(value), cy))
    }
}

@Composable
private fun filterLabel(filter: ImageEditFilter): String = when (filter) {
    ImageEditFilter.ORIGINAL -> stringResource(R.string.image_editor_filter_original)
    ImageEditFilter.WARM -> stringResource(R.string.image_editor_filter_warm)
    ImageEditFilter.MONO -> stringResource(R.string.image_editor_filter_mono)
    ImageEditFilter.COOL -> "Холодный"
    ImageEditFilter.VIVID -> "Яркий"
    ImageEditFilter.FADE -> "Выцвет"
    ImageEditFilter.SEPIA -> "Сепия"
    ImageEditFilter.NOIR -> "Нуар"
}

@Composable
private fun FilterControls(state: EditorState) {
    val thumb = remember(state.bitmap) { makeThumb(state.bitmap, 160) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ImageEditFilter.values().toList()) { f ->
                FilterThumb(thumb, f, selected = state.filter == f, label = filterLabel(f)) { state.filter = f }
            }
        }
        if (state.filter != ImageEditFilter.ORIGINAL) {
            SliderRow(
                label = Txt.STRENGTH,
                value = state.filterStrength,
                range = 0f..1f,
                defaultValue = 1f,
                snap = 0f,
                valueText = "${(state.filterStrength * 100f).roundToInt()}%",
                onChange = { state.filterStrength = it }
            )
        }
    }
}

@Composable
private fun FilterThumb(thumb: Bitmap, filter: ImageEditFilter, selected: Boolean, label: String, onClick: () -> Unit) {
    val paint = remember(filter) {
        newBitmapPaint(if (filter == ImageEditFilter.ORIGINAL) null else filter.matrixArray())
    }
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Canvas(
            Modifier
                .size(64.dp)
                .clip(shape)
                .then(if (selected) Modifier.border(2.dp, Accent, shape) else Modifier)
        ) {
            drawIntoCanvas {
                it.nativeCanvas.drawBitmap(thumb, null, AndroidRectF(0f, 0f, size.width, size.height), paint)
            }
        }
        Text(
            text = label,
            color = if (selected) AccentLight else Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) AccentLight else Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

private fun makeThumb(src: Bitmap, size: Int): Bitmap {
    val side = min(src.width, src.height)
    val x = (src.width - side) / 2
    val y = (src.height - side) / 2
    val square = Bitmap.createBitmap(src, x, y, side, side)
    val scaled = Bitmap.createScaledBitmap(square, size, size, true)
    if (square !== src && square !== scaled) square.recycle()
    return scaled
}

// ───────────────────────────── Рендер результата ─────────────────────────────

private fun renderAndSave(context: Context, bitmap: Bitmap, snap: EditSnapshot): Uri {
    val cropW = snap.crop.width
    val cropH = snap.crop.height
    require(cropW > 1f && cropH > 1f) { "preview_not_ready" }
    val nativeW = cropW / snap.scale
    val nativeH = cropH / snap.scale
    val k = min(1f, MAX_OUTPUT_SIDE / max(nativeW, nativeH))
    val outW = (nativeW * k).roundToInt().coerceAtLeast(1)
    val outH = (nativeH * k).roundToInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val file = File(context.cacheDir, "mayas_edit_${System.currentTimeMillis()}.jpg")
    try {
        val canvas = AndroidCanvas(out)
        canvas.drawColor(0xFF000000.toInt())
        val matrix = buildViewMatrix(bitmap.width, bitmap.height, snap.scale, snap.center, snap.theta, snap.flip)
        matrix.postTranslate(-snap.crop.left, -snap.crop.top)
        matrix.postScale(outW / cropW, outH / cropH)
        canvas.drawBitmap(bitmap, matrix, newBitmapPaint(snap.colorArray))
        if (snap.vignette > 0f) {
            canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), newVignettePaint(outW.toFloat(), outH.toFloat(), snap.vignette))
        }
        FileOutputStream(file).use { stream ->
            if (!out.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                throw IllegalStateException("jpeg_encode_failed")
            }
        }
        return Uri.fromFile(file)
    } catch (t: Throwable) {
        file.delete()
        throw t
    } finally {
        out.recycle()
    }
}

// ───────────────────────────── Декодирование ─────────────────────────────

private fun decodeOrientedBitmap(context: Context, uri: Uri): Bitmap {
    // API 28+: ImageDecoder умеет HEIC/HEIF/AVIF/WebP и сам применяет EXIF-поворот.
    if (Build.VERSION.SDK_INT >= 28) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_DECODE_SIDE) {
                    val k = MAX_DECODE_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * k).roundToInt().coerceAtLeast(1),
                        (info.size.height * k).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ImageDecoder failed, fallback to BitmapFactory, uri=$uri", t)
        }
    }
    return decodeWithBitmapFactory(context, uri)
}

private fun decodeWithBitmapFactory(context: Context, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IllegalArgumentException("openInputStream returned null (bounds), uri=$uri")
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IllegalArgumentException("unsupported image format (bounds ${bounds.outWidth}x${bounds.outHeight}), mime=${bounds.outMimeType}")
    }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODE_SIDE) sample *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IllegalArgumentException("decodeStream returned null, mime=${bounds.outMimeType}")
    val rotation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)
    if (rotation == 0f) return decoded
    val matrix = AndroidMatrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it !== decoded) decoded.recycle()
    }
}