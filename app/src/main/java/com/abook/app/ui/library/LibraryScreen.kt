package com.abook.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abook.app.data.local.BookEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenBook: (Long) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val books by viewModel.books.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedBookForMenu by remember { mutableStateOf<BookEntity?>(null) }
    var showEditCoverFor by remember { mutableStateOf<BookEntity?>(null) }
    var showEditInfoFor by remember { mutableStateOf<BookEntity?>(null) }
    var showFileInfoFor by remember { mutableStateOf<BookEntity?>(null) }
    var showSyncSheet by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.importBook(it) { /* success handled via Flow update */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchChange,
                            placeholder = { Text("ابحث عن كتاب أو مؤلف...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    } else {
                        Text("A Book")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = { showSyncSheet = true }) {
                        Icon(Icons.Filled.CloudSync, contentDescription = "مزامنة")
                    }
                    IconButton(onClick = viewModel::onViewModeToggle) {
                        Icon(Icons.Filled.List, contentDescription = "تبديل العرض")
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "فرز")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("الأحدث إضافة") }, onClick = {
                                viewModel.onSortChange(LibrarySortOrder.DATE_ADDED); sortMenuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("العنوان") }, onClick = {
                                viewModel.onSortChange(LibrarySortOrder.TITLE); sortMenuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("المؤلف") }, onClick = {
                                viewModel.onSortChange(LibrarySortOrder.AUTHOR); sortMenuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("نسبة التقدم") }, onClick = {
                                viewModel.onSortChange(LibrarySortOrder.PROGRESS); sortMenuExpanded = false
                            })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                filePicker.launch(arrayOf("application/epub+zip", "application/pdf"))
            }) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة كتاب")
                }
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            EmptyLibraryState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(books, key = { it.id }) { book ->
                    BookGridCard(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        onLongPress = { selectedBookForMenu = book }
                    )
                }
            }
        }
    }

    // قائمة السياق عند الضغط المطول
    selectedBookForMenu?.let { book ->
        BookContextMenuSheet(
            book = book,
            onDismiss = { selectedBookForMenu = null },
            onEditCover = { showEditCoverFor = book },
            onEditInfo = { showEditInfoFor = book },
            onToggleFavorite = { viewModel.toggleFavorite(book) },
            onShare = {
                runCatching {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", java.io.File(book.filePath)
                    )
                    val mimeType = if (book.format == com.abook.app.data.local.BookFormat.EPUB)
                        "application/epub+zip" else "application/pdf"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "مشاركة الكتاب"))
                }
            },
            onResetProgress = { viewModel.resetProgress(book) },
            onDelete = { bookToDelete = book },
            onShowInfo = { showFileInfoFor = book }
        )
    }

    // نافذة تعديل معلومات الكتاب
    showEditInfoFor?.let { book ->
        EditBookInfoDialog(
            book = book,
            onDismiss = { showEditInfoFor = null },
            onConfirm = { title, author ->
                viewModel.updateInfo(book, title, author)
                showEditInfoFor = null
            }
        )
    }

    // نافذة معلومات الملف
    showFileInfoFor?.let { book ->
        FileInfoDialog(book = book, onDismiss = { showFileInfoFor = null })
    }

    // نافذة مزامنة Google Drive
    if (showSyncSheet) {
        SyncSheet(onDismiss = { showSyncSheet = false })
    }

    // نافذة تعديل الغلاف
    showEditCoverFor?.let { book ->
        var isSaving by remember { mutableStateOf(false) }
        EditCoverDialog(
            onDismiss = { showEditCoverFor = null },
            onConfirm = { url ->
                isSaving = true
                viewModel.setCoverFromUrl(book, url) {
                    isSaving = false
                    showEditCoverFor = null
                }
            },
            onRestoreOriginal = { viewModel.restoreOriginalCover(book) },
            isSaving = isSaving
        )
    }

    // تأكيد الحذف
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("حذف الكتاب؟") },
            text = { Text("سيتم حذف \"${book.title}\" نهائيًا من المكتبة. هذا الإجراء لا يمكن التراجع عنه.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book)
                    bookToDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun EmptyLibraryState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("مكتبتك فارغة حاليًا", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "اضغط + لإضافة كتابك الأول (EPUB أو PDF)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
