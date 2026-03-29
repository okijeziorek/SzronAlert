package pl.oki.frostalert.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

interface BillingManagerInterface {
    /** Emits `true` when the user has an active PRO purchase. */
    val isPro: StateFlow<Boolean>

    /** Emits the last billing error message, or `null` when there is no error. */
    val purchaseError: StateFlow<String?>

    /** Queries Play Billing for PRO product details and delivers the result via [onDetailsReady]. */
    fun queryProductDetails(onDetailsReady: (ProductDetails?) -> Unit)

    /** Launches the Play Billing purchase sheet for the given [productDetails]. */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails)

    /** Clears the last purchase error so the UI can dismiss any error message. */
    fun clearError()

    /** Ends the billing client connection (call from lifecycle onDestroy). */
    fun disconnect()
}
