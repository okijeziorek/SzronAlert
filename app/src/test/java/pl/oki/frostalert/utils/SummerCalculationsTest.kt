package pl.oki.frostalert.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SummerCalculationsTest {

    @Test
    fun `hasStormOrHailRisk returns true for WMO storm and hail codes`() {
        assertTrue(SummerCalculations.hasStormOrHailRisk(95))
        assertTrue(SummerCalculations.hasStormOrHailRisk(96))
        assertTrue(SummerCalculations.hasStormOrHailRisk(99))
    }

    @Test
    fun `hasStormOrHailRisk returns false for non-storm weather codes`() {
        assertFalse(SummerCalculations.hasStormOrHailRisk(0))
        assertFalse(SummerCalculations.hasStormOrHailRisk(61))
        assertFalse(SummerCalculations.hasStormOrHailRisk(80))
    }

    // ── hasHeatRisk ─────────────────────────────────────────────────────────

    @Test
    fun `hasHeatRisk returns true when temperature equals threshold`() {
        assertTrue(SummerCalculations.hasHeatRisk(30.0, 30.0))
    }

    @Test
    fun `hasHeatRisk returns true when temperature exceeds threshold`() {
        assertTrue(SummerCalculations.hasHeatRisk(35.0, 30.0))
    }

    @Test
    fun `hasHeatRisk returns false when temperature is below threshold`() {
        assertFalse(SummerCalculations.hasHeatRisk(25.0, 30.0))
    }

    // ── needsWatering ────────────────────────────────────────────────────────

    @Test
    fun `needsWatering returns true for low precipitation and high tomorrow temp`() {
        assertTrue(SummerCalculations.needsWatering(dailyPrecipitationSum = 0.0, tomorrowMaxTemp = 28.0))
    }

    @Test
    fun `needsWatering returns false when precipitation is sufficient`() {
        assertFalse(SummerCalculations.needsWatering(dailyPrecipitationSum = 5.0, tomorrowMaxTemp = 28.0))
    }

    @Test
    fun `needsWatering returns false when tomorrow temp is not high enough`() {
        assertFalse(SummerCalculations.needsWatering(dailyPrecipitationSum = 0.0, tomorrowMaxTemp = 20.0))
    }

    @Test
    fun `needsWatering boundary - exactly 2mm precipitation returns false`() {
        assertFalse(SummerCalculations.needsWatering(dailyPrecipitationSum = 2.0, tomorrowMaxTemp = 28.0))
    }

    @Test
    fun `needsWatering boundary - exactly 25C tomorrow returns false`() {
        assertFalse(SummerCalculations.needsWatering(dailyPrecipitationSum = 0.0, tomorrowMaxTemp = 25.0))
    }

    // ── getUvDescription ─────────────────────────────────────────────────────

    @Test
    fun `getUvDescription returns Niskie for index below 3`() {
        assertEquals("Niskie", SummerCalculations.getUvDescription(0.0))
        assertEquals("Niskie", SummerCalculations.getUvDescription(2.9))
    }

    @Test
    fun `getUvDescription returns Umiarkowane for index 3 to 5`() {
        assertEquals("Umiarkowane", SummerCalculations.getUvDescription(3.0))
        assertEquals("Umiarkowane", SummerCalculations.getUvDescription(5.9))
    }

    @Test
    fun `getUvDescription returns Wysokie for index 6 to 7`() {
        assertEquals("Wysokie", SummerCalculations.getUvDescription(6.0))
        assertEquals("Wysokie", SummerCalculations.getUvDescription(7.9))
    }

    @Test
    fun `getUvDescription returns Bardzo wysokie for index 8 to 10`() {
        assertEquals("Bardzo wysokie", SummerCalculations.getUvDescription(8.0))
        assertEquals("Bardzo wysokie", SummerCalculations.getUvDescription(10.9))
    }

    @Test
    fun `getUvDescription returns Ekstremalne for index 11 and above`() {
        assertEquals("Ekstremalne", SummerCalculations.getUvDescription(11.0))
        assertEquals("Ekstremalne", SummerCalculations.getUvDescription(15.0))
    }

    // ── getSummerWarningMessage ───────────────────────────────────────────────

    @Test
    fun `getSummerWarningMessage includes hail storm warning for code 96`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 20.0,
            weatherCode = 96,
            uvIndex = 2.0,
            heatThreshold = 30.0,
            appMode = 0
        )
        assertTrue(msg.contains("GRAD") || msg.contains("BURZY"))
    }

    @Test
    fun `getSummerWarningMessage includes storm warning for code 95`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 20.0,
            weatherCode = 95,
            uvIndex = 2.0,
            heatThreshold = 30.0,
            appMode = 0
        )
        assertTrue(msg.contains("BURZY") || msg.contains("BURZA") || msg.contains("BURZ"))
    }

    @Test
    fun `getSummerWarningMessage includes heat risk for car mode when hot`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 35.0,
            weatherCode = 0,
            uvIndex = 2.0,
            heatThreshold = 30.0,
            appMode = 0
        )
        assertTrue(msg.contains("UPAŁ") || msg.contains("nagrzewa"))
    }

    @Test
    fun `getSummerWarningMessage does not include heat risk for garden mode`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 35.0,
            weatherCode = 0,
            uvIndex = 2.0,
            heatThreshold = 30.0,
            appMode = 1
        )
        assertFalse(msg.contains("nagrzewa"))
    }

    @Test
    fun `getSummerWarningMessage includes UV warning for high UV index`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 20.0,
            weatherCode = 0,
            uvIndex = 8.0,
            heatThreshold = 30.0,
            appMode = 0
        )
        assertTrue(msg.contains("UV"))
    }

    @Test
    fun `getSummerWarningMessage is empty for safe conditions`() {
        val msg = SummerCalculations.getSummerWarningMessage(
            currentTemp = 20.0,
            weatherCode = 1,
            uvIndex = 2.0,
            heatThreshold = 30.0,
            appMode = 0
        )
        assertTrue(msg.isEmpty())
    }
}
