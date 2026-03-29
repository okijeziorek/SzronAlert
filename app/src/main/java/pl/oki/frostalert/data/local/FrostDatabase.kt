package pl.oki.frostalert.data.local

import android.content.Context
import androidx.room.Database
import pl.oki.frostalert.data.local.GeofenceDao
import pl.oki.frostalert.data.local.GeofenceRecord
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TemperatureRecord::class, CalibrationFeedback::class, pl.oki.frostalert.data.local.GeofenceRecord::class], version = 3, exportSchema = false)
abstract class FrostDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun geofenceDao(): GeofenceDao

    companion object {
        @Volatile
        private var INSTANCE: FrostDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `calibration_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `actualFrostOccurred` INTEGER NOT NULL,
                        `predictedRisk` INTEGER NOT NULL,
                        `temperature` REAL NOT NULL,
                        `humidity` INTEGER NOT NULL,
                        `weatherCode` INTEGER NOT NULL,
                        `locationLat` REAL NOT NULL,
                        `locationLon` REAL NOT NULL,
                        `appMode` INTEGER NOT NULL,
                        `usedThreshold` REAL NOT NULL,
                        `usedHumidityThreshold` INTEGER NOT NULL,
                        `usedSensitivity` REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `geofence_record` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `direction` TEXT,
                        `minTemp` REAL NOT NULL,
                        `hasRisk` INTEGER NOT NULL,
                        `riskLevel` REAL NOT NULL,
                        `locationName` TEXT
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): FrostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FrostDatabase::class.java,
                    "frost_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Do NOT add fallbackToDestructiveMigration — explicit migrations are defined
                // for every version bump; a missing migration should surface as a hard error,
                // not silently delete user data.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}