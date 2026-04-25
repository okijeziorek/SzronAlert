package pl.oki.frostalert.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {

    private fun getSystemLocale(): Locale {
        return Resources.getSystem().configuration.locales[0]
    }

    /**
     * Updates the app's locale based on the language code.
     * @param languageCode Language code (e.g., "pl", "en", "de", "fr", "system")
     * @return A context with the updated locale applied
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "system" -> getSystemLocale()
            "pl" -> Locale.forLanguageTag("pl")
            "en" -> Locale.forLanguageTag("en")
            "de" -> Locale.forLanguageTag("de")
            "fr" -> Locale.forLanguageTag("fr")
            "es" -> Locale.forLanguageTag("es")
            "it" -> Locale.forLanguageTag("it")
            "uk" -> Locale.forLanguageTag("uk")
            "cs" -> Locale.forLanguageTag("cs")
            else -> getSystemLocale()
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        // Use ContextWrapper(context) so that the Activity remains reachable via the
        // baseContext chain. A bare createConfigurationContext() returns a ContextImpl
        // which breaks Hilt's findActivity() traversal and causes an
        // IllegalStateException when hiltViewModel() is called inside Compose.
        val localizedResources = context.createConfigurationContext(config).resources
        return object : ContextWrapper(context) {
            override fun getResources(): Resources = localizedResources
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
