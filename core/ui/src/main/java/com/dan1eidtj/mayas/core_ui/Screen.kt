package com.dan1eidtj.mayas.core_ui

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
    object Premium : Screen("premium")
    object Shop : Screen("shop")
    object AdminShop : Screen("admin_shop")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object Customization : Screen("customization")
    object Themes : Screen("themes")
    object HomeScreenLayout : Screen("home_screen_layout")
    object SidebarLayout : Screen("sidebar_layout")
    object ThemeEditor : Screen("theme_editor?themeName={themeName}") {
        fun create(themeName: String? = null) =
            if (themeName != null) "theme_editor?themeName=$themeName" else "theme_editor"
    }

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            return listOf(uid1, uid2).sorted().joinToString("_")
        }
    }
}