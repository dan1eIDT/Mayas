/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data.plugins

import com.dan1eidtj.data.R

object OfficialPlugins {
    fun all(): List<MayasPlugin> = listOf(MessageSignaturePlugin())
}

class MessageSignaturePlugin : MayasPlugin {
    override val manifest = PluginManifest(
        id = "mayas.official.signature",
        nameRes = R.string.plugin_signature_name,
        version = "1.0.0",
        descriptionRes = R.string.plugin_signature_desc,
        author = "ProjectIDT",
        origin = PluginOrigin.OFFICIAL,
        permissions = listOf(PluginPermission.TRANSFORM_OUTGOING_TEXT),
        settings = listOf(
            PluginSettingSpec("signature", R.string.plugin_signature_setting_text, PluginSettingType.TEXT, ""),
            PluginSettingSpec("new_line", R.string.plugin_signature_setting_newline, PluginSettingType.BOOLEAN, "true")
        )
    )

    override fun transformOutgoingText(text: String, context: PluginContext): String {
        val signature = context.settingString("signature").trim()
        if (signature.isEmpty()) return text
        val separator = if (context.settingBoolean("new_line")) "\n" else " "
        return text + separator + signature
    }
}
