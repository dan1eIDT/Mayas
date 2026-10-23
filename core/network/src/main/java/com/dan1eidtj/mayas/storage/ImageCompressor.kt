package com.dan1eidtj.mayas.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val AVATAR_MAX_SIDE_PX = 512
    private const val AVATAR_JPEG_QUALITY = 85
    private const val BANNER_MAX_SIDE_PX = 1600
    private const val BANNER_JPEG_QUALITY = 82
    suspend fun compressAvatar(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val original = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("Не удалось прочитать изображение: $uri")

        val scaled = scaleDown(original, AVATAR_MAX_SIDE_PX)

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, output)

        if (scaled !== original) original.recycle()
        scaled.recycle()

        output.toByteArray()
    }

    suspend fun compressProfileBanner(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("Не удалось прочитать изображение: $uri")

        var oriented: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            val exif = context.contentResolver.openInputStream(uri)?.use(::ExifInterface)
            val rotation = exif?.rotationDegrees ?: 0
            val mirrored = exif?.isFlipped == true
            val source = if (rotation != 0 || mirrored) {
                val matrix = Matrix().apply {
                    if (rotation != 0) postRotate(rotation.toFloat())
                    if (mirrored) postScale(-1f, 1f)
                }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    .also { oriented = it }
            } else decoded

            val scaledBitmap = scaleDown(source, BANNER_MAX_SIDE_PX)
            scaled = scaledBitmap
            val output = ByteArrayOutputStream()
            check(scaledBitmap.compress(Bitmap.CompressFormat.JPEG, BANNER_JPEG_QUALITY, output)) {
                "Не удалось сжать изображение баннера"
            }
            output.toByteArray()
        } finally {
            scaled?.takeIf { it !== decoded && it !== oriented }?.recycle()
            oriented?.takeIf { it !== decoded }?.recycle()
            decoded.recycle()
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxSide) return bitmap

        val scale = maxSide.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
