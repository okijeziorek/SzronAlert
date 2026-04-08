package pl.oki.frostalert.utils

import android.util.Log

/**
 * Rate limiter for geofence-triggered weather API calls.
 *
 * Each geofence trigger may check multiple nearby locations. This limiter ensures
 * at most [maxCallsPerTrigger] API calls are performed within a single trigger cycle,
 * preventing excessive network usage and staying within Open-Meteo fair-use limits.
 */
class GeofenceRateLimiter(
    private val maxCallsPerTrigger: Int = MAX_CALLS_PER_TRIGGER
) {

    companion object {
        /**
         * Maximum number of weather API calls allowed per geofence trigger.
         * This covers 8 nearby directional checks (N, S, E, W, NE, SE, NW, SW).
         * The current-location call is avoided by reusing already-fetched weather data.
         */
        const val MAX_CALLS_PER_TRIGGER = 8

        private const val TAG = "GeofenceRateLimiter"
    }

    private var callCount = 0

    /**
     * Attempts to acquire a slot for an API call.
     * @return `true` if the call is allowed, `false` if the limit has been reached.
     */
    fun tryAcquire(): Boolean {
        if (callCount >= maxCallsPerTrigger) {
            Log.w(TAG, "Rate limit reached: $callCount/$maxCallsPerTrigger calls used")
            return false
        }
        callCount++
        return true
    }

    /** Returns the number of remaining API call slots. */
    fun remaining(): Int = (maxCallsPerTrigger - callCount).coerceAtLeast(0)

    /** Resets the counter (call at the start of each trigger cycle). */
    fun reset() {
        callCount = 0
    }
}
