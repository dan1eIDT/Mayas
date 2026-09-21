/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [ChatEntity::class, MessageEntity::class, OutboxEntity::class],
    version = 19,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class MayasDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        private const val LEGACY_NAME = "mayas_chats_database"
        private const val PREFIX = "mayas_db_"
        private const val ANONYMOUS = "anonymous"

        private val instances = ConcurrentHashMap<String, MayasDatabase>()
        @Volatile
        private var legacyRemoved = false

        fun nameFor(uid: String): String = PREFIX + uid

        fun currentUid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ANONYMOUS

        fun getDatabase(context: Context): MayasDatabase = getDatabaseFor(context, currentUid())

        fun getDatabaseFor(context: Context, uid: String): MayasDatabase {
            val app = context.applicationContext
            if (!legacyRemoved) {
                synchronized(this) {
                    if (!legacyRemoved) {
                        app.deleteDatabase(LEGACY_NAME)
                        legacyRemoved = true
                    }
                }
            }
            return instances[uid] ?: synchronized(this) {
                instances[uid] ?: Room.databaseBuilder(app, MayasDatabase::class.java, nameFor(uid))
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instances[uid] = it }
            }
        }

        fun databaseFiles(context: Context): List<File> {
            val dir = context.applicationContext.getDatabasePath(nameFor("probe")).parentFile ?: return emptyList()
            return dir.listFiles { file -> file.name.startsWith(PREFIX) }?.toList() ?: emptyList()
        }

        fun closeAndDelete(context: Context, uid: String) {
            instances.remove(uid)?.close()
            context.applicationContext.deleteDatabase(nameFor(uid))
        }
    }
}
