/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object OfflineDataStore {

    fun currentUid(): String = MayasDatabase.currentUid()

    fun currentAccountFiles(context: Context): List<File> {
        val name = MayasDatabase.nameFor(MayasDatabase.currentUid())
        val dir = context.applicationContext.getDatabasePath(name).parentFile ?: return emptyList()
        return dir.listFiles { file -> file.name == name || file.name.startsWith("$name-") }?.toList() ?: emptyList()
    }

    suspend fun clearCurrentAccount(context: Context) = withContext(Dispatchers.IO) {
        MayasDatabase.getDatabase(context).clearAllTables()
    }

    fun deleteAccountData(context: Context, uid: String) {
        MayasDatabase.closeAndDelete(context, uid)
    }
}
