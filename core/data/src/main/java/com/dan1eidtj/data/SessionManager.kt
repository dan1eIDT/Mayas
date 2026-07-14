package com.dan1eidtj.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserSession(
    val uid: String,
    val email: String,
    val name: String,
    val avatarUrl: String = ""
)

class SessionManager(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "sessions")
        private val SESSIONS_KEY = stringPreferencesKey("active_sessions")
    }

    val sessions: Flow<List<UserSession>> = context.dataStore.data.map { prefs ->
        val json = prefs[SESSIONS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<UserSession>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            val json = prefs[SESSIONS_KEY] ?: "[]"
            val current = try {
                Json.decodeFromString<List<UserSession>>(json)
            } catch (e: Exception) {
                emptyList()
            }
            val updated = (current.filterNot { it.uid == session.uid } + session)
            prefs[SESSIONS_KEY] = Json.encodeToString(updated)
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
        }
    }
}
