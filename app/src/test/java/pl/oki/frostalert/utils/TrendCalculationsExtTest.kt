package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.data.local.TemperatureRecord

class TrendCalculationsExtTest {

    // ── getTrendDescription ───────────────────────────────────────────────────

    @Test
    fun `getTrendDescription returns high risk message when above 70 percent`() {
        val stats = makeTrendStats(frostRiskPercentage = 80.0)
        val desc = TrendCalculations.getTrendDescription(stats)
        assertTrue(desc.contains("Bardzo") || desc.contains("wysokie"))
    }

    @Test
    fun `getTrendDescription returns moderate risk message between 40 and 70 percent`() {
        val stats = makeTrendStats(frostRiskPercentage = 50.0)
        val desc = TrendCalculations.getTrendDescription(stats)
        assertTrue(desc.contains("Umiarkowane") || desc.contains("ryzyko"))
    }

    @Test
    fun `getTrendDescription returns sporadic message between 0 and 40 percent`() {
        val stats = makeTrendStats(frostRiskPercentage = 20.0)
        val desc = TrendCalculations.getTrendDescription(stats)
        assertTrue(desc.contains("Sporadyczne") || desc.contains("przymrozki"))
    }

    @Test
    fun `getTrendDescription returns no risk message when 0 percent`() {
        val stats = makeTrendStats(frostRiskPercentage = 0.0)
        val desc = TrendCalculations.getTrendDescription(stats)
        assertTrue(desc.contains("Brak"))
    }

    // ── getTrendEmoji ─────────────────────────────────────────────────────────

    @Test
    fun `getTrendEmoji returns upward emoji for UP direction`() {
        val emoji = TrendCalculations.getTrendEmoji(TrendCalculations.TrendDirection.UP)
        assertTrue(emoji.contains("📈"))
    }

    @Test
    fun `getTrendEmoji returns downward emoji for DOWN direction`() {
        val emoji = TrendCalculations.getTrendEmoji(TrendCalculations.TrendDirection.DOWN)
        assertTrue(emoji.contains("📉"))
    }

    @Test
    fun `getTrendEmoji returns stable emoji for STABLE direction`() {
        val emoji = TrendCalculations.getTrendEmoji(TrendCalculations.TrendDirection.STABLE)
        assertTrue(emoji.contains("➡️"))
    }

    // ── getDayOfWeekShort ─────────────────────────────────────────────────────

