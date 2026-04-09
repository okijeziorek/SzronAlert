package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.StringRes
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import pl.oki.frostalert.R

@Composable
fun MonetizationBanner(
    modifier: Modifier = Modifier,
    @StringRes adUnitResId: Int = R.string.admob_banner_unit_id
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val containerWidthPx = LocalWindowInfo.current.containerSize.width
    val adWidthDp = remember(containerWidthPx, density) {
        with(density) { containerWidthPx.toDp() }.value.toInt().coerceAtLeast(320)
    }
    val adSize = remember(adWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }
    val adUnitId = context.getString(adUnitResId)
    val adHeightDp = remember(adSize, density) {
        with(density) { adSize.getHeightInPixels(context).toDp() }
    }

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp),
        factory = { adView }
    )
}


