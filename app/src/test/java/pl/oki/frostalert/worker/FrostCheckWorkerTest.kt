package pl.oki.frostalert.worker

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.utils.NetworkMonitor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FrostCheckWorkerTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var locationRepository: LocationRepository
    private lateinit var temperatureDao: TemperatureDao
    private lateinit var networkMonitor: NetworkMonitor

    private fun buildPrefs(
        ignoreUntil: Long = 0L,
        isAutoModeEnabled: Boolean = true,
        isGeofencingEnabled: Boolean = false,
        isTrendChangeNotificationsEnabled: Boolean = false
    ) = UserPreferences(
        tempThreshold = 1.0,
        humidityThreshold = 75,
        precipitationThreshold = 0.2,
        sensitivity = 1.0,
        alertStartHour = 19,
        alertEndHour = 8,
        ignoreUntil = ignoreUntil,
        carModeHour = 7,
        isCarModeEnabled = true,
        isAutoModeEnabled = isAutoModeEnabled,
        isMataOptionEnabled = false,
        appMode = 0,
        lastFeedbackTimestamp = 0L,
        isProForced = false,
        heatThreshold = 30.0,
        isStormAlertEnabled = true,
        isWateringReminderEnabled = true,
        isGeofencingEnabled = isGeofencingEnabled,
        isTrendChangeNotificationsEnabled = isTrendChangeNotificationsEnabled,
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = mock()
        locationRepository = mock()
        temperatureDao = mock()
        networkMonitor = mock()
    }

    private fun buildWorker(inputData: androidx.work.Data = androidx.work.Data.EMPTY): FrostCheckWorker {
        return TestListenableWorkerBuilder<FrostCheckWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return FrostCheckWorker(
                        appContext,
                        workerParameters,
                        settingsDataStore,
                        locationRepository,
                        temperatureDao,
                        networkMonitor
                    )
                }
            })
            .build() as FrostCheckWorker
    }

    @Test
    fun `test notification returns success immediately`() = runTest {
        val inputData = androidx.work.Data.Builder()
            .putBoolean("IS_TEST", true)
            .build()
        val worker = buildWorker(inputData)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `network unavailable returns retry`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(false)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `ignoreUntil in the future returns success without fetching weather`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        val prefs = buildPrefs(ignoreUntil = System.currentTimeMillis() + 60_000)
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(MutableStateFlow(prefs))

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(locationRepository, never()).getEffectiveLocation()
    }

    @Test
    fun `location unavailable returns retry`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(MutableStateFlow(buildPrefs()))
        whenever(locationRepository.getEffectiveLocation()).thenReturn(null)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker does not crash when API is unreachable`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow).thenReturn(MutableStateFlow(buildPrefs()))
        val location = Location("test").apply {
            latitude = 52.2297
            longitude = 21.0122
        }
        whenever(locationRepository.getEffectiveLocation()).thenReturn(location)
        whenever(temperatureDao.getRecentRecords()).thenReturn(flowOf(emptyList()))

        // OpenMeteoApi is a static singleton — in test env the actual HTTP call will fail.
        // The worker should handle this gracefully (retry or failure, never crash).
        val worker = buildWorker()
        val result = worker.doWork()

        val isValidResult = result == ListenableWorker.Result.success() ||
                result == ListenableWorker.Result.retry() ||
                result == ListenableWorker.Result.failure()
        assertEquals(true, isValidResult)
    }
}
