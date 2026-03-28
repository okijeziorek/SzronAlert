package pl.oki.frostalert.geofence

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.LocationRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GeofenceRegistrarTest {

    private fun buildPrefs(isGeofencingEnabled: Boolean = false) = UserPreferences(
        tempThreshold = 1.0,
        humidityThreshold = 75,
        precipitationThreshold = 0.2,
        sensitivity = 1.0,
        alertStartHour = 19,
        alertEndHour = 8,
        ignoreUntil = 0L,
        carModeHour = 7,
        isCarModeEnabled = true,
        isAutoModeEnabled = true,
        isMataOptionEnabled = false,
        appMode = 0,
        lastFeedbackTimestamp = 0L,
        isProForced = false,
        heatThreshold = 30.0,
        isStormAlertEnabled = true,
        isWateringReminderEnabled = true,
        isGeofencingEnabled = isGeofencingEnabled,
        isTrendChangeNotificationsEnabled = true,
        lastTrend = null,
        pendingTrend = null,
        theme = 2,
        isManualLocationEnabled = false,
        manualLatitude = 52.2297,
        manualLongitude = 21.0122,
        manualLocationName = "Warszawa",
        isOnboardingCompleted = true,
        useFahrenheit = false,
        geofenceRadiusMeters = 20000.0
    )

    @Test
    fun `isRunning returns false before start`() {
        val prefsFlow = MutableStateFlow(buildPrefs())
        val settingsDataStore: SettingsDataStore = mock()
        val locationRepository: LocationRepository = mock()
        val geofenceManager: GeofenceManager = mock()
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(prefsFlow)

        val registrar = GeofenceRegistrar(
            context = mock(),
            settingsDataStore = settingsDataStore,
            locationRepository = locationRepository,
            geofenceManager = geofenceManager,
            debounceMs = 0L
        )

        assertFalse("Powinno być false przed start()", registrar.isRunning())
    }

    @Test
    fun `start sets isRunning to true and stop sets it back to false`() = runTest {
        val prefsFlow = MutableStateFlow(buildPrefs(isGeofencingEnabled = false))
        val settingsDataStore: SettingsDataStore = mock()
        val locationRepository: LocationRepository = mock()
        val geofenceManager: GeofenceManager = mock()
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(prefsFlow)
        whenever(locationRepository.getEffectiveLocation()).thenReturn(null)

        val registrar = GeofenceRegistrar(
            context = mock(),
            settingsDataStore = settingsDataStore,
            locationRepository = locationRepository,
            geofenceManager = geofenceManager,
            debounceMs = 0L
        )

        registrar.start()
        assertTrue("isRunning() powinno być true po start()", registrar.isRunning())

        registrar.stop()
        assertFalse("isRunning() powinno być false po stop()", registrar.isRunning())
    }

    @Test
    fun `calling start twice does not create duplicate observers`() = runTest {
        val prefsFlow = MutableStateFlow(buildPrefs(isGeofencingEnabled = false))
        val settingsDataStore: SettingsDataStore = mock()
        val locationRepository: LocationRepository = mock()
        val geofenceManager: GeofenceManager = mock()
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(prefsFlow)
        whenever(locationRepository.getEffectiveLocation()).thenReturn(null)

        val registrar = GeofenceRegistrar(
            context = mock(),
            settingsDataStore = settingsDataStore,
            locationRepository = locationRepository,
            geofenceManager = geofenceManager,
            debounceMs = 0L
        )

        registrar.start()
        registrar.start() // second call should be a no-op
        assertTrue("Podwójne wywołanie start() nie powinno zmienić stanu", registrar.isRunning())

        registrar.stop()
        assertFalse("stop() powinno zatrzymać rejestrator", registrar.isRunning())
    }

    @Test
    fun `stop without start does not throw`() {
        val prefsFlow = MutableStateFlow(buildPrefs())
        val settingsDataStore: SettingsDataStore = mock()
        val locationRepository: LocationRepository = mock()
        val geofenceManager: GeofenceManager = mock()
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(prefsFlow)

        val registrar = GeofenceRegistrar(
            context = mock(),
            settingsDataStore = settingsDataStore,
            locationRepository = locationRepository,
            geofenceManager = geofenceManager,
            debounceMs = 0L
        )

        // Should not throw
        registrar.stop()
        assertFalse("isRunning() powinno pozostać false", registrar.isRunning())
    }
}
