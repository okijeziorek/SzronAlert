package pl.oki.frostalert.data.local

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.Database
import pl.oki.frostalert.data.local.GeofenceDao
import pl.oki.frostalert.data.local.GeofenceRecord
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import pl.oki.frostalert.BuildConfig
import java.util.Locale

@Database(entities = [TemperatureRecord::class, CalibrationFeedback::class, pl.oki.frostalert.data.local.GeofenceRecord::class, Plant::class, UserPlant::class, SavedLocation::class, FrostPhoto::class, GardenZone::class, WateringLog::class], version = 8, exportSchema = false)
abstract class FrostDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun plantDao(): PlantDao
    abstract fun userPlantDao(): UserPlantDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun frostPhotoDao(): FrostPhotoDao
    abstract fun gardenZoneDao(): GardenZoneDao
    abstract fun wateringLogDao(): WateringLogDao

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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `watering_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `plantId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_log_plantId` ON `watering_log` (`plantId`)")
            }
        }

        private const val TAG = "FrostDatabase"
        private const val KEY_PREFS_FILE = "frost_db_key_prefs"
        private const val KEY_PREFS_PASSPHRASE = "db_passphrase"

        /**
         * Returns the database encryption passphrase.
         * On first call a random 32-byte key is generated and stored in
         * EncryptedSharedPreferences (backed by Android Keystore).
         * Subsequent calls return the same persisted key.
         */
        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                KEY_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val stored = prefs.getString(KEY_PREFS_PASSPHRASE, null)
            if (stored != null) {
                return Base64.decode(stored, Base64.DEFAULT)
            }

            val passphrase = ByteArray(32)
            java.security.SecureRandom().nextBytes(passphrase)
            prefs.edit().putString(KEY_PREFS_PASSPHRASE, Base64.encodeToString(passphrase, Base64.DEFAULT)).apply()
            return passphrase
        }

        fun getDatabase(context: Context): FrostDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FrostDatabase {
            return try {
                val passphrase = getOrCreatePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context,
                    FrostDatabase::class.java,
                    "frost_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // Do NOT add fallbackToDestructiveMigration — explicit migrations are defined
                // for every version bump; a missing migration should surface as a hard error,
                // not silently delete user data.
                .build()
                // Force-open to detect if an unencrypted DB exists under the same name.
                instance.openHelper.writableDatabase
                instance
            } catch (e: Exception) {
                // Closed testing only: allows one-time reset when testers upgrade from
                // pre-SQLCipher builds that left an unencrypted DB with the same name.
                // Production builds keep this disabled to avoid destructive data loss.
                if (BuildConfig.ENABLE_DB_RESET_FALLBACK && isLikelyUnencryptedDatabaseError(e)) {
                    Log.w(
                        TAG,
                        "Failed to open encrypted DB [${e::class.simpleName}]: ${e.message}. " +
                            "Closed testing fallback: deleting and recreating encrypted DB."
                    )
                    context.deleteDatabase("frost_database")
                    val passphrase = getOrCreatePassphrase(context)
                    val factory = SupportOpenHelperFactory(passphrase)
                    val recoveredInstance = Room.databaseBuilder(
                        context,
                        FrostDatabase::class.java,
                        "frost_database"
                    )
                        .openHelperFactory(factory)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                        .build()
                    recoveredInstance
                } else {
                    Log.e(
                        TAG,
                        "Failed to initialize encrypted DB [${e::class.simpleName}]: ${e.message}",
                        e
                    )
                    throw e
                }
            }
        }

        private fun isLikelyUnencryptedDatabaseError(error: Throwable): Boolean {
            val combinedMessage = buildString {
                var current: Throwable? = error
                while (current != null) {
                    if (!current.message.isNullOrBlank()) {
                        append(current.message)
                        append(' ')
                    }
                    current = current.cause
                }
            }.lowercase(Locale.ROOT)

            return combinedMessage.contains("file is not a database") ||
                combinedMessage.contains("file is encrypted or is not a database") ||
                combinedMessage.contains("not a database")
        }
    }
}
