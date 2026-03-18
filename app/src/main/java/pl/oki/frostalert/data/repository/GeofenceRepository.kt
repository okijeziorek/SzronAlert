package pl.oki.frostalert.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.GeofenceRecord

class GeofenceRepository(private val context: Context) {
    private val db = FrostDatabase.getDatabase(context)
    private val dao = db.geofenceDao()

    fun getRecentRecords(): Flow<List<GeofenceRecord>> = dao.getRecentGeofenceRecords()

    suspend fun deleteOlderThan(olderThan: Long) = dao.deleteOlderThan(olderThan)
}