    @Test
    fun `getDayOfWeekShort returns correct abbreviations for known dates`() {
        // 2024-01-01 is a Monday
        val calendar = java.util.Calendar.getInstance().apply {
            set(2024, java.util.Calendar.JANUARY, 1, 12, 0, 0)
        }
        val monday = calendar.timeInMillis
        assertEquals("Pn", TrendCalculations.getDayOfWeekShort(monday))

        // 2024-01-07 is a Sunday
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 6)
        assertEquals("Nd", TrendCalculations.getDayOfWeekShort(calendar.timeInMillis))
    }

    // ── calculateWeeklyTrend — additional edge cases ─────────────────────────

    @Test
    fun `calculateWeeklyTrend uses minimum temp per day when multiple records exist`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        // Two records on the same day — we expect the minimum to be used
        val records = listOf(
            TemperatureRecord(timestamp = now - day + 1_000, minTemp = -5.0, hasRisk = true),
            TemperatureRecord(timestamp = now - day + 2_000, minTemp = 3.0, hasRisk = false),
            TemperatureRecord(timestamp = now + 1_000, minTemp = 1.0, hasRisk = false)
        )

        val stats = TrendCalculations.calculateWeeklyTrend(records)

        // Should have 2 days
        assertEquals(2, stats.trendPoints.size)
        // First day point should use the record with minTemp -5.0
        val firstDayPoint = stats.trendPoints.first()
        assertEquals(-5.0, firstDayPoint.minTemp, 0.01)
        assertTrue(firstDayPoint.hasFrostRisk)
    }

    @Test
    fun `calculateWeeklyTrend frostRiskPercentage is correct`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        val records = (0 until 4).map { i ->
            TemperatureRecord(
                timestamp = now - (3 - i) * day,
                minTemp = if (i < 2) -1.0 else 5.0,
                hasRisk = i < 2
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(2, stats.nightsWithFrostRisk)
        assertEquals(50.0, stats.frostRiskPercentage, 0.1)
    }

    // ── calculateExtendedTrend ──────────────────────────────────────────────

    @Test
    fun `calculateExtendedTrend returns empty for null daily data`() {
        val response = pl.oki.frostalert.data.remote.WeatherResponse(
            current = pl.oki.frostalert.data.remote.CurrentWeather(
                temperature = 5.0, humidity = 60.0, precipitation = 0.0, weatherCode = 0, windSpeed = 5.0
            ),
            hourly = pl.oki.frostalert.data.remote.HourlyForecast(
                time = emptyList(), temperature = emptyList(), humidity = emptyList(),
                precipitation = emptyList(), weatherCode = emptyList(), windSpeed = emptyList()
            ),
            daily = null
        )
        val stats = TrendCalculations.calculateExtendedTrend(response)
        assertEquals(0, stats.totalDays)
        assertEquals(0, stats.nightsWithFrostRisk)
    }

    @Test
    fun `calculateExtendedTrend detects frost risk when minTemp below threshold`() {
        val times = (0..15).map { "2026-04-${String.format("%02d", it + 1)}" }
        val minTemps = (0..15).map { if (it < 5) -1.0 else 10.0 }
        val maxTemps = (0..15).map { 15.0 }
        val codes = (0..15).map { 0 }
        val precipSums = (0..15).map { 0.0 }
        val uvMaxes = (0..15).map { 3.0 }

        val response = pl.oki.frostalert.data.remote.WeatherResponse(
            current = pl.oki.frostalert.data.remote.CurrentWeather(
                temperature = 5.0, humidity = 60.0, precipitation = 0.0, weatherCode = 0, windSpeed = 5.0
            ),
            hourly = pl.oki.frostalert.data.remote.HourlyForecast(
                time = emptyList(), temperature = emptyList(), humidity = emptyList(),
                precipitation = emptyList(), weatherCode = emptyList(), windSpeed = emptyList()
            ),
            daily = pl.oki.frostalert.data.remote.DailyForecast(
                time = times, temperatureMin = minTemps, temperatureMax = maxTemps,
                weatherCode = codes, uvIndexMax = uvMaxes, precipitationSum = precipSums
            )
        )
        val stats = TrendCalculations.calculateExtendedTrend(response, frostThreshold = 2.0)
        assertTrue("Should have some frost nights", stats.nightsWithFrostRisk > 0)
        assertTrue("Should have reliable days", stats.reliableDays > 0)
        assertTrue("Should have total days", stats.totalDays > 0)
    }

    @Test
    fun `calculateExtendedTrend marks first 3 days as reliable`() {
        val times = (0..15).map { "2026-04-${String.format("%02d", it + 1)}" }
        val minTemps = (0..15).map { 5.0 }
        val maxTemps = (0..15).map { 15.0 }
        val codes = (0..15).map { 0 }
        val precipSums = (0..15).map { 0.0 }
        val uvMaxes = (0..15).map { 3.0 }

        val response = pl.oki.frostalert.data.remote.WeatherResponse(
            current = pl.oki.frostalert.data.remote.CurrentWeather(
                temperature = 5.0, humidity = 60.0, precipitation = 0.0, weatherCode = 0, windSpeed = 5.0
            ),
            hourly = pl.oki.frostalert.data.remote.HourlyForecast(
                time = emptyList(), temperature = emptyList(), humidity = emptyList(),
                precipitation = emptyList(), weatherCode = emptyList(), windSpeed = emptyList()
            ),
            daily = pl.oki.frostalert.data.remote.DailyForecast(
                time = times, temperatureMin = minTemps, temperatureMax = maxTemps,
                weatherCode = codes, uvIndexMax = uvMaxes, precipitationSum = precipSums
            )
        )
        val stats = TrendCalculations.calculateExtendedTrend(response)
        assertEquals(3, stats.reliableDays)
        assertTrue(stats.forecastPoints.take(3).all { it.isReliable })
        assertTrue(stats.forecastPoints.drop(3).none { it.isReliable })
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private fun makeTrendStats(frostRiskPercentage: Double) = TrendCalculations.WeeklyTrendStats(
        trendPoints = emptyList(),
        nightsWithFrostRisk = 0,
        frostRiskPercentage = frostRiskPercentage,
        averageMinTemp = 0.0,
        lowestTemp = 0.0,
        trend = TrendCalculations.TrendDirection.STABLE
    )
}
