package com.dan1eidtj.mayas.core.ui.theme

import android.content.Context

object LayoutPreferences {

    private const val PREFS_NAME = "mayas_layout_prefs"

    private const val KEY_HOME_SEARCH_POSITION = "home_search_position"
    private const val KEY_HOME_FOLDERS_POSITION = "home_folders_position"
    private const val KEY_HOME_FAB_POSITION = "home_fab_position"
    private const val KEY_HOME_AVATAR_POSITION = "home_avatar_position"
    private const val KEY_HOME_COMPACT_LIST = "home_compact_list"
    private const val KEY_HOME_SHOW_SEARCH_FIELD = "home_show_search_field"
    private const val KEY_HOME_SHOW_ADD_FRIEND = "home_show_add_friend"

    private const val KEY_SIDEBAR_PROFILE_POSITION = "sidebar_profile_position"
    private const val KEY_SIDEBAR_ICON_POSITION = "sidebar_icon_position"
    private const val KEY_SIDEBAR_ITEMS_ORDER = "sidebar_items_order"
    private const val KEY_SIDEBAR_COMPACT_MODE = "sidebar_compact_mode"
    private const val KEY_SIDEBAR_SHOW_QUICK_ACTIONS = "sidebar_show_quick_actions"
    private const val KEY_SIDEBAR_SHOW_FOLDERS = "sidebar_show_folders"
    private const val KEY_SIDEBAR_SHOW_APP_SECTION = "sidebar_show_app_section"
    private const val KEY_SIDEBAR_CUSTOM_LINKS = "sidebar_custom_links"
    private const val KEY_SIDEBAR_SHOW_PINNED_CHATS = "sidebar_show_pinned_chats"
    private const val KEY_SIDEBAR_PINNED_CHAT_IDS = "sidebar_pinned_chat_ids"

