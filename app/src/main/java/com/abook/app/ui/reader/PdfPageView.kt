package com.abook.app.ui.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** فتح PDF مرة واحدة وإبقاؤه مفتوحًا طيلة جلسة القراءة لتفادي تكلفة إعادة الفتح لكل صفحة. */
class PdfDocumentHolder(filePath: String) {
    private val pfd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)

    val pageCount: Int get() = renderer.pageCount

    fun renderPage(index: Int, targetWidthPx: Int, invertColors: Boolean): Bitmap {
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width
            val width = targetWidthPx
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            return if (invertColors) invertBitmapColors(bitmap) else bitmap
        }
    }

    private fun invertBitmapColors(source: Bitmap): Bitmap {
        val matrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun close() {
        renderer.close()
        pfd.close()
    }
}

@Composable
fun PdfPageView(
    document: PdfDocumentHolder,
    pageIndex: Int,
    invertColors: Boolean,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex, invertColors) { mutableStateOf<Bitmap?>(null) }
    val density = LocalDensity.current
    val widthPx = with(density) { 1080.dp.roundToPx() } // دقة عرض ثابتة معقولة للأداء

    LaunchedEffect(pageIndex, invertColors) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { document.renderPage(pageIndex, widthPx, invertColors) }.getOrNull()
        }
    }

    val bg = if (invertColors) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize().background(bg)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "صفحة ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
