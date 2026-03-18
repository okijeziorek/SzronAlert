package pl.oki.frostalert.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val repository: SettingsRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    private val defaultPrefs = UserPreferences(
        tempThreshold = 1.0,
        humidityThreshold = 75,
        precipitationThreshold = 0.2,
        sensitivity = 1.0,
        alertStartHour = 19,
        alertEndHour = 8,
        ignoreUntil = 0L,
        carModeHour = 7,
        isAutoModeEnabled = true,
        isCarModeEnabled = true,
        isMataOptionEnabled = false,
        appMode = 0,
        lastFeedbackTimestamp = 0L,
        isProForced = false,
        heatThreshold = 30.0,
        isStormAlertEnabled = true,
        isWateringReminderEnabled = true,
        // geofencing defaults
        isGeofencingEnabled = false,
        // trend notifications
        isTrendChangeNotificationsEnabled = true,
        lastTrend = null,
        theme = 2,
        isManualLocationEnabled = false,
        manualLatitude = 52.2297,
        manualLongitude = 21.0122,
        manualLocationName = "Warszawa",
        isOnboardingCompleted = false,
        useFahrenheit = false
        ,
        geofenceRadiusMeters = 20000.0
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.userPreferencesFlow).thenReturn(MutableStateFlow(defaultPrefs))
        // Use a simple fake registrar implementing the contract
        class FakeRegistrar : pl.oki.frostalert.geofence.GeofenceRegistrarContract {
            override fun registerForCurrentLocation(locationRepo: pl.oki.frostalert.data.repository.LocationRepository) {}
            override fun unregister() {}
        }

        val geofenceRegistrar = FakeRegistrar()
        val appContext: android.content.Context = mock()
        viewModel = SettingsViewModel(repository, geofenceRegistrar, appContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateTheme calls repository`() = runTest(testDispatcher) {
        viewModel.updateTheme(1)
        advanceUntilIdle()
        verify(repository).updateTheme(1)
    }

    @Test
    fun `updateAppMode calls repository`() = runTest(testDispatcher) {
        viewModel.updateAppMode(1)
        advanceUntilIdle()
        verify(repository).updateAppMode(1)
    }

    @Test
    fun `updateMataOptionEnabled calls repository`() = runTest(testDispatcher) {
        viewModel.updateMataOptionEnabled(true)
        advanceUntilIdle()
        verify(repository).updateMataOptionEnabled(true)
    }

    @Test
    fun `updateIsProForced calls repository`() = runTest(testDispatcher) {
        viewModel.updateIsProForced(true)
        advanceUntilIdle()
        verify(repository).updateIsProForced(true)
    }

    @Test
    fun `updateSensitivity calls repository`() = runTest(testDispatcher) {
        viewModel.updateSensitivity(1.5)
        advanceUntilIdle()
        verify(repository).updateSensitivity(1.5)
    }
}
