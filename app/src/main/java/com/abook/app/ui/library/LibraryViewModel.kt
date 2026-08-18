package com.abook.app.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abook.app.data.local.BookEntity
import com.abook.app.data.repository.BookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LibrarySortOrder { DATE_ADDED, TITLE, AUTHOR, PROGRESS }
enum class LibraryViewMode { GRID, LIST }

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(LibrarySortOrder.DATE_ADDED)
    val sortOrder: StateFlow<LibrarySortOrder> = _sortOrder

    private val _viewMode = MutableStateFlow(LibraryViewMode.GRID)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    val books: StateFlow<List<BookEntity>> = combine(
        repository.observeAllBooks(), _searchQuery, _sortOrder
    ) { books, query, sort ->
        val filtered = if (query.isBlank()) books else books.filter {
            it.title.contains(query, true) || (it.author?.contains(query, true) == true)
        }
        when (sort) {
            LibrarySortOrder.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
            LibrarySortOrder.TITLE -> filtered.sortedBy { it.title }
            LibrarySortOrder.AUTHOR -> filtered.sortedBy { it.author ?: "" }
            LibrarySortOrder.PROGRESS -> filtered.sortedByDescending { it.progressFraction }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChange(query: String) { _searchQuery.value = query }
    fun onSortChange(order: LibrarySortOrder) { _sortOrder.value = order }
    fun onViewModeToggle() {
        _viewMode.value = if (_viewMode.value == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
    }

    fun importBook(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            val id = repository.importBook(uri)
            _isImporting.value = false
            onDone(id != null)
        }
    }

    fun toggleFavorite(book: BookEntity) = viewModelScope.launch { repository.toggleFavorite(book) }
    fun resetProgress(book: BookEntity) = viewModelScope.launch { repository.resetProgress(book.id) }
    fun deleteBook(book: BookEntity) = viewModelScope.launch { repository.deleteBook(book) }
    fun updateInfo(book: BookEntity, title: String, author: String?) =
        viewModelScope.launch { repository.updateInfo(book.id, title, author) }

    fun setCoverFromUrl(book: BookEntity, url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.setCoverFromUrl(book, url)
            onResult(success)
        }
    }

    fun restoreOriginalCover(book: BookEntity) = viewModelScope.launch { repository.restoreOriginalCover(book) }
}
