/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.storage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class AutoDownloadType(val userDataKey: String, val preferenceKey: String) {
    PHOTO("autoDownloadPhotos", "auto_download_photos"),
    VIDEO("autoDownloadVideos", "auto_download_videos"),
}

object MediaCachePrefs {
    private const val FILE = "mayas_media_cache_prefs"
    private const val KEY_MAX_BYTES = "max_bytes"
    private const val KEY_KEEP_DAYS = "keep_days"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_AUTO_DOWNLOAD_ENABLED = "auto_download_enabled"

    private const val GB = 1024L * 1024L * 1024L

    const val UNLIMITED = Long.MAX_VALUE
    const val KEEP_FOREVER = 0

    val limitOptions: List<Long> = listOf(1 * GB, 2 * GB, 5 * GB, UNLIMITED)
    val keepOptions: List<Int> = listOf(3, 7, 30, KEEP_FOREVER)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun maxBytes(context: Context): Long = prefs(context).getLong(KEY_MAX_BYTES, 1 * GB)

    fun setMaxBytes(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_MAX_BYTES, value).apply()
    }

    fun keepDays(context: Context): Int = prefs(context).getInt(KEY_KEEP_DAYS, KEEP_FOREVER)

    fun setKeepDays(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_KEEP_DAYS, value).apply()
    }

    fun wifiOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_WIFI_ONLY, false)

    fun autoDownloadEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_DOWNLOAD_ENABLED, true)

    fun setAutoDownloadEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_DOWNLOAD_ENABLED, enabled).apply()
    }

    fun autoDownloadEnabled(context: Context, type: AutoDownloadType): Boolean {
        val preferences = prefs(context)
        return if (preferences.contains(type.preferenceKey)) {
            preferences.getBoolean(type.preferenceKey, true)
        } else {
            autoDownloadEnabled(context)
        }
    }

    fun setAutoDownloadEnabled(context: Context, type: AutoDownloadType, enabled: Boolean) {
        prefs(context).edit().putBoolean(type.preferenceKey, enabled).apply()
    }

    fun setWifiOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIFI_ONLY, value).apply()
    }

    fun isUnmetered(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun autoDownloadBlocked(context: Context, type: AutoDownloadType? = null): Boolean {
        val enabled = if (type == null) autoDownloadEnabled(context) else autoDownloadEnabled(context, type)
        return !enabled || (wifiOnly(context) && !isUnmetered(context))
    }
}
