package com.abook.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBookById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("UPDATE books SET currentUnit = :unit, currentUnitProgress = :progress, lastOpened = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, unit: Int, progress: Float, timestamp: Long)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE books SET coverPath = :coverPath, customCoverUrl = :customCoverUrl WHERE id = :id")
    suspend fun updateCover(id: Long, coverPath: String?, customCoverUrl: String?)

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :id")
    suspend fun updateInfo(id: Long, title: String, author: String?)

    @Query("UPDATE books SET currentUnit = 0, currentUnitProgress = 0 WHERE id = :id")
    suspend fun resetProgress(id: Long)
}
