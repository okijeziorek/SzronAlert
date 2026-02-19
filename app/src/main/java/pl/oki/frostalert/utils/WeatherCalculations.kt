package pl.oki.frostalert.utils

import pl.oki.frostalert.data.HourlyForecast
import pl.oki.frostalert.data.TemperatureRecord
import java.util.Locale
import kotlin.math.ln

object WeatherCalculations {
    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        val a = 17.27
        val b = 237.7
        val alpha = a * temp / (b + temp) + ln(humidity / 100.0)
        return b * alpha / (a - alpha)
    }

    fun estimateGlassTemp(temp: Double, weatherCode: Int): Double {
        val delta = when {
            weatherCode in 0..3 -> 3.0  // czyste niebo → mocne ochłodzenie
            else -> 1.0  // chmury → mniej
        }
        return temp - delta
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
        if (precip > precipitationThreshold) return false
        val dewPoint = calculateDewPoint(temp, humidity)
        val glassTemp = estimateGlassTemp(temp, weatherCode)
        return glassTemp <= tempThreshold && glassTemp <= dewPoint && humidity >= humidityThreshold
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
        val glassTemp = estimateGlassTemp(temp, weatherCode)
        val formattedTemp = String.format(Locale.US, "%.1f", temp)
        val formattedDewPoint = String.format(Locale.US, "%.1f", dewPoint)
        val formattedGlassTemp = String.format(Locale.US, "%.1f", glassTemp)

        return if (hasFrostRisk(temp, humidity, precip, weatherCode, tempThreshold, humidityThreshold, precipitationThreshold)) {
            "Wysokie ryzyko szronu!\nTemp: $formattedTemp °C\nPunkt rosy: $formattedDewPoint °C\nTemp szyby: $formattedGlassTemp °C"
        } else {
            "Bezpiecznie – brak ryzyka szronu\nTemp: $formattedTemp °C"
        }
    }
    fun getNightMinTemp(hourly: HourlyForecast): Double {
        val nightTemps = mutableListOf<Double>()
        hourly.time.forEachIndexed { index, time ->
            val hour = time.substring(11, 13).toInt()
            if (hour >= 20 || hour < 8) {
                nightTemps.add(hourly.temperature[index])
            }
        }
        return if (nightTemps.isNotEmpty()) nightTemps.minOrNull() ?: 0.0 else 0.0
    }
    fun calculateSeasonStats(records: List<TemperatureRecord>): Pair<Int, Double> {
        val riskCount = records.count { it.hasRisk }
        val avgMinTemp = if (records.isNotEmpty()) records.map { it.minTemp }.average() else 0.0
        return Pair(riskCount, avgMinTemp)
    }
}