package com.abook.app.data.repository

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import com.abook.app.data.local.AppDatabase
import com.abook.app.data.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val annotationDao = db.annotationDao()
    private val importer = BookImporter(context)

    fun observeAllBooks(): Flow<List<BookEntity>> = bookDao.observeAllBooks()

    fun observeBook(id: Long) = bookDao.observeBookById(id)

    fun observeAnnotations(bookId: Long) = annotationDao.observeForBook(bookId)

    suspend fun importBook(uri: Uri): Long? = withContext(Dispatchers.IO) {
        val info = importer.importFromUri(uri) ?: return@withContext null
        val entity = BookEntity(
            title = info.title,
            author = info.author,
            filePath = info.localFilePath,
            format = info.format,
            coverPath = info.coverPath,
            totalUnits = info.totalUnits
        )
        bookDao.insert(entity)
    }

    suspend fun updateProgress(bookId: Long, unit: Int, progress: Float) = withContext(Dispatchers.IO) {
        bookDao.updateProgress(bookId, unit, progress, System.currentTimeMillis())
    }

    suspend fun updateInfo(bookId: Long, title: String, author: String?) = withContext(Dispatchers.IO) {
        bookDao.updateInfo(bookId, title, author)
    }

    suspend fun toggleFavorite(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.setFavorite(book.id, !book.isFavorite)
    }

    suspend fun resetProgress(bookId: Long) = withContext(Dispatchers.IO) {
        bookDao.resetProgress(bookId)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.delete(book)
        // حذف الملف المحلي المخزن وأي غلاف مرتبط به
        runCatching { File(book.filePath).delete() }
        runCatching { book.coverPath?.let { File(it).delete() } }
    }

    /**
     * تحميل غلاف من رابط خارجي، حفظه محليًا (Cache)، وربطه بالكتاب.
     * إرجاع true لو نجحت العملية.
     */
    suspend fun setCoverFromUrl(book: BookEntity, url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val drawable = result.drawable ?: return@withContext false

            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val outFile = File(coversDir, "custom_${book.id}_${System.currentTimeMillis()}.jpg")

            val bitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap
            FileOutputStream(outFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }

            bookDao.updateCover(book.id, outFile.absolutePath, url)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreOriginalCover(book: BookEntity) = withContext(Dispatchers.IO) {
        if (book.format == com.abook.app.data.local.BookFormat.EPUB) {
            val info = importer.reExtractEpubCover(book.filePath)
            bookDao.updateCover(book.id, info, null)
        } else if (book.format == com.abook.app.data.local.BookFormat.PDF) {
            val info = importer.reExtractPdfCover(book.filePath)
            bookDao.updateCover(book.id, info, null)
        } else {
            bookDao.updateCover(book.id, null, null)
        }
    }
}
