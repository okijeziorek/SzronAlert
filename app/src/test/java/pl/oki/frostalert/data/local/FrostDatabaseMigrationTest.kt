package pl.oki.frostalert.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that:
 *  1. The database schema is consistent across all three tables.
 *  2. Explicit migration objects are defined (MIGRATION_1_2, MIGRATION_2_3).
 *  3. The database builds without [fallbackToDestructiveMigration].
 *
 * Uses an in-memory database via Robolectric — no emulator required.
 */
@RunWith(RobolectricTestRunner::class)
class FrostDatabaseMigrationTest {

    private lateinit var db: FrostDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FrostDatabase::class.java)
            .addMigrations(FrostDatabase.MIGRATION_1_2, FrostDatabase.MIGRATION_2_3)
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ── Schema integrity ──────────────────────────────────────────────────────

    @Test
    fun temperatureRecords_insertAndRead() = runBlocking {
        val record = TemperatureRecord(timestamp = 1_000_000L, minTemp = -2.5, hasRisk = true)
        db.temperatureDao().insert(record)
        val all = db.temperatureDao().getAllRecords()
        assertEquals(1, all.size)
        assertEquals(-2.5, all[0].minTemp, 0.01)
        assertTrue(all[0].hasRisk)
    }

    @Test
    fun calibrationFeedback_insertAndRead() = runBlocking {
        val feedback = CalibrationFeedback(
            timestamp = 2_000_000L,
            actualFrostOccurred = true,
            predictedRisk = false,
            temperature = 0.5,
            humidity = 80,
            weatherCode = 0,
            locationLat = 52.0,
            locationLon = 21.0,
            appMode = 0,
            usedThreshold = 1.0,
            usedHumidityThreshold = 75,
            usedSensitivity = 1.0
        )
        db.calibrationDao().insertFeedback(feedback)
        val count = db.calibrationDao().getTotalFeedbackCount()
        assertEquals(1, count)
    }

    @Test
    fun geofenceRecord_insertAndRead() = runBlocking {
        val geo = GeofenceRecord(
            timestamp = 3_000_000L,
            latitude = 52.0,
            longitude = 21.0,
            direction = "north",
            minTemp = 0.5,
            hasRisk = false,
            riskLevel = 0.1,
            locationName = "Warsaw"
        )
        db.geofenceDao().insert(geo)
        val list = db.geofenceDao().getRecentGeofenceRecords().first()
        assertEquals(1, list.size)
        assertEquals("Warsaw", list[0].locationName)
    }

    // ── Migration constants present ───────────────────────────────────────────

    @Test
    fun migrationObjects_areNotNull() {
        // Ensures MIGRATION_1_2 and MIGRATION_2_3 are properly declared as public
        // companion val — if they were removed or made private this test won't compile.
        assertTrue(FrostDatabase.MIGRATION_1_2.startVersion == 1)
        assertTrue(FrostDatabase.MIGRATION_1_2.endVersion == 2)
        assertTrue(FrostDatabase.MIGRATION_2_3.startVersion == 2)
        assertTrue(FrostDatabase.MIGRATION_2_3.endVersion == 3)
    }
}
