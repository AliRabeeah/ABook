package com.abook.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val durationMillis: Long,
    val date: Long = System.currentTimeMillis() // بداية اليوم بالمللي ثانية، يُستخدم لحساب أيام متتالية
)
