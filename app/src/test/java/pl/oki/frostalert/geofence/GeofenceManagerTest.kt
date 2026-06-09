package pl.oki.frostalert.geofence

import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceManagerTest {

    @Test
    fun `transitionTypes includes ENTER EXIT and DWELL when loitering delay positive`() {
        val transitions = GeofenceManager.transitionTypes(loiteringDelayMs = 60_000)
        val expected = Geofence.GEOFENCE_TRANSITION_ENTER or
            Geofence.GEOFENCE_TRANSITION_EXIT or
            Geofence.GEOFENCE_TRANSITION_DWELL
        assertEquals(expected, transitions)
    }

    @Test
    fun `transitionTypes includes ENTER and EXIT when loitering delay non positive`() {
        val transitions = GeofenceManager.transitionTypes(loiteringDelayMs = 0)
        val expected = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        assertEquals(expected, transitions)
    }
}
