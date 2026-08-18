package com.abook.app.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.abook.app.data.local.BookEntity
import com.abook.app.ui.theme.OrangeAccent

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BookGridCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = OrangeAccent
                            )
                        }
                    }
                }
            }

            if (book.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "مفضلة",
                    tint = OrangeAccent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }

            // شريط تقدم القراءة أسفل الغلاف
            if (book.progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { book.progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = OrangeAccent,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (book.author != null) {
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
