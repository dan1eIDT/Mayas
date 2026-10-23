/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ChatArchive {
    private const val FILE = "chat_archive_prefs"
    private const val FIELD = "archivedChats"

    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    private var boundUid: String? = null
    private var registration: ListenerRegistration? = null

    @Synchronized
    fun bind(context: Context, uid: String) {
        if (boundUid == uid) return
        registration?.remove()
        boundUid = uid
        val prefs = prefs(context)
        _ids.value = prefs.getStringSet(key(uid), emptySet())?.toSet() ?: emptySet()
        registration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                val remote = (doc.get(FIELD) as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toSet()
                    ?: emptySet()
                _ids.value = remote
                prefs.edit().putStringSet(key(uid), remote).apply()
            }
    }

    fun isArchived(chatId: String): Boolean = _ids.value.contains(chatId)

    fun setArchived(context: Context, uid: String, chatIds: Collection<String>, archived: Boolean) {
        if (chatIds.isEmpty()) return
        val updated = if (archived) _ids.value + chatIds else _ids.value - chatIds.toSet()
        _ids.value = updated
        prefs(context).edit().putStringSet(key(uid), updated).apply()
        val args = chatIds.toTypedArray()
        val op = if (archived) FieldValue.arrayUnion(*args) else FieldValue.arrayRemove(*args)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf(FIELD to op), SetOptions.merge())
    }

    private fun key(uid: String) = "archived_$uid"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
