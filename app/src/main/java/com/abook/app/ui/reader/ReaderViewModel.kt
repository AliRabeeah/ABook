package com.abook.app.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abook.app.data.local.AnnotationEntity
import com.abook.app.data.local.AnnotationType
import com.abook.app.data.local.BookEntity
import com.abook.app.data.local.BookFormat
import com.abook.app.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)
    private val settingsRepository = ReaderSettingsRepository(application)
    private val epubLoader = EpubContentLoader()

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book

    private val _epubChapters = MutableStateFlow<List<EpubChapter>>(emptyList())
    val epubChapters: StateFlow<List<EpubChapter>> = _epubChapters

    private var pdfDocument: PdfDocumentHolder? = null
    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount: StateFlow<Int> = _pdfPageCount

    val settings: StateFlow<ReadingSettings> = _book.filterNotNull().flatMapLatest { book ->
        settingsRepository.effectiveSettingsFlow(book.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingSettings())

    fun bookOverride(bookId: Long) = settingsRepository.bookOverrideFlow(bookId)

    fun setBookOverride(bookId: Long, enabled: Boolean, settings: ReadingSettings) {
        viewModelScope.launch { settingsRepository.setBookOverride(bookId, enabled, settings) }
    }

    fun annotationsFor(bookId: Long) = repository.observeAnnotations(bookId)

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val entity = repository.observeBook(bookId).firstOrNull()
                entity
            } ?: return@launch
            _book.value = loaded

            when (loaded.format) {
                BookFormat.EPUB -> {
                    val chapters = withContext(Dispatchers.IO) { epubLoader.loadChapters(loaded.filePath) }
                    _epubChapters.value = chapters
                }
                BookFormat.PDF -> {
                    withContext(Dispatchers.IO) {
                        pdfDocument?.close()
                        pdfDocument = PdfDocumentHolder(loaded.filePath)
                        _pdfPageCount.value = pdfDocument?.pageCount ?: 0
                    }
                }
                else -> {}
            }

            // تحديث تاريخ آخر فتح
            repository.updateProgress(loaded.id, loaded.currentUnit, loaded.currentUnitProgress)
        }
    }

    fun getPdfDocument(): PdfDocumentHolder? = pdfDocument

    fun saveProgress(unit: Int, progress: Float) {
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            repository.updateProgress(currentBook.id, unit, progress)
        }
    }

    fun addBookmark(unitIndex: Int, excerpt: String) {
        val currentBook = _book.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.abook.app.data.local.AppDatabase.getInstance(getApplication())
            db.annotationDao().insert(
                AnnotationEntity(
                    bookId = currentBook.id,
                    type = AnnotationType.BOOKMARK,
                    unitIndex = unitIndex,
                    excerpt = excerpt.take(80)
                )
            )
        }
    }

    fun addHighlight(unitIndex: Int, selectedText: String, colorHex: String) {
        val currentBook = _book.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.abook.app.data.local.AppDatabase.getInstance(getApplication())
            db.annotationDao().insert(
                AnnotationEntity(
                    bookId = currentBook.id,
                    type = AnnotationType.HIGHLIGHT,
                    unitIndex = unitIndex,
                    excerpt = selectedText.take(200),
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateSettings(transform: (ReadingSettings) -> ReadingSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun recordReadingSession(bookId: Long, durationMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.abook.app.data.local.AppDatabase.getInstance(getApplication())
            db.readingSessionDao().insert(
                com.abook.app.data.local.ReadingSessionEntity(bookId = bookId, durationMillis = durationMillis)
            )
        }
    }

    fun observeStatsForBook(bookId: Long) =
        com.abook.app.data.local.AppDatabase.getInstance(getApplication()).readingSessionDao()
            .observeTotalTimeForBook(bookId)

    override fun onCleared() {
        super.onCleared()
        pdfDocument?.close()
    }
}
