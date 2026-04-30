package pl.oki.frostalert.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.oki.frostalert.security.RootDetector
import pl.oki.frostalert.security.SecurityManager
import pl.oki.frostalert.utils.AppTelemetry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val securityManager: SecurityManager
) : BillingManagerInterface, PurchasesUpdatedListener {

    companion object {
        /** Legacy production in-app product ID (backwards compatibility). */
        const val PRO_PRODUCT_ID = "frostalert_pro"

        /** All supported product IDs for PRO features. */
        val ALL_PRO_PRODUCT_IDS = listOf(
            ProductOffering.Lifetime.productId,
            ProductOffering.Monthly.productId,
            ProductOffering.Yearly.productId,
            PRO_PRODUCT_ID  // Legacy support
        )

        private const val MAX_RETRY_ATTEMPTS = 3
        private const val TAG = "BillingClientWrapper"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPro = MutableStateFlow(false)
    override val isPro = _isPro.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    override val purchaseError = _purchaseError.asStateFlow()

    private val _purchaseCancelled = MutableStateFlow(false)
    override val purchaseCancelled = _purchaseCancelled.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductInfo>>(emptyList())
    val availableProducts = _availableProducts.asStateFlow()

    private val _subscriptionState = MutableStateFlow(SubscriptionState.inactive())
    val subscriptionState = _subscriptionState.asStateFlow()

    // Separate caches for each product type so two concurrent callbacks
    // don't race on a shared mutable list.
    @Volatile private var cachedInAppProducts: List<ProductInfo> = emptyList()
    @Volatile private var cachedSubsProducts: List<ProductInfo> = emptyList()

    private var retryCount = 0

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectWithRetry()
        queryAllProductDetails()
    }

    private fun connectWithRetry() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                retryCount = 0
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAllPurchases()
                    queryAllProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    retryCount++
                    connectWithRetry()
                }
            }
        })
    }

    override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        // Security: Check for root before allowing purchases
        if (RootDetector.isRooted(appContext)) {
            _purchaseError.value = "Zakupy niedostępne na urządzeniach z rootem"
            AppTelemetry.recordSecurityEvent(
                appContext,
                AppTelemetry.SecurityEvent(
                    type = "root_detected",
                    severity = "high",
                    details = "Purchase attempt blocked - rooted device"
                )
            )
            return
        }

        // Security: Perform integrity check before purchase
        scope.launch {
            try {
                val integrityResult = securityManager.checkAppIntegrity()

                if (!integrityResult.isUnmodified) {
                    _purchaseError.value = "Aplikacja została zmodyfikowana. Zakup niemożliwy."
                    AppTelemetry.recordSecurityEvent(
                        appContext,
                        AppTelemetry.SecurityEvent(
                            type = "integrity_failed",
                            severity = "critical",
                            details = "Purchase blocked - app modified: ${integrityResult.verdict}"
                        )
                    )
                    return@launch
                }

                if (!integrityResult.isGenuine) {
                    _purchaseError.value = "Wykryto nieautoryzowane urządzenie."
                    AppTelemetry.recordSecurityEvent(
                        appContext,
                        AppTelemetry.SecurityEvent(
                            type = "integrity_failed",
                            severity = "high",
                            details = "Purchase blocked - non-genuine device: ${integrityResult.verdict}"
                        )
                    )
                    return@launch
                }

                // Proceed with purchase flow
                // Find offer with a free trial phase; fall back to the first available offer
                val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                val offerToken = subscriptionOfferDetails
                    ?.firstOrNull { offer ->
                        offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                    }?.offerToken
                    ?.takeIf { it.isNotBlank() }
                    ?: subscriptionOfferDetails?.firstOrNull()?.offerToken?.takeIf { it.isNotBlank() }

                if (subscriptionOfferDetails != null && offerToken == null) {
                    _purchaseError.value = "Nie udało się pobrać oferty subskrypcji. Spróbuj ponownie później."
                    return@launch
                }

                val productDetailsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                if (subscriptionOfferDetails != null && offerToken != null) {
                    productDetailsBuilder.setOfferToken(offerToken)
                }
                val productDetailsParamsList = listOf(productDetailsBuilder.build())
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)

            } catch (e: Exception) {
                Log.e(TAG, "Security check failed", e)
                _purchaseError.value = "Błąd weryfikacji bezpieczeństwa"
            }
        }
    }

    override fun queryProductDetails(onDetailsReady: (ProductDetails?) -> Unit) {
        // Query for legacy product (backwards compatibility)
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onDetailsReady(productDetailsList.firstOrNull())
            } else {
                onDetailsReady(null)
            }
        }
    }

    /**
     * Queries all available PRO product offerings (subscriptions + one-time purchases).
     *
     * Each product type is queried independently. Results are stored in separate
     * [cachedInAppProducts] / [cachedSubsProducts] fields so that concurrent callbacks
     * from Play Billing never race on a shared mutable collection.
     */
    private fun queryAllProductDetails() {
        if (!billingClient.isReady) return

        scope.launch {
            // Query INAPP products (one-time purchases)
            val inAppQueryProducts = ProductOffering.allOfferings()
                .filter { it.productType == BillingClient.ProductType.INAPP }
                .map { offering ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(offering.productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }

            if (inAppQueryProducts.isNotEmpty()) {
                val inAppParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(inAppQueryProducts)
                    .build()

                billingClient.queryProductDetailsAsync(inAppParams) { result, detailsList ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cachedInAppProducts = detailsList.mapNotNull { details ->
                            val offering = ProductOffering.fromProductId(details.productId) ?: return@mapNotNull null
                            val oneTimeOffer = details.oneTimePurchaseOfferDetails ?: return@mapNotNull null
                            ProductInfo(
                                offering = offering,
                                productDetails = details,
                                priceFormatted = oneTimeOffer.formattedPrice,
                                priceAmountMicros = oneTimeOffer.priceAmountMicros,
                                priceCurrencyCode = oneTimeOffer.priceCurrencyCode
                            )
                        }
                        _availableProducts.value = cachedInAppProducts + cachedSubsProducts
                    }
                }
            }

            // Query SUBS products (subscriptions)
            val subsQueryProducts = ProductOffering.allOfferings()
                .filter { it.productType == BillingClient.ProductType.SUBS }
                .map { offering ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(offering.productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }

            if (subsQueryProducts.isNotEmpty()) {
                val subsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(subsQueryProducts)
                    .build()

                billingClient.queryProductDetailsAsync(subsParams) { result, detailsList ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cachedSubsProducts = detailsList.mapNotNull { details ->
                            val offering = ProductOffering.fromProductId(details.productId) ?: return@mapNotNull null
                            // Use the first available subscription offer returned by BillingClient.
                            val subscriptionOffer = details.subscriptionOfferDetails?.firstOrNull() ?: return@mapNotNull null
                            val pricingPhase = subscriptionOffer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
                            ProductInfo(
                                offering = offering,
                                productDetails = details,
                                priceFormatted = pricingPhase.formattedPrice,
                                priceAmountMicros = pricingPhase.priceAmountMicros,
                                priceCurrencyCode = pricingPhase.priceCurrencyCode
                            )
                        }
                        _availableProducts.value = cachedInAppProducts + cachedSubsProducts
                    }
                }
            }
        }
    }

    /**
     * Queries all purchases (both INAPP and SUBS) and updates PRO status.
     *
     * Unacknowledged purchases that are in PURCHASED state are re-acknowledged here so
     * that PRO access is not lost if the initial acknowledgment failed (e.g. due to a
     * transient network error at purchase time).
     */
    private fun queryAllPurchases() {
        var hasProFromInApp = false
        var hasProFromSubs = false

        // Query INAPP purchases
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(inAppParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Retry acknowledgment for any purchase that completed but was not yet acked.
                purchases.forEach { purchase ->
                    if (ALL_PRO_PRODUCT_IDS.contains(purchase.products.firstOrNull()) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !purchase.isAcknowledged
                    ) {
                        acknowledgePurchase(purchase)
                    }
                }
                hasProFromInApp = purchases.any { purchase ->
                    ALL_PRO_PRODUCT_IDS.contains(purchase.products.firstOrNull()) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.isAcknowledged
                }
                _isPro.value = hasProFromInApp || hasProFromSubs
                if (hasProFromInApp) {
                    AppTelemetry.recordBillingRestore(appContext)
                }
            }
        }

        // Query SUBS purchases
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(subsParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Retry acknowledgment for any subscription that was not yet acked.
                purchases.forEach { purchase ->
                    if (ALL_PRO_PRODUCT_IDS.contains(purchase.products.firstOrNull()) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !purchase.isAcknowledged
                    ) {
                        acknowledgePurchase(purchase)
                    }
                }

                val activeSub = purchases.firstOrNull { purchase ->
                    ALL_PRO_PRODUCT_IDS.contains(purchase.products.firstOrNull()) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (activeSub != null) {
                    hasProFromSubs = true
                    _subscriptionState.value = SubscriptionState(
                        isActive = true,
                        productId = activeSub.products.firstOrNull(),
                        purchaseToken = activeSub.purchaseToken,
                        expiryTimeMillis = null, // Would need server-side verification for exact expiry
                        isAutoRenewing = activeSub.isAutoRenewing,
                        isGracePeriod = false
                    )
                    AppTelemetry.recordBillingRestore(appContext)
                } else {
                    _subscriptionState.value = SubscriptionState.inactive()
                }

                _isPro.value = hasProFromInApp || hasProFromSubs
            }
        }
    }

    private fun queryPurchases() {
        // Legacy method for backward compatibility
        queryAllPurchases()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User dismissed the purchase sheet — signal the UI to clear its "purchasing" state.
                _purchaseCancelled.value = true
            }
            else -> {
                _purchaseError.value = billingResult.debugMessage
                AppTelemetry.recordBillingError(appContext)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPro.value = true
                    // Refresh full purchase state so subscriptionState is also updated.
                    queryAllPurchases()
                    AppTelemetry.recordBillingPurchase(appContext)
                } else {
                    _purchaseError.value = billingResult.debugMessage
                    AppTelemetry.recordBillingError(appContext)
                }
            }
        }
    }

    override fun clearError() {
        _purchaseError.value = null
    }

    override fun clearCancellation() {
        _purchaseCancelled.value = false
    }

    override fun restorePurchases() {
        if (!billingClient.isReady) {
            connectWithRetry()
            return
        }
        queryAllPurchases()
    }

    override fun disconnect() {
        billingClient.endConnection()
    }
}

