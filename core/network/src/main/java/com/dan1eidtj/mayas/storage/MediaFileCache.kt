/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object MediaFileCache {

    private const val DIR_NAME = "media_cache"
    const val DEFAULT_MAX_BYTES = 1024L * 1024L * 1024L

    private val locks = ConcurrentHashMap<String, Mutex>()

    fun directory(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, DIR_NAME).apply { mkdirs() }

    fun fileFor(context: Context, key: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        val extension = key.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 1..5 }
        return File(directory(context), if (extension != null) "$name.$extension" else name)
    }

    fun cachedFile(context: Context, key: String): File? {
        val file = fileFor(context, key)
        return if (file.isFile && file.length() > 0L) file else null
    }

    suspend fun obtain(context: Context, key: String, auto: Boolean = false): File? {
        cachedFile(context, key)?.let {
            it.setLastModified(System.currentTimeMillis())
            return it
        }
        if (auto && !key.startsWith("http") && MediaCachePrefs.autoDownloadBlocked(context)) return null
        val mutex = locks.getOrPut(key) { Mutex() }
        return mutex.withLock {
            cachedFile(context, key)?.let { return@withLock it }
            val destination = fileFor(context, key)
            val ok = B2MediaClient.downloadToFile(key, destination)
            if (ok) {
                trim(context, MediaCachePrefs.maxBytes(context))
                destination
            } else {
                null
            }
        }
    }

    fun cachedModel(context: Context, keyOrUrl: String?): String? {
        if (keyOrUrl.isNullOrBlank()) return null
        if (keyOrUrl.startsWith("http")) return keyOrUrl
        val file = cachedFile(context, keyOrUrl) ?: return null
        return android.net.Uri.fromFile(file).toString()
    }

    suspend fun resolveModel(context: Context, keyOrUrl: String?, auto: Boolean = false): String? {
        if (keyOrUrl.isNullOrBlank()) return null
        if (keyOrUrl.startsWith("http")) return keyOrUrl
        val file = obtain(context, keyOrUrl, auto)
        if (file == null && auto && MediaCachePrefs.autoDownloadBlocked(context)) return null
        if (file != null) return android.net.Uri.fromFile(file).toString()
        return runCatching { B2MediaClient.resolveDownloadUrl(keyOrUrl) }.getOrNull()
    }

    suspend fun storeLocalCopy(context: Context, key: String, source: File) {
        withContext(Dispatchers.IO) {
            try {
                val destination = fileFor(context, key)
                if (!destination.exists()) source.copyTo(destination, overwrite = true)
            } catch (_: Exception) {
            }
        }
    }

    fun cleanStale(context: Context, maxAgeMs: Long = 60L * 60L * 1000L) {
        val threshold = System.currentTimeMillis() - maxAgeMs
        val app = context.applicationContext
        app.cacheDir.listFiles()?.forEach { file ->
            val name = file.name
            val temporary = name.startsWith("circle_") || name.startsWith("temp_voice_") || name.endsWith(".part")
            if (file.isFile && temporary && file.lastModified() < threshold) file.delete()
        }
        directory(app).listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".part") && file.lastModified() < threshold) file.delete()
        }
    }

    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        directory(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun fileCount(context: Context): Int = withContext(Dispatchers.IO) {
        directory(context).walkTopDown().count { it.isFile }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        directory(context).listFiles()?.forEach { it.deleteRecursively() }
        Unit
    }

    suspend fun trimByAge(context: Context, days: Int) = withContext(Dispatchers.IO) {
        if (days <= 0) return@withContext
        val threshold = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        directory(context).listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < threshold) file.delete()
        }
    }

    suspend fun applyPolicy(context: Context) {
        trimByAge(context, MediaCachePrefs.keepDays(context))
        val limit = MediaCachePrefs.maxBytes(context)
        if (limit != MediaCachePrefs.UNLIMITED) trim(context, limit)
    }

    suspend fun trim(context: Context, maxBytes: Long) = withContext(Dispatchers.IO) {
        val files = directory(context).listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return@withContext
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }
}
