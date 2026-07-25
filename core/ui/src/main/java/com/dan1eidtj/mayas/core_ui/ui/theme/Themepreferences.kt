package com.dan1eidtj.mayas.ui.theme

import android.content.Context
import com.dan1eidtj.mayas.core.ui.theme.MayasColorScheme
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.parseThemeJson
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.toHexMap
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.toJson
import com.dan1eidtj.mayas.core.ui.theme.ThemeConverter.toMayasColorScheme
import org.json.JSONArray
import org.json.JSONObject


object ThemePreferences {
    private const val PREFS_NAME = "mayas_theme_prefs"
    private const val KEY_SELECTED_THEME = "selected_theme"
    private const val KEY_CUSTOM_THEMES = "custom_themes"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    fun saveSelectedScheme(context: Context, scheme: MayasColorScheme) {
        prefs(context).edit()
            .putString(KEY_SELECTED_THEME, scheme.toJson("selected"))
            .apply()
    }


    fun loadSelectedScheme(context: Context): MayasColorScheme? {
        val raw = prefs(context).getString(KEY_SELECTED_THEME, null) ?: return null
        return runCatching { parseThemeJson(raw).second }.getOrNull()
    }


    fun saveCustomThemes(context: Context, themes: List<Pair<String, MayasColorScheme>>) {
        val array = JSONArray()
        themes.forEach { (name, scheme) ->
            val obj = JSONObject()
            obj.put("name", name)
            val colors = JSONObject()
            scheme.toHexMap().forEach { (key, value) -> colors.put(key, value) }
            obj.put("colors", colors)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_CUSTOM_THEMES, array.toString()).apply()
    }


    fun loadCustomThemes(context: Context): List<Pair<String, MayasColorScheme>> {
        val raw = prefs(context).getString(KEY_CUSTOM_THEMES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val colorsJson = obj.getJSONObject("colors")
                val map = mutableMapOf<String, String>()
                colorsJson.keys().forEach { key -> map[key] = colorsJson.getString(key) }
                name to map.toMayasColorScheme()
            }
        }.getOrDefault(emptyList())
    }
}