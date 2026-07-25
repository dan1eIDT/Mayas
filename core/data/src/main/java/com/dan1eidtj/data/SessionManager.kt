package com.dan1eidtj.data

import android.content.Context
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


        const val MAX_SESSIONS = 3
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