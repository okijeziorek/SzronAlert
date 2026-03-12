package pl.oki.frostalert.utils

import pl.oki.frostalert.data.remote.WeatherResponse
import java.util.Locale

object SummerCalculations {

    /**
     * Sprawdza, czy występuje ryzyko burzy lub gradu na podstawie kodów pogodowych WMO.
     * Kody 95, 96, 99 oznaczają burze, przy czym 96 i 99 to burze z gradem.
     */
    fun hasStormOrHailRisk(weatherCode: Int): Boolean {
        return weatherCode in listOf(95, 96, 99)
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
    fun getUvDescription(uvIndex: Double): String {
        return when {
            uvIndex < 3 -> "Niskie"
            uvIndex < 6 -> "Umiarkowane"
            uvIndex < 8 -> "Wysokie"
            uvIndex < 11 -> "Bardzo wysokie"
            else -> "Ekstremalne"
        }
    }

    /**
     * Zwraca poradę dotyczącą bezpieczeństwa na słońcu.
     */
    fun getUvAdvice(uvIndex: Double): String {
        return when {
            uvIndex < 3 -> "Bezpiecznie. Nie są wymagane specjalne środki ostrożności."
            uvIndex < 6 -> "Użyj kremu z filtrem i załóż okulary przeciwsłoneczne."
            uvIndex < 8 -> "Ogranicz przebywanie na słońcu w godzinach 11-16. Szukaj cienia."
            else -> "Unikaj słońca! Ryzyko szybkiego oparzenia skóry i udaru."
        }
    }

    /**
     * Generuje komunikat ostrzegawczy dla trybu letniego.
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
            sb.append(if (weatherCode >= 96) "⚠️ UWAGA: Ryzyko BURZY Z GRADEM!\n" else "⛈️ Ryzyko BURZY!\n")
        }

        if (appMode == 0) { // Tryb Samochód
            if (hasHeatRisk(currentTemp, heatThreshold)) {
                sb.append("🔥 UPAŁ: Wnętrze auta szybko się nagrzewa. Nie zostawiaj dzieci ani zwierząt!\n")
            }
        }

        if (uvIndex >= 6) {
            sb.append("☀️ Wysoki indeks UV: ${String.format(Locale.US, "%.1f", uvIndex)} (${getUvDescription(uvIndex)})\n")
        }

        return sb.toString().trim()
    }
}
