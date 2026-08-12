package com.dan1eidtj.data.cache

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


object ImageCacheManager {

    private const val CACHE_DIR_NAME = "coil_image_cache"
    private const val MAX_CACHE_SIZE_BYTES = 300L * 1024 * 1024 // 300 МБ

    @Volatile
    private var imageLoader: ImageLoader? = null

    @Volatile
    private var cacheDirRef: File? = null

    fun build(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: run {
                val dir = context.applicationContext.cacheDir.resolve(CACHE_DIR_NAME)
                cacheDirRef = dir
                ImageLoader.Builder(context.applicationContext)
                    .memoryCache {
                        MemoryCache.Builder(context)
                            .maxSizePercent(0.20)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(dir)
                            .maxSizeBytes(MAX_CACHE_SIZE_BYTES)
                            .build()
                    }


                    .respectCacheHeaders(false)
                    .build()
                    .also {
                        imageLoader = it
                        Coil.setImageLoader(it)
                    }
            }
        }
    }


    suspend fun getCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        build(context).diskCache?.size ?: 0L
    }


    suspend fun getCacheStats(context: Context): CacheStats = withContext(Dispatchers.IO) {
        val loader = build(context)
        val cache = loader.diskCache
        val sizeBytes = cache?.size ?: 0L
        val fileCount = cacheDirRef?.takeIf { it.exists() }
            ?.walkTopDown()
            ?.count { it.isFile }
            ?: 0
        CacheStats(
            sizeBytes = sizeBytes,
            maxSizeBytes = cache?.maxSize ?: MAX_CACHE_SIZE_BYTES,
            fileCount = fileCount
        )
    }


    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val loader = build(context)
        loader.diskCache?.clear()
        loader.memoryCache?.clear()
    }
}

data class CacheStats(
    val sizeBytes: Long,
    val maxSizeBytes: Long,
    val fileCount: Int
)


fun formatCacheSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.2f ГБ", gb)
        mb >= 1 -> String.format("%.1f МБ", mb)
        kb >= 1 -> String.format("%.0f КБ", kb)
        else -> "$bytes Б"
    }
}