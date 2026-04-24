# 🏛️ Architektura SzronAlert

Dokument opisuje warstwową architekturę aplikacji, przepływ danych i kluczowe wzorce implementacyjne.

---

## Ogólny diagram przepływu

```
┌──────────────────────────────────────────────┐
│               UI Layer (Compose)             │
│  HomeScreen  HistoryScreen  GardenScreen ... │
└────────────────────┬─────────────────────────┘
                     │ collectAsState / StateFlow
┌────────────────────▼─────────────────────────┐
│            ViewModel Layer                   │
│  HomeViewModel  HistoryViewModel  ...        │
│  (viewModelScope, sealed UiState classes)    │
└──────┬───────────────────────────┬───────────┘
       │ suspend / Flow            │ suspend / Flow
┌──────▼───────┐         ┌────────▼────────────┐
│  Repository  │         │  SettingsDataStore   │
│  Layer       │         │  (DataStore<Prefs>)  │
└──────┬───────┘         └─────────────────────┘
       │
  ┌────┴────────────────────────┐
  │         Data Sources        │
  ├─────────────────────────────┤
  │ OpenMeteoApi (Ktor, REST)   │
  │ FrostDatabase (Room)        │
  └─────────────────────────────┘
```

---

## Warstwy

### 1. UI Layer (`ui/screens/`)

- Zbudowana w **Jetpack Compose** z Material 3 i Dynamic Colors.
- Każdy ekran renderuje się na podstawie `StateFlow`/`UiState` z ViewModel (brak bezpośredniego dostępu do danych).
- Sealed klasy stanu: `HomeUiState` (Loading / Success / Error), `HistoryUiState`, `GardenUiState` itd.
- Nawigacja przez **HorizontalPager** (`MainScreen.kt`) – 6 zakładek (7 w debug buildzie).
- Animowane przejście tła zależne od aktywnej zakładki.

### 2. ViewModel Layer (`ui/screens/*ViewModel.kt`)

- Dziedziczy z `ViewModel`, używa `viewModelScope` dla coroutines.
- Wstrzykiwany przez **Hilt** (`hiltViewModel()`).
- Emituje stan przez `StateFlow` przy użyciu `.stateIn(WhileSubscribed(5000))`.
- Nie zawiera logiki warstwy danych – deleguje do repozytoriów.

### 3. Repository Layer (`data/repository/`)

| Repozytorium | Odpowiedzialność |
|---|---|
| `LocationRepository` | GPS auto-detekcja + manualna lokalizacja z ustawień; `checkGeofencingRisk()` z ogranicznikiem wywołań |
| `SettingsRepository` | Fasada dla `SettingsDataStore` |

### 4. Data Layer

#### 4a. Remote – `OpenMeteoApi` (`data/remote/`)
- Klient **Ktor** (bez klucza API).
- Pobiera bieżącą pogodę + prognozę godzinową 48h.
- Zwraca `AppResult<T>` (sealed: `Success` / `Error`).
- Serializacja przez **Kotlinx Serialization** (`@Serializable`, `@SerialName`).

#### 4b. Local DB – `FrostDatabase` (`data/local/`)
- **Room** wersja **8**, z jawnie zdefiniowanymi migracjami (brak `fallbackToDestructiveMigration`).
- Encje: `TemperatureRecord`, `Plant`, `UserPlant`, `GardenZone`, `GeofenceRecord`, `SavedLocation`, `CalibrationFeedback`, `FrostPhoto`, `WateringLog`.
- DAOs: `TemperatureDao`, `PlantDao`, `UserPlantDao`, `GardenZoneDao`, `GeofenceDao`, `SavedLocationDao`, `CalibrationDao`, `FrostPhotoDao`, `WateringLogDao`.
- Baza jest szyfrowana przy użyciu **SQLCipher** (`net.zetetic:sqlcipher-android`).
- Reaktywne zapytania zwracają `Flow<List<T>>`.

#### 4c. Settings – `SettingsDataStore`
- `DataStore<Preferences>` (nie Proto – łatwiejsze migracje).
- `UserPreferences` – data class z 40+ polami:
  - Progi: `tempThreshold`, `humidityThreshold`, `precipitationThreshold`, `sensitivity`
  - Tryb: `appMode` (0=auto, 1=ogród), `isCarModeEnabled`, `carModeHour`
  - Czas alertów: `alertStartHour`, `alertEndHour`, `ignoreUntil`
  - Flagi: `isAutoModeEnabled`, `isMataOptionEnabled`, `isProForced`, `useFahrenheit`, `isTtsEnabled`, `isCalendarSyncEnabled`
  - Letnie: `heatThreshold`, `isStormAlertEnabled`, `isWateringReminderEnabled`
  - Geofencing: `isGeofencingEnabled`, `geofenceRadiusMeters`, `lastTrend`, `pendingTrend`, `isTrendChangeNotificationsEnabled`
  - Lokalizacja: `manualLatitude`, `manualLongitude`, `manualLocationName`, `isManualLocationEnabled`, `activeLocationId`
  - UI: `theme` (0=light, 1=dark, 2=system), `isOnboardingCompleted`, `enabledDashboardCards`, `dashboardCardOrder`
  - Smart Home: `isSmartHomeEnabled`, `smartHomeWebhookUrl`, `smartHomeThreshold`, `smartHomeIftttKey`
  - Morning Brief: `isMorningBriefEnabled`, `morningLearningDays`, `morningBriefDelayMinutes`, `morningWakeHistoryJson`, `morningMedianWakeMinute`, `morningWindowStartMinute`, `morningWindowEndMinute`, `morningLastNotificationEpochDay`, `morningLastUnlockEpochDay`, `morningLastScheduledAtMs`

