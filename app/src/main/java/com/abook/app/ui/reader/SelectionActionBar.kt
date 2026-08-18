package com.abook.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val highlightColors = listOf("#FF7A29", "#FFEB3B", "#4CAF50", "#2196F3")

@Composable
fun SelectionActionBar(
    selectedText: String,
    onHighlight: (colorHex: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1A1A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            highlightColors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .clickable { onHighlight(hex); onDismiss() }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = {
                // يفتح نظام أندرويد لاختيار تطبيق قاموس/ترجمة مثبت — بدون أي مفتاح API
                val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, selectedText)
                }
                runCatching {
                    context.startActivity(android.content.Intent.createChooser(intent, "بحث/ترجمة"))
                }
                onDismiss()
            }) {
                Icon(Icons.Filled.Translate, contentDescription = "قاموس/ترجمة", tint = Color.White)
            }
        }
    }
}