    private const val ITEMS_DELIMITER = "||"
    private const val LINK_FIELD_DELIMITER = "::"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHomeScreenLayoutPrefs(context: Context): HomeScreenLayoutPrefs {
        val p = prefs(context)
        val defaults = HomeScreenLayoutPrefs()
        return HomeScreenLayoutPrefs(
            searchPosition = p.getString(KEY_HOME_SEARCH_POSITION, null)
                ?.let { runCatching { VerticalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.searchPosition,
            foldersPosition = p.getString(KEY_HOME_FOLDERS_POSITION, null)
                ?.let { runCatching { VerticalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.foldersPosition,
            fabPosition = p.getString(KEY_HOME_FAB_POSITION, null)
                ?.let { runCatching { HorizontalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.fabPosition,
            avatarPosition = p.getString(KEY_HOME_AVATAR_POSITION, null)
                ?.let { runCatching { HorizontalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.avatarPosition,
            compactList = p.getBoolean(KEY_HOME_COMPACT_LIST, defaults.compactList),
            showSearchField = p.getBoolean(KEY_HOME_SHOW_SEARCH_FIELD, defaults.showSearchField),
            showAddFriendButton = p.getBoolean(KEY_HOME_SHOW_ADD_FRIEND, defaults.showAddFriendButton)
        )
    }

    fun saveHomeScreenLayoutPrefs(context: Context, prefs: HomeScreenLayoutPrefs) {
        prefs(context).edit()
            .putString(KEY_HOME_SEARCH_POSITION, prefs.searchPosition.name)
            .putString(KEY_HOME_FOLDERS_POSITION, prefs.foldersPosition.name)
            .putString(KEY_HOME_FAB_POSITION, prefs.fabPosition.name)
            .putString(KEY_HOME_AVATAR_POSITION, prefs.avatarPosition.name)
            .putBoolean(KEY_HOME_COMPACT_LIST, prefs.compactList)
            .putBoolean(KEY_HOME_SHOW_SEARCH_FIELD, prefs.showSearchField)
            .putBoolean(KEY_HOME_SHOW_ADD_FRIEND, prefs.showAddFriendButton)
            .apply()
    }

    fun loadSidebarLayoutPrefs(context: Context): SidebarLayoutPrefs {
        val p = this.prefs(context)
        val defaults = SidebarLayoutPrefs()
        val savedOrder = p.getString(KEY_SIDEBAR_ITEMS_ORDER, null)
            ?.split(ITEMS_DELIMITER)
            ?.filter { it.isNotBlank() }

        // Если сохранённый порядок повреждён/устарел (набор пунктов изменился) — используем дефолт
        val itemsOrder = if (savedOrder != null && savedOrder.toSet() == defaults.itemsOrder.toSet()) {
            savedOrder
        } else {
            defaults.itemsOrder
        }

        return SidebarLayoutPrefs(
            profileBlockPosition = p.getString(KEY_SIDEBAR_PROFILE_POSITION, null)
                ?.let { runCatching { VerticalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.profileBlockPosition,
            actionsIconPosition = p.getString(KEY_SIDEBAR_ICON_POSITION, null)
                ?.let { runCatching { HorizontalSlot.valueOf(it) }.getOrNull() }
                ?: defaults.actionsIconPosition,
            itemsOrder = itemsOrder,
            compactMode = p.getBoolean(KEY_SIDEBAR_COMPACT_MODE, defaults.compactMode),
            showQuickActions = p.getBoolean(KEY_SIDEBAR_SHOW_QUICK_ACTIONS, defaults.showQuickActions),
            showFolders = p.getBoolean(KEY_SIDEBAR_SHOW_FOLDERS, defaults.showFolders),
            showAppSection = p.getBoolean(KEY_SIDEBAR_SHOW_APP_SECTION, defaults.showAppSection),
            customLinks = p.getString(KEY_SIDEBAR_CUSTOM_LINKS, null)
                ?.split(ITEMS_DELIMITER)
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { entry ->
                    val parts = entry.split(LINK_FIELD_DELIMITER)
                    if (parts.size == 3) {
                        SidebarCustomLink(id = parts[0], label = parts[1], url = parts[2])
                    } else null
                }
                ?: defaults.customLinks,
            showPinnedChats = p.getBoolean(KEY_SIDEBAR_SHOW_PINNED_CHATS, defaults.showPinnedChats),
            pinnedChatIds = p.getString(KEY_SIDEBAR_PINNED_CHAT_IDS, null)
                ?.split(ITEMS_DELIMITER)
                ?.filter { it.isNotBlank() }
                ?: defaults.pinnedChatIds
        )
    }

    fun saveSidebarLayoutPrefs(context: Context, prefs: SidebarLayoutPrefs) {
        this.prefs(context).edit()
            .putString(KEY_SIDEBAR_PROFILE_POSITION, prefs.profileBlockPosition.name)
            .putString(KEY_SIDEBAR_ICON_POSITION, prefs.actionsIconPosition.name)
            .putString(KEY_SIDEBAR_ITEMS_ORDER, prefs.itemsOrder.joinToString(ITEMS_DELIMITER))
            .putBoolean(KEY_SIDEBAR_COMPACT_MODE, prefs.compactMode)
            .putBoolean(KEY_SIDEBAR_SHOW_QUICK_ACTIONS, prefs.showQuickActions)
            .putBoolean(KEY_SIDEBAR_SHOW_FOLDERS, prefs.showFolders)
            .putBoolean(KEY_SIDEBAR_SHOW_APP_SECTION, prefs.showAppSection)
            .putString(
                KEY_SIDEBAR_CUSTOM_LINKS,
                prefs.customLinks.joinToString(ITEMS_DELIMITER) { link ->
                    "${link.id}$LINK_FIELD_DELIMITER${link.label}$LINK_FIELD_DELIMITER${link.url}"
                }
            )
            .putBoolean(KEY_SIDEBAR_SHOW_PINNED_CHATS, prefs.showPinnedChats)
            .putString(KEY_SIDEBAR_PINNED_CHAT_IDS, prefs.pinnedChatIds.joinToString(ITEMS_DELIMITER))
            .apply()
    }
}
