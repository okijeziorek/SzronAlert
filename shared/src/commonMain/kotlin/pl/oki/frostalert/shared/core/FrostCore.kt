package pl.oki.frostalert.shared.core

import kotlin.math.ln

object FrostCore {

    enum class ProbabilityLevel {
        VERY_HIGH, HIGH, MODERATE, LOW, MINIMAL
    }

    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        if (humidity <= 0.0 || humidity > 100.0) return temp
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temp) / (b + temp)) + ln(humidity / 100.0)
        val result = (b * alpha) / (a - alpha)
        return if (result.isNaN() || result.isInfinite()) temp else result
    }

    fun estimateSurfaceTemp(temp: Double, weatherCode: Int, sensitivity: Double = 1.0, appMode: Int = 0): Double {
        if (appMode == 1) return temp - 1.0
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
        if (windSpeed > 15.0) return 0

        val dewPoint = calculateDewPoint(temp, humidity)
        val surfaceTemp = estimateSurfaceTemp(temp, weatherCode, sensitivity, appMode)
        val windAdjustment = if (windSpeed > 5.0) 0.5 else 0.0
        val effectiveSurface = surfaceTemp + windAdjustment

        val tempDelta = tempThreshold - effectiveSurface
        val tempFactor = (tempDelta.coerceIn(0.0, 8.0) / 8.0 * 40.0).toInt()

        val humidityExcess = humidity - humidityThreshold
        val humidityFactor = if (humidityExcess >= 0) {
            val range = 100.0 - humidityThreshold
            if (range > 0) (humidityExcess / range * 25.0).toInt() else 25
        } else {
            0
        }

        val skyFactor = when (weatherCode) {
            0 -> 20
            1 -> 15
            2 -> 10
            3 -> 5
            else -> 0
        }

        val dewPointBonus = if (effectiveSurface <= dewPoint) 15 else 0
        var score = tempFactor + humidityFactor + skyFactor + dewPointBonus

        when {
            windSpeed > 10.0 -> score -= 30
            windSpeed > 5.0 -> score -= 15
        }

        if (precip > precipitationThreshold && weatherCode < 70) {
            score -= 20
        }

        return score.coerceIn(0, 100)
    }

    fun getFrostProbabilityLevel(probability: Int): ProbabilityLevel = when {
        probability >= 80 -> ProbabilityLevel.VERY_HIGH
        probability >= 60 -> ProbabilityLevel.HIGH
        probability >= 40 -> ProbabilityLevel.MODERATE
        probability >= 20 -> ProbabilityLevel.LOW
        else -> ProbabilityLevel.MINIMAL
    }

    fun calculateRiskLevel(hasRisk: Boolean, minTemp: Double): Double = when {
        hasRisk && minTemp < -5.0 -> 1.0
        hasRisk && minTemp < 0.0 -> 0.7
        hasRisk -> 0.5
        minTemp < 2.0 -> 0.2
        else -> 0.0
    }
}
