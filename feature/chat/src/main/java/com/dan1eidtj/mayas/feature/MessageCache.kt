/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import com.dan1eidtj.mayas.db.MessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object MessageCache {

    fun toEntity(chatId: String, message: Message): MessageEntity =
        MessageEntity(
            messageId = message.id,
            chatId = chatId,
            text = message.text.orEmpty(),
            senderId = message.senderId,
            senderName = message.senderName,
            timestamp = message.timestamp?.time ?: 0L,
            replyToText = message.replyToText,
            replyToName = message.replyToName,
            status = message.status,
            readBy = message.readBy,
            mediaUrl = message.mediaUrl,
            mediaUrls = message.mediaUrls,
            mediaTypes = message.mediaTypes,
            isPremium = message.isPremium,
            messageStyle = message.messageStyle,
            messageEffect = message.messageEffect,
            circleVideoUrl = message.circleVideoUrl,
            circleVideoDuration = message.circleVideoDuration,
            forwardedFromName = message.forwardedFromName,
            isEdited = message.isEdited,
            editedAt = message.editedAt?.time,
            payloadJson = encode(message)
        )

    fun fromEntity(entity: MessageEntity): Message? {
        if (entity.payloadJson.isBlank()) return null
        return try {
            decode(entity.messageId, JSONObject(entity.payloadJson))
        } catch (e: Exception) {
            null
        }
    }

    private fun encode(m: Message): String {
        val json = JSONObject()
        json.put("senderId", m.senderId)
        json.put("senderName", m.senderName)
        json.putOpt("text", m.text)
        json.putOpt("mediaUrl", m.mediaUrl)
        json.putOpt("mediaKey", m.mediaKey)
        json.put("mediaUrls", JSONArray(m.mediaUrls))
        json.put("mediaTypes", JSONArray(m.mediaTypes))
        json.put("timestamp", m.timestamp?.time ?: 0L)
        json.put("readBy", JSONArray(m.readBy))
        json.putOpt("replyToText", m.replyToText)
        json.putOpt("replyToName", m.replyToName)
        json.put("isPremium", m.isPremium)
        json.putOpt("messageStyle", m.messageStyle)
        json.putOpt("messageEffect", m.messageEffect)
        json.put("status", m.status)
        val reactions = JSONObject()
        m.reactions.forEach { (uid, value) -> reactions.put(uid, value) }
        json.put("reactions", reactions)
        json.putOpt("voiceUrl", m.voiceUrl)
        json.putOpt("voiceKey", m.voiceKey)
        json.put("voiceDuration", m.voiceDuration)
        json.putOpt("circleVideoUrl", m.circleVideoUrl)
        json.put("circleVideoDuration", m.circleVideoDuration)
        json.put("type", m.type)
        json.putOpt("systemAction", m.systemAction)
        json.putOpt("systemRefMessageId", m.systemRefMessageId)
        json.putOpt("callType", m.callType)
        json.putOpt("callStatus", m.callStatus)
        json.put("callDurationSec", m.callDurationSec)
        json.putOpt("forwardedFromName", m.forwardedFromName)
        json.put("viewedBy", JSONArray(m.viewedBy))
        json.put("ttlSeconds", m.ttlSeconds)
        json.put("expireAt", m.expireAt?.time ?: 0L)
        json.put("isSilent", m.isSilent)
        json.put("isRead", m.isRead)
        json.put("scheduledFor", m.scheduledFor?.time ?: 0L)
        json.put("messageState", m.messageState)
        json.put("isEdited", m.isEdited)
        json.put("editedAt", m.editedAt?.time ?: 0L)
        return json.toString()
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONObject.strings(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { array.getString(it) }
    }

    private fun JSONObject.dateOrNull(name: String): Date? {
        val value = optLong(name, 0L)
        return if (value > 0L) Date(value) else null
    }

    private fun decode(id: String, json: JSONObject): Message {
        val reactionsJson = json.optJSONObject("reactions")
        val reactions = mutableMapOf<String, String>()
        reactionsJson?.keys()?.forEach { key -> reactions[key] = reactionsJson.getString(key) }

        return Message(
            id = id,
            senderId = json.optString("senderId"),
            senderName = json.optString("senderName"),
            text = json.optNullableString("text"),
            mediaUrl = json.optNullableString("mediaUrl"),
            mediaKey = json.optNullableString("mediaKey"),
            mediaUrls = json.strings("mediaUrls"),
            mediaTypes = json.strings("mediaTypes"),
            timestamp = json.dateOrNull("timestamp"),
            readBy = json.strings("readBy"),
            replyToText = json.optNullableString("replyToText"),
            replyToName = json.optNullableString("replyToName"),
            isPremium = json.optBoolean("isPremium"),
            messageStyle = json.optNullableString("messageStyle"),
            messageEffect = json.optNullableString("messageEffect"),
            status = json.optInt("status", 1),
            reactions = reactions,
            voiceUrl = json.optNullableString("voiceUrl"),
            voiceKey = json.optNullableString("voiceKey"),
            voiceDuration = json.optInt("voiceDuration"),
            circleVideoUrl = json.optNullableString("circleVideoUrl"),
            circleVideoDuration = json.optInt("circleVideoDuration"),
            type = json.optString("type", MessageType.TEXT),
            systemAction = json.optNullableString("systemAction"),
            systemRefMessageId = json.optNullableString("systemRefMessageId"),
            callType = json.optNullableString("callType"),
            callStatus = json.optNullableString("callStatus"),
            callDurationSec = json.optInt("callDurationSec"),
            forwardedFromName = json.optNullableString("forwardedFromName"),
            viewedBy = json.strings("viewedBy"),
            ttlSeconds = json.optLong("ttlSeconds"),
            expireAt = json.dateOrNull("expireAt"),
            isSilent = json.optBoolean("isSilent"),
            isRead = json.optBoolean("isRead"),
            scheduledFor = json.dateOrNull("scheduledFor"),
            messageState = json.optString("messageState", MessageState.SENT),
            isEdited = json.optBoolean("isEdited"),
            editedAt = json.dateOrNull("editedAt")
        )
    }
}
