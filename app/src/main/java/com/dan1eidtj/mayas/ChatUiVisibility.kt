/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas

import java.util.concurrent.atomic.AtomicReference

object ChatUiVisibility {

    private val openChatId = AtomicReference<String?>(null)

    fun setOpenChat(chatId: String?) {
        openChatId.set(chatId)
    }

    fun isChatOpen(chatId: String): Boolean {
        return openChatId.get() == chatId
    }
}
