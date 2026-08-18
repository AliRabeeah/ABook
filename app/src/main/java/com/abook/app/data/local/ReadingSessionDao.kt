package com.abook.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {

    @Insert
    suspend fun insert(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions ORDER BY date DESC")
    fun observeAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT SUM(durationMillis) FROM reading_sessions")
    fun observeTotalReadingTime(): Flow<Long?>

    @Query("SELECT SUM(durationMillis) FROM reading_sessions WHERE bookId = :bookId")
    fun observeTotalTimeForBook(bookId: Long): Flow<Long?>
}
