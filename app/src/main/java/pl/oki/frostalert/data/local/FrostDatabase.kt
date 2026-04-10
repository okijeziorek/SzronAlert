package pl.oki.frostalert.data.local

import android.content.Context
import androidx.room.Database
import pl.oki.frostalert.data.local.GeofenceDao
import pl.oki.frostalert.data.local.GeofenceRecord
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TemperatureRecord::class, CalibrationFeedback::class, pl.oki.frostalert.data.local.GeofenceRecord::class, Plant::class, UserPlant::class, SavedLocation::class, FrostPhoto::class, GardenZone::class], version = 7, exportSchema = false)
abstract class FrostDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun plantDao(): PlantDao
    abstract fun userPlantDao(): UserPlantDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun frostPhotoDao(): FrostPhotoDao
    abstract fun gardenZoneDao(): GardenZoneDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE temperature_records ADD COLUMN frostProbability INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `plants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `frostThresholdCelsius` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `iconEmoji` TEXT NOT NULL DEFAULT '🌱'
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_plants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `plantId` INTEGER NOT NULL,
                        `addedTimestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `saved_locations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `addedTimestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `frost_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `temperatureRecordId` INTEGER,
                        `note` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `garden_zones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `temperatureCorrection` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `iconEmoji` TEXT NOT NULL DEFAULT '🌡️',
                        `addedTimestamp` INTEGER NOT NULL
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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