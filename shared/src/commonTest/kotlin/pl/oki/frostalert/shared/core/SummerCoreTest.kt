package pl.oki.frostalert.shared.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SummerCoreTest {

    @Test
    fun `storm and hail codes are detected`() {
        assertTrue(SummerCore.hasStormOrHailRisk(95))
        assertTrue(SummerCore.hasStormOrHailRisk(96))
        assertTrue(SummerCore.hasStormOrHailRisk(99))
        assertFalse(SummerCore.hasStormOrHailRisk(3))
    }

    @Test
    fun `needsWatering is true only for low precipitation and high temp`() {
        assertTrue(SummerCore.needsWatering(1.5, 26.0))
        assertFalse(SummerCore.needsWatering(2.5, 26.0))
        assertFalse(SummerCore.needsWatering(1.5, 20.0))
    }
}
