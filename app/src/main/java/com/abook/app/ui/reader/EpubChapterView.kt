package com.abook.app.ui.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.abook.app.data.repository.ReadingSettings
import com.abook.app.data.repository.TextAlignMode

/**
 * يعرض محتوى فصل EPUB (HTML) داخل WebView، مع حقن CSS يعكس إعدادات القراءة
 * الحالية، ويفعّل تحديد النص لدعم التظليل والبحث عن معنى الكلمة (Process Text).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubChapterView(
    htmlContent: String,
    settings: ReadingSettings,
    isDarkBackground: Boolean,
    onTextSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgColor = if (isDarkBackground || settings.forceDarkContent) "#000000" else "#FAFAFA"
    val textColor = when {
        settings.highContrastMode && (isDarkBackground || settings.forceDarkContent) -> "#FFFFFF"
        settings.highContrastMode -> "#000000"
        isDarkBackground || settings.forceDarkContent -> "#F5F5F5"
        else -> "#1C1C1C"
    }
    val textAlign = if (settings.textAlign == TextAlignMode.JUSTIFY) "justify" else "start"

    val styledHtml = """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body {
                background-color: $bgColor;
                color: $textColor;
                font-size: ${settings.fontSizeSp}px;
                line-height: ${settings.lineHeightMultiplier};
                letter-spacing: ${settings.letterSpacing}px;
                text-align: $textAlign;
                margin: ${settings.marginDp}px;
                padding: 0;
                word-wrap: break-word;
                -webkit-user-select: text;
            }
            img { max-width: 100%; height: auto; }
            a { color: #FF7A29; }
            ::selection { background: #FF7A29; color: #000000; }
        </style>
        </head>
        <body>
        $htmlContent
        <script>
            document.addEventListener('selectionchange', function() {
                var text = window.getSelection().toString();
                if (text && text.length > 0) {
                    AndroidSelection.onSelectionChanged(text);
                }
            });
        </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                this.settings.javaScriptEnabled = true // مطلوب فقط لالتقاط تحديد النص (لا نحمّل أي سكربت خارجي)
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onSelectionChanged(text: String) { onTextSelected(text) }
                }, "AndroidSelection")
                setBackgroundColor(android.graphics.Color.parseColor(bgColor))
            }
        },
        update = { webView ->
            webView.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        }
    )
}
