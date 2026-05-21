package pl.oki.frostalert.shared.core

object SummerCore {

    fun hasStormOrHailRisk(weatherCode: Int): Boolean {
        return weatherCode == 95 || weatherCode == 96 || weatherCode == 99
    }

    fun hasHeatRisk(currentTemp: Double, heatThreshold: Double): Boolean {
        return currentTemp >= heatThreshold
    }

    fun needsWatering(dailyPrecipitationSum: Double, tomorrowMaxTemp: Double): Boolean {
        return dailyPrecipitationSum < 2.0 && tomorrowMaxTemp > 25.0
    }
}
