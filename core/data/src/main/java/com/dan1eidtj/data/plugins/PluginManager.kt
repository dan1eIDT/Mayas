/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data.plugins

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object PluginManager {
    private const val PREFS_NAME = "mayas_plugins"
    private const val MAX_TEXT_LENGTH = 4096
    private const val TAG = "PluginManager"

    private val plugins = LinkedHashMap<String, MayasPlugin>()
    private var prefs: SharedPreferences? = null

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (plugins.isEmpty()) {
            OfficialPlugins.all().forEach { register(it) }
        }
    }

    @Synchronized
    fun register(plugin: MayasPlugin) {
        val id = plugin.manifest.id
        require(id.matches(Regex("^[a-z0-9_.]{3,64}$"))) { "Invalid plugin id: $id" }
        plugins[id] = plugin
    }

    @Synchronized
    fun installed(): List<MayasPlugin> = plugins.values.toList()

    @Synchronized
    fun find(id: String): MayasPlugin? = plugins[id]

    fun isEnabled(id: String): Boolean = prefs?.getBoolean("enabled_$id", false) ?: false

    fun setEnabled(id: String, enabled: Boolean) {
        val plugin = find(id) ?: return
        val editor = prefs?.edit() ?: return
        editor.putBoolean("enabled_$id", enabled)
        if (enabled) {
            editor.putStringSet("granted_$id", plugin.manifest.permissions.map { it.id }.toSet())
        } else {
            editor.remove("granted_$id")
        }
        editor.apply()
    }

    fun hasPermission(id: String, permission: PluginPermission): Boolean {
        val plugin = find(id) ?: return false
        if (permission !in plugin.manifest.permissions) return false
        return prefs?.getStringSet("granted_$id", emptySet())?.contains(permission.id) == true
    }

    fun getSetting(id: String, key: String): String {
        val spec = find(id)?.manifest?.settings?.firstOrNull { it.key == key } ?: return ""
        return prefs?.getString("setting_${id}_$key", spec.defaultValue) ?: spec.defaultValue
    }

    fun setSetting(id: String, key: String, value: String) {
        val spec = find(id)?.manifest?.settings?.firstOrNull { it.key == key } ?: return
        val clean = when (spec.type) {
            PluginSettingType.BOOLEAN -> if (value == "true") "true" else "false"
            PluginSettingType.TEXT -> value.take(200)
        }
        prefs?.edit()?.putString("setting_${id}_$key", clean)?.apply()
    }

    fun transformOutgoingText(text: String): String {
        var current = text
        for (plugin in installed()) {
            val id = plugin.manifest.id
            if (!isEnabled(id) || !hasPermission(id, PluginPermission.TRANSFORM_OUTGOING_TEXT)) continue
            try {
                val result = plugin.transformOutgoingText(current, PluginContext(id, this))
                if (result.isNotBlank() && result.length <= MAX_TEXT_LENGTH) current = result
            } catch (e: Exception) {
                Log.w(TAG, "Plugin $id failed in transformOutgoingText", e)
            }
        }
        return current
    }
}
