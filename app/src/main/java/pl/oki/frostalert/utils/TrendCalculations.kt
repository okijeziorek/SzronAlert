package pl.oki.frostalert.utils

import android.content.Context
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.TemperatureRecord
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TrendCalculations {

    /** Minimalna różnica median (°C) wymagana do uznania trendu za UP lub DOWN */
    private const val TREND_DELTA_THRESHOLD_C = 2.0

    /** Minimalna liczba punktów dziennych wymagana do obliczenia kierunku trendu */
    private const val MIN_POINTS_FOR_TREND = 4

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
     * Wylicza trend 7-dniowy na podstawie ostatnich rekordów.
     *
     * @param timeZone timezone used for day-boundary calculations; defaults to
     *        the system default timezone. Pass an explicit timezone to get
     *        deterministic results independent of the device's locale.
     */
    fun calculateWeeklyTrend(
        records: List<TemperatureRecord>,
        timeZone: TimeZone = TimeZone.getDefault(),
        context: Context? = null
    ): WeeklyTrendStats {
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
            .groupBy { startOfDay(it.timestamp, timeZone) }
            .toList()
            .sortedByDescending { it.first }
            .take(7)
            .sortedBy { it.first }

        val trendPoints = dailySummaries.map { (dayStart, dayRecords) ->
            val minRecord = dayRecords.minBy { it.minTemp }
            DailyTrendPoint(
                dayLabel = if (context != null) getRelativeDayLabelLocalized(context, dayStart, timeZone)
                           else getRelativeDayLabel(dayStart, timeZone),
                minTemp = minRecord.minTemp,
                hasFrostRisk = dayRecords.any { it.hasRisk },
                timestamp = dayStart
            )
        }

        val nightsWithRisk = trendPoints.count { it.hasFrostRisk }
        val percentage = if (trendPoints.isNotEmpty()) (nightsWithRisk.toDouble() / trendPoints.size) * 100.0 else 0.0
        val avgTemp = trendPoints.map { it.minTemp }.average()
        val lowestTemp = trendPoints.minOfOrNull { it.minTemp } ?: 0.0

        return WeeklyTrendStats(
            trendPoints = trendPoints,
            nightsWithFrostRisk = nightsWithRisk,
            frostRiskPercentage = percentage,
            averageMinTemp = avgTemp,
            lowestTemp = lowestTemp,
            trend = computeTrendDirection(trendPoints)
        )
    }

    /**
     * Wylicza przyszły trend 7-dniowy na podstawie prognozy pogody.
     *
     * @param timeZone timezone used for parsing hourly timestamps and computing
     *        day boundaries; defaults to system default. The Open-Meteo API returns
     *        timestamps in the location's local time (controlled by `timezone=auto`),
     *        so passing the forecast location's timezone produces correct results.
     */
    fun calculateFutureWeeklyTrend(
        weatherResponse: pl.oki.frostalert.data.remote.WeatherResponse,
        timeZone: TimeZone = TimeZone.getDefault(),
        context: Context? = null
    ): WeeklyTrendStats {
        val hourly = weatherResponse.hourly
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(timeZone)

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
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        sdf.timeZone = timeZone
        for (i in hourly.time.indices) {
            val timeMillis = sdf.parse(hourly.time[i])?.time ?: 0
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
                    val t = hourly.temperature[index] ?: continue
                    minTemp = minOf(minTemp, t)
                }
            }
            if (minTemp == Double.MAX_VALUE) minTemp = hourly.temperature.getOrElse(dayStartIndex + 20) { null } ?: 0.0

            // Oblicz ryzyko szronu (uproszczone: temp < 2°C, humidity > 70)
            val humidity = hourly.humidity.getOrElse(dayStartIndex + 20) { null } ?: 80.0
            val hasFrostRisk = minTemp < 2.0 && humidity > 70.0

            val timestamp = tomorrowStart + (day * 24 * 60 * 60 * 1000L)
            trendPoints.add(DailyTrendPoint(
                dayLabel = if (context != null) getFutureDayLabelLocalized(context, day)
                           else getFutureDayLabel(day),
                minTemp = minTemp,
                hasFrostRisk = hasFrostRisk,
                timestamp = timestamp
            ))
        }

        val nightsWithRisk = trendPoints.count { it.hasFrostRisk }
        val percentage = if (trendPoints.isNotEmpty()) (nightsWithRisk.toDouble() / trendPoints.size) * 100.0 else 0.0
        val avgTemp = trendPoints.map { it.minTemp }.average()
        val lowestTemp = trendPoints.minOfOrNull { it.minTemp } ?: 0.0

        return WeeklyTrendStats(
            trendPoints = trendPoints,
            nightsWithFrostRisk = nightsWithRisk,
            frostRiskPercentage = percentage,
            averageMinTemp = avgTemp,
            lowestTemp = lowestTemp,
            trend = computeTrendDirection(trendPoints)
        )
    }

    /**
     * Zwraca opis trendu w naturalnym języku (bez kontekstu – polskie stringi)
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
     * Zwraca opis trendu w naturalnym języku
     */
    fun getTrendDescription(context: Context, stats: WeeklyTrendStats): String {
        return when {
            stats.frostRiskPercentage > 70 -> context.getString(R.string.trend_description_very_high)
            stats.frostRiskPercentage > 40 -> context.getString(R.string.trend_description_moderate)
            stats.frostRiskPercentage > 0 -> context.getString(R.string.trend_description_low)
            else -> context.getString(R.string.trend_description_none)
        }
    }

    /**
     * Zwraca emotikonę dla kierunku trendu (bez kontekstu – hardcoded)
     */
    fun getTrendEmoji(trend: TrendDirection): String = when (trend) {
        TrendDirection.UP -> "📈 Robi się cieplej"
        TrendDirection.DOWN -> "📉 Robi się chłodniej"
        TrendDirection.STABLE -> "➡️ Brak zmian"
    }

    /**
     * Zwraca emotikonę dla kierunku trendu
     */
    fun getTrendEmoji(context: Context, trend: TrendDirection): String = when (trend) {
        TrendDirection.UP -> context.getString(R.string.trend_direction_up)
        TrendDirection.DOWN -> context.getString(R.string.trend_direction_down)
        TrendDirection.STABLE -> context.getString(R.string.trend_direction_stable)
    }

    /**
     * Wyznacza kierunek trendu temperaturowego na podstawie listy punktów dziennych.
     *
     * Algorytm:
     * - Mniej niż [MIN_POINTS_FOR_TREND] punktów → STABLE (za mało danych)
     * - 4–5 punktów → mediana pierwszych 2 vs mediana ostatnich 2
     * - 6+ punktów → mediana pierwszych 3 vs mediana ostatnich 3
     *
     * Użycie mediany zamiast średniej zapewnia odporność na jednorazowe skoki
     * temperatury (np. nagły mróz w ciepłym tygodniu lub odwrotnie).
     */
    private fun computeTrendDirection(points: List<DailyTrendPoint>): TrendDirection {
        if (points.size < MIN_POINTS_FOR_TREND) return TrendDirection.STABLE

        val groupSize = if (points.size >= 6) 3 else 2
        val firstMedian = medianOf(points.take(groupSize).map { it.minTemp })
        val lastMedian = medianOf(points.takeLast(groupSize).map { it.minTemp })
        val delta = lastMedian - firstMedian

        return when {
            delta > TREND_DELTA_THRESHOLD_C -> TrendDirection.UP
            delta < -TREND_DELTA_THRESHOLD_C -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
    }

    /** Zwraca medianę listy wartości. Lista musi być niepusta. */
    private fun medianOf(values: List<Double>): Double {
        require(values.isNotEmpty()) { "Values list must not be empty" }
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    /**
     * Zwraca etykietę dnia (Dziś, Wczoraj, itd.)
     *
     * NOTE: Since this is a private function used in data processing,
     * we use hardcoded Polish labels. For UI display, callers should use
     * localized string resources from getRelativeDayLabelLocalized().
     */
    private fun getRelativeDayLabel(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val daysAgo = ((startOfDay(System.currentTimeMillis(), timeZone) - startOfDay(timestamp, timeZone)) / MILLIS_IN_DAY).toInt()
        return when (daysAgo) {
            0 -> "Dziś"
            1 -> "Wczoraj"
            in 2..6 -> "$daysAgo dni temu"
            else -> getDayOfWeekShort(timestamp, timeZone)
        }
    }

    /**
     * Zwraca zlokalizowaną etykietę dnia (Today, Yesterday, itd.) dla UI.
     * Użyj tej funkcji zamiast prywatnej getRelativeDayLabel() w kodzie UI.
     */
    fun getRelativeDayLabelLocalized(context: Context, timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val daysAgo = ((startOfDay(System.currentTimeMillis(), timeZone) - startOfDay(timestamp, timeZone)) / MILLIS_IN_DAY).toInt()
        return when (daysAgo) {
            0 -> context.getString(R.string.day_label_today)
            1 -> context.getString(R.string.day_label_yesterday)
            in 2..6 -> context.getString(R.string.day_label_days_ago, daysAgo)
            else -> getDayOfWeekShortLocalized(context, timestamp, timeZone)
        }
    }

    /**
     * Returns the start-of-day (midnight) timestamp for the given [timestamp].
     *
     * @param timeZone timezone used to determine midnight; defaults to system default.
     */
    internal fun startOfDay(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
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
     *
     * NOTE: Since this is a private function used in data processing,
     * we use hardcoded Polish labels. For UI display, callers should use
     * localized string resources from getFutureDayLabelLocalized().
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
     * Zwraca zlokalizowaną etykietę przyszłego dnia dla UI.
     */
    fun getFutureDayLabelLocalized(context: Context, daysAhead: Int): String = when (daysAhead) {
        0 -> context.getString(R.string.day_label_tomorrow)
        1 -> context.getString(R.string.day_label_day_after_tomorrow)
        2 -> context.getString(R.string.day_label_in_n_days, 3)
        3 -> context.getString(R.string.day_label_in_n_days, 4)
        4 -> context.getString(R.string.day_label_in_n_days, 5)
        5 -> context.getString(R.string.day_label_in_n_days, 6)
        6 -> context.getString(R.string.day_label_in_n_days, 7)
        else -> context.getString(R.string.day_label_later)
    }

    /**
     * Zwraca skrót dnia tygodnia.
     *
     * @param timeZone timezone used for day-of-week determination; defaults to system default.
     */
    /**
     * Punkt prognozy dziennej z poziomu daily API.
     */
    data class DailyForecastPoint(
        val dayLabel: String,
        val date: String,
        val minTemp: Double,
        val maxTemp: Double,
        val weatherCode: Int,
        val precipitationSum: Double,
        val hasFrostRisk: Boolean,
        val isReliable: Boolean,
        val timestamp: Long
    )

    /**
     * Statystyki trendu rozszerzonego (14-dniowego).
     */
    data class ExtendedTrendStats(
        val forecastPoints: List<DailyForecastPoint>,
        val nightsWithFrostRisk: Int,
        val frostRiskPercentage: Double,
        val averageMinTemp: Double,
        val lowestTemp: Double,
        val trend: TrendDirection,
        val reliableDays: Int,
        val totalDays: Int
    )

    /**
     * Oblicza rozszerzony trend 14-dniowy na podstawie daily forecast z Open-Meteo.
     * Dni 1-3 oznaczone jako "pewna prognoza", dni 4-14 jako "orientacyjna".
     */
    fun calculateExtendedTrend(
        weatherResponse: pl.oki.frostalert.data.remote.WeatherResponse,
        frostThreshold: Double = 2.0,
        context: Context? = null
    ): ExtendedTrendStats {
        val daily = weatherResponse.daily ?: return ExtendedTrendStats(
            forecastPoints = emptyList(),
            nightsWithFrostRisk = 0,
            frostRiskPercentage = 0.0,
            averageMinTemp = 0.0,
            lowestTemp = 0.0,
            trend = TrendDirection.STABLE,
            reliableDays = 0,
            totalDays = 0
        )

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val points = daily.time.indices.drop(1).take(14).map { i ->
            val minTemp = daily.temperatureMin.getOrElse(i) { null } ?: 0.0
            val maxTemp = daily.temperatureMax.getOrElse(i) { null } ?: 0.0
            val weatherCode = daily.weatherCode.getOrElse(i) { null } ?: 0
            val precip = daily.precipitationSum.getOrElse(i) { null } ?: 0.0
            val hasFrost = minTemp <= frostThreshold
            val dateStr = daily.time.getOrElse(i) { "" }
            val timestamp = try { sdf.parse(dateStr)?.time ?: 0L } catch (_: Exception) { 0L }

            DailyForecastPoint(
                dayLabel = if (context != null) getExtendedFutureDayLabelLocalized(context, i)
                           else getExtendedFutureDayLabel(i),
                date = dateStr,
                minTemp = minTemp,
                maxTemp = maxTemp,
                weatherCode = weatherCode,
                precipitationSum = precip,
                hasFrostRisk = hasFrost,
                isReliable = i <= 3,
                timestamp = timestamp
            )
        }

        val nightsWithRisk = points.count { it.hasFrostRisk }
        val percentage = if (points.isNotEmpty()) (nightsWithRisk.toDouble() / points.size) * 100.0 else 0.0
        val avgTemp = if (points.isNotEmpty()) points.map { it.minTemp }.average() else 0.0
        val lowestTemp = points.minOfOrNull { it.minTemp } ?: 0.0

        val trendPoints = points.map {
            DailyTrendPoint(dayLabel = it.dayLabel, minTemp = it.minTemp, hasFrostRisk = it.hasFrostRisk, timestamp = it.timestamp)
        }

        return ExtendedTrendStats(
            forecastPoints = points,
            nightsWithFrostRisk = nightsWithRisk,
            frostRiskPercentage = percentage,
            averageMinTemp = avgTemp,
            lowestTemp = lowestTemp,
            trend = computeTrendDirection(trendPoints),
            reliableDays = points.count { it.isReliable },
            totalDays = points.size
        )
    }

    private fun getExtendedFutureDayLabel(dayIndex: Int): String = when (dayIndex) {
        1 -> "Jutro"
        2 -> "Pojutrze"
        else -> "Za $dayIndex dni"
    }

    /**
     * Zwraca zlokalizowaną etykietę rozszerzonej prognozy dla UI.
     */
    fun getExtendedFutureDayLabelLocalized(context: Context, dayIndex: Int): String = when (dayIndex) {
        1 -> context.getString(R.string.day_label_tomorrow)
        2 -> context.getString(R.string.day_label_day_after_tomorrow)
        else -> context.getString(R.string.day_label_in_n_days, dayIndex)
    }

    fun getDayOfWeekShort(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val calendar = Calendar.getInstance(timeZone)
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

    /**
     * Zwraca zlokalizowany skrót dnia tygodnia dla UI.
     */
    fun getDayOfWeekShortLocalized(context: Context, timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = timestamp
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> context.getString(R.string.day_of_week_mon)
            Calendar.TUESDAY -> context.getString(R.string.day_of_week_tue)
            Calendar.WEDNESDAY -> context.getString(R.string.day_of_week_wed)
            Calendar.THURSDAY -> context.getString(R.string.day_of_week_thu)
            Calendar.FRIDAY -> context.getString(R.string.day_of_week_fri)
            Calendar.SATURDAY -> context.getString(R.string.day_of_week_sat)
            Calendar.SUNDAY -> context.getString(R.string.day_of_week_sun)
            else -> "?"
        }
    }
}
