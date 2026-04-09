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
    fun `calculateWeeklyTrend returns STABLE when fewer than 4 days of data`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(-5.0, -4.0, 5.0) // 3 days only — below MIN_POINTS_FOR_TREND

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

    @Test
    fun `calculateWeeklyTrend detects cooling trend despite spike in last position`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // Wyraźnie malejąca temperatura przez 5 dni, a potem nagły skok na plus w dniu 6.
        // Mediana ostatnich 3: sorted([4, 2, 15]) = [2, 4, 15] → mediana = 4
        // Mediana pierwszych 3: sorted([10, 8, 6]) = [6, 8, 10] → mediana = 8
        // delta = 4 - 8 = -4 → DOWN
        val temps = listOf(10.0, 8.0, 6.0, 4.0, 2.0, 15.0)

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
    fun `calculateWeeklyTrend detects warming trend despite spike in first position`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // Ekstremalnie zimny pierwszy dzień, potem wyraźne ocieplenie.
        // Mediana pierwszych 3: sorted([-15, -3, -1]) = [-15, -3, -1] → mediana = -3
        // Mediana ostatnich 3: sorted([1, 3, 5]) = [1, 3, 5] → mediana = 3
        // delta = 3 - (-3) = 6 → UP
        val temps = listOf(-15.0, -3.0, -1.0, 1.0, 3.0, 5.0)

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
    fun `calculateWeeklyTrend detects warming trend with only 4 days of data`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // 4 dni: wyraźne ocieplenie z -5 do +4
        // first2 median = (-5 + -4) / 2 = -4.5, last2 median = (2 + 4) / 2 = 3
        // delta = 3 - (-4.5) = 7.5 → UP
        val temps = listOf(-5.0, -4.0, 2.0, 4.0)

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
    fun `calculateWeeklyTrend detects cooling trend with only 4 days of data`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // 4 dni: wyraźne ochłodzenie z +5 do -4
        // first2 median = (5 + 4) / 2 = 4.5, last2 median = (-2 + -4) / 2 = -3
        // delta = -3 - 4.5 = -7.5 → DOWN
        val temps = listOf(5.0, 4.0, -2.0, -4.0)

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
    fun `calculateWeeklyTrend returns STABLE when all temperatures are identical`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(2.0, 2.0, 2.0, 2.0, 2.0, 2.0)

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = false
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(TrendCalculations.TrendDirection.STABLE, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend is deterministic for same input`() {
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

        val result1 = TrendCalculations.calculateWeeklyTrend(records)
        val result2 = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(result1.trend, result2.trend)
        assertEquals(result1.frostRiskPercentage, result2.frostRiskPercentage, 0.001)
        assertEquals(result1.averageMinTemp, result2.averageMinTemp, 0.001)
        assertEquals(result1.lowestTemp, result2.lowestTemp, 0.001)
    }

    @Test
    fun `calculateWeeklyTrend handles extreme oscillating temperatures (minus5 to plus5 to minus10)`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        // Oscillation: −5 → +5 → −10 → +3 → −8 → +1
        // First 3 medians: sorted([-5, 5, -10]) → [-10, -5, 5] → median = -5
        // Last 3 medians: sorted([3, -8, 1]) → [-8, 1, 3] → median = 1
        // delta = 1 - (-5) = 6 → UP (despite wild oscillations)
        val temps = listOf(-5.0, 5.0, -10.0, 3.0, -8.0, 1.0)

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
    fun `calculateWeeklyTrend handles 7 days (maximum window)`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val temps = listOf(-7.0, -6.0, -5.0, -4.0, -3.0, -2.0, -1.0)

        val records = temps.mapIndexed { index, temp ->
            TemperatureRecord(
                timestamp = now - day * (temps.size - 1L - index),
                minTemp = temp,
                hasRisk = true
            )
        }

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(7, stats.trendPoints.size)
        assertEquals(7, stats.nightsWithFrostRisk)
        assertEquals(100.0, stats.frostRiskPercentage, 0.01)
        assertEquals(TrendCalculations.TrendDirection.UP, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend handles single record`() {
        val record = TemperatureRecord(
            timestamp = System.currentTimeMillis(),
            minTemp = -3.0,
            hasRisk = true
        )

        val stats = TrendCalculations.calculateWeeklyTrend(listOf(record))
        assertEquals(1, stats.trendPoints.size)
        assertEquals(TrendCalculations.TrendDirection.STABLE, stats.trend)
    }

    @Test
    fun `calculateWeeklyTrend picks minimum temp per day from multiple records`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        // Multiple records in the same day — should use minimum
        val records = listOf(
            TemperatureRecord(timestamp = now + 1_000, minTemp = 5.0, hasRisk = false),
            TemperatureRecord(timestamp = now + 2_000, minTemp = -2.0, hasRisk = true),
            TemperatureRecord(timestamp = now + 3_000, minTemp = 3.0, hasRisk = false)
        )

        val stats = TrendCalculations.calculateWeeklyTrend(records)
        assertEquals(1, stats.trendPoints.size)
        assertEquals(-2.0, stats.trendPoints[0].minTemp, 0.01)
    }
}
