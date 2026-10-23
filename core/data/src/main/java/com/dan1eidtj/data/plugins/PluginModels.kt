/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data.plugins

import androidx.annotation.StringRes
import com.dan1eidtj.data.R

enum class PluginPermission(val id: String, @StringRes val labelRes: Int) {
    TRANSFORM_OUTGOING_TEXT("message.transform_outgoing", R.string.plugin_perm_transform_outgoing)
}

enum class PluginOrigin { OFFICIAL, THIRD_PARTY }

enum class PluginSettingType { BOOLEAN, TEXT }

data class PluginSettingSpec(
    val key: String,
    @StringRes val labelRes: Int,
    val type: PluginSettingType,
    val defaultValue: String
)

data class PluginManifest(
    val id: String,
    @StringRes val nameRes: Int,
    val version: String,
    @StringRes val descriptionRes: Int,
    val author: String,
    val origin: PluginOrigin,
    val permissions: List<PluginPermission>,
    val settings: List<PluginSettingSpec> = emptyList()
)

class PluginContext internal constructor(
    private val pluginId: String,
    private val manager: PluginManager
) {
    fun settingString(key: String): String = manager.getSetting(pluginId, key)

    fun settingBoolean(key: String): Boolean = manager.getSetting(pluginId, key) == "true"
}

interface MayasPlugin {
    val manifest: PluginManifest

    fun transformOutgoingText(text: String, context: PluginContext): String = text
}
