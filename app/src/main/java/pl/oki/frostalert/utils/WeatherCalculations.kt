package pl.oki.frostalert.utils

import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.HourlyForecast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln

object WeatherCalculations {
    
    // Oblicza temperaturę punktu rosy (bardziej precyzyjny wzór Magnusa-Tetensa)
    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temp) / (b + temp)) + ln(humidity / 100.0)
        return (b * alpha) / (a - alpha)
    }

    // Estymuje temperaturę powierzchni (szyby/gruntu) na podstawie radiacji i zachmurzenia
    fun estimateSurfaceTemp(temp: Double, weatherCode: Int, sensitivity: Double = 1.0): Double {
        val baseCoolingFactor = when (weatherCode) {
            0 -> 4.5  // Czyste niebo: silne wychłodzenie radiacyjne
            1 -> 3.5  // Małe zachmurzenie
            2 -> 2.5  // Częściowe zachmurzenie
            3 -> 1.5  // Zachmurzenie duże
            else -> 0.5 // Całkowite zachmurzenie / opady
        }
        return temp - (baseCoolingFactor * sensitivity)
    }

    fun hasFrostRisk(
        temp: Double,
        humidity: Double,
        precip: Double,
        weatherCode: Int,
        tempThreshold: Double,
        humidityThreshold: Double,
        precipitationThreshold: Double,
        sensitivity: Double = 1.0,
        windSpeed: Double = 0.0
    ): Boolean {
        // Nowa logika: silny wiatr (powyżej 15 km/h) drastycznie zmniejsza szansę na osiadanie szronu
        if (windSpeed > 15.0) return false
        
        if (precip > precipitationThreshold && weatherCode < 70) return false
        
        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity)
        
        // Wiatr lekki (5-15 km/h) lekko podnosi temperaturę powierzchniową (miesza powietrze)
        val windAdjustment = if (windSpeed > 5.0) 0.5 else 0.0
        
        return (surfaceTemp + windAdjustment) <= tempThreshold && 
               (surfaceTemp + windAdjustment) <= dewPoint && 
               humidity >= humidityThreshold
    }

    fun celsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9 / 5) + 32
    }

    fun formatTemperature(temp: Double, useFahrenheit: Boolean): String {
        val converted = if (useFahrenheit) celsiusToFahrenheit(temp) else temp
        val unit = if (useFahrenheit) "°F" else "°C"
        return String.format(Locale.US, "%.1f%s", converted, unit)
    }

    fun getWarningMessage(
        temp: Double, 
        humidity: Double, 
        precip: Double, 
        weatherCode: Int, 
        tempThreshold: Double, 
        humidityThreshold: Double, 
        precipitationThreshold: Double,
        sensitivity: Double = 1.0,
        windSpeed: Double = 0.0,
        useFahrenheit: Boolean = false
    ): String {
        if (windSpeed > 15.0) return "Bezpiecznie – silny wiatr zapobiega osadzaniu szronu."
        
        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity)
        
        val formattedTemp = formatTemperature(temp, useFahrenheit)
        val formattedDewPoint = formatTemperature(dewPoint, useFahrenheit)
        val formattedSurface = formatTemperature(surfaceTemp, useFahrenheit)

        return if (hasFrostRisk(temp, humidity, precip, weatherCode, tempThreshold, humidityThreshold, precipitationThreshold, sensitivity, windSpeed)) {
            "Wysokie ryzyko szronu!\nPowietrze: $formattedTemp | Szyba: $formattedSurface\nPunkt rosy: $formattedDewPoint"
        } else {
            "Bezpiecznie – brak ryzyka szronu\nTemperatura: $formattedTemp"
        }
    }

    fun getNightMinTemp(hourly: HourlyForecast): Double {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val startCalendar = Calendar.getInstance()
        if (currentHour >= 8) {
            startCalendar.set(Calendar.HOUR_OF_DAY, 20)
            startCalendar.set(Calendar.MINUTE, 0)
        } else {
            startCalendar.add(Calendar.DAY_OF_YEAR, -1)
            startCalendar.set(Calendar.HOUR_OF_DAY, 20)
            startCalendar.set(Calendar.MINUTE, 0)
        }
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)
        
        val endCalendar = Calendar.getInstance()
        endCalendar.time = startCalendar.time
        endCalendar.add(Calendar.HOUR_OF_DAY, 12) 
        
        val startTime = startCalendar.timeInMillis
        val endTime = endCalendar.timeInMillis

        val nightTemps = mutableListOf<Double>()
        hourly.time.forEachIndexed { index, timeStr ->
            try {
                val time = sdf.parse(timeStr)?.time ?: 0L
                if (time in startTime..endTime) {
                    nightTemps.add(hourly.temperature[index])
                }
            } catch (e: Exception) {}
        }
        
        return nightTemps.minOrNull() ?: hourly.temperature.take(12).minOrNull() ?: 0.0
    }

    fun calculateSeasonStats(records: List<TemperatureRecord>): Pair<Int, Double> {
        val riskCount = records.count { it.hasRisk }
        val avgMinTemp = if (records.isNotEmpty()) records.map { it.minTemp }.average() else 0.0
        return Pair(riskCount, avgMinTemp)
    }
}
