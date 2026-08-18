package com.abook.app.ui.reader

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.data.repository.EpubChapter
import com.abook.app.data.repository.ReadingSettings

/**
 * يعرض كل فصول EPUB متتالية بقائمة تمرير عمودية واحدة،
 * كبديل لتقليب الصفحات لمن يفضل التمرير المستمر.
 */
@Composable
fun ContinuousScrollReader(
    chapters: List<EpubChapter>,
    settings: ReadingSettings,
    isDarkBackground: Boolean,
    listState: LazyListState,
    onTextSelected: (chapterIndex: Int, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(chapters, key = { it.index }) { chapter ->
            EpubChapterView(
                htmlContent = chapter.htmlContent,
                settings = settings,
                isDarkBackground = isDarkBackground,
                onTextSelected = { text -> onTextSelected(chapter.index, text) },
                modifier = Modifier.fillMaxSize()
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
