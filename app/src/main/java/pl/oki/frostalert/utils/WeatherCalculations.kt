package pl.oki.frostalert.utils

import android.content.Context
import android.util.Log
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.HourlyForecast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

object WeatherCalculations {

    private const val TAG = "WeatherCalculations"
    
    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        if (humidity <= 0.0 || humidity > 100.0) {
            Log.w(TAG, "calculateDewPoint: invalid humidity=$humidity, falling back to temp=$temp")
            return temp
        }
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temp) / (b + temp)) + ln(humidity / 100.0)
        val result = (b * alpha) / (a - alpha)
        if (result.isNaN() || result.isInfinite()) {
            Log.w(TAG, "calculateDewPoint: NaN/Inf result for temp=$temp humidity=$humidity, falling back to temp")
            return temp
        }
        return result
    }

    fun estimateSurfaceTemp(temp: Double, weatherCode: Int, sensitivity: Double = 1.0, appMode: Int = 0): Double {
        if (appMode == 1) { // Tryb Ogród: mniejszy wpływ radiacji na liście niż na szkło
            return temp - 1.0 
        }
        
        val baseCoolingFactor = when (weatherCode) {
            0 -> 4.5
            1 -> 3.5
            2 -> 2.5
            3 -> 1.5
            else -> 0.5
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
        windSpeed: Double = 0.0,
        appMode: Int = 0
    ): Boolean {
        if (windSpeed > 15.0) return false
        if (precip > precipitationThreshold && weatherCode < 70) return false
        
        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity, appMode)
        
        val windAdjustment = if (windSpeed > 5.0) 0.5 else 0.0
        
        return (surfaceTemp + windAdjustment) <= tempThreshold && 
               (surfaceTemp + windAdjustment) <= dewPoint && 
               humidity >= humidityThreshold
    }

    /**
     * Oblicza prawdopodobieństwo szronu jako wartość 0-100.
     * Uwzględnia: odległość temperatury od progu, punkt rosy, wilgotność,
     * kod pogody (czyste niebo = wyższe ryzyko) oraz wiatr.
     */
    fun calculateFrostProbability(
        temp: Double,
        humidity: Double,
        precip: Double,
        weatherCode: Int,
        tempThreshold: Double,
        humidityThreshold: Double,
        precipitationThreshold: Double,
        sensitivity: Double = 1.0,
        windSpeed: Double = 0.0,
        appMode: Int = 0
    ): Int {
        // Wiatr >15 km/h eliminuje ryzyko szronu
        if (windSpeed > 15.0) return 0

        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity, appMode)
        val windAdjustment = if (windSpeed > 5.0) 0.5 else 0.0
        val effectiveSurface = surfaceTemp + windAdjustment

        // 1. Czynnik temperaturowy: ile poniżej progu (0-40 punktów)
        val tempDelta = tempThreshold - effectiveSurface
        val tempFactor = (tempDelta.coerceIn(0.0, 8.0) / 8.0 * 40.0).toInt()

        // 2. Czynnik wilgotności: bliskość 100% (0-25 punktów)
        val humidityExcess = humidity - humidityThreshold
        val humidityFactor = if (humidityExcess >= 0) {
            val range = 100.0 - humidityThreshold
            if (range > 0) (humidityExcess / range * 25.0).toInt() else 25
        } else {
            0
        }

        // 3. Czynnik klarowności nieba (0-20 punktów)
        val skyFactor = when (weatherCode) {
            0 -> 20
            1 -> 15
            2 -> 10
            3 -> 5
            else -> 0
        }

        // 4. Bliskość punktu rosy: powierzchnia <= punkt rosy → +15
        val dewPointBonus = if (effectiveSurface <= dewPoint) 15 else 0

        var score = tempFactor + humidityFactor + skyFactor + dewPointBonus

        // 5. Kara za wiatr
        when {
            windSpeed > 10.0 -> score -= 30
            windSpeed > 5.0 -> score -= 15
        }

        // Opady z kodem pogody < 70 (deszcz, nie śnieg/mgła) zmniejszają ryzyko szronu
        if (precip > precipitationThreshold && weatherCode < 70) {
            score -= 20
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Poziom ryzyka szronu na podstawie prawdopodobieństwa.
     */
    enum class FrostProbabilityLevel {
        VERY_HIGH, HIGH, MODERATE, LOW, MINIMAL
    }

    /**
     * Zwraca poziom ryzyka szronu na podstawie prawdopodobieństwa (0-100).
     */
    fun getFrostProbabilityLevel(probability: Int): FrostProbabilityLevel {
        return when {
            probability >= 80 -> FrostProbabilityLevel.VERY_HIGH
            probability >= 60 -> FrostProbabilityLevel.HIGH
            probability >= 40 -> FrostProbabilityLevel.MODERATE
            probability >= 20 -> FrostProbabilityLevel.LOW
            else -> FrostProbabilityLevel.MINIMAL
        }
    }

    /**
     * Oblicza czas trwania przymrozku (ile godzin poniżej progu)
     */
    fun calculateFrostDuration(hourly: HourlyForecast, threshold: Double): Int {
        return hourly.temperature.count { it != null && it <= threshold }
    }

    /**
     * Zwraca konkretną poradę dla ogrodnika na podstawie intensywności mrozu (bez kontekstu – hardcoded Polish strings).
     * Używany w testach jednostkowych i kodzie niewymagającym lokalizacji.
     */
    fun getGardenTip(minTemp: Double): String {
        return when {
            minTemp > 0 -> "Bezpiecznie dla roślin"
            minTemp > -2 -> "Lekki przymrozek – okryj wrażliwe rośliny"
            minTemp > -5 -> "Umiarkowany mróz – zabezpiecz rośliny"
            else -> "Silny mróz – ryzyko poważnych szkód"
        }
    }

    /**
     * Zwraca konkretną poradę dla ogrodnika na podstawie intensywności mrozu
     */
    fun getGardenTip(context: Context, minTemp: Double): String {
        return when {
            minTemp > 0 -> context.getString(R.string.garden_tip_safe)
            minTemp > -2 -> context.getString(R.string.garden_tip_light_frost)
            minTemp > -5 -> context.getString(R.string.garden_tip_moderate_frost)
            else -> context.getString(R.string.garden_tip_severe_frost)
        }
    }

    fun celsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9 / 5) + 32
    }

    fun formatTemperature(temp: Double, useFahrenheit: Boolean): String {
        val converted = if (useFahrenheit) celsiusToFahrenheit(temp) else temp
        val unit = if (useFahrenheit) "°F" else "°C"
        return String.format(Locale.US, "%.1f%s", converted, unit)
    }

    /**
     * Generuje komunikat ostrzegawczy (bez kontekstu – hardcoded Polish strings).
     * Używany w testach jednostkowych i kodzie niewymagającym lokalizacji.
     */
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
        appMode: Int = 0,
        useFahrenheit: Boolean = false
    ): String {
        if (windSpeed > 15.0) return "⚠️ Brak ryzyka: silny wiatr powyżej 15 km/h"

        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity, appMode)

        val formattedTemp = formatTemperature(temp, useFahrenheit)
        val formattedDewPoint = formatTemperature(dewPoint, useFahrenheit)
        val formattedSurface = formatTemperature(surfaceTemp, useFahrenheit)

        return if (hasFrostRisk(temp, humidity, precip, weatherCode, tempThreshold, humidityThreshold, precipitationThreshold, sensitivity, windSpeed, appMode)) {
            val prefix = if (appMode == 1) "⚠️ Ryzyko szronu w ogrodzie!" else "⚠️ Wysokie ryzyko szronu!"
            "$prefix Temp: $formattedTemp, powierzchnia: $formattedSurface, punkt rosy: $formattedDewPoint"
        } else {
            "✅ Bezpiecznie – brak ryzyka szronu. Temperatura: $formattedTemp"
        }
    }

    fun getWarningMessage(
        context: Context,
        temp: Double,
        humidity: Double,
        precip: Double,
        weatherCode: Int,
        tempThreshold: Double,
        humidityThreshold: Double,
        precipitationThreshold: Double,
        sensitivity: Double = 1.0,
        windSpeed: Double = 0.0,
        appMode: Int = 0,
        useFahrenheit: Boolean = false
    ): String {
        if (windSpeed > 15.0) return context.getString(R.string.warning_strong_wind)

        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity, appMode)

        val formattedTemp = formatTemperature(temp, useFahrenheit)
        val formattedDewPoint = formatTemperature(dewPoint, useFahrenheit)
        val formattedSurface = formatTemperature(surfaceTemp, useFahrenheit)

        return if (hasFrostRisk(temp, humidity, precip, weatherCode, tempThreshold, humidityThreshold, precipitationThreshold, sensitivity, windSpeed, appMode)) {
            val prefix = if (appMode == 1)
                context.getString(R.string.warning_frost_risk_garden)
            else
                context.getString(R.string.warning_frost_risk_high)
            context.getString(R.string.warning_frost_air_surface, prefix, formattedTemp, formattedSurface, formattedDewPoint)
        } else {
            context.getString(R.string.warning_no_frost_risk, formattedTemp)
        }
    }

    fun getNightMinTemp(hourly: HourlyForecast, referenceTime: Long = System.currentTimeMillis()): Double {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        val calendar = Calendar.getInstance().apply { timeInMillis = referenceTime }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val startCalendar = Calendar.getInstance().apply { timeInMillis = referenceTime }
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
        
        val endCalendar = Calendar.getInstance().apply { timeInMillis = startCalendar.timeInMillis }
        endCalendar.add(Calendar.HOUR_OF_DAY, 12) 
        
        val startTime = startCalendar.timeInMillis
        val endTime = endCalendar.timeInMillis

        val nightTemps = mutableListOf<Double>()
        hourly.time.forEachIndexed { index, timeStr ->
            try {
                if (index >= hourly.temperature.size) return@forEachIndexed
                val temp = hourly.temperature[index] ?: return@forEachIndexed
                val time = sdf.parse(timeStr)?.time ?: 0L
                if (time in startTime..endTime) {
                    nightTemps.add(temp)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing time entry at index $index: ${e.message}")
            }
        }
        
        val fallback = hourly.temperature.take(12).filterNotNull().minOrNull()
        if (fallback == null) {
            Log.w(TAG, "getNightMinTemp: no temperature data available in forecast, returning 0.0")
        } else if (nightTemps.isEmpty()) {
            Log.w(TAG, "getNightMinTemp: no data in night window, using first 12 hours fallback=$fallback")
        }
        return nightTemps.minOrNull() ?: fallback ?: 0.0
    }

    fun calculateSeasonStats(records: List<TemperatureRecord>): Pair<Int, Double> {
        val riskCount = records.count { it.hasRisk }
        val avgMinTemp = if (records.isNotEmpty()) records.map { it.minTemp }.average() else 0.0
        return Pair(riskCount, avgMinTemp)
    }

    /**
     * Converts a boolean frost risk + minTemp into a 0.0–1.0 risk level score
     * used by geofencing and broadcast receivers for comparison purposes.
     */
    fun calculateRiskLevel(hasRisk: Boolean, minTemp: Double): Double {
        return when {
            hasRisk && minTemp < -5.0 -> 1.0
            hasRisk && minTemp < 0.0 -> 0.7
            hasRisk -> 0.5
            minTemp < 2.0 -> 0.2
            else -> 0.0
        }
    }
}
