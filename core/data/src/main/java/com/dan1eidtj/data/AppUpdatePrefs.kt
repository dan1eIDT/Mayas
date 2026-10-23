/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data

import android.content.Context

object AppUpdatePrefs {
    private const val PREFS_NAME = "mayas_update_prefs"
    private const val KEY_LAST_SEEN_VERSION = "lastSeenVersion"

    fun lastSeenVersion(context: Context): String? =
        prefs(context).getString(KEY_LAST_SEEN_VERSION, null)

    fun markVersionSeen(context: Context, version: String) {
        prefs(context).edit().putString(KEY_LAST_SEEN_VERSION, version).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
