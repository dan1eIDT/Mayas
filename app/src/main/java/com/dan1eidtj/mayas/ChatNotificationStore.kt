package com.dan1eidtj.mayas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject


object ChatNotificationStore {
    private const val PREFS_NAME = "mayas_chat_notifications"


    private const val MAX_MESSAGES = 8

    data class StoredMessage(
        val text: String,
        val timestampMs: Long,

        val mine: Boolean,
    )


    fun appendMessage(
        context: Context,
        chatId: String,
        text: String,
        timestampMs: Long,
        mine: Boolean,
    ): List<StoredMessage> {
        val updated = (getMessages(context, chatId) + StoredMessage(text, timestampMs, mine))
            .takeLast(MAX_MESSAGES)
        save(context, chatId, updated)
        return updated
    }

    fun getMessages(context: Context, chatId: String): List<StoredMessage> {
        val raw = prefs(context).getString(chatId, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                StoredMessage(
                    text = obj.getString("text"),
                    timestampMs = obj.getLong("ts"),
                    mine = obj.getBoolean("mine")
                )
            }
        }.getOrDefault(emptyList())
    }


    fun clear(context: Context, chatId: String) {
        prefs(context).edit().remove(chatId).apply()
    }

    private fun save(context: Context, chatId: String, messages: List<StoredMessage>) {
        val array = JSONArray()
        messages.forEach { msg ->
            array.put(
                JSONObject().apply {
                    put("text", msg.text)
                    put("ts", msg.timestampMs)
                    put("mine", msg.mine)
                }
            )
        }
        prefs(context).edit().putString(chatId, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
