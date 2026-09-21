/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data

import android.content.Context

object NotificationPrefs {
    private const val PREFS_NAME = "mayas_notification_prefs"

    const val KEY_PUSH_ENABLED = "pushEnabled"
    const val KEY_SOUND = "notifSound"
    const val KEY_VIBRATION = "notifVibration"
    const val KEY_PREVIEW = "notifPreview"
    const val KEY_GROUP_MESSAGES = "notifGroupMessages"
    const val KEY_CALLS = "notifCalls"

    fun pushEnabled(context: Context): Boolean = get(context, KEY_PUSH_ENABLED)
    fun soundEnabled(context: Context): Boolean = get(context, KEY_SOUND)
    fun vibrationEnabled(context: Context): Boolean = get(context, KEY_VIBRATION)
    fun previewEnabled(context: Context): Boolean = get(context, KEY_PREVIEW)
    fun groupMessagesEnabled(context: Context): Boolean = get(context, KEY_GROUP_MESSAGES)
    fun callsEnabled(context: Context): Boolean = get(context, KEY_CALLS)

    fun set(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    private fun get(context: Context, key: String): Boolean =
        prefs(context).getBoolean(key, true)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
