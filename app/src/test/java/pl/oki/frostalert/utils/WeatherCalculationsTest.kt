package pl.oki.frostalert.utils

import org.junit.Assert.*
import org.junit.Test
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.HourlyForecast
import java.text.SimpleDateFormat
import java.util.*

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
    }

    @Test
    fun `test sensitivity multiplier`() {
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
    fun `getNightMinTemp returns minimum temperature from hourly data`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        
        // Zafiksujmy czas na 1 stycznia 2024, godzina 21:00
        val baseDate = sdf.parse("2024-01-01T21:00")!!
        
        val times = mutableListOf<String>()
        val temps = mutableListOf<Double>()
        
        // Generujemy 48h danych startując od baseDate - 24h
        val tempCalendar = Calendar.getInstance().apply { time = baseDate }
        tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
        
        repeat(48) {
            val timeStr = sdf.format(tempCalendar.time)
            times.add(timeStr)
            
            // Ustawiamy minimum (-4.0) dokładnie o 3:00 rano dnia "jutrzejszego"
            // (względem baseDate który jest o 21:00, okno to 20:00 - 08:00)
            if (tempCalendar.get(Calendar.HOUR_OF_DAY) == 3 && tempCalendar.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().apply { time = baseDate }.get(Calendar.DAY_OF_YEAR)) {
                temps.add(-4.0)
            } else {
                temps.add(5.0)
            }
            tempCalendar.add(Calendar.HOUR_OF_DAY, 1)
        }
        
        val hourly = HourlyForecast(
            time = times,
            temperature = temps,
            humidity = List(48) { 80.0 },
            precipitation = List(48) { 0.0 },
            weatherCode = List(48) { 0 },
            windSpeed = List(48) { 5.0 }
        )
        
        val minTemp = WeatherCalculations.getNightMinTemp(hourly, baseDate.time)
        assertEquals(-4.0, minTemp, 0.1)
    }

    @Test
    fun `calculateDewPoint returns correct value`() {
        val dewPoint = WeatherCalculations.calculateDewPoint(10.0, 50.0)
        assertEquals(0.1, dewPoint, 0.5)
    }

    @Test
    fun `celsiusToFahrenheit converts correctly`() {
        assertEquals(32.0, WeatherCalculations.celsiusToFahrenheit(0.0), 0.1)
        assertEquals(68.0, WeatherCalculations.celsiusToFahrenheit(20.0), 0.1)
    }

    // ── estimateSurfaceTemp ───────────────────────────────────────────────────

    @Test
    fun `estimateSurfaceTemp returns temp minus 1 for garden mode`() {
        val surface = WeatherCalculations.estimateSurfaceTemp(
            temp = 5.0, weatherCode = 0, sensitivity = 1.0, appMode = 1
        )
        assertEquals(4.0, surface, 0.01)
    }

    @Test
    fun `estimateSurfaceTemp applies maximum cooling for clear sky in car mode`() {
        val surface = WeatherCalculations.estimateSurfaceTemp(
            temp = 5.0, weatherCode = 0, sensitivity = 1.0, appMode = 0
        )
        assertEquals(0.5, surface, 0.01) // 5 - 4.5*1.0 = 0.5
    }

    @Test
    fun `estimateSurfaceTemp scales with sensitivity`() {
        val surface = WeatherCalculations.estimateSurfaceTemp(
            temp = 5.0, weatherCode = 0, sensitivity = 2.0, appMode = 0
        )
        assertEquals(-4.0, surface, 0.01) // 5 - 4.5*2.0 = -4.0
    }

    @Test
    fun `estimateSurfaceTemp applies minimal cooling for overcast sky`() {
        val surface = WeatherCalculations.estimateSurfaceTemp(
            temp = 5.0, weatherCode = 3, sensitivity = 1.0, appMode = 0
        )
        assertEquals(3.5, surface, 0.01) // 5 - 1.5*1.0 = 3.5
    }

    // ── getGardenTip ─────────────────────────────────────────────────────────

    @Test
    fun `getGardenTip returns safe message for positive temperature`() {
        val tip = WeatherCalculations.getGardenTip(0.5)
        assertTrue(tip.contains("Bezpiecznie"))
    }

    @Test
    fun `getGardenTip returns light frost message for temp between 0 and -2`() {
        val tip = WeatherCalculations.getGardenTip(-1.0)
        assertTrue(tip.contains("Lekki"))
    }

    @Test
    fun `getGardenTip returns moderate frost message for temp between -2 and -5`() {
        val tip = WeatherCalculations.getGardenTip(-3.0)
        assertTrue(tip.contains("Umiarkowany"))
    }

    @Test
    fun `getGardenTip returns severe frost message below -5`() {
        val tip = WeatherCalculations.getGardenTip(-6.0)
        assertTrue(tip.contains("Silny"))
    }

    // ── calculateFrostDuration ────────────────────────────────────────────────

    @Test
    fun `calculateFrostDuration counts hours at or below threshold`() {
        val hourly = HourlyForecast(
            time = List(5) { "2024-01-01T0${it}:00" },
            temperature = listOf(-2.0, -1.0, 0.0, 1.0, 2.0),
            humidity = List(5) { 80.0 },
            precipitation = List(5) { 0.0 },
            weatherCode = List(5) { 0 },
            windSpeed = List(5) { 5.0 }
        )
        val duration = WeatherCalculations.calculateFrostDuration(hourly, threshold = 0.0)
        assertEquals(3, duration) // -2, -1, 0 are <= 0.0
    }

    @Test
    fun `calculateFrostDuration returns 0 when no hours below threshold`() {
        val hourly = HourlyForecast(
            time = List(3) { "2024-01-01T0${it}:00" },
            temperature = listOf(5.0, 6.0, 7.0),
            humidity = List(3) { 80.0 },
            precipitation = List(3) { 0.0 },
            weatherCode = List(3) { 0 },
            windSpeed = List(3) { 5.0 }
        )
        val duration = WeatherCalculations.calculateFrostDuration(hourly, threshold = 0.0)
        assertEquals(0, duration)
    }

    // ── calculateSeasonStats ─────────────────────────────────────────────────

    @Test
    fun `calculateSeasonStats returns correct risk count and average`() {
        val records = listOf(
            TemperatureRecord(timestamp = 1000L, minTemp = -3.0, hasRisk = true),
            TemperatureRecord(timestamp = 2000L, minTemp = 1.0, hasRisk = false),
            TemperatureRecord(timestamp = 3000L, minTemp = -1.0, hasRisk = true)
        )
        val (riskCount, avgTemp) = WeatherCalculations.calculateSeasonStats(records)
        assertEquals(2, riskCount)
        assertEquals((-3.0 + 1.0 + -1.0) / 3, avgTemp, 0.01)
    }

    @Test
    fun `calculateSeasonStats returns zero for empty list`() {
        val (riskCount, avgTemp) = WeatherCalculations.calculateSeasonStats(emptyList())
        assertEquals(0, riskCount)
        assertEquals(0.0, avgTemp, 0.01)
    }

    // ── getWarningMessage ─────────────────────────────────────────────────────

    @Test
    fun `getWarningMessage returns safe message for high wind`() {
        val msg = WeatherCalculations.getWarningMessage(
            temp = -2.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 0.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            windSpeed = 20.0
        )
        assertTrue(msg.contains("silny wiatr") || msg.contains("Bezpiecznie"))
    }

    @Test
    fun `getWarningMessage returns frost risk message when risk is present`() {
        val msg = WeatherCalculations.getWarningMessage(
            temp = 1.0,
            humidity = 85.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 1.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2
        )
        assertTrue(msg.contains("ryzyko") || msg.contains("Ryzyko"))
    }

    @Test
    fun `getWarningMessage returns garden prefix in garden mode`() {
        val msg = WeatherCalculations.getWarningMessage(
            temp = 1.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 2.0,
            humidityThreshold = 75.0,
            precipitationThreshold = 0.2,
            sensitivity = 1.0,
            windSpeed = 0.0,
            appMode = 1
        )
        assertTrue(msg.contains("ogrodzie") || msg.contains("Bezpiecznie"))
    }

    // ── calculateFrostProbability ──────────────────────────────────────────────

    @Test
    fun `calculateFrostProbability returns 0 for high wind`() {
        val prob = WeatherCalculations.calculateFrostProbability(
            temp = -5.0, humidity = 95.0, precip = 0.0, weatherCode = 0,
            tempThreshold = 1.0, humidityThreshold = 75.0, precipitationThreshold = 0.2,
            windSpeed = 20.0
        )
        assertEquals(0, prob)
    }

    @Test
    fun `calculateFrostProbability returns high value for extreme frost conditions`() {
        val prob = WeatherCalculations.calculateFrostProbability(
            temp = -5.0, humidity = 95.0, precip = 0.0, weatherCode = 0,
            tempThreshold = 1.0, humidityThreshold = 75.0, precipitationThreshold = 0.2,
            windSpeed = 0.0
        )
        assertTrue("Should be high probability: $prob", prob >= 70)
    }

    @Test
    fun `calculateFrostProbability returns moderate value for borderline conditions`() {
        val prob = WeatherCalculations.calculateFrostProbability(
            temp = 2.0, humidity = 80.0, precip = 0.0, weatherCode = 2,
            tempThreshold = 1.0, humidityThreshold = 75.0, precipitationThreshold = 0.2,
            windSpeed = 3.0
        )
        assertTrue("Should be moderate probability: $prob", prob in 20..70)
    }

    @Test
    fun `calculateFrostProbability returns low value for warm conditions`() {
        val prob = WeatherCalculations.calculateFrostProbability(
            temp = 10.0, humidity = 50.0, precip = 0.0, weatherCode = 3,
            tempThreshold = 1.0, humidityThreshold = 75.0, precipitationThreshold = 0.2,
            windSpeed = 0.0
        )
        assertTrue("Should be low probability: $prob", prob < 20)
    }

    @Test
    fun `calculateFrostProbability is always between 0 and 100`() {
        val testCases = listOf(
            Triple(-20.0, 100.0, 0.0),  // extreme cold
            Triple(30.0, 10.0, 0.0),     // extreme warm
            Triple(0.0, 80.0, 5.0),      // moderate with precip
        )
        testCases.forEach { (temp, humidity, wind) ->
            val prob = WeatherCalculations.calculateFrostProbability(
                temp = temp, humidity = humidity, precip = 0.0, weatherCode = 0,
                tempThreshold = 1.0, humidityThreshold = 75.0, precipitationThreshold = 0.2,
                windSpeed = wind
            )
            assertTrue("Probability should be 0-100, got $prob for temp=$temp", prob in 0..100)
        }
    }

    @Test
    fun `getFrostProbabilityLevel returns correct levels`() {
        assertEquals(WeatherCalculations.FrostProbabilityLevel.MINIMAL, WeatherCalculations.getFrostProbabilityLevel(10))
        assertEquals(WeatherCalculations.FrostProbabilityLevel.LOW, WeatherCalculations.getFrostProbabilityLevel(25))
        assertEquals(WeatherCalculations.FrostProbabilityLevel.MODERATE, WeatherCalculations.getFrostProbabilityLevel(50))
        assertEquals(WeatherCalculations.FrostProbabilityLevel.HIGH, WeatherCalculations.getFrostProbabilityLevel(65))
        assertEquals(WeatherCalculations.FrostProbabilityLevel.VERY_HIGH, WeatherCalculations.getFrostProbabilityLevel(90))
    }
}
