package pl.oki.frostalert.utils

import android.content.Context
import pl.oki.frostalert.R
import java.util.Locale

object SummerCalculations {

    /**
     * Sprawdza, czy występuje ryzyko burzy lub gradu na podstawie kodów pogodowych WMO.
     * Kody 95, 96, 99 oznaczają burze, przy czym 96 i 99 to burze z gradem.
     */
    fun hasStormOrHailRisk(weatherCode: Int): Boolean {
        return weatherCode == 95 || weatherCode == 96 || weatherCode == 99
    }

    /**
     * Sprawdza, czy występuje ryzyko upału w samochodzie.
     */
    fun hasHeatRisk(currentTemp: Double, heatThreshold: Double): Boolean {
        return currentTemp >= heatThreshold
    }

    /**
     * Oblicza ryzyko suszy i potrzebę podlewania dla ogrodu.
     * Logika: Jeśli suma opadów z dzisiaj jest niska (< 2mm) i temperatura jutro ma być wysoka (> 25°C).
     */
    fun needsWatering(dailyPrecipitationSum: Double, tomorrowMaxTemp: Double): Boolean {
        return dailyPrecipitationSum < 2.0 && tomorrowMaxTemp > 25.0
    }

    /**
     * Zwraca opis poziomu zagrożenia promieniowaniem UV.
     */
    fun getUvDescription(context: Context, uvIndex: Double): String {
        return when {
            uvIndex < 3 -> context.getString(R.string.uv_description_low)
            uvIndex < 6 -> context.getString(R.string.uv_description_moderate)
            uvIndex < 8 -> context.getString(R.string.uv_description_high)
            uvIndex < 11 -> context.getString(R.string.uv_description_very_high)
            else -> context.getString(R.string.uv_description_extreme)
        }
    }

    /**
     * Zwraca poradę dotyczącą bezpieczeństwa na słońcu.
     */
    fun getUvAdvice(context: Context, uvIndex: Double): String {
        return when {
            uvIndex < 3 -> context.getString(R.string.uv_advice_low)
            uvIndex < 6 -> context.getString(R.string.uv_advice_moderate)
            uvIndex < 8 -> context.getString(R.string.uv_advice_high)
            else -> context.getString(R.string.uv_advice_extreme)
        }
    }

    /**
     * Generuje komunikat ostrzegawczy dla trybu letniego (bez kontekstu – hardcoded).
     * Używany w testach jednostkowych i kodzie niewymagającym lokalizacji.
     */
    fun getSummerWarningMessage(
        currentTemp: Double,
        weatherCode: Int,
        uvIndex: Double,
        heatThreshold: Double,
        appMode: Int // 0: Car, 1: Garden
    ): String {
        val sb = StringBuilder()

        if (hasStormOrHailRisk(weatherCode)) {
            sb.append(if (weatherCode >= 96) "⛈️ GRAD! Schroń pojazd." else "⛈️ BURZA! Zachowaj ostrożność.")
        }

        if (appMode == 0) { // Tryb Samochód
            if (hasHeatRisk(currentTemp, heatThreshold)) {
                sb.append("🌡️ UPAŁ! Samochód nagrzewa się niebezpiecznie.")
            }
        }

        if (uvIndex >= 6) {
            val formattedUv = String.format(Locale.US, "%.1f", uvIndex)
            sb.append("☀️ Wysokie UV: $formattedUv. Chroń skórę.")
        }

        return sb.toString().trim()
    }

    /**
     * Generuje komunikat ostrzegawczy dla trybu letniego.
     */
    fun getSummerWarningMessage(
        context: Context,
        currentTemp: Double,
        weatherCode: Int,
        uvIndex: Double,
        heatThreshold: Double,
        appMode: Int // 0: Car, 1: Garden
    ): String {
        val sb = StringBuilder()

        if (hasStormOrHailRisk(weatherCode)) {
            sb.append(if (weatherCode >= 96)
                context.getString(R.string.summer_warning_hail)
            else
                context.getString(R.string.summer_warning_storm))
        }

        if (appMode == 0) { // Tryb Samochód
            if (hasHeatRisk(currentTemp, heatThreshold)) {
                sb.append(context.getString(R.string.summer_warning_heat_car))
            }
        }

        if (uvIndex >= 6) {
            val formattedUv = String.format(Locale.US, "%.1f", uvIndex)
            val uvDesc = getUvDescription(context, uvIndex)
            sb.append(context.getString(R.string.summer_warning_uv_high, formattedUv, uvDesc))
        }

        return sb.toString().trim()
    }
}
