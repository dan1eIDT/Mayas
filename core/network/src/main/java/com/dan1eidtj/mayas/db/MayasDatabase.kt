package com.dan1eidtj.mayas.db // Твой пакет, проверь его!

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Аннотация @Database говорит: "Это главная база данных".
// В entities перечисляем все таблицы. version = 1 — это первая версия нашей БД.
@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MayasDatabase : RoomDatabase() {

    // Объявляем абстрактные функции для получения пультов управления (DAO).
    // Room сам сгенерирует для них рабочий код под капотом.
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: MayasDatabase? = null

        // Функция-одиночка (Singleton). Она гарантирует, что на весь мессенджер
        // будет создан только один экземпляр базы данных, чтобы не перегружать память устройства.
        fun getDatabase(context: Context): MayasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MayasDatabase::class.java,
                    "mayas_chats_database" // Имя файла, который создастся на телефоне
                )
                    // Если мы обновим структуру таблиц в будущем, эта строка не даст приложению упасть,
                    // а просто безопасно пересоздаст базу
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}