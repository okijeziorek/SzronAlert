package pl.oki.frostalert.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.oki.frostalert.data.local.CalibrationDao
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.repository.HistoryRepository
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.SettingsRepository
import pl.oki.frostalert.data.repository.SettingsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FrostDatabase {
        return FrostDatabase.getDatabase(context)
    }

    @Provides
    fun provideTemperatureDao(database: FrostDatabase): TemperatureDao {
        return database.temperatureDao()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: SettingsDataStore): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: TemperatureDao): HistoryRepository {
        return HistoryRepository(dao)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        dataStore: SettingsDataStore
    ): LocationRepository {
        return LocationRepository(context, dataStore)
    }

    @Provides
    fun provideCalibrationDao(database: FrostDatabase): CalibrationDao {
        return database.calibrationDao()
    }
}
