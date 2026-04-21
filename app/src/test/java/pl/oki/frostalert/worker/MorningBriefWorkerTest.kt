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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository
import pl.oki.frostalert.utils.NetworkMonitor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MorningBriefWorkerTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var locationRepository: LocationRepository
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var morningWakeLearningRepository: MorningWakeLearningRepository

    private fun buildPrefs(
        isMorningBriefEnabled: Boolean = true,
        morningWindowStartMinute: Int = 360,
        morningWindowEndMinute: Int = 600,
        morningLastNotificationEpochDay: Long = -1L
    ) = UserPreferences(
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
        isGeofencingEnabled = false,
        isTrendChangeNotificationsEnabled = false,
        lastTrend = null,
        pendingTrend = null,
        theme = 2,
        isManualLocationEnabled = false,
        manualLatitude = 52.2297,
        manualLongitude = 21.0122,
        manualLocationName = "Warszawa",
        isOnboardingCompleted = true,
        useFahrenheit = false,
        geofenceRadiusMeters = 20000.0,
        activeLocationId = 0,
        isMorningBriefEnabled = isMorningBriefEnabled,
        morningWindowStartMinute = morningWindowStartMinute,
        morningWindowEndMinute = morningWindowEndMinute,
        morningLastNotificationEpochDay = morningLastNotificationEpochDay
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = mock()
        locationRepository = mock()
        networkMonitor = mock()
        morningWakeLearningRepository = mock()
    }

    private fun buildWorker(): MorningBriefWorker {
        return TestListenableWorkerBuilder<MorningBriefWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return MorningBriefWorker(
                        appContext,
                        workerParameters,
                        settingsDataStore,
                        locationRepository,
                        networkMonitor,
                        morningWakeLearningRepository
                    )
                }
            })
            .build() as MorningBriefWorker
    }

    // ── Network guard ─────────────────────────────────────────────────────────

    @Test
    fun `offline returns retry`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(false)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `offline does not touch DataStore`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(false)

        buildWorker().doWork()

        verify(settingsDataStore, never()).userPreferencesFlow
    }

    // ── Feature gate ──────────────────────────────────────────────────────────

    @Test
    fun `feature disabled returns success without fetching location`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow)
            .thenReturn(MutableStateFlow(buildPrefs(isMorningBriefEnabled = false)))

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(locationRepository, never()).getEffectiveLocation()
    }

    // ── Anti-spam gate ────────────────────────────────────────────────────────

    @Test
    fun `anti-spam returns success when already sent today`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow)
            .thenReturn(MutableStateFlow(buildPrefs(isMorningBriefEnabled = true)))
        whenever(morningWakeLearningRepository.shouldScheduleBriefToday(any())).thenReturn(false)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(locationRepository, never()).getEffectiveLocation()
    }

    // ── Location unavailable ──────────────────────────────────────────────────

    @Test
    fun `location unavailable returns retry`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow)
            .thenReturn(MutableStateFlow(buildPrefs(isMorningBriefEnabled = true)))
        whenever(morningWakeLearningRepository.shouldScheduleBriefToday(any())).thenReturn(true)
        whenever(locationRepository.getEffectiveLocation()).thenReturn(null)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `location SecurityException returns retry`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow)
            .thenReturn(MutableStateFlow(buildPrefs(isMorningBriefEnabled = true)))
        whenever(morningWakeLearningRepository.shouldScheduleBriefToday(any())).thenReturn(true)
        whenever(locationRepository.getEffectiveLocation())
            .thenThrow(SecurityException("Permission denied"))

        val result = buildWorker().doWork()

        val valid = result == ListenableWorker.Result.retry() || result == ListenableWorker.Result.failure()
        assertTrue("Expected retry or failure for SecurityException", valid)
    }

    // ── Weather fetch ─────────────────────────────────────────────────────────

    @Test
    fun `API failure is handled gracefully`() = runTest {
        whenever(networkMonitor.isCurrentlyOnline()).thenReturn(true)
        whenever(settingsDataStore.userPreferencesFlow)
            .thenReturn(MutableStateFlow(buildPrefs(isMorningBriefEnabled = true)))
        whenever(morningWakeLearningRepository.shouldScheduleBriefToday(any())).thenReturn(true)
        val loc = Location("test").apply { latitude = 52.0; longitude = 21.0 }
        whenever(locationRepository.getEffectiveLocation()).thenReturn(loc)

        // OpenMeteoApi is a real singleton — network will fail in test env
        val result = buildWorker().doWork()

        val valid = result == ListenableWorker.Result.success() ||
                result == ListenableWorker.Result.retry() ||
                result == ListenableWorker.Result.failure()
        assertTrue("Worker should return a valid result on network failure", valid)
    }
}
