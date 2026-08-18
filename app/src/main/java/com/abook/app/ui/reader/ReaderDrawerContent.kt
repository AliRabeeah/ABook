package com.abook.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.data.local.AnnotationEntity
import com.abook.app.data.repository.ReadingSettings
import com.abook.app.ui.theme.OrangeAccent

@Composable
fun ReaderDrawerContent(
    chapterTitles: List<String>,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit,
    annotations: List<AnnotationEntity>,
    onAnnotationSelected: (Int) -> Unit,
    settings: ReadingSettings,
    onSettingsChange: ((ReadingSettings) -> ReadingSettings) -> Unit,
    onOpenFullSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by remember { mutableStateOf(0) }

    ModalDrawerSheet(modifier = modifier) {
        // إعدادات سريعة أعلى القائمة
        Column(modifier = Modifier.padding(16.dp)) {
            Text("إعدادات سريعة", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.FormatSize, contentDescription = null, tint = OrangeAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = { newSize -> onSettingsChange { it.copy(fontSizeSp = newSize) } },
                    valueRange = 12f..28f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.Brightness6, contentDescription = null, tint = OrangeAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = if (settings.brightness < 0f) 0.5f else settings.brightness,
                    onValueChange = { newBrightness -> onSettingsChange { it.copy(brightness = newBrightness) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DarkMode, contentDescription = null, tint = OrangeAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("خلفية داكنة إجبارية", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.forceDarkContent,
                    onCheckedChange = { checked -> onSettingsChange { it.copy(forceDarkContent = checked) } }
                )
            }

            TextButton(onClick = onOpenFullSettings) { Text("كل الإعدادات") }
        }

        HorizontalDivider()

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("المحتويات") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("الإشارات") })
        }

        if (tabIndex == 0) {
            LazyColumn {
                items(chapterTitles.size) { index ->
                    ListItem(
                        headlineContent = { Text(chapterTitles[index]) },
                        modifier = Modifier.clickable { onChapterSelected(index) },
                        colors = ListItemDefaults.colors(
                            headlineColor = if (index == currentChapter) OrangeAccent
                                            else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        } else {
            if (annotations.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("لا توجد إشارات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(annotations) { annotation ->
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = OrangeAccent) },
                            headlineContent = { Text(annotation.excerpt, maxLines = 1) },
                            modifier = Modifier.clickable { onAnnotationSelected(annotation.unitIndex) }
                        )
                    }
                }
            }
        }
    }
}
