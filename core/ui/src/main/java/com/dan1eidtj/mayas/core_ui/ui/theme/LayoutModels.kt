package com.dan1eidtj.mayas.core.ui.theme

/**
 * Модели положений элементов интерфейса.
 * Лежат в core (а не в settings), т.к. отсюда их использует LayoutPreferences,
 * а settings и так зависит от core — так избегаем циклической зависимости модулей.
 */

enum class VerticalSlot(val label: String) { TOP("Сверху"), BOTTOM("Снизу") }
enum class HorizontalSlot(val label: String) { START("Слева"), END("Справа") }

data class HomeScreenLayoutPrefs(
    val searchPosition: VerticalSlot = VerticalSlot.TOP,
    val foldersPosition: VerticalSlot = VerticalSlot.TOP,
    val fabPosition: HorizontalSlot = HorizontalSlot.END,
    val avatarPosition: HorizontalSlot = HorizontalSlot.START,
    val compactList: Boolean = false,
    val showSearchField: Boolean = true,
    val showAddFriendButton: Boolean = true
)

data class SidebarCustomLink(
    val id: String,
    val label: String,
    val url: String
)

data class SidebarLayoutPrefs(
    val profileBlockPosition: VerticalSlot = VerticalSlot.TOP,
    val actionsIconPosition: HorizontalSlot = HorizontalSlot.START,
    val itemsOrder: List<String> = listOf("Мой профиль", "Папки чатов", "Настройки", "О приложении", "Выйти"),
    val compactMode: Boolean = false,
    val showQuickActions: Boolean = true,
    val showFolders: Boolean = true,
    val showAppSection: Boolean = true,
    val customLinks: List<SidebarCustomLink> = emptyList(),
    val showPinnedChats: Boolean = true,
    val pinnedChatIds: List<String> = emptyList()
)
