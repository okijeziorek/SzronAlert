package pl.oki.frostalert.shared.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrostCoreTest {

    @Test
    fun `calculateDewPoint falls back to temp on invalid humidity`() {
        assertEquals(3.0, FrostCore.calculateDewPoint(3.0, 0.0))
        assertEquals(3.0, FrostCore.calculateDewPoint(3.0, 200.0))
    }

    @Test
    fun `hasFrostRisk returns false for strong wind`() {
        val risk = FrostCore.hasFrostRisk(
            temp = 0.0,
            humidity = 90.0,
            precip = 0.0,
            weatherCode = 0,
            tempThreshold = 1.0,
            humidityThreshold = 80.0,
            precipitationThreshold = 1.0,
            windSpeed = 20.0
        )
        assertFalse(risk)
    }

    @Test
    fun `probability level mapping is stable`() {
        assertEquals(FrostCore.ProbabilityLevel.VERY_HIGH, FrostCore.getFrostProbabilityLevel(90))
        assertEquals(FrostCore.ProbabilityLevel.HIGH, FrostCore.getFrostProbabilityLevel(70))
        assertEquals(FrostCore.ProbabilityLevel.MODERATE, FrostCore.getFrostProbabilityLevel(45))
        assertEquals(FrostCore.ProbabilityLevel.LOW, FrostCore.getFrostProbabilityLevel(20))
        assertEquals(FrostCore.ProbabilityLevel.MINIMAL, FrostCore.getFrostProbabilityLevel(5))
    }

    @Test
    fun `calculateRiskLevel returns full score for high risk frost`() {
        assertTrue(FrostCore.calculateRiskLevel(hasRisk = true, minTemp = -6.0) == 1.0)
    }
}
