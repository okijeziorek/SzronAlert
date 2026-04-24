package pl.oki.frostalert.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.oki.frostalert.R
import pl.oki.frostalert.billing.ProductOffering
import pl.oki.frostalert.billing.SubscriptionState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card displaying active PRO subscription status and management options.
 */
@Composable
fun SubscriptionStatusCard(
    subscriptionState: SubscriptionState,
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPro) return

    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.subscription_active),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subscription Details
            if (subscriptionState.isActive) {
                // Product Name
                subscriptionState.productId?.let { productId ->
                    val offering = ProductOffering.fromProductId(productId)
                    offering?.let {
                        Text(
                            text = stringResource(it.titleResId),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Auto-renewal status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (subscriptionState.isAutoRenewing) Icons.Default.Refresh else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (subscriptionState.isAutoRenewing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = stringResource(
                            if (subscriptionState.isAutoRenewing) {
                                R.string.subscription_auto_renew_on
                            } else {
                                R.string.subscription_auto_renew_off
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Grace period indicator
                if (subscriptionState.isGracePeriod) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.subscription_grace_period),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Expiry date (if available)
                subscriptionState.expiryTimeMillis?.let { expiryMillis ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    val expiryDate = Date(expiryMillis)
                    Text(
                        text = stringResource(R.string.subscription_expires, dateFormat.format(expiryDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manage Subscription Button
                TextButton(
                    onClick = {
                        // Open Google Play subscription management
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/account/subscriptions")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.subscription_manage))
                }
            } else {
                // One-time purchase (Lifetime)
                Text(
                    text = stringResource(R.string.product_lifetime_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ " + stringResource(R.string.product_lifetime_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact PRO badge for displaying in lists/headers.
 */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
