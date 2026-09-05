package com.dan1eidtj.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class UserSession(
    val uid: String,
    val email: String,
    val name: String,
    val avatarUrl: String = "",
    val lastActiveAt: Long = 0L
)

class SessionManager(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "sessions")
        private val SESSIONS_KEY = stringPreferencesKey("active_sessions")
        private val ACTIVE_UID_KEY = stringPreferencesKey("active_session_uid")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")


        const val MAX_SESSIONS = 3

        /**
         * Человекочитаемое имя устройства для списка "активных сессий" — например
         * "Honor 90, Android 14" или "Samsung SM-S911B, Android 15". Собирается из
         * системных Build.MANUFACTURER/Build.MODEL, поэтому работает для любого
         * производителя без хардкода конкретных брендов.
         */
        fun deviceDisplayName(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            val name = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }
            return "$name, Android ${Build.VERSION.RELEASE}"
        }
    }

    /**
     * Стабильный ID этой установки приложения — не привязан к конкретному
     * аккаунту, живёт, пока приложение не переустановят или не почистят
     * данные. Используется как id документа в users/{uid}/sessions/{deviceId},
     * чтобы у одного физического устройства всегда был один и тот же "слот"
     * сессии в списке активных сессий, независимо от того, сколько раз на
     * нём логинились.
     */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID_KEY]
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs -> prefs[DEVICE_ID_KEY] = newId }
        return newId
    }


    val sessions: Flow<List<UserSession>> = context.dataStore.data.map { prefs ->
        val json = prefs[SESSIONS_KEY] ?: "[]"
        val list = try {
            Json.decodeFromString<List<UserSession>>(json)
        } catch (e: Exception) {
            emptyList()
        }
        list.sortedByDescending { it.lastActiveAt }
    }

    val activeSessionUid: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_UID_KEY]
    }

    val activeSession: Flow<UserSession?> = combine(sessions, activeSessionUid) { list, uid ->
        list.firstOrNull { it.uid == uid } ?: list.firstOrNull()
    }


    suspend fun saveSession(session: UserSession): Boolean {
        var saved = true
        context.dataStore.edit { prefs ->
            val json = prefs[SESSIONS_KEY] ?: "[]"
            val current = try {
                Json.decodeFromString<List<UserSession>>(json)
            } catch (e: Exception) {
                emptyList()
            }

            val isNewAccount = current.none { it.uid == session.uid }
            if (isNewAccount && current.size >= MAX_SESSIONS) {
                saved = false
                return@edit
            }

            val toStore = session.copy(lastActiveAt = System.currentTimeMillis())
            val updated = current.filterNot { it.uid == session.uid } + toStore
            prefs[SESSIONS_KEY] = Json.encodeToString(updated)
            prefs[ACTIVE_UID_KEY] = session.uid
        }
        return saved
    }
    suspend fun switchSession(uid: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[SESSIONS_KEY] ?: "[]"
            val current = try {
                Json.decodeFromString<List<UserSession>>(json)
            } catch (e: Exception) {
                emptyList()
            }

            val target = current.firstOrNull { it.uid == uid } ?: return@edit
            val updated = current.filterNot { it.uid == uid } + target.copy(lastActiveAt = System.currentTimeMillis())
            prefs[SESSIONS_KEY] = Json.encodeToString(updated)
            prefs[ACTIVE_UID_KEY] = uid
        }
    }

    suspend fun removeSession(uid: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[SESSIONS_KEY] ?: "[]"
            val current = try {
                Json.decodeFromString<List<UserSession>>(json)
            } catch (e: Exception) {
                emptyList()
            }
            val updated = current.filterNot { it.uid == uid }
            prefs[SESSIONS_KEY] = Json.encodeToString(updated)

            if (prefs[ACTIVE_UID_KEY] == uid) {
                val next = updated.maxByOrNull { it.lastActiveAt }
                if (next != null) {
                    prefs[ACTIVE_UID_KEY] = next.uid
                } else {
                    prefs.remove(ACTIVE_UID_KEY)
                }
            }
        }
    }

    suspend fun clearAllSessions() {
        context.dataStore.edit { prefs ->
            prefs[SESSIONS_KEY] = "[]"
            prefs.remove(ACTIVE_UID_KEY)
        }
    }
    suspend fun canAddSession(): Boolean {
        return sessions.first().size < MAX_SESSIONS
    }

    suspend fun hasSession(uid: String): Boolean {
        return sessions.first().any { it.uid == uid }
    }
}