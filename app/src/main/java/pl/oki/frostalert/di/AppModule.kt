package pl.oki.frostalert.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.billing.BillingManagerInterface
import pl.oki.frostalert.data.local.CalibrationDao
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.repository.HistoryRepository
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.SettingsRepository
import pl.oki.frostalert.data.repository.SettingsRepositoryImpl
import pl.oki.frostalert.utils.NetworkMonitor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBillingManager(billingClientWrapper: BillingClientWrapper): BillingManagerInterface {
        return billingClientWrapper
    }

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
    @Singleton
    fun provideGeofenceDao(database: FrostDatabase): pl.oki.frostalert.data.local.GeofenceDao {
        return database.geofenceDao()
    }

    @Provides
    @Singleton
    fun provideGeofenceRepository(@ApplicationContext context: Context): pl.oki.frostalert.data.repository.GeofenceRepository {
        return pl.oki.frostalert.data.repository.GeofenceRepository(context)
    }

    @Provides
    fun provideGeofenceManager(@ApplicationContext context: Context): pl.oki.frostalert.geofence.GeofenceManager {
        return pl.oki.frostalert.geofence.GeofenceManager(context)
    }

    @Provides
    @Singleton
    fun provideGeofenceRegistrar(
        @ApplicationContext context: Context,
        settingsDataStore: pl.oki.frostalert.data.local.SettingsDataStore,
        locationRepository: LocationRepository,
        geofenceManager: pl.oki.frostalert.geofence.GeofenceManager
    ): pl.oki.frostalert.geofence.GeofenceRegistrarContract {
        return pl.oki.frostalert.geofence.GeofenceRegistrar(context, settingsDataStore, locationRepository, geofenceManager)
    }

    @Provides
    fun provideCalibrationDao(database: FrostDatabase): CalibrationDao {
        return database.calibrationDao()
    }

    @Provides
    fun providePlantDao(database: FrostDatabase): pl.oki.frostalert.data.local.PlantDao {
        return database.plantDao()
    }

    @Provides
    fun provideUserPlantDao(database: FrostDatabase): pl.oki.frostalert.data.local.UserPlantDao {
        return database.userPlantDao()
    }

    @Provides
    fun provideSavedLocationDao(database: FrostDatabase): pl.oki.frostalert.data.local.SavedLocationDao {
        return database.savedLocationDao()
    }

    @Provides
    fun provideFrostPhotoDao(database: FrostDatabase): pl.oki.frostalert.data.local.FrostPhotoDao {
        return database.frostPhotoDao()
    }

    @Provides
    fun provideGardenZoneDao(database: FrostDatabase): pl.oki.frostalert.data.local.GardenZoneDao {
        return database.gardenZoneDao()
    }

    @Provides
    @Singleton
    fun provideSmartHomeRepository(): pl.oki.frostalert.data.repository.SmartHomeRepository {
        return pl.oki.frostalert.data.repository.SmartHomeRepository()
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): pl.oki.frostalert.data.repository.CalendarRepository {
        return pl.oki.frostalert.data.repository.CalendarRepository(context)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
