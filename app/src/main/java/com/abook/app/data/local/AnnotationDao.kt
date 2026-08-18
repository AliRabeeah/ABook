package com.abook.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY unitIndex ASC")
    fun observeForBook(bookId: Long): Flow<List<AnnotationEntity>>

    @Insert
    suspend fun insert(annotation: AnnotationEntity): Long

    @Delete
    suspend fun delete(annotation: AnnotationEntity)
}
