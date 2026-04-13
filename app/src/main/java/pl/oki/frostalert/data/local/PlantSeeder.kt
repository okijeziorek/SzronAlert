package pl.oki.frostalert.data.local

object PlantSeeder {
    fun getDefaultPlants(): List<Plant> = listOf(
        // Warzywa
        Plant(name = "Pomidor", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Wrażliwy na mróz, wymaga ochrony. Podlewaj regularnie co 2-3 dni.", iconEmoji = "🍅"),
        Plant(name = "Ogórek", category = "Warzywa", frostThresholdCelsius = 3.0, description = "Bardzo wrażliwy na niskie temperatury. Lubi ciepłą i wilgotną ziemię.", iconEmoji = "🥒"),
        Plant(name = "Papryka", category = "Warzywa", frostThresholdCelsius = 4.0, description = "Ciepłolubna, nie znosi przymrozków. Wymaga stanowiska słonecznego.", iconEmoji = "🌶️"),
        Plant(name = "Dynia", category = "Warzywa", frostThresholdCelsius = 1.0, description = "Wrażliwa na przymrozki. Potrzebuje dużo przestrzeni i słońca.", iconEmoji = "🎃"),
        Plant(name = "Cukinia", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Ciepłolubna, wymaga ochrony. Obficie owocuje przy regularnym podlewaniu.", iconEmoji = "🥬"),
        Plant(name = "Fasola", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Nie toleruje mrozu. Sadzaj po ostatnich przymrozkach.", iconEmoji = "🫘"),
        Plant(name = "Marchew", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Odporna na lekkie przymrozki – mróz nawet dosładza korzenie.", iconEmoji = "🥕"),
        Plant(name = "Ziemniak", category = "Warzywa", frostThresholdCelsius = -1.0, description = "Część nadziemna wrażliwa na mróz. Obsypuj ziemią przy wzroście.", iconEmoji = "🥔"),
        Plant(name = "Sałata", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Toleruje lekkie przymrozki. Idealna do uprawy wiosennej i jesiennej.", iconEmoji = "🥬"),
        Plant(name = "Szpinak", category = "Warzywa", frostThresholdCelsius = -5.0, description = "Bardzo odporny na mróz. Można siać jesienią pod osłony.", iconEmoji = "🥬"),
        Plant(name = "Brokuł", category = "Warzywa", frostThresholdCelsius = -4.0, description = "Odporny na przymrozki. Lubi chłodniejszą pogodę.", iconEmoji = "🥦"),
        Plant(name = "Kalafior", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Umiarkowanie odporny. Wymaga przykrycia główki liśćmi.", iconEmoji = "🥦"),
        Plant(name = "Kapusta", category = "Warzywa", frostThresholdCelsius = -5.0, description = "Bardzo odporna. Mróz poprawia smak kapusty kwaszonej.", iconEmoji = "🥬"),
        Plant(name = "Por", category = "Warzywa", frostThresholdCelsius = -8.0, description = "Mocno mrozoodporny, można zbierać zimą.", iconEmoji = "🧅"),
        Plant(name = "Cebula", category = "Warzywa", frostThresholdCelsius = -3.0, description = "Odporna na przymrozki. Sadzonki znoszą lekki mróz.", iconEmoji = "🧅"),
        Plant(name = "Czosnek", category = "Warzywa", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporny. Sadzi się jesienią, zbiera latem.", iconEmoji = "🧄"),
        Plant(name = "Groch", category = "Warzywa", frostThresholdCelsius = -3.0, description = "Odporny na wiosenne przymrozki. Siej wcześnie.", iconEmoji = "🟢"),
        Plant(name = "Burak", category = "Warzywa", frostThresholdCelsius = -2.0, description = "Umiarkowanie odporny. Korzenie wytrzymują lekki mróz.", iconEmoji = "🟣"),
        Plant(name = "Seler", category = "Warzywa", frostThresholdCelsius = -1.0, description = "Wrażliwy na przymrozki po posadzeniu. Wymaga długiego sezonu.", iconEmoji = "🌿"),
        Plant(name = "Pomidor koktajlowy", category = "Warzywa", frostThresholdCelsius = 2.0, description = "Tak samo wrażliwy jak zwykły pomidor. Doskonały do balkonów.", iconEmoji = "🍅"),

        // Owoce
        Plant(name = "Truskawka", category = "Owoce", frostThresholdCelsius = -1.0, description = "Kwiaty wrażliwe na przymrozki. Okrywaj słomą na zimę.", iconEmoji = "🍓"),
        Plant(name = "Malina", category = "Owoce", frostThresholdCelsius = -2.0, description = "Umiarkowanie odporna. Przycinaj pędy po owocowaniu.", iconEmoji = "🫐"),
        Plant(name = "Jabłoń", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty wrażliwe na wiosenne przymrozki. Drzewo długowieczne.", iconEmoji = "🍎"),
        Plant(name = "Grusza", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty delikatniejsze niż jabłoni, bardziej wrażliwa.", iconEmoji = "🍐"),
        Plant(name = "Śliwa", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty kwitną wcześnie – narażone na przymrozki.", iconEmoji = "🫐"),
        Plant(name = "Wiśnia", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwiaty wrażliwe podczas kwitnienia. Owoc bogaty w antyoksydanty.", iconEmoji = "🍒"),
        Plant(name = "Czereśnia", category = "Owoce", frostThresholdCelsius = -2.0, description = "Kwitnie wcześnie, narażona na przymrozki.", iconEmoji = "🍒"),
        Plant(name = "Borówka", category = "Owoce", frostThresholdCelsius = -3.0, description = "Dość odporna na mróz. Wymaga kwaśnej gleby (pH 4–5).", iconEmoji = "🫐"),
        Plant(name = "Porzeczka", category = "Owoce", frostThresholdCelsius = -3.0, description = "Odporna, ale kwiaty wrażliwe na wiosenne przymrozki.", iconEmoji = "🍇"),
        Plant(name = "Agrest", category = "Owoce", frostThresholdCelsius = -3.0, description = "Bardzo odporny krzew. Toleruje cień i mróz.", iconEmoji = "🍇"),
        Plant(name = "Winorośl", category = "Owoce", frostThresholdCelsius = -1.0, description = "Wrażliwa na przymrozki wiosną. Sadzaj przy murze.", iconEmoji = "🍇"),
        Plant(name = "Aronia", category = "Owoce", frostThresholdCelsius = -5.0, description = "Bardzo mrozoodporna, łatwa w uprawie.", iconEmoji = "🫐"),
        Plant(name = "Leszczyna", category = "Owoce", frostThresholdCelsius = -5.0, description = "Odporna na mróz. Kwitnie zimą, ale kwiaty przeżywają mróz.", iconEmoji = "🌰"),

        // Kwiaty
        Plant(name = "Róża", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Odporna, ale młode pędy wrażliwe. Okrywaj kopczykiem ziemi.", iconEmoji = "🌹"),
        Plant(name = "Pelargonia", category = "Kwiaty", frostThresholdCelsius = 2.0, description = "Nie znosi mrozu. Przenoś do środka przed jesienią.", iconEmoji = "🌺"),
        Plant(name = "Dalia", category = "Kwiaty", frostThresholdCelsius = 0.0, description = "Bulwy wrażliwe na mróz. Wykopuj na zimę i przechowuj w piwnicy.", iconEmoji = "🌸"),
        Plant(name = "Begonia", category = "Kwiaty", frostThresholdCelsius = 3.0, description = "Ciepłolubna. Idealna do doniczek i skrzynek balkonowych.", iconEmoji = "🌺"),
        Plant(name = "Bratek", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Bardzo odporny na mróz, kwitnie wiosną i jesienią.", iconEmoji = "🌼"),
        Plant(name = "Lawenda", category = "Kwiaty", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna. Lubi suche i słoneczne stanowisko.", iconEmoji = "💜"),
        Plant(name = "Chryzantema", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Odporna na jesienne przymrozki – kwitnie do pierwszych mrozów.", iconEmoji = "🌻"),
        Plant(name = "Tulipan", category = "Kwiaty", frostThresholdCelsius = -3.0, description = "Cebulka wytrzymuje mróz w glebie. Kwiaty wrażliwe na przymrozki.", iconEmoji = "🌷"),
        Plant(name = "Narcyz", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Odporna cebulka. Kwiaty znoszą lekkie przymrozki.", iconEmoji = "🌼"),
        Plant(name = "Hiacynt", category = "Kwiaty", frostThresholdCelsius = -3.0, description = "Kwitnie wiosną. Cebulka odporna, kwiaty nie.", iconEmoji = "💐"),
        Plant(name = "Hortensja", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Pąki kwiatowe mogą przemarzać w surowe zimy.", iconEmoji = "💐"),
        Plant(name = "Piwonia", category = "Kwiaty", frostThresholdCelsius = -5.0, description = "Bardzo odporna. Roślina długowieczna.", iconEmoji = "🌸"),
        Plant(name = "Surfinia", category = "Kwiaty", frostThresholdCelsius = 2.0, description = "Wrażliwa na chłód. Sadzaj po 15 maja.", iconEmoji = "🌺"),

        // Zioła
        Plant(name = "Bazylia", category = "Zioła", frostThresholdCelsius = 3.0, description = "Bardzo wrażliwa na chłód. Uprawiaj w cieple, podlewaj umiarkowanie.", iconEmoji = "🌿"),
        Plant(name = "Mięta", category = "Zioła", frostThresholdCelsius = -5.0, description = "Odporna na mróz. Sadź w doniczkach, bo rozrasta się agresywnie.", iconEmoji = "🌿"),
        Plant(name = "Rozmaryn", category = "Zioła", frostThresholdCelsius = -5.0, description = "Dość odporna. W surowe zimy okrywaj agrowłókniną.", iconEmoji = "🌿"),
        Plant(name = "Tymianek", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna. Doskonała roślina okrywowa.", iconEmoji = "🌿"),
        Plant(name = "Pietruszka", category = "Zioła", frostThresholdCelsius = -5.0, description = "Odporna na przymrozki. Możesz zbierać aż do mrozów.", iconEmoji = "🌿"),
        Plant(name = "Koper", category = "Zioła", frostThresholdCelsius = -3.0, description = "Umiarkowanie odporny. Rośnie szybko, siej co 3 tygodnie.", iconEmoji = "🌿"),
        Plant(name = "Oregano", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporna. Suszone intensywniejsze niż świeże.", iconEmoji = "🌿"),
        Plant(name = "Szałwia", category = "Zioła", frostThresholdCelsius = -8.0, description = "Odporna na mróz. Lubi suche, słoneczne stanowisko.", iconEmoji = "🌿"),
        Plant(name = "Estragon", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo mrozoodporny. Kłącza przeżywają zimę w gruncie.", iconEmoji = "🌿"),
        Plant(name = "Kolendra", category = "Zioła", frostThresholdCelsius = -1.0, description = "Wrażliwa na mróz. Siej co kilka tygodni dla ciągłych zbiorów.", iconEmoji = "🌿"),
        Plant(name = "Lubczyk", category = "Zioła", frostThresholdCelsius = -10.0, description = "Bardzo odporna. Rośnie przez wiele lat bez pielęgnacji.", iconEmoji = "🌿"),
        Plant(name = "Melisa", category = "Zioła", frostThresholdCelsius = -10.0, description = "Mrozoodporna. Przeżywa zimy i odrasta wiosną.", iconEmoji = "🌿"),

        // Drzewa i krzewy
        Plant(name = "Lipa", category = "Drzewa", frostThresholdCelsius = -20.0, description = "Bardzo mrozoodporna. Dostarcza nektaru pszczołom.", iconEmoji = "🌳"),
        Plant(name = "Dąb", category = "Drzewa", frostThresholdCelsius = -25.0, description = "Wyjątkowo odporny na mróz. Długowieczny.", iconEmoji = "🌳"),
        Plant(name = "Brzoza", category = "Drzewa", frostThresholdCelsius = -30.0, description = "Bardzo mrozoodporna, pionierski gatunek.", iconEmoji = "🌲"),
        Plant(name = "Świerk", category = "Drzewa", frostThresholdCelsius = -30.0, description = "Odporny na surowe zimy. Stosowany jako żywopłot.", iconEmoji = "🌲"),
        Plant(name = "Sosna", category = "Drzewa", frostThresholdCelsius = -30.0, description = "Bardzo mrozoodporna. Rośnie na ubogich glebach.", iconEmoji = "🌲"),
        Plant(name = "Tuja", category = "Drzewa", frostThresholdCelsius = -15.0, description = "Odporna na mróz. Popularna jako żywopłot.", iconEmoji = "🌲"),
        Plant(name = "Forsycja", category = "Drzewa", frostThresholdCelsius = -10.0, description = "Odporna. Kwitnie bardzo wczesną wiosną (feb/marzec).", iconEmoji = "🌼"),
        Plant(name = "Lilak", category = "Drzewa", frostThresholdCelsius = -15.0, description = "Bardzo mrozoodporny. Kwitnie w maju.", iconEmoji = "💜"),
        Plant(name = "Magnolia", category = "Drzewa", frostThresholdCelsius = -5.0, description = "Kwiaty wrażliwe na przymrozki – mogą przemarzać w marcu/kwietniu.", iconEmoji = "🌸"),
        Plant(name = "Głóg", category = "Drzewa", frostThresholdCelsius = -20.0, description = "Bardzo odporny na mróz i warunki zewnętrzne.", iconEmoji = "🌳"),
        Plant(name = "Bez czarny", category = "Drzewa", frostThresholdCelsius = -10.0, description = "Odporna, szybko rosnąca. Owoce do nalewek i syropów.", iconEmoji = "🫐"),
        Plant(name = "Pigwowiec", category = "Drzewa", frostThresholdCelsius = -15.0, description = "Odporna. Owoc do przetworów, kwiaty ozdobne.", iconEmoji = "🍎"),

        // Trawy i zboża
        Plant(name = "Pszenica ozima", category = "Zboża", frostThresholdCelsius = -15.0, description = "Siana jesienią. Bardzo odporna na mróz pod śniegiem.", iconEmoji = "🌾"),
        Plant(name = "Żyto", category = "Zboża", frostThresholdCelsius = -20.0, description = "Najbardziej mrozoodporne zboże. Siane jesienią.", iconEmoji = "🌾"),
        Plant(name = "Kukurydza", category = "Zboża", frostThresholdCelsius = 2.0, description = "Bardzo wrażliwa na mróz. Sadź po 15 maja.", iconEmoji = "🌽"),
        Plant(name = "Słonecznik", category = "Zboża", frostThresholdCelsius = 1.0, description = "Wrażliwy na mróz. Lubi pełne słońce i żyzną glebę.", iconEmoji = "🌻"),
        Plant(name = "Len", category = "Zboża", frostThresholdCelsius = -3.0, description = "Znosi lekkie przymrozki. Piękne niebieskie kwiaty.", iconEmoji = "🌾")
    )
}
