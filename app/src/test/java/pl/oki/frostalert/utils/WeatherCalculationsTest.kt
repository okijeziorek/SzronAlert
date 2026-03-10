package pl.oki.frostalert.utils

import org.junit.Assert.*
import org.junit.Test
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
}
