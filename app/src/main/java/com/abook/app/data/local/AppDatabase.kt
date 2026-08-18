package com.abook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, AnnotationEntity::class, ReadingSessionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abook_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
