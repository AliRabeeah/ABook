package com.abook.app.ui.reader

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.abook.app.data.repository.PageTurnEffect
import kotlin.math.absoluteValue

/**
 * يغلّف HorizontalPager ويطبّق تأثير بصري حسب اختيار المستخدم.
 * ملاحظة: تأثير CURL (محاكاة انحناء الورقة الحقيقي) مخطط لمرحلة لاحقة
 * لأنه يتطلب رسم Canvas مخصص بمعادلات هندسية؛ حاليًا يُعامل كـ SLIDE.
 */
@Composable
fun ReaderPager(
    pagerState: PagerState,
    effect: PageTurnEffect,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    when (effect) {
                        PageTurnEffect.FADE -> {
                            alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        }
                        PageTurnEffect.SLIDE, PageTurnEffect.CURL -> {
                            // الانزلاق الافتراضي لـ HorizontalPager يكفي بصريًا هنا
                            alpha = 1f
                        }
                    }
                }
        ) {
            pageContent(page)
        }
    }
}
