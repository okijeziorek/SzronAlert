package pl.oki.frostalert.utils

import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AnalyticsHelper].
 *
 * Firebase SDK cannot be initialized in a JVM unit test (it requires a real Android/Firebase
 * environment), so these tests verify the "not initialized" path — every public method must be
 * a safe no-op when [AnalyticsHelper.init] has never been called.
 */
class AnalyticsHelperTest {

    @Before
    fun resetAnalyticsHelper() {
        // Use reflection to null out the private fields so each test starts from a clean state.
        val analyticsField = AnalyticsHelper::class.java.getDeclaredField("analytics")
        analyticsField.isAccessible = true
        analyticsField.set(AnalyticsHelper, null)

        val crashlyticsField = AnalyticsHelper::class.java.getDeclaredField("crashlytics")
        crashlyticsField.isAccessible = true
        crashlyticsField.set(AnalyticsHelper, null)
    }

    @Test
    fun `logWorkerSuccess does not throw when not initialized`() {
        AnalyticsHelper.logWorkerSuccess()
    }

    @Test
    fun `logWidgetRefresh does not throw when not initialized`() {
        AnalyticsHelper.logWidgetRefresh()
    }

    @Test
    fun `logPurchaseAttempt does not throw when not initialized`() {
        AnalyticsHelper.logPurchaseAttempt("frostalert_pro")
    }

    @Test
    fun `logPurchaseComplete does not throw when not initialized`() {
        AnalyticsHelper.logPurchaseComplete("frostalert_pro")
    }

    @Test
    fun `logBillingError does not throw when not initialized`() {
        AnalyticsHelper.logBillingError()
    }

    @Test
    fun `logFrostAlertSent does not throw when not initialized`() {
        AnalyticsHelper.logFrostAlertSent(appMode = 0, minTemp = -2.5)
        AnalyticsHelper.logFrostAlertSent(appMode = 1, minTemp = 0.0)
    }

    @Test
    fun `logGeofenceTrigger does not throw when not initialized`() {
        AnalyticsHelper.logGeofenceTrigger()
    }

    @Test
    fun `recordNonFatalException does not throw when not initialized`() {
        AnalyticsHelper.recordNonFatalException(RuntimeException("test"))
    }

    @Test
    fun `setCustomKey does not throw when not initialized`() {
        AnalyticsHelper.setCustomKey("test_key", "test_value")
    }

    @Test
    fun `analytics field is null before init`() {
        val field = AnalyticsHelper::class.java.getDeclaredField("analytics")
        field.isAccessible = true
        assertNull(field.get(AnalyticsHelper))
    }

    @Test
    fun `crashlytics field is null before init`() {
        val field = AnalyticsHelper::class.java.getDeclaredField("crashlytics")
        field.isAccessible = true
        assertNull(field.get(AnalyticsHelper))
    }
}
