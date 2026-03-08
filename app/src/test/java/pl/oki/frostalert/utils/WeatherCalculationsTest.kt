package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.data.remote.HourlyForecast

class WeatherCalculationsTest {

    @Test
    fun `calculateDewPoint returns correct value for typical conditions`() {
        // Given: 20°C, 60% humidity
        val temp = 20.0
        val humidity = 60.0

        // When
        val dewPoint = WeatherCalculations.calculateDewPoint(temp, humidity)

        // Then: Expected dew point around 12°C
        assertEquals(12.0, dewPoint, 1.0)
    }

    @Test
    fun `calculateDewPoint returns correct value for high humidity`() {
        // Given: 25°C, 80% humidity
        val temp = 25.0
        val humidity = 80.0

        // When
        val dewPoint = WeatherCalculations.calculateDewPoint(temp, humidity)

        // Then: Expected dew point around 21°C
        assertEquals(21.0, dewPoint, 1.0)
    }

    @Test
    fun `estimateSurfaceTemp reduces temperature for clear sky`() {
        // Given: 10°C, clear sky (code 0), normal sensitivity
        val temp = 10.0
        val weatherCode = 0
        val sensitivity = 1.0

        // When
        val surfaceTemp = WeatherCalculations.estimateSurfaceTemp(temp, weatherCode, sensitivity)

        // Then: Should be significantly cooler (10 - 4.5 = 5.5°C)
        assertEquals(5.5, surfaceTemp, 0.1)
    }

    @Test
    fun `estimateSurfaceTemp sensitivity affects cooling rate`() {
        // Given: Same conditions, higher sensitivity
        val temp = 10.0
        val weatherCode = 0
        val normalSensitivity = 1.0
        val highSensitivity = 1.5

        // When
        val normalTemp = WeatherCalculations.estimateSurfaceTemp(temp, weatherCode, normalSensitivity)
        val highTemp = WeatherCalculations.estimateSurfaceTemp(temp, weatherCode, highSensitivity)

        // Then: Higher sensitivity should result in lower temperature
        assertTrue(highTemp < normalTemp)
    }

    @Test
    fun `hasFrostRisk returns true for frost conditions`() {
        // Given: Cold temperature, high humidity, clear sky
        val temp = -2.0
        val humidity = 85.0
        val precip = 0.0
        val weatherCode = 0
        val tempThreshold = 1.0
        val humidityThreshold = 75.0
        val precipitationThreshold = 0.2

        // When
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp, humidity, precip, weatherCode,
            tempThreshold, humidityThreshold, precipitationThreshold
        )

        // Then
        assertTrue(hasRisk)
    }

    @Test
    fun `hasFrostRisk returns false for warm temperature`() {
        // Given: Warm temperature
        val temp = 15.0
        val humidity = 85.0
        val precip = 0.0
        val weatherCode = 0
        val tempThreshold = 1.0
        val humidityThreshold = 75.0
        val precipitationThreshold = 0.2

        // When
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp, humidity, precip, weatherCode,
            tempThreshold, humidityThreshold, precipitationThreshold
        )

        // Then
        assertFalse(hasRisk)
    }

    @Test
    fun `hasFrostRisk returns false for strong wind`() {
        // Given: Cold but strong wind
        val temp = -2.0
        val humidity = 85.0
        val precip = 0.0
        val weatherCode = 0
        val tempThreshold = 1.0
        val humidityThreshold = 75.0
        val precipitationThreshold = 0.2
        val windSpeed = 20.0 // Strong wind

        // When
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp, humidity, precip, weatherCode,
            tempThreshold, humidityThreshold, precipitationThreshold,
            windSpeed = windSpeed
        )

        // Then: Strong wind should prevent frost risk
        assertFalse(hasRisk)
    }

    @Test
    fun `hasFrostRisk returns false for precipitation`() {
        // Given: Cold but raining
        val temp = -2.0
        val humidity = 85.0
        val precip = 0.5 // Raining
        val weatherCode = 61 // Rain code
        val tempThreshold = 1.0
        val humidityThreshold = 75.0
        val precipitationThreshold = 0.2

        // When
        val hasRisk = WeatherCalculations.hasFrostRisk(
            temp, humidity, precip, weatherCode,
            tempThreshold, humidityThreshold, precipitationThreshold
        )

        // Then: Precipitation should prevent frost risk
        assertFalse(hasRisk)
    }

    @Test
    fun `getNightMinTemp returns minimum temperature from hourly data`() {
        // Given: Hourly temperatures with minimum at night
        val hourly = HourlyForecast(
            time = List(24) { "2024-01-01T${it.toString().padStart(2, '0')}:00" },
            temperature = listOf(
                15.0, 12.0, 10.0, 8.0, 6.0, 4.0, 2.0, 1.0,  // Day
                0.0, -1.0, -2.0, -3.0, -4.0, -3.0, -2.0, -1.0, // Night (min -4.0)
                0.0, 2.0, 5.0, 8.0, 12.0, 15.0, 18.0, 20.0   // Next day
            ),
            humidity = List(24) { 50.0 },
            precipitation = List(24) { 0.0 },
            weatherCode = List(24) { 0 },
            windSpeed = List(24) { 5.0 }
        )

        // When
        val minTemp = WeatherCalculations.getNightMinTemp(hourly)

        // Then: Should return -4.0 (minimum during night hours)
        assertEquals(-4.0, minTemp, 0.1)
    }

    @Test
    fun `getNightMinTemp handles empty data gracefully`() {
        // Given: Empty hourly data
        val hourly = HourlyForecast(
            time = emptyList(),
            temperature = emptyList(),
            humidity = emptyList(),
            precipitation = emptyList(),
            weatherCode = emptyList(),
            windSpeed = emptyList()
        )

        // When
        val minTemp = WeatherCalculations.getNightMinTemp(hourly)

        // Then: Should return a reasonable default (0.0)
        assertEquals(0.0, minTemp, 0.1)
    }

    @Test
    fun `celsiusToFahrenheit converts correctly`() {
        // Given
        val celsius = 20.0

        // When
        val fahrenheit = WeatherCalculations.celsiusToFahrenheit(celsius)

        // Then: 20°C = 68°F
        assertEquals(68.0, fahrenheit, 0.1)
    }

    @Test
    fun `celsiusToFahrenheit converts freezing point correctly`() {
        // Given
        val celsius = 0.0

        // When
        val fahrenheit = WeatherCalculations.celsiusToFahrenheit(celsius)

        // Then: 0°C = 32°F
        assertEquals(32.0, fahrenheit, 0.1)
    }
}
