package com.abook.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.abook.app.data.local.BookEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileInfoDialog(book: BookEntity, onDismiss: () -> Unit) {
    val file = File(book.filePath)
    val sizeInMb = if (file.exists()) "%.2f".format(file.length() / (1024f * 1024f)) else "غير معروف"
    val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar"))

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("معلومات الملف", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                InfoRow("الصيغة", book.format.name)
                InfoRow("الحجم", "$sizeInMb MB")
                InfoRow("تاريخ الإضافة", dateFormat.format(Date(book.dateAdded)))
                InfoRow("آخر فتح", book.lastOpened?.let { dateFormat.format(Date(it)) } ?: "لم يُفتح بعد")
                InfoRow("عدد الوحدات", book.totalUnits.toString())
                InfoRow("نسبة الإنجاز", "${(book.progressFraction * 100).toInt()}%")

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
