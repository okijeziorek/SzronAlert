package pl.oki.frostalert

import org.junit.Assert.*
import org.junit.Test
import pl.oki.frostalert.utils.WeatherCalculations

class WeatherCalculationsTest {

    @Test
    fun `test frost risk with clear sky and freezing temperature`() {
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
    fun `test wind impact on frost risk`() {
        // Warunki idealne do szronu, ale silny wiatr (20 km/h)
        val windRisk = WeatherCalculations.hasFrostRisk(
            temp = -2.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 0.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            windSpeed = 20.0
        )
        assertFalse("Silny wiatr powinien wyeliminować ryzyko szronu", windRisk)
        
        // Słaby wiatr (7 km/h) - ryzyko powinno nadal istnieć (z małą korektą)
        val lowWindRisk = WeatherCalculations.hasFrostRisk(
            temp = -2.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 0.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            windSpeed = 7.0
        )
        assertTrue("Słaby wiatr nie eliminuje ryzyka całkowicie", lowWindRisk)
    }

    @Test
    fun `test sensitivity multiplier`() {
        val noSensitivityRisk = WeatherCalculations.hasFrostRisk(
            temp = 5.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 0.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            sensitivity = 1.0
        )
        assertFalse("Brak ryzyka przy standardowej czułości", noSensitivityRisk)

        val highSensitivityRisk = WeatherCalculations.hasFrostRisk(
            temp = 5.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 0.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            sensitivity = 2.0
        )
        assertTrue("Ryzyko przy podwojonej czułości", highSensitivityRisk)
    }

    @Test
    fun `test surface temp estimation`() {
        val surfaceTemp = WeatherCalculations.estimateSurfaceTemp(5.0, 0)
        assertEquals(0.5, surfaceTemp, 0.1)
    }

    @Test
    fun `test celsius to fahrenheit conversion`() {
        assertEquals(32.0, WeatherCalculations.celsiusToFahrenheit(0.0), 0.1)
        assertEquals(212.0, WeatherCalculations.celsiusToFahrenheit(100.0), 0.1)
    }
}
