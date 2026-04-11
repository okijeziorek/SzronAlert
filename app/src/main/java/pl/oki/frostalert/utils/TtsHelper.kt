package pl.oki.frostalert.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Helper for Text-to-Speech voice notifications.
 * Speaks frost alerts when TTS is enabled in settings.
 */
object TtsHelper {
    private const val TAG = "TtsHelper"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun initialize(context: Context, onReady: (() -> Unit)? = null) {
        if (tts != null) {
            onReady?.invoke()
            return
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("pl-PL"))
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isInitialized) {
                    // Fallback to default locale
                    tts?.setLanguage(Locale.getDefault())
                    isInitialized = true
                }
                onReady?.invoke()
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                isInitialized = false
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "frost_alert_tts")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
