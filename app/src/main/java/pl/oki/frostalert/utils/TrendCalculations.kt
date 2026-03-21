package pl.oki.frostalert.utils

import pl.oki.frostalert.data.local.TemperatureRecord
import java.util.Calendar
import java.util.Locale

object TrendCalculations {
    
    /**
     * Dane trendu na 7 dni (dzisiaj + 6 dni wstecz)
     */
    data class DailyTrendPoint(
        val dayLabel: String,              // "Dziś", "Wczoraj", itd. 
        val minTemp: Double,
        val hasFrostRisk: Boolean,
        val timestamp: Long
    )

    /**
     * Statystyka trendu 7-dniowego
     */
    data class WeeklyTrendStats(
        val trendPoints: List<DailyTrendPoint>,
        val nightsWithFrostRisk: Int,              // Ile nocy z ryzykiem szronu
        val frostRiskPercentage: Double,           // 0-100: odsetek nocy zagrożonych
        val averageMinTemp: Double,                // Średnia najniższa temp
        val lowestTemp: Double,                    // Najniższa temperatura w tygodniu
        val trend: TrendDirection                  // UP, DOWN, STABLE
    )

    enum class TrendDirection {
        UP,      // Robi się coraz cieplej
        DOWN,    // Robi się coraz chłodniej
        STABLE   // Bez zmian
    }

    /**
     * Wylicza trend 7-dniowy na podstawie ostatnich rekordów
     */
    fun calculateWeeklyTrend(records: List<TemperatureRecord>): WeeklyTrendStats {
        if (records.isEmpty()) {
            return WeeklyTrendStats(
                trendPoints = emptyList(),
                nightsWithFrostRisk = 0,
                frostRiskPercentage = 0.0,
                averageMinTemp = 0.0,
                lowestTemp = 0.0,
                trend = TrendDirection.STABLE
            )
        }

        val dailySummaries = records
            .groupBy { startOfDay(it.timestamp) }
            .toList()
            .sortedByDescending { it.first }
            .take(7)
            .sortedBy { it.first }

        val trendPoints = dailySummaries.map { (dayStart, dayRecords) ->
            val minRecord = dayRecords.minBy { it.minTemp }
            DailyTrendPoint(
                dayLabel = getRelativeDayLabel(dayStart),
                minTemp = minRecord.minTemp,
                hasFrostRisk = dayRecords.any { it.hasRisk },
                timestamp = dayStart
            )
        }

        val nightsWithRisk = trendPoints.count { it.hasFrostRisk }
        val percentage = if (trendPoints.isNotEmpty()) (nightsWithRisk.toDouble() / trendPoints.size) * 100.0 else 0.0
        val avgTemp = trendPoints.map { it.minTemp }.average()
        val lowestTemp = trendPoints.minOfOrNull { it.minTemp } ?: 0.0

        // Trend liczony od najstarszych punktów do najnowszych.
        val trend = if (trendPoints.size >= 6) {
            val firstThreeAvg = trendPoints.take(3).map { it.minTemp }.average()
            val lastThreeAvg = trendPoints.takeLast(3).map { it.minTemp }.average()
            val delta = lastThreeAvg - firstThreeAvg

            when {
                delta > 1.5 -> TrendDirection.UP
                delta < -1.5 -> TrendDirection.DOWN
                else -> TrendDirection.STABLE
            }
        } else {
            TrendDirection.STABLE
        }

        return WeeklyTrendStats(
            trendPoints = trendPoints,
            nightsWithFrostRisk = nightsWithRisk,
            frostRiskPercentage = percentage,
            averageMinTemp = avgTemp,
            lowestTemp = lowestTemp,
            trend = trend
        )
    }

