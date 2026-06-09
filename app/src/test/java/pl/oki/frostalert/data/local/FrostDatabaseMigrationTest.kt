package pl.oki.frostalert.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that:
 *  1. The database schema is consistent across all three tables.
 *  2. Explicit migration objects are defined (MIGRATION_1_2, ..., MIGRATION_8_9).
 *  3. The database builds without [fallbackToDestructiveMigration].
 *  4. Data integrity is maintained across insert/read cycles.
 *  5. Edge-case data values are handled correctly.
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
            .addMigrations(
                FrostDatabase.MIGRATION_1_2,
                FrostDatabase.MIGRATION_2_3,
                FrostDatabase.MIGRATION_3_4,
                FrostDatabase.MIGRATION_4_5,
                FrostDatabase.MIGRATION_5_6,
                FrostDatabase.MIGRATION_6_7,
                FrostDatabase.MIGRATION_7_8,
                FrostDatabase.MIGRATION_8_9
            )
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
        // Ensures migration objects are properly declared as public
        // companion val — if they were removed or made private this test won't compile.
        assertTrue(FrostDatabase.MIGRATION_1_2.startVersion == 1)
        assertTrue(FrostDatabase.MIGRATION_1_2.endVersion == 2)
        assertTrue(FrostDatabase.MIGRATION_2_3.startVersion == 2)
        assertTrue(FrostDatabase.MIGRATION_2_3.endVersion == 3)
        assertTrue(FrostDatabase.MIGRATION_8_9.startVersion == 8)
        assertTrue(FrostDatabase.MIGRATION_8_9.endVersion == 9)
    }

    @Test
    fun migration8to9_addsTimestampIndexes() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration8to9_${System.nanoTime()}"
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val sqliteDb = openHelper.writableDatabase
        try {
            sqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS `temperature_records` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, `minTemp` REAL NOT NULL, " +
                    "`hasRisk` INTEGER NOT NULL, `frostProbability` INTEGER NOT NULL DEFAULT 0)"
            )
            sqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS `geofence_record` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, `direction` TEXT, `minTemp` REAL NOT NULL, " +
                    "`hasRisk` INTEGER NOT NULL, `riskLevel` REAL NOT NULL, `locationName` TEXT)"
            )

            FrostDatabase.MIGRATION_8_9.migrate(sqliteDb)

            val temperatureIndexes = indexNames(sqliteDb, "temperature_records")
            val geofenceIndexes = indexNames(sqliteDb, "geofence_record")
            assertTrue(temperatureIndexes.contains("index_temperature_records_timestamp"))
            assertTrue(geofenceIndexes.contains("index_geofence_record_timestamp"))
        } finally {
            sqliteDb.close()
            openHelper.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun indexNames(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val cursor = db.query("PRAGMA index_list(`$tableName`)")
        return cursor.use {
            val indexColumn = it.getColumnIndexOrThrow("name")
            buildSet {
                while (it.moveToNext()) {
                    add(it.getString(indexColumn))
                }
            }
        }
    }

    @Test
    fun migration7to8_wateringLog_createsTableAndCanInsertAndRead() = runBlocking {
        // Verify the MIGRATION_7_8 object covers the right versions.
        assertTrue(FrostDatabase.MIGRATION_7_8.startVersion == 7)
        assertTrue(FrostDatabase.MIGRATION_7_8.endVersion == 8)

        // Insert a plant and then log a watering entry for it.
        val plant = Plant(name = "Pomidor", category = "Warzywa", frostThresholdCelsius = 2.0)
        db.plantDao().insertAll(listOf(plant))
        val plantId = db.plantDao().getAllPlants().first().first { it.name == "Pomidor" }.id

        val log = WateringLog(plantId = plantId, timestamp = 1_000_000L, note = "first watering")
        db.wateringLogDao().insert(log)

        val entries = db.wateringLogDao().getLastWateringPerPlant().first()
        assertEquals(1, entries.size)
        assertEquals(plantId, entries[0].plantId)
        assertEquals(1_000_000L, entries[0].lastTimestamp)
    }

    @Test
    fun migration7to8_wateringLog_deleteForPlant_removesEntries() = runBlocking {
        val plant = Plant(name = "Ogórek", category = "Warzywa", frostThresholdCelsius = 3.0)
        db.plantDao().insertAll(listOf(plant))
        val plantId = db.plantDao().getAllPlants().first().first { it.name == "Ogórek" }.id

        db.wateringLogDao().insert(WateringLog(plantId = plantId, timestamp = 2_000_000L))
        db.wateringLogDao().insert(WateringLog(plantId = plantId, timestamp = 3_000_000L))
        db.wateringLogDao().deleteForPlant(plantId)

        val entries = db.wateringLogDao().getLastWateringPerPlant().first()
        assertTrue(entries.none { it.plantId == plantId })
    }

    // ── Data integrity ────────────────────────────────────────────────────────

    @Test
    fun temperatureRecords_multipleInserts_preserveOrder() = runBlocking {
        val records = (1..10).map { i ->
            TemperatureRecord(
                timestamp = System.currentTimeMillis() - (10 - i) * 3600_000L,
                minTemp = -5.0 + i,
                hasRisk = i < 5
            )
        }
        records.forEach { db.temperatureDao().insert(it) }

        val all = db.temperatureDao().getAllRecords()
        assertEquals(10, all.size)
        // getAllRecords() is ordered by timestamp DESC
        assertTrue("Records should be ordered by timestamp DESC", all[0].timestamp >= all[1].timestamp)
    }

    @Test
    fun temperatureRecords_recentRecordsLimit30() = runBlocking {
        // Insert 35 records
        (1..35).forEach { i ->
            db.temperatureDao().insert(
                TemperatureRecord(
                    timestamp = i * 1000L,
                    minTemp = i.toDouble(),
                    hasRisk = false
                )
            )
        }
        val recent = db.temperatureDao().getRecentRecords().first()
        assertEquals(30, recent.size)
    }

    @Test
    fun temperatureRecords_edgeCaseTemperatures() = runBlocking {
        // Test extreme temperature values
        val extremeRecords = listOf(
            TemperatureRecord(timestamp = 1L, minTemp = -50.0, hasRisk = true),
            TemperatureRecord(timestamp = 2L, minTemp = 0.0, hasRisk = false),
            TemperatureRecord(timestamp = 3L, minTemp = 50.0, hasRisk = false),
            TemperatureRecord(timestamp = 4L, minTemp = -0.001, hasRisk = true)
        )
        extremeRecords.forEach { db.temperatureDao().insert(it) }

        val all = db.temperatureDao().getAllRecords()
        assertEquals(4, all.size)
        assertEquals(-50.0, all.minOf { it.minTemp }, 0.001)
        assertEquals(50.0, all.maxOf { it.minTemp }, 0.001)
    }

    @Test
    fun temperatureRecords_aggregateQueries() = runBlocking {
        db.temperatureDao().insert(TemperatureRecord(timestamp = 1L, minTemp = -5.0, hasRisk = true))
        db.temperatureDao().insert(TemperatureRecord(timestamp = 2L, minTemp = 5.0, hasRisk = false))
        db.temperatureDao().insert(TemperatureRecord(timestamp = 3L, minTemp = 0.0, hasRisk = true))

        val riskCount = db.temperatureDao().getRiskCount()
        assertEquals(2, riskCount)

        val avgTemp = db.temperatureDao().getAverageMinTemp()
        assertNotNull(avgTemp)
        assertEquals(0.0, avgTemp!!, 0.01)

        val minTemp = db.temperatureDao().getAbsoluteMinTemp()
        assertNotNull(minTemp)
        assertEquals(-5.0, minTemp!!, 0.01)
    }

    @Test
    fun temperatureRecords_emptyTableReturnsDefaults() = runBlocking {
        val all = db.temperatureDao().getAllRecords()
        assertTrue(all.isEmpty())

        val riskCount = db.temperatureDao().getRiskCount()
        assertEquals(0, riskCount)

        val avgTemp = db.temperatureDao().getAverageMinTemp()
        assertNull(avgTemp)
    }

    @Test
    fun geofenceRecord_nullableDirectionAndLocationName() = runBlocking {
        val geo = GeofenceRecord(
            timestamp = 1L,
            latitude = 0.0,
            longitude = 0.0,
            direction = null,
            minTemp = 0.0,
            hasRisk = false,
            riskLevel = 0.0,
            locationName = null
        )
        db.geofenceDao().insert(geo)
        val list = db.geofenceDao().getRecentGeofenceRecords().first()
        assertEquals(1, list.size)
        assertNull(list[0].direction)
        assertNull(list[0].locationName)
    }

    @Test
    fun geofenceRecord_deleteOlderThan() = runBlocking {
        db.geofenceDao().insert(GeofenceRecord(timestamp = 1000L, latitude = 0.0, longitude = 0.0, direction = null, minTemp = 0.0, hasRisk = false, riskLevel = 0.0, locationName = null))
        db.geofenceDao().insert(GeofenceRecord(timestamp = 2000L, latitude = 0.0, longitude = 0.0, direction = null, minTemp = 0.0, hasRisk = false, riskLevel = 0.0, locationName = null))
        db.geofenceDao().insert(GeofenceRecord(timestamp = 3000L, latitude = 0.0, longitude = 0.0, direction = null, minTemp = 0.0, hasRisk = false, riskLevel = 0.0, locationName = null))

        db.geofenceDao().deleteOlderThan(2500L)

        val remaining = db.geofenceDao().getRecentGeofenceRecords().first()
        assertEquals(1, remaining.size)
        assertEquals(3000L, remaining[0].timestamp)
    }
}
