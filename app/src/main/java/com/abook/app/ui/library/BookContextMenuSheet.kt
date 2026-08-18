package com.abook.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.data.local.BookEntity

private data class ContextMenuAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookContextMenuSheet(
    book: BookEntity,
    onDismiss: () -> Unit,
    onEditCover: () -> Unit,
    onEditInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onResetProgress: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val actions = listOf(
                ContextMenuAction("تعديل الغلاف", Icons.Filled.Image, onClick = onEditCover),
                ContextMenuAction("تعديل المعلومات", Icons.Filled.Edit, onClick = onEditInfo),
                ContextMenuAction(
                    if (book.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                    Icons.Filled.Star, onClick = onToggleFavorite
                ),
                ContextMenuAction("مشاركة", Icons.Filled.Share, onClick = onShare),
                ContextMenuAction("إعادة تعيين التقدم", Icons.Filled.Replay, onClick = onResetProgress),
                ContextMenuAction("معلومات الملف", Icons.Filled.Info, onClick = onShowInfo),
                ContextMenuAction("حذف من المكتبة", Icons.Filled.Delete, isDestructive = true, onClick = onDelete),
            )

            actions.forEach { action ->
                ListItem(
                    headlineContent = {
                        Text(
                            action.label,
                            color = if (action.isDestructive) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = if (action.isDestructive) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        action.onClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}
