package pl.oki.frostalert.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeofenceBroadcastReceiverTest {

    // ── Valid prefixed format: "geofence:<lat>:<lon>" ──────────────────────

    @Test
    fun `parseRequestId with prefixed format returns correct lat and lon`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("geofence:52.2297:21.0122")
        assertNotNull(result)
        assertEquals(52.2297, result!!.latitude, 0.0001)
        assertEquals(21.0122, result.longitude, 0.0001)
        assertNull(result.direction)
    }

    @Test
    fun `parseRequestId with prefixed format and direction returns all fields`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("geofence:52.2297:21.0122:north")
        assertNotNull(result)
        assertEquals(52.2297, result!!.latitude, 0.0001)
        assertEquals(21.0122, result.longitude, 0.0001)
        assertEquals("north", result.direction)
    }

    // ── Valid bare format: "<lat>:<lon>" ───────────────────────────────────

    @Test
    fun `parseRequestId with bare format returns correct lat and lon`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("52.2297:21.0122")
        assertNotNull(result)
        assertEquals(52.2297, result!!.latitude, 0.0001)
        assertEquals(21.0122, result.longitude, 0.0001)
        assertNull(result.direction)
    }

    @Test
    fun `parseRequestId with bare format and direction returns all fields`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("52.2297:21.0122:south")
        assertNotNull(result)
        assertEquals(52.2297, result!!.latitude, 0.0001)
        assertEquals(21.0122, result.longitude, 0.0001)
        assertEquals("south", result.direction)
    }

    // ── Negative coordinates ──────────────────────────────────────────────

    @Test
    fun `parseRequestId handles negative coordinates`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("geofence:-33.8688:151.2093")
        assertNotNull(result)
        assertEquals(-33.8688, result!!.latitude, 0.0001)
        assertEquals(151.2093, result.longitude, 0.0001)
    }

    @Test
    fun `parseRequestId handles both negative coordinates`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("-33.8688:-151.2093:east")
        assertNotNull(result)
        assertEquals(-33.8688, result!!.latitude, 0.0001)
        assertEquals(-151.2093, result.longitude, 0.0001)
        assertEquals("east", result.direction)
    }

    // ── Malformed IDs ─────────────────────────────────────────────────────

    @Test
    fun `parseRequestId returns null for null input`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId(null))
    }

    @Test
    fun `parseRequestId returns null for empty string`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId(""))
    }

    @Test
    fun `parseRequestId returns null for blank string`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("   "))
    }

    @Test
    fun `parseRequestId returns null for single element`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence"))
    }

    @Test
    fun `parseRequestId returns null for prefix only with colon`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence:"))
    }

    @Test
    fun `parseRequestId returns null when prefix present but lat is missing`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence::21.0"))
    }

    @Test
    fun `parseRequestId returns null when lat is non-numeric`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence:abc:21.0"))
    }

    @Test
    fun `parseRequestId returns null when lon is non-numeric`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence:52.2:xyz"))
    }

    @Test
    fun `parseRequestId returns null when prefix present but only lat provided`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("geofence:52.2"))
    }

    @Test
    fun `parseRequestId returns null for completely random string`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("not-a-geofence-id"))
    }

    @Test
    fun `parseRequestId returns null for single numeric value`() {
        assertNull(GeofenceBroadcastReceiver.parseRequestId("52.2297"))
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    fun `parseRequestId with zero coordinates succeeds`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("0.0:0.0")
        assertNotNull(result)
        assertEquals(0.0, result!!.latitude, 0.0001)
        assertEquals(0.0, result.longitude, 0.0001)
    }

    @Test
    fun `parseRequestId with integer coordinates succeeds`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("52:21")
        assertNotNull(result)
        assertEquals(52.0, result!!.latitude, 0.0001)
        assertEquals(21.0, result.longitude, 0.0001)
    }

    @Test
    fun `parseRequestId ignores blank direction`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("geofence:52.2:21.0:")
        assertNotNull(result)
        assertNull(result!!.direction)
    }

    @Test
    fun `parseRequestId with extra colons treats fourth part as direction`() {
        val result = GeofenceBroadcastReceiver.parseRequestId("geofence:52.2:21.0:north:extra")
        assertNotNull(result)
        assertEquals("north", result!!.direction)
    }
}
