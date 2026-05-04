# ❄️ SzronAlert (FrostAlert)

> Inteligentna aplikacja Android do prognozowania ryzyka szronu oparta na fizyce atmosferycznej – nie tylko na prognozie temperatury.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Weather%20API-Open--Meteo%20(free)-orange)](https://open-meteo.com)
[![Version](https://img.shields.io/badge/Version-1.3-lightgrey)](https://github.com/okijeziorek/SzronAlert)
[![DB](https://img.shields.io/badge/Room%20DB-v8-blue)](https://developer.android.com/training/data-storage/room)

---

## 📖 O aplikacji

**SzronAlert** to zaawansowana aplikacja dla kierowców i ogrodników, która przewiduje ryzyko szronu/oblodzenia poprzez **analizę fizyki atmosferycznej** – oblicza punkt rosy, wychłodzenie radiacyjne powierzchni i wilgotność powietrza. Dzięki temu jest znacznie dokładniejsza od zwykłych prognoz temperatury.

> **Tryb samochodowy**: Wykrywa, czy szyby auta będą pokryte szronem rano (szkło wychładza się nawet o 4,5°C mocniej niż otaczające powietrze przy bezchmurnym niebie).  
> **Tryb ogrodowy**: Monitoruje ryzyko przymrozków dla roślin, zraszanie oraz alerty burzowe.

---

## ✨ Funkcje

### 🧠 Silnik prognozowania szronu
| Funkcja | Opis |
|---|---|
| **Punkt rosy (Magnus)** | Oblicza dokładną temp. kondensacji pary wodnej |
| **Wychłodzenie radiacyjne** | Uwzględnia zachmurzenie (kody WMO): czyste niebo = −4,5°C, pochmurne = −1,5°C |
| **Okno nocne 20:00–08:00** | Skupia prognozę na godzinach krytycznych dla szronu |
| **Wrażliwość (sensitivity)** | Użytkownik reguluje konserwatywność algorytmu |
| **Prawdopodobieństwo szronu (0–100%)** | Dodatkowa metryka z 5-poziomową skalą ryzyka |
| **Odporność na wiatr** | Wiatr > 15 km/h automatycznie anuluje ryzyko szronu |

### 📱 Ekrany aplikacji
| Ekran | Opis |
|---|---|
| **Strona główna** | Aktualny status ryzyka, prognoza godzinowa (24h), wskaźniki temp./wilgotności |
| **Ustawienia** | Progi alertów, tryb auto, lokalizacja, czas alertów, motyw, PRO |
| **Historia** | Wykres liniowy ostatnich nocy (yCharts), eksport CSV, statystyki sezonu |
| **Trend (7 dni)** | Kierunek trendu: UP / STABLE / DOWN z wizualizacją |
| **Ogród** | Monitorowanie roślin, alerty upałów/burz, przypomnienia o podlewaniu |
| **Mapa szronu** | Wizualizacja geograficzna ryzyka – OpenStreetMap (OSMDroid, PRO) |
| **Mikroklimat** | Analiza lokalnych warunków i korekt temperatury dla stref ogrodu |
| **Konfiguracja dashboardu** | Personalizacja widocznych kafelków i ich kolejności |
| **Smart Home** | Integracja z urządzeniami przez webhook IFTTT |
| **Debug** *(tylko debug build)* | Symulator, generowanie danych, diagnostyka widgetów, geofencing |

### 🔔 System powiadomień
- Powiadomienie o ryzyku szronu z akcjami inline:
  - **"Ignoruj dziś"** – wyciszenie do 23:59
  - **"Zastosowałem matę"** – drzemka do 08:00 następnego dnia
  - **"Przypomnij za 2h"** – jednorazowy worker z opóźnieniem
- Alerty zmiany trendu tygodniowego
- Alerty geofencingu (ryzyko w pobliskiej lokalizacji)

### 🗓️ Praca w tle
- **WorkManager** z cyklicznym `FrostCheckWorker` (co godzinę)
- **`MorningBriefWorker`** – inteligentne poranne podsumowanie ryzyka (uczy się pory wstawania)
- **`IntegrityCheckWorker`** – weryfikacja integralności środowiska (root/hook detection)
- Ograniczenie: wymaga połączenia z siecią
- Retry z backoffem przy błędach API
- Monitor sieci (`NetworkMonitor`) – pomija wykonanie offline

### 🧩 Widgety i integracja systemowa
- **Jetpack Glance Widget** – nowoczesny widget z przyciskiem odświeżania (Material 3)
- **Classic AppWidget** (`FrostWidgetProvider`) – fallback kompatybilny ze starszymi launcherami
- **Quick Settings Tile** (`FrostTileService`) – jednym dotknięciem sprawdź ryzyko
- **App Shortcuts** – skrót do historii z ikony aplikacji
- **Splash Screen API** – płynny start

### 🔒 Bezpieczeństwo
- **Szyfrowana baza danych** (SQLCipher) – chroni dane użytkownika w spoczynku
- **Play Integrity API** (`IntegrityChecker`) – weryfikuje integralność instalacji
- **Root Detection** (`RootDetector`) – wykrywa roota/odblokowany bootloader
- **Hook Detection** (`HookDetector`) – wykrywa frameworki do hooking'u (np. Magisk, Xposed)
- `SecurityManager` orkiestruje wszystkie powyższe kontrole

### 🌱 Tryb ogrodowy
- Baza roślin z progami mrozoodporności (Room DB)
- Strefy ogrodu (`GardenZone`) z indywidualnymi ustawieniami
- Dziennik podlewania (`WateringLog`) – historia nawodnień dla każdej rośliny
- Alerty upałów (konfigurowalny próg, domyślnie 30°C)
- Detekcja burz/gradu (kody WMO: 95, 96, 99)
- Przypomnienia o podlewaniu (< 2 mm opadu AND jutrzejsza temp. > 25°C)
- Sezonowe porady ogrodnicze (`GardenSeasonalTips`)
- Dokumentacja fotograficzna ogrodu (`PhotoDocumentationScreen`)

### 📍 Geofencing i lokalizacje
- Automatyczny GPS lub ręcznie ustawiona lokalizacja
- Wiele zapisanych lokalizacji (`SavedLocation`, DB v6+)
- Geofencing – monitoring ryzyka w promieniu N metrów
- `GeofenceRateLimiter` – maks. 8 wywołań API na zdarzenie geofence
- Historia zdarzeń geofencingu (`GeofenceHistoryScreen`)

### 🏠 Smart Home
- Integracja z systemami automatyki domowej przez **webhook** (HTTP POST)
- Obsługa **IFTTT** (`smartHomeIftttKey`)
- Konfigurowalny próg ryzyka szronu wywołujący automatyzację (`smartHomeThreshold`)
- Dedykowany ekran konfiguracji (`SmartHomeSettingsScreen`)

### 💳 PRO i monetyzacja
- Google Play Billing (`BillingClientWrapper`) z subskrypcją PRO
- Reklamy Google Mobile Ads (wersja darmowa)
- PRO odblokowuje: mapę szronu, zaawansowane statystyki, usunięcie reklam

### 🌍 Lokalizacja
- **Polski**, **Angielski**, **Niemiecki**, **Francuski**, **Hiszpański**, **Włoski**, **Ukraiński**, **Czeski** oraz tryb **Polski (wulgarny)**
- Tryb Fahrenheit/Celsius (przełączany w ustawieniach)
- Wszystkie ciągi znaków w zasobach (`res/values/strings.xml`)

---

## 🏛️ Architektura

```
UI Layer (Jetpack Compose)
    ↕ StateFlow / collectAsState
ViewModel Layer (viewModelScope)
    ↕ suspend functions / Flow
Repository Layer
    ├── LocationRepository   (GPS + manual location)
    ├── WeatherRepository    (nie bezpośrednia – przez ViewModel)
    └── SettingsRepository   (DataStore preferences)
Data Layer
    ├── OpenMeteoApi         (Ktor, REST, free, no key)
    ├── FrostDatabase (Room) (TemperatureRecord, Plant, GardenZone, GeofenceRecord, SavedLocation…)
    └── SettingsDataStore    (DataStore<Preferences>, 25+ ustawień)
```

Szczegółowy opis architektury → [`ARCHITECTURE.md`](ARCHITECTURE.md)

---

## 🛠️ Stos technologiczny

| Kategoria | Technologia |
|---|---|
| Język | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 + Dynamic Colors |
| Architektura | MVVM (ViewModel, Repository, StateFlow) |
| DI | Hilt (+ Hilt WorkManager) |
| Baza danych | Room (wersja **8**, z migracjami) + SQLCipher **4.6.1** (szyfrowanie) |
| Preferencje | Jetpack DataStore (`DataStore<Preferences>`) |
| Sieć | Ktor Client + Kotlinx Serialization |
| Praca w tle | WorkManager (HiltWorker) |
| Widgety | Jetpack Glance + classic AppWidget |
| Wykresy | yCharts |
| Mapy | OSMDroid (OpenStreetMap) |
| Lokalizacja | Google Play Services Location |
| Reklamy | Google Mobile Ads |
| Zakupy | Google Play Billing KTX |
| Bezpieczeństwo | SQLCipher, Play Integrity API, Security Crypto |
| Testy | JUnit 4, Mockito-kotlin, Robolectric, Espresso |

---

## ⚙️ Szybki start

### Wymagania
- Android Studio **Ladybug** lub nowszy
- JDK 17
- Android SDK: minSdk **26** (Android 8.0), targetSdk **36**

### Instalacja

```bash
git clone https://github.com/okijeziorek/SzronAlert.git
cd SzronAlert
```

1. Otwórz projekt w Android Studio.
2. Uruchom **Gradle Sync** (automatycznie pobiera zależności).
3. **Klucze API**: brak wymaganych – Open-Meteo jest darmowe i bez klucza.
4. Wybierz wariant `debug` lub `release` i uruchom.

### Przydatne polecenia Gradle

```bash
# Buduj debug APK
./gradlew assembleDebug

# Buduj release APK (z ProGuard)
./gradlew assembleRelease

# Uruchom testy jednostkowe
./gradlew :app:testDebugUnitTest

# Uruchom testy instrumentalne (wymaga urządzenia/emulatora)
./gradlew connectedAndroidTest

# Lint (domyślnie wyłączony w CI, włącz przez -PlintEnabled=true)
./gradlew lint -PlintEnabled=true
```

---

## 🛡️ Uprawnienia

| Uprawnienie | Powód |
|---|---|
| `ACCESS_FINE_LOCATION` | Automatyczne pobieranie lokalizacji GPS |
| `ACCESS_BACKGROUND_LOCATION` | Sprawdzanie pogody w nocy (tło) |
| `POST_NOTIFICATIONS` | Ostrzeżenia o szronie |
| `SCHEDULE_EXACT_ALARM` | Tryb samochodowy – dokładna godzina alarmu |
| `RECEIVE_BOOT_COMPLETED` | Wznowienie workerów po restarcie |

---

## 🧪 Testy

```
app/src/test/
└── pl/oki/frostalert/
    ├── worker/         FrostCheckWorkerTest
    ├── utils/          WeatherCalculationsTest, TrendCalculationsTest, ...
    └── viewmodel/      HomeViewModelTest, HistoryViewModelTest, ...
```

- Coroutines: `StandardTestDispatcher` + `runTest`
- Worker tests: `TestListenableWorkerBuilder` + custom `WorkerFactory` z mockami Hilt
- Robolectric: `@Config(sdk = [33])` (SDK 36 nie jest jeszcze wspierany przez Robolectric 4.10.3)

---

## 📁 Struktura projektu

```
app/src/main/java/pl/oki/frostalert/
├── FrostApplication.kt          # Hilt entry point, WorkManager config
├── MainActivity.kt
├── billing/                     # Google Play Billing (PRO subscription)
├── data/
│   ├── local/                   # Room entities, DAOs, FrostDatabase, SettingsDataStore
│   ├── remote/                  # OpenMeteoApi (Ktor)
│   └── repository/              # LocationRepository, SettingsRepository
├── di/                          # Hilt modules (@Provides)
├── geofence/                    # GeofenceManager, GeofenceRegistrar
├── receiver/                    # BroadcastReceivers (powiadomienia, alarmy, bootup, morning brief)
├── security/                    # RootDetector, HookDetector, IntegrityChecker, SecurityManager
├── ui/
│   ├── screens/                 # Wszystkie ekrany Compose + ViewModels
│   └── theme/                   # Material 3 theme
├── utils/                       # WeatherCalculations, SummerCalculations, NotificationHelper, TtsHelper, ...
├── widget/                      # FrostGlanceWidget, FrostWidgetProvider
└── worker/                      # FrostCheckWorker, MorningBriefWorker, IntegrityCheckWorker
```

---

## 🤝 Współpraca (Contributing)

Szczegółowy przewodnik → [`CONTRIBUTING.md`](CONTRIBUTING.md)

Krótkie zasady:
1. Zachowaj wzorzec **MVVM** (UI → ViewModel → Repository → Data).
2. Nowe ustawienia dodawaj do `UserPreferences` + `SettingsDataStore`.
3. Zmiany schematu bazy danych wymagają **migracji Room** (kolejna wersja: `MIGRATION_8_9`; bieżąca: v8).
4. Po zapisie danych zawsze aktualizuj **oba widgety** (Glance + AppWidget).
5. Workerzy Hilt wymagają `@HiltWorker` + `@Assisted` w konstruktorze.
6. Uruchom testy przed PR: `./gradlew :app:testDebugUnitTest`.

---

## 📜 Licencja

Projekt jest własnością autora. Szczegóły dotyczące licencji na żądanie.

---

## 👨‍💻 Autor

Stworzone z pasją do meteorologii i mroźnych poranków. ❄️  
Kontakt przez Issues lub Discussions na GitHub.