    /**
     * Wylicza przyszły trend 7-dniowy na podstawie prognozy pogody
     */
    fun calculateFutureWeeklyTrend(weatherResponse: pl.oki.frostalert.data.remote.WeatherResponse): WeeklyTrendStats {
        val hourly = weatherResponse.hourly
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Znajdź indeks dla jutra 00:00
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val tomorrowStart = calendar.timeInMillis

        // Znajdź indeks w hourly.time najbliższy do tomorrowStart
        var startIndex = 0
        for (i in hourly.time.indices) {
            val timeMillis = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(hourly.time[i])?.time ?: 0
            if (timeMillis >= tomorrowStart) {
                startIndex = i
                break
            }
        }

        val trendPoints = mutableListOf<DailyTrendPoint>()
        for (day in 0 until 7) {
            val dayStartIndex = startIndex + (day * 24)
            if (dayStartIndex + 24 > hourly.temperature.size) break

            // Znajdź min temp w nocy (20:00 - 08:00 następnego dnia)
            var minTemp = Double.MAX_VALUE
            for (hour in 20 until 32) { // 20:00 dziś do 08:00 jutro
                val index = dayStartIndex + hour
                if (index < hourly.temperature.size) {
                    minTemp = minOf(minTemp, hourly.temperature[index])
                }
            }
            if (minTemp == Double.MAX_VALUE) minTemp = hourly.temperature.getOrElse(dayStartIndex + 20) { 0.0 }

            // Oblicz ryzyko szronu (uproszczone: temp < 2°C, humidity > 70)
            val humidity = hourly.humidity.getOrElse(dayStartIndex + 20) { 80.0 }
            val hasFrostRisk = minTemp < 2.0 && humidity > 70.0

            val timestamp = tomorrowStart + (day * 24 * 60 * 60 * 1000L)
            trendPoints.add(DailyTrendPoint(
                dayLabel = getFutureDayLabel(day),
                minTemp = minTemp,
                hasFrostRisk = hasFrostRisk,
                timestamp = timestamp
            ))
        }

        val nightsWithRisk = trendPoints.count { it.hasFrostRisk }
        val percentage = if (trendPoints.isNotEmpty()) (nightsWithRisk.toDouble() / trendPoints.size) * 100.0 else 0.0
        val avgTemp = trendPoints.map { it.minTemp }.average()
        val lowestTemp = trendPoints.minOfOrNull { it.minTemp } ?: 0.0

        // Określ trend: porównaj średnią pierwszych 3 dni vs ostatnich 3 dni
        val trend = if (trendPoints.size >= 6) {
            val firstThreeAvg = trendPoints.take(3).map { it.minTemp }.average()
            val lastThreeAvg = trendPoints.takeLast(3).map { it.minTemp }.average()

            when {
                lastThreeAvg > firstThreeAvg + 2.0 -> TrendDirection.UP
                firstThreeAvg > lastThreeAvg + 2.0 -> TrendDirection.DOWN
                else -> TrendDirection.STABLE
            }
        } else {
            TrendDirection.STABLE
        }

        return WeeklyTrendStats(
            trendPoints = trendPoints,
            nightsWithFrostRisk = nightsWithRisk,
            frostRiskPercentage = percentage,
            averageMinTemp = avgTemp,
            lowestTemp = lowestTemp,
            trend = trend
        )
    }

    /**
     * Zwraca opis trendu w naturalnym języku
     */
    fun getTrendDescription(stats: WeeklyTrendStats): String {
        return when {
            stats.frostRiskPercentage > 70 -> "⚠️ Bardzo wysokie ryzyko szronu w tym tygodniu"
            stats.frostRiskPercentage > 40 -> "⚡ Umiarkowane ryzyko szronu"
            stats.frostRiskPercentage > 0 -> "🧊 Sporadyczne przymrozki"
            else -> "✅ Brak ryzyka szronu"
        }
    }

    /**
     * Zwraca emotikonę dla kierunku trendu
     */
    fun getTrendEmoji(trend: TrendDirection): String = when (trend) {
        TrendDirection.UP -> "📈 Robi się cieplej"
        TrendDirection.DOWN -> "📉 Robi się chłodniej"
        TrendDirection.STABLE -> "➡️ Brak zmian"
    }

    /**
     * Zwraca etykietę dnia (Dziś, Wczoraj, itd.)
     */
    private fun getRelativeDayLabel(timestamp: Long): String {
        val daysAgo = ((startOfDay(System.currentTimeMillis()) - startOfDay(timestamp)) / MILLIS_IN_DAY).toInt()
        return when (daysAgo) {
            0 -> "Dziś"
            1 -> "Wczoraj"
            in 2..6 -> "$daysAgo dni temu"
            else -> getDayOfWeekShort(timestamp)
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L

    /**
     * Zwraca etykietę przyszłego dnia (Jutro, Pojutrze, itd.)
     */
    private fun getFutureDayLabel(daysAhead: Int): String = when (daysAhead) {
        0 -> "Jutro"
        1 -> "Pojutrze"
        2 -> "Za 3 dni"
        3 -> "Za 4 dni"
        4 -> "Za 5 dni"
        5 -> "Za 6 dni"
        6 -> "Za 7 dni"
        else -> "Później"
    }

    /**
     * Zwraca skrót dnia tygodnia
     */
    fun getDayOfWeekShort(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Pn"
            Calendar.TUESDAY -> "Wt"
            Calendar.WEDNESDAY -> "Śr"
            Calendar.THURSDAY -> "Czw"
            Calendar.FRIDAY -> "Pt"
            Calendar.SATURDAY -> "Sb"
            Calendar.SUNDAY -> "Nd"
            else -> "?"
        }
        return dayOfWeek
    }
}
