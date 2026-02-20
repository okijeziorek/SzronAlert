package pl.oki.frostalert.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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