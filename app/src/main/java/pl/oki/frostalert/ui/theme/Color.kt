package pl.oki.frostalert.ui.theme

import androidx.compose.ui.graphics.Color

// --- Frost Brand Colors: icy-blue primary palette ---
// Light variants (used in dark theme)
val FrostIcyBlue80 = Color(0xFFB3E5FC)
val FrostCoolGrey80 = Color(0xFFB0BEC5)
val FrostMint80 = Color(0xFFB2DFDB)

// Dark variants (used in light theme)
val FrostIcyBlue40 = Color(0xFF0277BD)
val FrostCoolGrey40 = Color(0xFF546E7A)
val FrostMint40 = Color(0xFF00796B)

// --- Risk-level Semantic Colors (used across the app) ---
/** Green — no frost risk. */
val RiskNone = Color(0xFF43A047)
/** Light green — low risk. */
val RiskLow = Color(0xFF8BC34A)
/** Amber — moderate risk. */
val RiskModerate = Color(0xFFFFA726)
/** Red — high risk. */
val RiskHigh = Color(0xFFF44336)
/** Deep red — very high risk. */
val RiskVeryHigh = Color(0xFFB71C1C)

// Legacy colors retained for backward-compatibility (no longer used in theme fallbacks)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
