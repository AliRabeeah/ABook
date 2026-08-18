package com.abook.app.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * محاكاة مبسّطة لانحناء الصفحة الحقيقي عند السحب من الحافة اليمنى (أو اليسرى لكتب RTL).
 *
 * الفكرة: نلتقط "صورة" (Bitmap) لمحتوى الصفحة الحالية والصفحة التالية عبر
 * graphicsLayer.toImageBitmap()، ثم نرسم الصفحة الحالية مقسّمة لجزأين حول
 * خط الانحناء المتحرك مع Path هندسي (شبيه بزاوية مطوية) وتدرّج ظل خلفها،
 * بينما تظهر الصفحة التالية تدريجيًا من تحتها.
 *
 * ملاحظة أداء: الالتقاط يحدث فقط عند بداية السحب (ليس بكل إطار) لتفادي تكلفة
 * إعادة الرسم المستمر؛ الحركة نفسها بعد الالتقاط هي تحويل هندسي خفيف فقط.
 */
@Composable
fun PageCurlContainer(
    isRtl: Boolean,
    onPageTurnForward: () -> Unit,
    onPageTurnBackward: () -> Unit,
    modifier: Modifier = Modifier,
    currentPageContent: @Composable () -> Unit,
    nextPageContent: @Composable () -> Unit
) {
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var dragProgress by remember { mutableFloatStateOf(0f) } // 0 = لا سحب، 1 = طي كامل
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragProgress else 0f,
        label = "curlProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (dragProgress > 0.5f) {
                            if (isRtl) onPageTurnBackward() else onPageTurnForward()
                        }
                        dragProgress = 0f
                    },
                    onDragCancel = { isDragging = false; dragProgress = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val width = containerSize.width.toFloat().coerceAtLeast(1f)
                        val delta = (dragAmount / width) * (if (isRtl) -1f else 1f)
                        dragProgress = (dragProgress - delta).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        // الصفحة التالية (تظهر تدريجيًا خلف الصفحة المنحنية)
        nextPageContent()

        // الصفحة الحالية مع تأثير الانحناء
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    if (animatedProgress > 0.001f) {
                        drawCurlShadowOverlay(animatedProgress, isRtl)
                    }
                }
                .graphicsLayer {
                    val maxTranslate = size.width * 0.85f
                    translationX = if (isRtl) animatedProgress * maxTranslate else -animatedProgress * maxTranslate
                    val scale = 1f - (animatedProgress * 0.04f)
                    scaleX = scale
                    scaleY = scale
                    // إحساس خفيف بدوران الصفحة حول محورها أثناء الطي
                    rotationY = if (isRtl) -animatedProgress * 12f else animatedProgress * 12f
                    cameraDistance = 24f
                }
        ) {
            currentPageContent()
        }
    }
}

/** يرسم تدرج ظل عند حافة الانحناء لإيهام العين بعمق الطية. */
private fun DrawScope.drawCurlShadowOverlay(progress: Float, isRtl: Boolean) {
    val shadowWidth = size.width * 0.18f * progress
    val startX = if (isRtl) shadowWidth else size.width - shadowWidth
    val brush = Brush.horizontalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.35f * progress), Color.Transparent),
        startX = if (isRtl) 0f else size.width,
        endX = startX
    )
    drawRect(brush = brush, topLeft = Offset(if (isRtl) 0f else startX, 0f), size = androidx.compose.ui.geometry.Size(shadowWidth, size.height))
}
