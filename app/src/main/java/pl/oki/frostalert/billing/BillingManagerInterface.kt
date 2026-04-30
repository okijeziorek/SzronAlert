package pl.oki.frostalert.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

interface BillingManagerInterface {
    /** Emits `true` when the user has an active PRO purchase. */
    val isPro: StateFlow<Boolean>

    /** Emits the last billing error message, or `null` when there is no error. */
    val purchaseError: StateFlow<String?>

    /**
     * Emits `true` when the user explicitly cancels the Play purchase sheet
     * (`BillingResponseCode.USER_CANCELED`). The UI should observe this to reset
     * any "purchasing in progress" indicator. Reset to `false` by calling [clearCancellation].
     */
    val purchaseCancelled: StateFlow<Boolean>

    /** Queries Play Billing for PRO product details and delivers the result via [onDetailsReady]. */
    fun queryProductDetails(onDetailsReady: (ProductDetails?) -> Unit)

    /** Launches the Play Billing purchase sheet for the given [productDetails]. */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails)

    /** Restores existing purchases (e.g. after reinstall or device switch). */
    fun restorePurchases()

    /** Clears the last purchase error so the UI can dismiss any error message. */
    fun clearError()

    /** Clears the purchase-cancelled signal after the UI has handled it. */
    fun clearCancellation()

    /** Ends the billing client connection (call from lifecycle onDestroy). */
    fun disconnect()
}
