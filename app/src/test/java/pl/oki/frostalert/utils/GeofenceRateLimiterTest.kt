package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceRateLimiterTest {

    @Test
    fun `tryAcquire allows calls up to max limit`() {
        val limiter = GeofenceRateLimiter(maxCallsPerTrigger = 3)

        assertTrue(limiter.tryAcquire()) // 1
        assertTrue(limiter.tryAcquire()) // 2
        assertTrue(limiter.tryAcquire()) // 3
        assertFalse(limiter.tryAcquire()) // 4 — over limit
    }

    @Test
    fun `default limit is MAX_CALLS_PER_TRIGGER (8)`() {
        val limiter = GeofenceRateLimiter()

        repeat(8) {
            assertTrue("Call ${it + 1} should be allowed", limiter.tryAcquire())
        }
        assertFalse("Call 9 should be blocked", limiter.tryAcquire())
    }

    @Test
    fun `remaining returns correct count`() {
        val limiter = GeofenceRateLimiter(maxCallsPerTrigger = 5)

        assertEquals(5, limiter.remaining())
        limiter.tryAcquire()
        assertEquals(4, limiter.remaining())
        limiter.tryAcquire()
        assertEquals(3, limiter.remaining())
    }

    @Test
    fun `remaining never returns negative`() {
        val limiter = GeofenceRateLimiter(maxCallsPerTrigger = 1)
        limiter.tryAcquire()
        limiter.tryAcquire() // over limit
        assertEquals(0, limiter.remaining())
    }

    @Test
    fun `reset restores full capacity`() {
        val limiter = GeofenceRateLimiter(maxCallsPerTrigger = 2)
        limiter.tryAcquire()
        limiter.tryAcquire()
        assertFalse(limiter.tryAcquire())

        limiter.reset()
        assertEquals(2, limiter.remaining())
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun `zero max disallows all calls`() {
        val limiter = GeofenceRateLimiter(maxCallsPerTrigger = 0)
        assertFalse(limiter.tryAcquire())
        assertEquals(0, limiter.remaining())
    }
}
