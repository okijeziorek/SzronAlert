package pl.oki.frostalert

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pl.oki.frostalert.data.TemperatureRecord

@Database(entities = [TemperatureRecord::class], version = 1)
abstract class FrostDatabase : RoomDatabase() {
    abstract fun temperatureDao(): TemperatureDao

    companion object {
        @Volatile
        private var INSTANCE: FrostDatabase? = null

        fun getDatabase(context: Context): FrostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FrostDatabase::class.java,
                    "frost_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}