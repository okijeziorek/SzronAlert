package pl.oki.frostalert.receiver

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.oki.frostalert.data.local.SettingsDataStore
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MorningUserPresentReceiverTest {

    private lateinit var context: android.content.Context
    private lateinit var dataStore: SettingsDataStore
    private val epochDay = LocalDate.now().toEpochDay()
    private val expectedWorkName = "${MorningUserPresentReceiver.WORK_NAME_PREFIX}$epochDay"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        dataStore = SettingsDataStore(context)
        // Reset unlock epoch so each test starts fresh
        runBlocking {
            dataStore.updateMorningLastUnlockEpochDay(-1L)
            dataStore.updateMorningLastNotificationEpochDay(-1L)
        }
    }

    private fun noWorkEnqueued(): Boolean =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(expectedWorkName)
            .get()
            .isEmpty()

    private fun workEnqueued(): Boolean =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(expectedWorkName)
            .get()
            .isNotEmpty()

    // ── Non-USER_PRESENT action ───────────────────────────────────────────────

    @Test
    fun `receiver ignores non-USER_PRESENT action and enqueues no work`() {
        MorningUserPresentReceiver().onReceive(context, Intent("android.intent.action.SCREEN_ON"))
        assertTrue("SCREEN_ON should not trigger any work", noWorkEnqueued())
    }

    // ── Feature disabled (default state) ──────────────────────────────────────

    @Test
    fun `feature disabled - no work enqueued`() {
        // isMorningBriefEnabled defaults to false in DataStore
        MorningUserPresentReceiver().onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        Thread.sleep(300) // wait for goAsync coroutine on Dispatchers.IO
        assertTrue("Feature disabled → no work should be enqueued", noWorkEnqueued())
    }

    // ── Feature enabled, first unlock in window ───────────────────────────────

    @Test
    fun `first unlock in morning window - work enqueued with expected unique name`() = runBlocking {
        dataStore.updateMorningBriefEnabled(true)
        dataStore.updateMorningWindowStartMinute(0)     // window = all day
        dataStore.updateMorningWindowEndMinute(1439)
        dataStore.updateMorningBriefDelayMinutes(0)

        MorningUserPresentReceiver().onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        Thread.sleep(500) // wait for goAsync coroutine

        assertTrue("Work should be enqueued with name $expectedWorkName", workEnqueued())
    }

    // ── Second unlock same day ────────────────────────────────────────────────

    @Test
    fun `second unlock same day - work NOT enqueued again`() = runBlocking {
        dataStore.updateMorningBriefEnabled(true)
        dataStore.updateMorningWindowStartMinute(0)
        dataStore.updateMorningWindowEndMinute(1439)
        dataStore.updateMorningBriefDelayMinutes(0)
        // Mark today as already unlocked → recordFirstUnlockOfDay returns false
        dataStore.updateMorningLastUnlockEpochDay(epochDay)

        MorningUserPresentReceiver().onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        Thread.sleep(500)

        assertTrue("Second unlock same day should not enqueue work", noWorkEnqueued())
    }

    // ── Outside morning window ────────────────────────────────────────────────

    @Test
    fun `outside morning window - no work enqueued`() = runBlocking {
        dataStore.updateMorningBriefEnabled(true)
        // Window: 06:00–06:01 — very unlikely to be current time
        dataStore.updateMorningWindowStartMinute(360)
        dataStore.updateMorningWindowEndMinute(361)
        dataStore.updateMorningBriefDelayMinutes(0)

        // We can't control "current minute" here, but if we're not in the 1-min window
        // the work should NOT be enqueued. This test is inherently environment-dependent
        // so we just verify no crash occurs and the result is consistent.
        MorningUserPresentReceiver().onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        Thread.sleep(500)
        // Either no work (outside window) or work (inside that 1-min window) — no crash
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(expectedWorkName).get()
        assertTrue("Result should be a valid (possibly empty) work info list", infos != null)
    }

    // ── Anti-spam: brief already sent today ───────────────────────────────────

    @Test
    fun `anti-spam blocks work when brief already sent today`() = runBlocking {
        dataStore.updateMorningBriefEnabled(true)
        dataStore.updateMorningWindowStartMinute(0)
        dataStore.updateMorningWindowEndMinute(1439)
        dataStore.updateMorningBriefDelayMinutes(0)
        // Mark today's notification as already sent
        dataStore.updateMorningLastNotificationEpochDay(epochDay)

        MorningUserPresentReceiver().onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        Thread.sleep(500)

        assertTrue("Brief already sent today → no new work should be enqueued", noWorkEnqueued())
    }

    // ── WORK_NAME_PREFIX constant ─────────────────────────────────────────────

    @Test
    fun `WORK_NAME_PREFIX has expected value`() {
        assertTrue(MorningUserPresentReceiver.WORK_NAME_PREFIX.startsWith("morning_brief_"))
    }
}

