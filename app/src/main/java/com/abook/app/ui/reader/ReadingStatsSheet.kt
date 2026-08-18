package com.abook.app.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.ui.theme.OrangeAccent
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsSheet(totalTimeFlow: Flow<Long?>, onDismiss: () -> Unit) {
    val totalMillis by totalTimeFlow.collectAsState(initial = 0L)
    val totalMinutes = ((totalMillis ?: 0L) / 60000)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("إحصائيات هذا الكتاب", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = OrangeAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hours > 0) "${hours} ساعة و${minutes} دقيقة إجمالي وقت القراءة"
                    else "${minutes} دقيقة إجمالي وقت القراءة"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