---

## Algorytm wykrywania szronu (`WeatherCalculations.kt`)

```
Input: temperature (°C), humidity (%), weatherCode (WMO), windSpeed (km/h)

1. calculateDewPoint(temp, humidity)
   → Magnus formula: a=17.27, b=237.7
   → dewPoint = b * γ / (a - γ),  γ = a*T/(b+T) + ln(H/100)

2. estimateSurfaceTemp(temp, weatherCode, mode, sensitivity)
   → cooling = when(weatherCode) {
       0       → -4.5°C   // clear sky (car) / -3.5°C (garden)
       1, 2    → -3.0°C
       3       → -1.5°C
       else    → -1.0°C
     } * sensitivity

3. hasFrostRisk():
   if windSpeed > 15 km/h → NO RISK (wind prevents frost)
   RISK = (surfaceTemp <= tempThreshold)
       AND (surfaceTemp <= dewPoint)
       AND (humidity >= humidityThreshold)

4. calculateFrostProbability() → Int (0..100)
   Skala: VERY_HIGH / HIGH / MODERATE / LOW / MINIMAL
```

---

## System pracy w tle

```
FrostApplication.onCreate()
  └── WorkManager.initialize() z HiltWorkerFactory
  └── enqueueUniquePeriodicWork("FrostCheck", KEEP, hourlyRequest)
  └── enqueueUniquePeriodicWork("IntegrityCheck", KEEP, dailyRequest)

FrostCheckWorker.doWork():
  1. Check NetworkMonitor → skip if offline
  2. LocationRepository.getLocation()
  3. OpenMeteoApi.getWeather()
  4. WeatherCalculations.hasFrostRisk()
  5. TemperatureDao.insert(record)
  6. NotificationHelper.sendNotification() [if risk AND in alert window]
  7. FrostGlanceWidget().updateAll()  +  FrostWidgetProvider.update()
  8. TrendCalculations → send trend-change notification [optional]
  9. LocationRepository.checkGeofencingRisk() [optional]
 10. SmartHome webhook trigger [if isSmartHomeEnabled AND risk >= threshold]

MorningBriefWorker.doWork():
  1. Sprawdza historię budzenia (morningWakeHistoryJson)
  2. Oblicza medianę pory wstawania
  3. Jeśli użytkownik w oknie aktywności → wysyła podsumowanie ryzyka

IntegrityCheckWorker.doWork():
  1. SecurityManager.runChecks()
  2. RootDetector, HookDetector, IntegrityChecker
  3. Loguje wynik w AppTelemetry (brak zewnętrznego serwera)
```

### Triggery poza cyklem

| Trigger | Mechanizm |
|---|---|
| Tryb samochodowy | `CarModeReceiver` + `AlarmManager` (exact alarm) → worker one-time |
| Kafelek szybkich ustawień | `FrostTileService` → worker one-time |
| Ręczne odświeżenie widgetu | `RefreshActionCallback` → worker one-time |
| Akcje z powiadomienia | `NotificationActionReceiver` → `SettingsDataStore.update*()` |
| Morning Brief (odblokowanie) | `MorningUserPresentReceiver` → `MorningBriefWorker` one-time |

---

## Geofencing

```
GeofenceRegistrar (reaktywny)
  └── obserwuje: isGeofencingEnabled, geofenceRadius, activeLocation (Flow)
  └── przy zmianie: GeofenceManager.unregisterAll() → registerGeofences()

GeofenceBroadcastReceiver
  └── trigger ENTER → LocationRepository.checkGeofencingRisk(weatherResponse)
  └── GeofenceRateLimiter: max 8 wywołań API / trigger
  └── NotificationHelper.sendNotification() jeśli ryzyko
  └── GeofenceDao.insert(record)
```

ID geofence ma format `geofence:<lat>:<lon>` – parsowany w receiverze.

---

## Obsługa błędów

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()
}

