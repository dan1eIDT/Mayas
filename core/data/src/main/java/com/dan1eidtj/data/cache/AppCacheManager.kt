/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.data.cache

import android.content.Context
import android.text.format.Formatter
import com.dan1eidtj.mayas.db.OfflineDataStore
import com.dan1eidtj.mayas.storage.MediaFileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CacheBreakdown(
    val imagesBytes: Long,
    val imagesCount: Int,
    val mediaBytes: Long,
    val mediaCount: Int,
    val offlineDataBytes: Long,
    val firestoreBytes: Long,
    val tempBytes: Long,
    val tempCount: Int
) {
    val totalBytes: Long
        get() = imagesBytes + mediaBytes + offlineDataBytes + firestoreBytes + tempBytes

    val clearableBytes: Long
        get() = imagesBytes + mediaBytes + tempBytes
}

object AppCacheManager {

    private const val IMAGE_CACHE_DIR = "coil_image_cache"

    private fun sizeOf(files: List<File>): Long = files.sumOf { if (it.isFile) it.length() else 0L }

    private fun firestoreFiles(context: Context): List<File> {
        val dir = context.applicationContext.getDatabasePath("probe").parentFile ?: return emptyList()
        return dir.listFiles { file -> file.name.startsWith("firestore.") }?.toList() ?: emptyList()
    }

    private fun tempFiles(context: Context): List<File> {
        val cacheDir = context.applicationContext.cacheDir
        val imageDir = File(cacheDir, IMAGE_CACHE_DIR)
        return cacheDir.walkTopDown()
            .onEnter { it != imageDir }
            .filter { it.isFile }
            .toList()
    }

    suspend fun breakdown(context: Context): CacheBreakdown = withContext(Dispatchers.IO) {
        val images = ImageCacheManager.getCacheStats(context)
        val temp = tempFiles(context)
        CacheBreakdown(
            imagesBytes = images.sizeBytes,
            imagesCount = images.fileCount,
            mediaBytes = MediaFileCache.sizeBytes(context),
            mediaCount = MediaFileCache.fileCount(context),
            offlineDataBytes = sizeOf(OfflineDataStore.currentAccountFiles(context)),
            firestoreBytes = sizeOf(firestoreFiles(context)),
            tempBytes = sizeOf(temp),
            tempCount = temp.size
        )
    }

    suspend fun clearImages(context: Context) {
        ImageCacheManager.clearCache(context)
    }

    suspend fun clearMedia(context: Context) {
        MediaFileCache.clear(context)
    }

    suspend fun clearTemp(context: Context) = withContext(Dispatchers.IO) {
        tempFiles(context).forEach { it.delete() }
    }

    suspend fun clearOfflineData(context: Context) {
        OfflineDataStore.clearCurrentAccount(context)
    }

    suspend fun clearAll(context: Context) {
        clearImages(context)
        clearMedia(context)
        clearTemp(context)
    }

    fun format(context: Context, bytes: Long): String =
        Formatter.formatFileSize(context.applicationContext, bytes)
}
