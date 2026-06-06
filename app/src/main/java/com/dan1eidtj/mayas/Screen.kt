package com.dan1eidtj.mayas

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Chats : Screen("chats")
    object Credits : Screen("credits")
    object Chat : Screen("chat/{chatId}") {
        fun create(chatId: String) = "chat/$chatId"
    }
    object Profile : Screen("profile/{uid}/{isGroup}") {
        fun create(uid: String, isGroup: Boolean = false) = "profile/$uid/$isGroup"
    }

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            return listOf(uid1, uid2).sorted().joinToString("_")
        }
    }
}