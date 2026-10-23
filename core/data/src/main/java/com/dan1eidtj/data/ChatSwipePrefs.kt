/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ChatSwipeAction(val id: String) {
    READ("read"),
    ARCHIVE("archive"),
    MUTE("mute"),
    PIN("pin");

    companion object {
        fun fromId(id: String?): ChatSwipeAction =
            values().firstOrNull { it.id == id } ?: ARCHIVE
    }
}

object ChatSwipePrefs {
    private const val FILE = "chat_swipe_prefs"
    private const val KEY_LEFT_ACTION = "swipe_left_action"

    private val _action = MutableStateFlow(ChatSwipeAction.ARCHIVE)
    val action: StateFlow<ChatSwipeAction> = _action.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInit(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            _action.value = ChatSwipeAction.fromId(prefs(context).getString(KEY_LEFT_ACTION, null))
            initialized = true
        }
    }

    fun set(context: Context, value: ChatSwipeAction) {
        prefs(context).edit().putString(KEY_LEFT_ACTION, value.id).apply()
        _action.value = value
        initialized = true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
