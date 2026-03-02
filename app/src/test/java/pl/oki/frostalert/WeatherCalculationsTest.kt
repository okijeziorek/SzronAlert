package pl.oki.frostalert

import org.junit.Assert.*
import org.junit.Test
import pl.oki.frostalert.utils.WeatherCalculations

class WeatherCalculationsTest {

    @Test
    fun `test frost risk with clear sky and freezing temperature`() {
        // Czyste niebo (0), temp 1.0C, wysoka wilgotność 85%, brak opadów
        // Radiacyjne wychłodzenie szyb powinno spowodować ryzyko
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp = 1.0,
            humidity = 85.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 1.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2
        )
        assertTrue("Powinno być ryzyko przy czystym niebie i 1.0C", hasRisk)
    }

    @Test
    fun `test no frost risk with cloudy sky and 3C`() {
        // Zachmurzenie duże (3), temp 3.0C
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp = 3.0,
            humidity = 80.0,
            precip = 0.0,
            weatherCode = 3,
            tempThreshold = 1.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2
        )
        assertFalse("Nie powinno być ryzyka przy 3.0C i chmurach", hasRisk)
    }

    @Test
    fun `test dew point calculation`() {
        // Dla 10C i 50% wilgotności punkt rosy to ok 0.1C
        val dewPoint = WeatherCalculations.calculateDewPoint(10.0, 50.0)
        assertEquals(0.1, dewPoint, 0.5)
    }

    @Test
    fun `test surface temp estimation`() {
        // Przy czystym niebie (0) wychłodzenie to 4.5 stopnia
        val surfaceTemp = WeatherCalculations.estimateSurfaceTemp(5.0, 0)
        assertEquals(0.5, surfaceTemp, 0.1)
    }

    @Test
    fun `test celsius to fahrenheit conversion`() {
        assertEquals(32.0, WeatherCalculations.celsiusToFahrenheit(0.0), 0.1)
        assertEquals(212.0, WeatherCalculations.celsiusToFahrenheit(100.0), 0.1)
        assertEquals(-4.0, WeatherCalculations.celsiusToFahrenheit(-20.0), 0.1)
    }
}
