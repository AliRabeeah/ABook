package com.abook.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.data.local.BookEntity
import com.abook.app.data.local.BookFormat
import com.abook.app.data.repository.EpubChapter

data class SearchResult(val chapterIndex: Int, val snippet: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InBookSearchSheet(
    book: BookEntity,
    epubChapters: List<EpubChapter>,
    onResultSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val results = remember(query, epubChapters) {
        if (query.length < 2 || book.format != BookFormat.EPUB) emptyList()
        else epubChapters.mapNotNull { chapter ->
            val plainText = chapter.htmlContent.replace(Regex("<[^>]*>"), " ")
            val index = plainText.indexOf(query, ignoreCase = true)
            if (index == -1) null
            else {
                val start = (index - 30).coerceAtLeast(0)
                val end = (index + query.length + 30).coerceAtMost(plainText.length)
                SearchResult(chapter.index, "..." + plainText.substring(start, end).trim() + "...")
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("ابحث داخل هذا الكتاب") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (book.format == BookFormat.PDF) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "البحث داخل ملفات PDF غير مدعوم حاليًا — يتطلب مكتبة استخراج نص إضافية (مخطط لمرحلة لاحقة).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(results) { result ->
                    ListItem(
                        headlineContent = { Text("الفصل ${result.chapterIndex + 1}") },
                        supportingContent = { Text(result.snippet, maxLines = 2) },
                        modifier = Modifier.clickable { onResultSelected(result.chapterIndex) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
