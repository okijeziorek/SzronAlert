package pl.oki.frostalert.data.local

object PlantSeeder {
    fun getDefaultPlants(): List<Plant> = listOf(
        // Warzywa
        Plant(name = "Pomidor", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Wrażliwy na mróz, wymaga ochrony", iconEmoji = "🍅"),
        Plant(name = "Ogórek", category = "Warzywa", frostThresholdCelsius = 3.0, description = "Bardzo wrażliwy na niskie temperatury", iconEmoji = "🥒"),
        Plant(name = "Papryka", category = "Warzywa", frostThresholdCelsius = 4.0, description = "Ciepłolubna, nie znosi przymrozków", iconEmoji = "🌶️"),
        Plant(name = "Dynia", category = "Warzywa", frostThresholdCelsius = 1.0, description = "Wrażliwa na przymrozki", iconEmoji = "🎃"),
        Plant(name = "Cukinia", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Ciepłolubna, wymaga ochrony", iconEmoji = "🥬"),
        Plant(name = "Fasola", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Nie toleruje mrozu", iconEmoji = "🫘"),
        Plant(name = "Marchew", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Odporna na lekkie przymrozki", iconEmoji = "🥕"),
        Plant(name = "Ziemniak", category = "Warzywa", frostThresholdCelsius = -1.0, description = "Część nadziemna wrażliwa na mróz", iconEmoji = "🥔"),
        Plant(name = "Sałata", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Toleruje lekkie przymrozki", iconEmoji = "🥬"),
        Plant(name = "Szpinak", category = "Warzywa", frostThresholdCelsius = -5.0, description = "Bardzo odporny na mróz", iconEmoji = "🥬"),

        // Owoce
        Plant(name = "Truskawka", category = "Owoce", frostThresholdCelsius = -1.0, description = "Kwiaty wrażliwe na przymrozki", iconEmoji = "🍓"),
        Plant(name = "Malina", category = "Owoce", frostThresholdCelsius = -2.0, description = "Umiarkowanie odporna", iconEmoji = "🫐"),
        Plant(name = "Jabłoń", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty wrażliwe na wiosenne przymrozki", iconEmoji = "🍎"),
        Plant(name = "Wiśnia", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty wrażliwe podczas kwitnienia", iconEmoji = "🍒"),
        Plant(name = "Borówka", category = "Owoce", frostThresholdCelsius = -3.0, description = "Dość odporna na mróz", iconEmoji = "🫐"),
        Plant(name = "Porzeczka", category = "Owoce", frostThresholdCelsius = -3.0, description = "Odporna, ale kwiaty wrażliwe", iconEmoji = "🍇"),

        // Kwiaty
        Plant(name = "Róża", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Odporna, ale młode pędy wrażliwe", iconEmoji = "🌹"),
        Plant(name = "Pelargonia", category = "Kwiaty", frostThresholdCelsius = 2.0, description = "Nie znosi mrozu", iconEmoji = "🌺"),
        Plant(name = "Dalia", category = "Kwiaty", frostThresholdCelsius = 0.0, description = "Bulwy wrażliwe na mróz", iconEmoji = "🌸"),
        Plant(name = "Begonia", category = "Kwiaty", frostThresholdCelsius = 3.0, description = "Ciepłolubna", iconEmoji = "🌺"),
        Plant(name = "Bratek", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Bardzo odporny na mróz", iconEmoji = "🌼"),
        Plant(name = "Lawenda", category = "Kwiaty", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna", iconEmoji = "💜"),
        Plant(name = "Chryzantema", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Odporna na jesienne przymrozki", iconEmoji = "🌻"),

        // Zioła
        Plant(name = "Bazylia", category = "Zioła", frostThresholdCelsius = 3.0, description = "Bardzo wrażliwa na chłód", iconEmoji = "🌿"),
        Plant(name = "Mięta", category = "Zioła", frostThresholdCelsius = -5.0, description = "Odporna na mróz", iconEmoji = "🌿"),
        Plant(name = "Rozmaryn", category = "Zioła", frostThresholdCelsius = -5.0, description = "Dość odporna", iconEmoji = "🌿"),
        Plant(name = "Tymianek", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna", iconEmoji = "🌿"),
        Plant(name = "Pietruszka", category = "Zioła", frostThresholdCelsius = -5.0, description = "Odporna na przymrozki", iconEmoji = "🌿"),
        Plant(name = "Koper", category = "Zioła", frostThresholdCelsius = -3.0, description = "Umiarkowanie odporny", iconEmoji = "🌿"),
        Plant(name = "Oregano", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna", iconEmoji = "🌿")
    )
}
