package pl.oki.frostalert.utils

import android.content.Context
import android.os.Build
import java.util.Locale

object LocaleHelper {

    /**
     * Updates the app's locale based on the language code.
     * @param languageCode Language code (e.g., "pl", "en", "de", "fr", "system")
     * @return A context with the updated locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        if (languageCode == "system") {
            // Use system default - no override
            return context
        }

        val locale = when (languageCode) {
            "pl" -> Locale("pl")
            "en" -> Locale("en")
            "de" -> Locale("de")
            "fr" -> Locale("fr")
            "es" -> Locale("es")
            "it" -> Locale("it")
            "uk" -> Locale("uk")
            "cs" -> Locale("cs")
            else -> Locale.getDefault()
        }

        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Returns human-readable language name for the given code
     */
    fun getLanguageName(context: Context, languageCode: String): String {
        return when (languageCode) {
            "system" -> context.getString(pl.oki.frostalert.R.string.language_system)
            "pl" -> context.getString(pl.oki.frostalert.R.string.language_polish)
            "en" -> context.getString(pl.oki.frostalert.R.string.language_english)
            "de" -> context.getString(pl.oki.frostalert.R.string.language_german)
            "fr" -> context.getString(pl.oki.frostalert.R.string.language_french)
            "es" -> context.getString(pl.oki.frostalert.R.string.language_spanish)
            "it" -> context.getString(pl.oki.frostalert.R.string.language_italian)
            "uk" -> context.getString(pl.oki.frostalert.R.string.language_ukrainian)
            "cs" -> context.getString(pl.oki.frostalert.R.string.language_czech)
            else -> languageCode
        }
    }

    /**
     * List of supported languages
     */
    val supportedLanguages = listOf(
        "system",
        "pl",
        "en",
        "de",
        "fr",
        "es",
        "it",
        "uk",
        "cs"
    )
}
