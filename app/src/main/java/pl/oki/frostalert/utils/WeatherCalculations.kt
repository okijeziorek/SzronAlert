package pl.oki.frostalert.utils

import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.HourlyForecast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object WeatherCalculations {
    
    // Oblicza temperaturę punktu rosy (bardziej precyzyjny wzór Magnusa-Tetensa)
    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temp) / (b + temp)) + ln(humidity / 100.0)
        return (b * alpha) / (a - alpha)
    }

    // Estymuje temperaturę powierzchni (szyby/gruntu) na podstawie radiacji i zachmurzenia
    // Czyste niebo (weatherCode 0-1) powoduje radiacyjne wychłodzenie powierzchni poniżej temp. powietrza
    fun estimateSurfaceTemp(temp: Double, weatherCode: Int): Double {
        val coolingFactor = when (weatherCode) {
            0 -> 4.5  // Czyste niebo: silne wychłodzenie radiacyjne
            1 -> 3.5  // Małe zachmurzenie
            2 -> 2.5  // Częściowe zachmurzenie
            3 -> 1.5  // Zachmurzenie duże
            else -> 0.5 // Całkowite zachmurzenie / opady: minimalna różnica
        }
        return temp - coolingFactor
    }

    fun hasFrostRisk(
        temp: Double,
        humidity: Double,
        precip: Double,
        weatherCode: Int,
        tempThreshold: Double,
        humidityThreshold: Double,
        precipitationThreshold: Double
    ): Boolean {
        // Jeśli pada deszcz (nie śnieg), ryzyko szronu na szybach maleje (zmywanie), 
        // ale przy niskich temp może powstać gołoledź. Tu skupiamy się na szronie.
        if (precip > precipitationThreshold && weatherCode < 70) return false
        
        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode)
        
        // Szron powstaje gdy:
        // 1. Temp. powierzchni jest poniżej punktu zamarzania (lub progu użytkownika)
        // 2. Temp. powierzchni jest poniżej punktu rosy (resublimacja pary wodnej)
        // 3. Wilgotność jest wystarczająco wysoka
        return surfaceTemp <= tempThreshold && surfaceTemp <= dewPoint && humidity >= humidityThreshold
    }

    fun getWarningMessage(
        temp: Double, 
        humidity: Double, 
        precip: Double, 
        weatherCode: Int, 
        tempThreshold: Double, 
        humidityThreshold: Double, 
        precipitationThreshold: Double
    ): String {
        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode)
        val formattedTemp = String.format(Locale.US, "%.1f", temp)
        val formattedDewPoint = String.format(Locale.US, "%.1f", dewPoint)
        val formattedSurface = String.format(Locale.US, "%.1f", surfaceTemp)

        return if (hasFrostRisk(temp, humidity, precip, weatherCode, tempThreshold, humidityThreshold, precipitationThreshold)) {
            "Wysokie ryzyko szronu!\nPowietrze: $formattedTemp°C | Szyba: $formattedSurface°C\nPunkt rosy: $formattedDewPoint°C"
        } else {
            "Bezpiecznie – brak ryzyka szronu\nTemperatura: $formattedTemp°C"
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
