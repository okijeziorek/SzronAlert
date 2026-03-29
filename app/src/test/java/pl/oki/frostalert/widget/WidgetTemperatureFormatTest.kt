package pl.oki.frostalert.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.utils.WeatherCalculations

/**
 * Verifies that temperature formatting for widgets respects the useFahrenheit setting.
 * (The actual RemoteViews update requires an Android context; these tests cover the
 * formatting logic in isolation.)
 */
class WidgetTemperatureFormatTest {

    @Test
    fun `formatTemperature returns Celsius string when useFahrenheit is false`() {
        val result = WeatherCalculations.formatTemperature(-3.5, useFahrenheit = false)
        assertTrue("Powinien zawierać °C", result.contains("°C"))
        assertFalse("Nie powinien zawierać °F", result.contains("°F"))
    }

    @Test
    fun `formatTemperature returns Fahrenheit string when useFahrenheit is true`() {
        val result = WeatherCalculations.formatTemperature(-3.5, useFahrenheit = true)
        assertTrue("Powinien zawierać °F", result.contains("°F"))
        assertFalse("Nie powinien zawierać °C", result.contains("°C"))
    }

    @Test
    fun `formatTemperature converts 0 Celsius to 32 Fahrenheit`() {
        val result = WeatherCalculations.formatTemperature(0.0, useFahrenheit = true)
        assertTrue("0°C powinno być 32.0°F", result.contains("32.0°F"))
    }

    @Test
    fun `formatTemperature shows correct Celsius value`() {
        val result = WeatherCalculations.formatTemperature(-5.0, useFahrenheit = false)
        assertTrue("Powinno pokazywać -5.0°C", result.contains("-5.0°C"))
    }
}
