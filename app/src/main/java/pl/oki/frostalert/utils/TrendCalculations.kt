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

        // Grupuj rekordy po dniach (użyj początku dnia jako klucz)
        val calendar = Calendar.getInstance()
        val dailyRecords = records.groupBy { record ->
            calendar.timeInMillis = record.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.mapValues { (_, dayRecords) ->
            // Dla każdego dnia weź rekord z najniższą temperaturą (największe ryzyko)
            dayRecords.minBy { it.minTemp }
        }.values.sortedByDescending { it.timestamp }.take(7)

        val trendPoints = dailyRecords.mapIndexed { index, record ->
            DailyTrendPoint(
                dayLabel = getDayLabel(index),
                minTemp = record.minTemp,
                hasFrostRisk = record.hasRisk,
                timestamp = record.timestamp
            )
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
                lastThreeAvg > firstThreeAvg + 2.0 -> TrendDirection.UP      // Co najmniej 2°C cieplej
                firstThreeAvg > lastThreeAvg + 2.0 -> TrendDirection.DOWN    // Co najmniej 2°C chłodniej
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
    private fun getDayLabel(daysAgo: Int): String = when (daysAgo) {
        0 -> "Dziś"
        1 -> "Wczoraj"
        2 -> "2 dni temu"
        3 -> "3 dni temu"
        4 -> "4 dni temu"
        5 -> "5 dni temu"
        6 -> "6 dni temu"
        else -> "Starsze"
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