sealed class AppError {
    object NetworkError : AppError()
    object DatabaseError : AppError()
    object LocationError : AppError()
    data class UnknownError(val message: String) : AppError()
}
```

Wzorzec użycia:
```kotlin
when (val result = repository.fetchWeather()) {
    is AppResult.Success -> handleData(result.data)
    is AppResult.Error   -> showError(result.error)
}
```

---

## Widgety – dwie ścieżki aktualizacji

Po każdym zapisie danych należy aktualizować **oba widgety**:

```kotlin
// Glance (nowoczesny, Material 3)
FrostGlanceWidget().updateAll(context)

// Classic AppWidget (stare launchery)
FrostWidgetProvider.update(context, appWidgetManager, widgetId)
// lub przez WidgetSyncHelper.syncAll(context)
```

**Uwaga**: Glance nie aktualizuje się automatycznie – wymaga jawnego wywołania.

---

## Telemetria (`AppTelemetry.kt`)

Dane diagnostyczne przechowywane w SharedPreferences (brak zewnętrznego serwera):

| Metoda | Zdarzenie |
|---|---|
| `recordWorkerSuccess/Failure()` | Wyniki cyklu WorkManager |
| `recordGeofenceTrigger/Error()` | Zdarzenia geofencingu |
| `recordWidgetUpdate/Error()` | Aktualizacje widgetów |
| `recordBillingPurchase/Restore/Error()` | Zdarzenia zakupów |

Dostęp przez `DiagnosticsHelper` w DebugScreen.

---

## Moduł bezpieczeństwa (`security/`)

Wszystkie kontrole są wykonywane lokalnie – brak zewnętrznego serwera.

| Klasa | Odpowiedzialność |
|---|---|
| `RootDetector` | Wykrywa root/odblokowany bootloader (sprawdza ścieżki su, uprawnienia) |
| `HookDetector` | Wykrywa frameworki hook'ujące (Magisk, Xposed, Frida) |
| `IntegrityChecker` | Wywołuje **Play Integrity API** – weryfikuje autentyczność instalacji z Google Play |
| `SecurityManager` | Orkiestruje wszystkie kontrole; zwraca zbiorczy wynik bezpieczeństwa |

Baza danych jest szyfrowana przy użyciu **SQLCipher** (`net.zetetic:sqlcipher-android:4.5.4`).  
Klucz przechowywany przez `androidx.security:security-crypto` (EncryptedSharedPreferences).

---

## Migracje bazy danych

| Wersja | Zmiana |
|---|---|
| 1 → 2 | Dodano `calibration_feedback` |
| 2 → 3 | Dodano `geofence_record` |
| 3 → 4 | Dodano kolumnę `frostProbability` do `temperature_records` |
| 4 → 5 | Dodano tabele `plants`, `user_plants` (tryb ogrodowy) |
| 5 → 6 | Dodano tabelę `saved_locations` (multi-lokalizacja) |
| 6 → 7 | Dodano tabele `frost_photos`, `garden_zones` |
| 7 → 8 | Dodano tabelę `watering_log` (dziennik podlewania roślin) |
| **→ 9** | Następna migracja: `MIGRATION_8_9` |

---

## Kluczowe pliki

| Plik | Rola |
|---|---|
| `WeatherCalculations.kt` | Silnik fizyczny – punkt rosy, wychłodzenie, ryzyko szronu |
| `SummerCalculations.kt` | Upały, burze (WMO 95/96/99), podlewanie |
| `FrostCheckWorker.kt` | Godzinne zadanie w tle |
| `MorningBriefWorker.kt` | Poranne podsumowanie ryzyka (uczące się pory wstawania) |
| `IntegrityCheckWorker.kt` | Dzienne sprawdzenie integralności środowiska |
| `TrendCalculations.kt` | Trend 7-dniowy (UP/STABLE/DOWN) |
| `HomeViewModel.kt` | Stan ekranu głównego |
| `GeofenceRegistrar.kt` | Reaktywny sync geofenców z ustawieniami |
| `NotificationHelper.kt` | Tworzenie powiadomień + akcje |
| `LocationRepository.kt` | GPS + manualna lokalizacja + geofencing |
| `SettingsDataStore.kt` | Trwałość preferencji (DataStore) |
| `OpenMeteoApi.kt` | Klient HTTP (Ktor) |
| `FrostGlanceWidget.kt` | Widget Glance (Material 3) |
| `FrostWidgetProvider.kt` | Widget klasyczny (RemoteViews) |
| `BillingClientWrapper.kt` | Subskrypcja PRO (Google Play Billing) |
| `FrostTileService.kt` | Kafelek szybkich ustawień |
| `AppTelemetry.kt` | Diagnostyka wewnętrzna |
| `SecurityManager.kt` | Orkiestrator kontroli bezpieczeństwa (root/hook/integrity) |
| `TtsHelper.kt` | Synteza mowy (Text-to-Speech) |
| `ReportGenerator.kt` | Eksport raportów (CSV/tekst) |
| `GardenSeasonalTips.kt` | Sezonowe porady ogrodnicze |
