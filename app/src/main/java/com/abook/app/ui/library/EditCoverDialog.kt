package com.abook.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
fun EditCoverDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onRestoreOriginal: () -> Unit,
    isSaving: Boolean
) {
    var url by remember { mutableStateOf("") }
    val isValidUrl = url.startsWith("http://") || url.startsWith("https://")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("تعديل غلاف الكتاب", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("رابط صورة الغلاف") },
                    placeholder = { Text("https://example.com/cover.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // معاينة فورية للصورة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isValidUrl) {
                        AsyncImage(
                            model = url,
                            contentDescription = "معاينة الغلاف",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            "أدخل رابطًا صالحًا لمعاينة الصورة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onRestoreOriginal(); onDismiss() }) {
                        Text("استعادة الأصلي")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(url) },
                        enabled = isValidUrl && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }
}
