package pl.oki.frostalert.utils

import java.util.Calendar

object GardenSeasonalTips {

    data class MonthlyTip(
        val emoji: String,
        val tip: String
    )

    private val tips: Map<Int, List<MonthlyTip>> = mapOf(
        Calendar.JANUARY to listOf(
            MonthlyTip("❄️", "Sprawdź okrycia zimowe roślin po każdej odwilży."),
            MonthlyTip("📋", "Planuj ogród na nadchodzący sezon, zamów nasiona z katalogów."),
            MonthlyTip("✂️", "Przycinaj drzewa owocowe w czasie mrozów, gdy soki nie płyną.")
        ),
        Calendar.FEBRUARY to listOf(
            MonthlyTip("🌱", "Zacznij wysiew warzyw na rozsady w domu (pomidory, papryki)."),
            MonthlyTip("✂️", "Przycinaj krzewy ozdobne przed przebudzeniem."),
            MonthlyTip("🐛", "Sprawdź rośliny pod kątem szkodników zimujących pod korą.")
        ),
        Calendar.MARCH to listOf(
            MonthlyTip("🌷", "Zdejmuj okrycia zimowe gdy temperatury nie spadają poniżej -5°C."),
            MonthlyTip("🌱", "Wysiej warzywa wczesne: sałatę, szpinak, rzodkiewkę."),
            MonthlyTip("💧", "Wznów podlewanie, gdy ziemia odmrozi się w warstwach.")
        ),
        Calendar.APRIL to listOf(
            MonthlyTip("⚠️", "Uwaga na przymrozki! Maj to miesiąc 'zimnych ogrodników'."),
            MonthlyTip("🍅", "Przesadzaj rozsady do doniczek, nie wstawiaj na zewnątrz przed 15 maja."),
            MonthlyTip("🌿", "Siej zioła w gruncie: pietruszka, koper, kolendra.")
        ),
        Calendar.MAY to listOf(
            MonthlyTip("🌡️", "Po 15 maja sadzaj rozsady ciepłolubnych warzyw na zewnątrz."),
            MonthlyTip("💧", "Zacznij regularnie podlewać – deszcze wiosenne często są niewystarczające."),
            MonthlyTip("🌹", "Odwiązuj i rozwijaj okrycia róż i bylin.")
        ),
        Calendar.JUNE to listOf(
            MonthlyTip("☀️", "Mulchuj glebę, by zatrzymać wilgoć i hamować chwasty."),
            MonthlyTip("💧", "Podlewaj rano lub wieczorem, unikaj południa."),
            MonthlyTip("🍓", "Zbiory truskawek – nie zostawiaj owoców na krzakach za długo.")
        ),
        Calendar.JULY to listOf(
            MonthlyTip("🌡️", "Sprawdź indeks UV – ogranicz pracę w ogrodzie między 11 a 15."),
            MonthlyTip("💧", "Warzywa w skwarze wymagają codziennego podlewania, szczególnie pomidory."),
            MonthlyTip("🌿", "Zbieraj zioła rano, gdy olejki eteryczne są najbardziej intensywne.")
        ),
        Calendar.AUGUST to listOf(
            MonthlyTip("🥦", "Siej warzywa jesienne: brokuły, kalarepę, sałatę."),
            MonthlyTip("🍎", "Zbiory wcześniejszych odmian jabłek i śliwek."),
            MonthlyTip("💧", "Ogranicz podlewanie pod koniec miesiąca, by zahartować rośliny.")
        ),
        Calendar.SEPTEMBER to listOf(
            MonthlyTip("🌾", "Zbiory dyni i cukinii przed pierwszymi przymrozkami."),
            MonthlyTip("🧅", "Sadzaj cebule tulipanów, narcyzów i hiacyntów."),
            MonthlyTip("❄️", "Przygotuj agrowłókninę – przymrozki mogą pojawić się już w tym miesiącu.")
        ),
        Calendar.OCTOBER to listOf(
            MonthlyTip("🌹", "Okrywaj róże i byliny wrażliwe na mróz."),
            MonthlyTip("🥕", "Zbieraj korzeniowe: marchew, pietruszkę, pasternak – mróz je dosładza."),
            MonthlyTip("🍂", "Kompostuj liście – doskonały nawóz na przyszły rok.")
        ),
        Calendar.NOVEMBER to listOf(
            MonthlyTip("❄️", "Zabezpieczaj rośliny przed mrozem: agrowłóknina, słoma, igliwie."),
            MonthlyTip("🪴", "Przenieś doniczkowe rośliny wrażliwe do chłodnego pomieszczenia."),
            MonthlyTip("📋", "Notuj obserwacje z kończącego się sezonu, by planować lepiej.")
        ),
        Calendar.DECEMBER to listOf(
            MonthlyTip("🌟", "Sprawdzaj okrycia po intensywnych opadach śniegu."),
            MonthlyTip("📚", "Czas na lektury ogrodnicze i planowanie ogrodu na nowy rok."),
            MonthlyTip("🐦", "Pamiętaj o ptakach w ogrodzie – zawieś karmnik.")
        )
    )

    fun getCurrentMonthTips(): List<MonthlyTip> {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return tips[month] ?: emptyList()
    }

    fun getMonthName(): String {
        return when (Calendar.getInstance().get(Calendar.MONTH)) {
            Calendar.JANUARY -> "Styczeń"
            Calendar.FEBRUARY -> "Luty"
            Calendar.MARCH -> "Marzec"
            Calendar.APRIL -> "Kwiecień"
            Calendar.MAY -> "Maj"
            Calendar.JUNE -> "Czerwiec"
            Calendar.JULY -> "Lipiec"
            Calendar.AUGUST -> "Sierpień"
            Calendar.SEPTEMBER -> "Wrzesień"
            Calendar.OCTOBER -> "Październik"
            Calendar.NOVEMBER -> "Listopad"
            Calendar.DECEMBER -> "Grudzień"
            else -> ""
        }
    }
}
