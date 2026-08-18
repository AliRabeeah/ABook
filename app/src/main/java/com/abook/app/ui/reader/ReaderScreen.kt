package com.abook.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abook.app.data.local.BookFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    LaunchedEffect(bookId) { viewModel.loadBook(bookId) }

    val book by viewModel.book.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val epubChapters by viewModel.epubChapters.collectAsState()
    val pdfPageCount by viewModel.pdfPageCount.collectAsState()

    var barsVisible by remember { mutableStateOf(true) }
    var showFullSettings by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var showBreakReminder by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current
    val activity = view.context as? android.app.Activity

    // وضع ملء الشاشة: إخفاء/إظهار أشرطة النظام تبعًا لظهور شريط أدوات القراءة
    DisposableEffect(barsVisible, activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            if (barsVisible) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {}
    }

    val currentBook = book ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalUnits = when (currentBook.format) {
        BookFormat.EPUB -> epubChapters.size.coerceAtLeast(1)
        BookFormat.PDF -> pdfPageCount.coerceAtLeast(1)
        else -> 1
    }

    val pagerState = rememberPagerState(
        initialPage = currentBook.currentUnit.coerceIn(0, (totalUnits - 1).coerceAtLeast(0)),
        pageCount = { totalUnits }
    )

    val annotations by produceState(initialValue = emptyList<com.abook.app.data.local.AnnotationEntity>(), currentBook.id) {
        viewModel.annotationsFor(currentBook.id).collect { value = it }
    }

    val ttsController = rememberTtsController()

    // حفظ التقدم عند تغيير الصفحة
    LaunchedEffect(pagerState.currentPage) {
        viewModel.saveProgress(pagerState.currentPage, 0f)
    }

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // تطبيق "إبقاء الشاشة مضاءة" فعليًا حسب الإعداد
    DisposableEffect(settings.keepScreenOn, activity) {
        val window = activity?.window
        if (settings.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // تذكير أخذ استراحة بعد 60 دقيقة قراءة متواصلة + تتبع جلسة القراءة للإحصائيات
    LaunchedEffect(currentBook.id) {
        val sessionStart = System.currentTimeMillis()
        kotlinx.coroutines.delay(60 * 60 * 1000L)
        showBreakReminder = true
        viewModel.recordReadingSession(currentBook.id, System.currentTimeMillis() - sessionStart)
    }
    DisposableEffect(currentBook.id) {
        val sessionStart = System.currentTimeMillis()
        onDispose {
            val elapsed = System.currentTimeMillis() - sessionStart
            if (elapsed > 5000) viewModel.recordReadingSession(currentBook.id, elapsed)
            ttsController.stop()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val chapterTitles = when (currentBook.format) {
                BookFormat.EPUB -> epubChapters.mapIndexed { i, _ -> "الفصل ${i + 1}" }
                BookFormat.PDF -> (0 until pdfPageCount).map { "صفحة ${it + 1}" }
                else -> emptyList()
            }
            ReaderDrawerContent(
                chapterTitles = chapterTitles,
                currentChapter = pagerState.currentPage,
                onChapterSelected = { index ->
                    scope.launch {
                        pagerState.scrollToPage(index)
                        drawerState.close()
                    }
                },
                annotations = annotations,
                onAnnotationSelected = { unitIndex ->
                    scope.launch {
                        pagerState.scrollToPage(unitIndex)
                        drawerState.close()
                    }
                },
                settings = settings,
                onSettingsChange = { transform -> viewModel.updateSettings(transform) },
                onOpenFullSettings = { showFullSettings = true; scope.launch { drawerState.close() } }
            )
        }
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    // التنقل بأزرار مستوى الصوت
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalUnits - 1)) }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .pointerInput(settings.useTapZoneNavigation) {
                    detectTapGestures(onTap = { offset ->
                        val width = size.width
                        when {
                            !settings.useTapZoneNavigation -> barsVisible = !barsVisible
                            offset.x < width * 0.3f -> {
                                // المنطقة اليسرى: صفحة سابقة (أو تالية لو الكتاب RTL)
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                            }
                            offset.x > width * 0.7f -> {
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalUnits - 1)) }
                            }
                            else -> barsVisible = !barsVisible
                        }
                    })
                }
        ) {
            if (settings.useContinuousScroll && currentBook.format == BookFormat.EPUB) {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                ContinuousScrollReader(
                    chapters = epubChapters,
                    settings = settings,
                    isDarkBackground = isDarkTheme || settings.forceDarkContent,
                    listState = listState,
                    onTextSelected = { chapterIndex, text -> selectedText = text to chapterIndex },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (settings.pageTurnEffect == com.abook.app.data.repository.PageTurnEffect.CURL) {
                val currentPage = pagerState.currentPage
                PageCurlContainer(
                    isRtl = false, // TODO: تحديد اتجاه الكتاب تلقائيًا من لغة EPUB (metadata dir)
                    onPageTurnForward = {
                        scope.launch { pagerState.scrollToPage((currentPage + 1).coerceAtMost(totalUnits - 1)) }
                    },
                    onPageTurnBackward = {
                        scope.launch { pagerState.scrollToPage((currentPage - 1).coerceAtLeast(0)) }
                    },
                    modifier = Modifier.fillMaxSize(),
                    currentPageContent = {
                        ReaderPageContent(
                            book = currentBook, page = currentPage, epubChapters = epubChapters,
                            settings = settings, isDarkTheme = isDarkTheme, viewModel = viewModel,
                            onTextSelected = { text -> selectedText = text to currentPage }
                        )
                    },
                    nextPageContent = {
                        val nextPage = (currentPage + 1).coerceAtMost(totalUnits - 1)
                        ReaderPageContent(
                            book = currentBook, page = nextPage, epubChapters = epubChapters,
                            settings = settings, isDarkTheme = isDarkTheme, viewModel = viewModel,
                            onTextSelected = {}
                        )
                    }
                )
            } else {
            ReaderPager(
                pagerState = pagerState,
                effect = settings.pageTurnEffect,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ReaderPageContent(
                    book = currentBook, page = page, epubChapters = epubChapters,
                    settings = settings, isDarkTheme = isDarkTheme, viewModel = viewModel,
                    onTextSelected = { text -> selectedText = text to page }
                )
            }
            }

            // شريط علوي قابل للإخفاء
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = { Text(currentBook.title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        if (currentBook.format == BookFormat.EPUB) {
                            IconButton(onClick = {
                                if (ttsController.isSpeaking) {
                                    ttsController.stop()
                                } else {
                                    val chapter = epubChapters.getOrNull(pagerState.currentPage)
                                    if (chapter != null) ttsController.speak(htmlToPlainText(chapter.htmlContent))
                                }
                            }) {
                                Icon(
                                    if (ttsController.isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                    contentDescription = "قراءة صوتية"
                                )
                            }
                        }
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "بحث بالكتاب")
                        }
                        IconButton(onClick = { showStatsSheet = true }) {
                            Icon(Icons.Filled.Timer, contentDescription = "إحصائيات")
                        }
                        IconButton(onClick = {
                            viewModel.addBookmark(pagerState.currentPage, "الفصل/الصفحة ${pagerState.currentPage + 1}")
                        }) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = "إضافة إشارة")
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "القائمة")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.85f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }

            // شريط تقدم سفلي رفيع دائم الظهور
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / totalUnits },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = com.abook.app.ui.theme.OrangeAccent,
                trackColor = Color.DarkGray
            )

            // شريط إجراءات النص المحدد (تظليل + قاموس/ترجمة)
            selectedText?.let { (text, page) ->
                SelectionActionBar(
                    selectedText = text,
                    onHighlight = { colorHex -> viewModel.addHighlight(page, text, colorHex) },
                    onDismiss = { selectedText = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }

    if (showFullSettings) {
        val overrideState by viewModel.bookOverride(currentBook.id).collectAsState(
            initial = com.abook.app.data.repository.BookSettingsOverride()
        )
        ReaderSettingsSheet(
            settings = settings,
            onSettingsChange = { transform -> viewModel.updateSettings(transform) },
            onDismiss = { showFullSettings = false },
            bookOverrideEnabled = overrideState.hasOverride,
            onToggleBookOverride = { enabled -> viewModel.setBookOverride(currentBook.id, enabled, settings) }
        )
    }

    if (showSearchSheet) {
        InBookSearchSheet(
            book = currentBook,
            epubChapters = epubChapters,
            onResultSelected = { unitIndex ->
                scope.launch { pagerState.scrollToPage(unitIndex) }
                showSearchSheet = false
            },
            onDismiss = { showSearchSheet = false }
        )
    }

    if (showStatsSheet) {
        ReadingStatsSheet(
            totalTimeFlow = viewModel.observeStatsForBook(currentBook.id),
            onDismiss = { showStatsSheet = false }
        )
    }

    if (showBreakReminder) {
        AlertDialog(
            onDismissRequest = { showBreakReminder = false },
            title = { Text("وقت استراحة؟") },
            text = { Text("أنت تقرأ منذ ساعة متواصلة. خذ استراحة قصيرة لراحة عينيك.") },
            confirmButton = {
                TextButton(onClick = { showBreakReminder = false }) { Text("متابعة القراءة") }
            }
        )
    }
}
