/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.core_ui.emoji

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class EmojiStyle { STANDARD, MAYAS }

object EmojiStyleState {
    private const val FILE = "mayas_appearance"
    private const val KEY = "emoji_style"

    private var initialized = false

    var style by mutableStateOf(EmojiStyle.STANDARD)
        private set

    fun ensureInit(context: Context) {
        if (initialized) return
        initialized = true
        val stored = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, null)
        style = if (stored == EmojiStyle.MAYAS.name) EmojiStyle.MAYAS else EmojiStyle.STANDARD
    }

    fun set(context: Context, newStyle: EmojiStyle) {
        ensureInit(context)
        style = newStyle
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, newStyle.name)
            .apply()
    }
}
