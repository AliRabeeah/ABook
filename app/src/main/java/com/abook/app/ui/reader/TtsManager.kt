package com.abook.app.ui.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.Locale

class TtsController(context: Context) {
    private var tts: TextToSpeech? = null
    var isReady by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        isSpeaking = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "abook_chapter")
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

@Composable
fun rememberTtsController(): TtsController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { TtsController(context) }
    DisposableEffect(Unit) {
        onDispose { controller.shutdown() }
    }
    return controller
}

/** يستخرج نصًا صافيًا من HTML لتمريره لمحرك TTS. */
fun htmlToPlainText(html: String): String =
    html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
