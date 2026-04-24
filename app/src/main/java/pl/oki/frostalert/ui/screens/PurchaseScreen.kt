package pl.oki.frostalert.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.oki.frostalert.R
import pl.oki.frostalert.billing.ProductInfo
import pl.oki.frostalert.billing.ProductOffering

/**
 * Purchase screen displaying all PRO plan options with feature comparison.
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
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PRO Features Section
            item {
                ProFeaturesCard()
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Product Cards
            items(availableProducts.sortedBy { getProductPriority(it.offering) }) { product ->
                ProductCard(
                    productInfo = product,
                    isRecommended = false,
                    isBestValue = product.offering is ProductOffering.Yearly,
                    onClick = {
                        if (activity != null && !isPurchasing) {
                            onProductSelected(product)
                        }
                    },
                    enabled = !isPurchasing
                )
            }

            // Restore Purchases Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ProductCard(
    productInfo: ProductInfo,
    isRecommended: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val borderColor = if (isRecommended) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge
            if (isBestValue || isRecommended) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (isBestValue) {
                            stringResource(R.string.purchase_best_value)
                        } else {
                            stringResource(R.string.purchase_most_popular)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Product Name
            Text(
                text = stringResource(productInfo.offering.titleResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = stringResource(productInfo.offering.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Button(
                    onClick = onClick,
                    enabled = enabled
                ) {
                    Text(stringResource(R.string.purchase_button))
                }
            }
        }
    }
}

/**
 * Returns display priority (lower = higher priority).
 */
private fun getProductPriority(offering: ProductOffering): Int {
    return when (offering) {
        is ProductOffering.Yearly -> 1    // Show first (most popular)
        is ProductOffering.Lifetime -> 2  // Show second
        is ProductOffering.Monthly -> 3   // Show third
    }
}
