package pl.oki.frostalert.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
