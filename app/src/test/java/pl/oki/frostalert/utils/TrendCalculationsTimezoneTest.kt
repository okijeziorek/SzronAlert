package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import pl.oki.frostalert.data.local.TemperatureRecord
import java.util.Calendar
import java.util.TimeZone

class TrendCalculationsTimezoneTest {

    private val day = 24 * 60 * 60 * 1000L

    /**
     * Verifies that startOfDay returns midnight in the specified timezone,
     * not the system default.
     */
    @Test
    fun `startOfDay uses explicit timezone for midnight calculation`() {
        val utcZone = TimeZone.getTimeZone("UTC")
        val tokyoZone = TimeZone.getTimeZone("Asia/Tokyo") // UTC+9

        // 2026-04-08T03:00:00 UTC = 2026-04-08T12:00:00 Tokyo
        val cal = Calendar.getInstance(utcZone).apply {
            set(2026, Calendar.APRIL, 8, 3, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = cal.timeInMillis

        val startUtc = TrendCalculations.startOfDay(timestamp, utcZone)
        val startTokyo = TrendCalculations.startOfDay(timestamp, tokyoZone)

        // UTC midnight = 2026-04-08T00:00 UTC
        val expectedUtc = Calendar.getInstance(utcZone).apply {
            set(2026, Calendar.APRIL, 8, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Tokyo midnight = 2026-04-08T00:00 JST = 2026-04-07T15:00 UTC
        val expectedTokyo = Calendar.getInstance(tokyoZone).apply {
            set(2026, Calendar.APRIL, 8, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expectedUtc, startUtc)
        assertEquals(expectedTokyo, startTokyo)
        assertNotEquals(startUtc, startTokyo)
    }

    /**
     * Records near midnight in different timezones may belong to different days.
     */
    @Test
    fun `calculateWeeklyTrend groups records correctly across timezone boundaries`() {
        val utcZone = TimeZone.getTimeZone("UTC")
        val hawaiiZone = TimeZone.getTimeZone("Pacific/Honolulu") // UTC-10

        // Create a record at 2026-04-08T01:00 UTC → still April 8 in UTC, but April 7 in Hawaii
        val cal = Calendar.getInstance(utcZone).apply {
            set(2026, Calendar.APRIL, 8, 1, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ts1 = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val ts2 = cal.timeInMillis

        val records = listOf(
            TemperatureRecord(timestamp = ts1, minTemp = -2.0, hasRisk = true),
            TemperatureRecord(timestamp = ts2, minTemp = 3.0, hasRisk = false)
        )

        val statsUtc = TrendCalculations.calculateWeeklyTrend(records, utcZone)
        val statsHawaii = TrendCalculations.calculateWeeklyTrend(records, hawaiiZone)

        // In UTC: ts1 and ts2 are different days → 2 trend points
        assertEquals(2, statsUtc.trendPoints.size)

        // In Hawaii: ts1 (2026-04-07T15:00 HST) and ts2 (2026-04-06T15:00 HST)
        // are still different days → still 2 trend points
        assertEquals(2, statsHawaii.trendPoints.size)
    }

    /**
     * getDayOfWeekShort should respect the given timezone.
     */
    @Test
    fun `getDayOfWeekShort returns correct day for timezone`() {
        val utcZone = TimeZone.getTimeZone("UTC")
        val aucklandZone = TimeZone.getTimeZone("Pacific/Auckland") // UTC+12/+13

        // 2026-04-07 23:30 UTC = Tuesday in UTC, but Wednesday in Auckland
        val cal = Calendar.getInstance(utcZone).apply {
            set(2026, Calendar.APRIL, 7, 23, 30, 0) // Tuesday 23:30 UTC
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = cal.timeInMillis

        val dayUtc = TrendCalculations.getDayOfWeekShort(timestamp, utcZone)
        val dayAuckland = TrendCalculations.getDayOfWeekShort(timestamp, aucklandZone)

        assertEquals("Wt", dayUtc) // Tuesday
        assertEquals("Śr", dayAuckland) // Wednesday (next day in Auckland)
    }

    /**
     * Default timezone parameter should produce the same result as the system timezone.
     */
    @Test
    fun `calculateWeeklyTrend with default timezone matches explicit system timezone`() {
        val now = System.currentTimeMillis()
        val records = listOf(
            TemperatureRecord(timestamp = now - day * 3, minTemp = 1.0, hasRisk = false),
            TemperatureRecord(timestamp = now - day * 2, minTemp = -1.0, hasRisk = true),
            TemperatureRecord(timestamp = now - day, minTemp = 0.5, hasRisk = true),
            TemperatureRecord(timestamp = now, minTemp = 2.0, hasRisk = false)
        )

        val statsDefault = TrendCalculations.calculateWeeklyTrend(records)
        val statsExplicit = TrendCalculations.calculateWeeklyTrend(records, TimeZone.getDefault())

        assertEquals(statsDefault.trendPoints.size, statsExplicit.trendPoints.size)
        assertEquals(statsDefault.trend, statsExplicit.trend)
        assertEquals(statsDefault.frostRiskPercentage, statsExplicit.frostRiskPercentage, 0.001)
    }
}
