package pl.oki.frostalert.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TemperatureRecord::class, CalibrationFeedback::class], version = 2, exportSchema = false)
abstract class FrostDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
    abstract fun calibrationDao(): CalibrationDao

    companion object {
        @Volatile
        private var INSTANCE: FrostDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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

        fun getDatabase(context: Context): FrostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FrostDatabase::class.java,
                    "frost_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}