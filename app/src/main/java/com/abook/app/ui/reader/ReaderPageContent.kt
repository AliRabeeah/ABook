package com.abook.app.ui.reader

import androidx.compose.runtime.Composable
import com.abook.app.data.local.BookEntity
import com.abook.app.data.local.BookFormat
import com.abook.app.data.repository.EpubChapter
import com.abook.app.data.repository.ReadingSettings

@Composable
fun ReaderPageContent(
    book: BookEntity,
    page: Int,
    epubChapters: List<EpubChapter>,
    settings: ReadingSettings,
    isDarkTheme: Boolean,
    viewModel: ReaderViewModel,
    onTextSelected: (String) -> Unit
) {
    when (book.format) {
        BookFormat.EPUB -> {
            val chapter = epubChapters.getOrNull(page)
            if (chapter != null) {
                EpubChapterView(
                    htmlContent = chapter.htmlContent,
                    settings = settings,
                    isDarkBackground = isDarkTheme || settings.forceDarkContent,
                    onTextSelected = onTextSelected
                )
            }
        }
        BookFormat.PDF -> {
            viewModel.getPdfDocument()?.let { doc ->
                PdfPageView(
                    document = doc,
                    pageIndex = page,
                    invertColors = settings.forceDarkContent
                )
            }
        }
        else -> {}
    }
}
