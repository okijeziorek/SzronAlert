package pl.oki.frostalert.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

/**
 * Represents a product offering with its type and metadata.
 */
sealed class ProductOffering(
    val productId: String,
    val productType: String,
    val titleResId: Int,
    val descriptionResId: Int
) {
    /**
     * One-time lifetime PRO purchase.
     * Optimal for users who want permanent access without recurring charges.
     */
    data object Lifetime : ProductOffering(
        productId = "frostalert_pro_lifetime",
        productType = BillingClient.ProductType.INAPP,
        titleResId = pl.oki.frostalert.R.string.product_lifetime_title,
        descriptionResId = pl.oki.frostalert.R.string.product_lifetime_desc
    )

    /**
     * Monthly subscription (auto-renewing).
     * Optimal for users who want to try PRO features with lower initial commitment.
     */
    data object Monthly : ProductOffering(
        productId = "frostalert_pro_monthly",
        productType = BillingClient.ProductType.SUBS,
        titleResId = pl.oki.frostalert.R.string.product_monthly_title,
        descriptionResId = pl.oki.frostalert.R.string.product_monthly_desc
    )

    /**
     * Yearly subscription (auto-renewing).
     * Optimal price-per-month for committed users. Best value proposition.
     */
    data object Yearly : ProductOffering(
        productId = "frostalert_pro_yearly",
        productType = BillingClient.ProductType.SUBS,
        titleResId = pl.oki.frostalert.R.string.product_yearly_title,
        descriptionResId = pl.oki.frostalert.R.string.product_yearly_desc
    )

    companion object {
        /**
         * All available product offerings in display order (most popular first).
         */
        fun allOfferings(): List<ProductOffering> = listOf(Yearly, Lifetime, Monthly)

        /**
         * Returns the ProductOffering for a given product ID, or null if not found.
         */
        fun fromProductId(productId: String): ProductOffering? {
            return when (productId) {
                Lifetime.productId -> Lifetime
                Monthly.productId -> Monthly
                Yearly.productId -> Yearly
                "frostalert_pro" -> Lifetime // Legacy product ID mapping
                else -> null
            }
        }
    }
}

/**
 * Represents a product with its Play Store details and pricing.
 */
data class ProductInfo(
    val offering: ProductOffering,
    val productDetails: ProductDetails,
    val priceFormatted: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String
) {
    /**
     * For subscriptions, returns the monthly equivalent price for comparison.
     * For one-time purchases, returns null.
     */
    val monthlyEquivalentMicros: Long? = when (offering) {
        is ProductOffering.Monthly -> priceAmountMicros
        is ProductOffering.Yearly -> priceAmountMicros / 12
        is ProductOffering.Lifetime -> null
    }

    /**
     * User-friendly display of monthly cost (for subscriptions only).
     */
    val monthlyPriceFormatted: String? = monthlyEquivalentMicros?.let {
        val monthlyPrice = it / 1_000_000.0
        String.format("%.2f %s/miesiąc", monthlyPrice, priceCurrencyCode)
    }
}

/**
 * Subscription state for managing active subscriptions.
 */
data class SubscriptionState(
    val isActive: Boolean,
    val productId: String?,
    val purchaseToken: String?,
    val expiryTimeMillis: Long?,
    val isAutoRenewing: Boolean,
    val isGracePeriod: Boolean = false
) {
    companion object {
        fun inactive() = SubscriptionState(
            isActive = false,
            productId = null,
            purchaseToken = null,
            expiryTimeMillis = null,
            isAutoRenewing = false
        )
    }
}
