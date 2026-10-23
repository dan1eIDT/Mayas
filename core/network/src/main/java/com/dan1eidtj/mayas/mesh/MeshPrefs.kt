/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.mesh

import android.content.Context
import android.os.Build
import java.security.SecureRandom
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

internal class MeshPrefs(context: Context) {
    companion object {
        private const val TRUSTED_KEY_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    }

    private val prefs = context.applicationContext.getSharedPreferences("mayas_mesh", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()

    var autoEnable: Boolean
        get() = prefs.getBoolean("autoEnable", false)
        set(value) = prefs.edit().putBoolean("autoEnable", value).apply()

    var displayName: String
        get() = prefs.getString("displayName", null) ?: Build.MODEL.take(24)
        set(value) = prefs.edit().putString("displayName", value.trim().take(24)).apply()

    val ownId: String
        get() {
            val existing = prefs.getString("ownId", null)
            if (existing != null) return existing
            val bytes = ByteArray(4)
            SecureRandom().nextBytes(bytes)
            val created = bytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("ownId", created).apply()
            return created
        }


    fun loadTrustedMeshPublicKey(uid: String, meshId: String): String? {
        if (uid.isBlank() || !meshId.matches(Regex("[0-9a-f]{8}"))) return null
        val key = trustedKeyPreferenceName(uid, meshId)
        val savedAt = prefs.getLong("${key}_savedAt", 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > TRUSTED_KEY_MAX_AGE_MS) {
            removeTrustedMeshPublicKey(uid, meshId)
            return null
        }
        return prefs.getString(key, null)
    }

    fun saveTrustedMeshPublicKey(uid: String, meshId: String, publicKey: String) {
        if (uid.isBlank() || !meshId.matches(Regex("[0-9a-f]{8}")) || publicKey.isBlank() || publicKey.length > 1024) return
        val key = trustedKeyPreferenceName(uid, meshId)
        prefs.edit()
            .putString(key, publicKey)
            .putLong("${key}_savedAt", System.currentTimeMillis())
            .apply()
    }

    fun removeTrustedMeshPublicKey(uid: String, meshId: String) {
        if (uid.isBlank() || !meshId.matches(Regex("[0-9a-f]{8}"))) return
        val key = trustedKeyPreferenceName(uid, meshId)
        prefs.edit().remove(key).remove("${key}_savedAt").apply()
    }

    private fun trustedKeyPreferenceName(uid: String, meshId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$uid\n$meshId".toByteArray(Charsets.UTF_8))
        val suffix = digest.take(16).joinToString("") { "%02x".format(it) }
        return "trustedMeshKey_$suffix"
    }

    fun loadMessages(): List<MeshMessage> = runCatching {
        val raw = prefs.getString("messageHistory", "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val srcId = item.optString("srcId")
                val text = item.optString("text")
                if (id.isBlank() || srcId.isBlank() || text.isBlank()) continue
                add(
                    MeshMessage(
                        id = id,
                        srcId = srcId,
                        srcName = item.optString("srcName"),
                        dst = item.optString("dst"),
                        text = text.take(1000),
                        timestamp = item.optLong("timestamp"),
                        outgoing = item.optBoolean("outgoing"),
                        hops = item.optInt("hops"),
                        srcUid = item.optString("srcUid"),
                        dstUid = item.optString("dstUid"),
                        privateChat = item.optBoolean("privateChat"),
                        identityVerified = item.optBoolean("identityVerified", false),
                        delivered = item.optBoolean("delivered"),
                        deliveryFailed = item.optBoolean("deliveryFailed"),
                        replyToText = item.optString("replyToText").takeIf { it.isNotBlank() },
                        replyToName = item.optString("replyToName").takeIf { it.isNotBlank() },
                        receivedAt = item.optLong("receivedAt").coerceAtLeast(0L),
                        deliveredAt = item.optLong("deliveredAt").coerceAtLeast(0L),
                        attempts = item.optInt("attempts").coerceIn(0, 6)
                    )
                )
            }
        }.takeLast(200)
    }.getOrDefault(emptyList())

    fun saveMessages(messages: List<MeshMessage>) {
        val array = JSONArray()
        messages.takeLast(200).forEach { message ->
            array.put(
                JSONObject().apply {
                    put("id", message.id)
                    put("srcId", message.srcId)
                    put("srcName", message.srcName)
                    put("dst", message.dst)
                    put("text", message.text.take(1000))
                    put("timestamp", message.timestamp)
                    put("outgoing", message.outgoing)
                    put("hops", message.hops)
                    put("srcUid", message.srcUid)
                    put("dstUid", message.dstUid)
                    put("privateChat", message.privateChat)
                    put("identityVerified", message.identityVerified)
                    put("delivered", message.delivered)
                    put("deliveryFailed", message.deliveryFailed)
                    put("replyToText", message.replyToText.orEmpty())
                    put("replyToName", message.replyToName.orEmpty())
                    put("receivedAt", message.receivedAt)
                    put("deliveredAt", message.deliveredAt)
                    put("attempts", message.attempts)
                }
            )
        }
        prefs.edit().putString("messageHistory", array.toString()).apply()
    }

    fun loadPendingDirectMessages(): List<MeshPendingDirectMessage> = runCatching {
        val raw = prefs.getString("pendingDirectMessages", "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val dstUid = item.optString("dstUid")
                val dstMeshId = item.optString("dstMeshId")
                val packetJson = item.optString("packetJson")
                if (id.isBlank() || id.length > 80 || dstUid.isBlank() || dstUid.length > 128 ||
                    dstMeshId.length != 8 || !dstMeshId.all { it in '0'..'9' || it in 'a'..'f' } ||
                    packetJson.isBlank() || packetJson.length > 8192
                ) continue
                val packet = runCatching { JSONObject(packetJson) }.getOrNull() ?: continue
                if (packet.optString("t") != "msg" || packet.optString("id") != id ||
                    packet.optString("dstUid") != dstUid || packet.optString("dst") != dstMeshId ||
                    !packet.optBoolean("privateChat", false)
                ) continue
                add(
                    MeshPendingDirectMessage(
                        id = id,
                        dstUid = dstUid,
                        dstMeshId = dstMeshId,
                        packetJson = packetJson,
                        createdAt = item.optLong("createdAt"),
                        attemptCount = item.optInt("attemptCount").coerceIn(0, 6),
                        lastAttemptAt = item.optLong("lastAttemptAt").coerceAtLeast(0L)
                    )
                )
            }
        }.takeLast(100)
    }.getOrDefault(emptyList())

    fun savePendingDirectMessages(messages: List<MeshPendingDirectMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach { message ->
            if (message.packetJson.length > 8192) return@forEach
            array.put(
                JSONObject().apply {
                    put("id", message.id)
                    put("dstUid", message.dstUid)
                    put("dstMeshId", message.dstMeshId)
                    put("packetJson", message.packetJson)
                    put("createdAt", message.createdAt)
                    put("attemptCount", message.attemptCount.coerceIn(0, 6))
                    put("lastAttemptAt", message.lastAttemptAt.coerceAtLeast(0L))
                }
            )
        }
        prefs.edit().putString("pendingDirectMessages", array.toString()).apply()
    }

}
