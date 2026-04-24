package pl.oki.frostalert.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.oki.frostalert.BuildConfig
import pl.oki.frostalert.R
import pl.oki.frostalert.billing.ProductInfo
import pl.oki.frostalert.billing.ProductOffering

/**
 * Purchase screen displaying all PRO plan options with feature comparison.
 * Always shows all three tiers (Monthly, Yearly, Lifetime).
 * When [availableProducts] haven't loaded from Play Store yet, purchase buttons
 * are disabled and a loading indicator is shown instead of the price.
 */
@Composable
fun PurchaseScreen(
    availableProducts: List<ProductInfo>,
    isPurchasing: Boolean,
    onProductSelected: (ProductInfo) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val productsLoaded = availableProducts.isNotEmpty()

    // Map of productId -> ProductInfo for quick lookup
    val productMap = remember(availableProducts) {
        availableProducts.associateBy { it.offering.productId }
    }

    // Display order defined in ProductOffering
    val allOfferings = remember { listOf(
        ProductOffering.Yearly,
        ProductOffering.Lifetime,
        ProductOffering.Monthly
    ) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.purchase_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(android.R.string.cancel)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Debug banner – shown only when products haven't loaded in debug builds
            if (BuildConfig.DEBUG && !productsLoaded) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.purchase_debug_no_products),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // PRO Features Section
            item {
                ProFeaturesCard()
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // One card per offering – always show all three
            items(
                count = allOfferings.size,
                key = { allOfferings[it].productId }
            ) { index ->
                val offering = allOfferings[index]
                val productInfo = productMap[offering.productId]
                OfferingCard(
                    offering = offering,
                    productInfo = productInfo,
                    isBestValue = offering is ProductOffering.Yearly,
                    isPurchasing = isPurchasing,
                    onClick = {
                        if (productInfo != null && activity != null && !isPurchasing) {
                            onProductSelected(productInfo)
                        }
                    }
                )
            }

            // Restore Purchases Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPurchasing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.purchase_restore))
                }
            }
        }

        // Loading indicator
        if (isPurchasing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ProFeaturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.pro_features_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProFeatureItem(stringResource(R.string.pro_feature_no_ads))
            ProFeatureItem(stringResource(R.string.pro_feature_frost_map))
            ProFeatureItem(stringResource(R.string.pro_feature_extended_forecast))
            ProFeatureItem(stringResource(R.string.pro_feature_multi_location))
            ProFeatureItem(stringResource(R.string.pro_feature_pdf_reports))
            ProFeatureItem(stringResource(R.string.pro_feature_priority_support))
        }
    }
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Card for a single offering. Shows real price when [productInfo] is available,
 * or a loading placeholder otherwise.
 */
@Composable
private fun OfferingCard(
    offering: ProductOffering,
    productInfo: ProductInfo?,
    isBestValue: Boolean,
    isPurchasing: Boolean,
    onClick: () -> Unit
) {
    val isAvailable = productInfo != null
    val borderColor = if (isBestValue && isAvailable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (isAvailable && !isPurchasing) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isBestValue && isAvailable) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge
            if (isBestValue) {
                Surface(
                    color = if (isAvailable) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.purchase_best_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAvailable) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Product Name
            Text(
                text = stringResource(offering.titleResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = stringResource(offering.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAvailable) {
                    Column {
                        Text(
                            text = productInfo.priceFormatted,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        productInfo.monthlyPriceFormatted?.let { monthly ->
                            Text(
                                text = monthly,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = onClick, enabled = !isPurchasing) {
                        Text(stringResource(R.string.purchase_button))
                    }
                } else {
                    // Loading placeholder
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.purchase_loading_price),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = {}, enabled = false) {
                        Text(stringResource(R.string.purchase_button))
                    }
                }
            }
        }
    }
}
