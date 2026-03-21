package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.oki.frostalert.data.local.TemperatureRecord

class TrendCalculationsTest {

    @Test
    fun `calculateWeeklyTrend groups records per day and keeps chronological order`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        val records = listOf(
            TemperatureRecord(timestamp = now - day * 2 + 1_000, minTemp = -1.0, hasRisk = true),
            TemperatureRecord(timestamp = now - day * 2 + 2_000, minTemp = 1.5, hasRisk = false),
            TemperatureRecord(timestamp = now - day + 1_000, minTemp = 2.0, hasRisk = false),
            TemperatureRecord(timestamp = now - day + 2_000, minTemp = 0.5, hasRisk = true),
            TemperatureRecord(timestamp = now + 1_000, minTemp = 3.0, hasRisk = false)
        )

        val stats = TrendCalculations.calculateWeeklyTrend(records)

        assertEquals(3, stats.trendPoints.size)
        assertTrue(stats.trendPoints[0].timestamp <= stats.trendPoints[1].timestamp)
        assertTrue(stats.trendPoints[1].timestamp <= stats.trendPoints[2].timestamp)
        assertEquals(2, stats.nightsWithFrostRisk)
    }

    @Test
    fun `calculateWeeklyTrend detects warming trend`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(-5.0, -4.0, -3.0, 0.0, 1.0, 2.0)

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = temp <= 1.0
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(TrendCalculations.TrendDirection.UP, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend detects cooling trend`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(5.0, 4.0, 3.0, 0.0, -1.0, -3.0)

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = temp <= 1.0
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(TrendCalculations.TrendDirection.DOWN, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend returns STABLE when delta is below 2 degrees threshold`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // firstThreeAvg = (-2 + -1.5 + -1) / 3 = -1.5
        // lastThreeAvg  = (0 + 0.5 + 0.75) / 3 ≈ 0.417
        // delta ≈ 1.917 → below TREND_DELTA_THRESHOLD_C (2.0) → STABLE
        val temps = listOf(-2.0, -1.5, -1.0, 0.0, 0.5, 0.75)

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = temp <= 1.0
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(TrendCalculations.TrendDirection.STABLE, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend returns STABLE when fewer than 6 days of data`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(-5.0, -4.0, 0.0, 3.0, 5.0) // 5 days only

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = temp <= 1.0
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(TrendCalculations.TrendDirection.STABLE, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend returns STABLE for empty records`() {
        val stats = TrendCalculations.calculateWeeklyTrend(emptyList())
        assertEquals(TrendCalculations.TrendDirection.STABLE, stats.trend)
        assertEquals(0, stats.nightsWithFrostRisk)
        assertEquals(0.0, stats.frostRiskPercentage, 0.001)
    }
}
