package pl.oki.frostalert.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)

class GeofenceDaoTest {
    private lateinit var db: FrostDatabase
    private lateinit var dao: GeofenceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FrostDatabase::class.java).build()
        dao = db.geofenceDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndReadGeofence() = runBlocking {
        val record = GeofenceRecord(
            timestamp = System.currentTimeMillis(),
            latitude = 52.0,
            longitude = 21.0,
            direction = "północ",
            minTemp = -2.0,
            hasRisk = true,
            riskLevel = 0.7,
            locationName = "Warszawa"
        )
        dao.insert(record)
        val list = dao.getRecentGeofenceRecords().first()
        assertEquals(1, list.size)
        assertEquals("Warszawa", list[0].locationName)
    }
}

