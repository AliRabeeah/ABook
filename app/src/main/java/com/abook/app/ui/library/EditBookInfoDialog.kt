package com.abook.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.abook.app.data.local.BookEntity

@Composable
fun EditBookInfoDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("تعديل معلومات الكتاب", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("المؤلف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(title.trim(), author.trim().ifBlank { null }) },
                        enabled = title.isNotBlank()
                    ) { Text("حفظ") }
                }
            }
        }
    }
}
