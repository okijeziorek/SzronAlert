package pl.oki.frostalert.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for billing purchase states via [BillingManagerInterface].
 *
 * These tests operate on a [FakeBillingManager] that simulates billing events
 * (purchase success, cancel, error, restore) without touching real Play Billing.
 */
class BillingClientWrapperTest {

    // ---------------------------------------------------------------------------
    // Fake implementation used in tests
    // ---------------------------------------------------------------------------

    private class FakeBillingManager : BillingManagerInterface {

        private val _isPro = MutableStateFlow(false)
        override val isPro: StateFlow<Boolean> = _isPro

        private val _purchaseError = MutableStateFlow<String?>(null)
        override val purchaseError: StateFlow<String?> = _purchaseError

        private val _purchaseCancelled = MutableStateFlow(false)
        override val purchaseCancelled: StateFlow<Boolean> = _purchaseCancelled

        private var storedProductDetails: ProductDetails? = null
        var lastLaunchedActivity: Activity? = null
        var disconnectCalled = false
        var restoreCalled = false

        fun simulatePurchaseSuccess() {
            _isPro.value = true
        }

        fun simulatePurchaseError(message: String) {
            _purchaseError.value = message
        }

        fun simulateUserCancelled() {
            _purchaseCancelled.value = true
        }

        fun simulateRestore(hasPro: Boolean) {
            _isPro.value = hasPro
        }

        fun setProductDetails(details: ProductDetails?) {
            storedProductDetails = details
        }

        override fun queryProductDetails(onDetailsReady: (ProductDetails?) -> Unit) {
            onDetailsReady(storedProductDetails)
        }

        override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
            lastLaunchedActivity = activity
        }

        override fun restorePurchases() {
            restoreCalled = true
        }

        override fun clearError() {
            _purchaseError.value = null
        }

        override fun clearCancellation() {
            _purchaseCancelled.value = false
        }

        override fun disconnect() {
            disconnectCalled = true
        }
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `initial isPro state is false`() = runTest {
        val billing = FakeBillingManager()
        assertFalse(billing.isPro.value)
    }

    @Test
    fun `purchase success sets isPro to true`() = runTest {
        val billing = FakeBillingManager()
        billing.simulatePurchaseSuccess()
        assertTrue(billing.isPro.value)
    }

    @Test
    fun `purchase error exposes error message`() = runTest {
        val billing = FakeBillingManager()
        billing.simulatePurchaseError("Item already owned")
        assertEquals("Item already owned", billing.purchaseError.value)
    }

    @Test
    fun `clearError resets purchaseError to null`() = runTest {
        val billing = FakeBillingManager()
        billing.simulatePurchaseError("Some error")
        billing.clearError()
        assertNull(billing.purchaseError.value)
    }

    @Test
    fun `user cancel does not set isPro and leaves no error`() = runTest {
        val billing = FakeBillingManager()
        billing.simulateUserCancelled()
        // Cancel must not grant PRO and must not set an error
        assertFalse(billing.isPro.value)
        assertNull(billing.purchaseError.value)
        // But the cancellation signal must be raised
        assertTrue(billing.purchaseCancelled.value)
    }

    @Test
    fun `clearCancellation resets purchaseCancelled to false`() = runTest {
        val billing = FakeBillingManager()
        billing.simulateUserCancelled()
        assertTrue(billing.purchaseCancelled.value)
        billing.clearCancellation()
        assertFalse(billing.purchaseCancelled.value)
    }

    @Test
    fun `restore with active purchase sets isPro to true`() = runTest {
        val billing = FakeBillingManager()
        billing.simulateRestore(hasPro = true)
        assertTrue(billing.isPro.value)
    }

    @Test
    fun `restore without active purchase keeps isPro false`() = runTest {
        val billing = FakeBillingManager()
        billing.simulateRestore(hasPro = false)
        assertFalse(billing.isPro.value)
    }

    @Test
    fun `queryProductDetails returns null when store unavailable`() = runTest {
        val billing = FakeBillingManager()
        billing.setProductDetails(null)
        var received: ProductDetails? = null
        billing.queryProductDetails { received = it }
        assertNull(received)
    }

    @Test
    fun `PRO_PRODUCT_ID is production value not test placeholder`() {
        assertFalse(
            "Product ID must not be a Google static test response ID",
            BillingClientWrapper.PRO_PRODUCT_ID.startsWith("android.test.")
        )
        assertEquals(
            "Product ID must match the registered Play Console in-app product",
            "frostalert_pro",
            BillingClientWrapper.PRO_PRODUCT_ID
        )
    }

    @Test
    fun `disconnect is called on cleanup`() = runTest {
        val billing = FakeBillingManager()
        billing.disconnect()
        assertTrue(billing.disconnectCalled)
    }

    @Test
    fun `restorePurchases triggers restore flow`() = runTest {
        val billing = FakeBillingManager()
        billing.restorePurchases()
        assertTrue(billing.restoreCalled)
    }

    @Test
    fun `purchase error then clearError then new purchase succeeds`() = runTest {
        val billing = FakeBillingManager()
        billing.simulatePurchaseError("Network error")
        assertEquals("Network error", billing.purchaseError.value)
        assertFalse(billing.isPro.value)

        billing.clearError()
        assertNull(billing.purchaseError.value)

        billing.simulatePurchaseSuccess()
        assertTrue(billing.isPro.value)
    }
}
