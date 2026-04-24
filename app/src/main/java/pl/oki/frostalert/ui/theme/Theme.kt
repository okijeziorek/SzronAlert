package pl.oki.frostalert.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Frost-branded dark color scheme — used on Android < 12 (no dynamic color).
 * Primary: icy blue, secondary: cool grey, tertiary: frosty mint.
 */
private val FrostDarkColorScheme = darkColorScheme(
    primary = FrostIcyBlue80,
    secondary = FrostCoolGrey80,
    tertiary = FrostMint80
)

/**
 * Frost-branded light color scheme — used on Android < 12 (no dynamic color).
 */
private val FrostLightColorScheme = lightColorScheme(
    primary = FrostIcyBlue40,
    secondary = FrostCoolGrey40,
    tertiary = FrostMint40
)

@Composable
fun FrostAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is enabled by default to support Material You on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FrostDarkColorScheme
        else -> FrostLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
