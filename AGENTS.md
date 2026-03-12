# FrostAlert Agent Guide

**FrostAlert** to zaawansowana aplikacja Android dla kierowców i ogrodników, która prognozuje ryzyko szronu poprzez analitykę fizykę pary wodnej, a nie zwykłe prognozowanie temperatury.

## 🎯 Big Picture Architecture

### Three-Layer Pattern: UI → ViewModel → Repository → Data
- **UI Layer** (`ui/screens/`): Jetpack Compose screens z `StateFlow`-based state management
- **ViewModel Layer** (`ui/screens/*ViewModel.kt`): Business logic using `viewModelScope`, sealed state classes
- **Data Layer**: Split into three sources:
  - **Remote**: `OpenMeteoApi` (free Open-Meteo API, no key required)
  - **Local DB**: Room database via `TemperatureDao` (stores 30-day history of frost risk records)
  - **Settings**: `SettingsDataStore` (DataStore preferences for user configuration)

**Key Flow**: HomeViewModel requests location → LocationRepository gets GPS → OpenMeteoApi fetch → WeatherCalculations apply physics → save to DB → update Glance widget

### Spatial Data Boundaries
- **Temperature Physics** (`utils/WeatherCalculations.kt`): Core frost risk algorithm—handles all meteorological math
- **Garden-specific Logic** (`utils/SummerCalculations.kt`): Separate module for summer features (storm risk, watering reminders)
- **Notification System** (`utils/NotificationHelper.kt`): Centralized notification with action buttons that trigger WorkManager tasks

## 🧊 Critical Algorithm: Frost Risk Detection

**Located**: `WeatherCalculations.kt` - this is the intellectual center of the app.

**Core Physics**:
1. **Dew Point**: `calculateDewPoint(temp, humidity)` using Magnus formula (a=17.27, b=237.7)
2. **Surface Temperature Cooling**: `estimateSurfaceTemp()` applies weather-code-based radiative cooling:
   - Clear sky (code 0): **-4.5°C** cooling (highest risk)
   - Overcast (code 3): **-1.5°C** cooling (lower risk)
   - Sensitivity multiplier allows users to adjust conservativeness
3. **Risk Threshold**: frost risk = `(surfaceTemp <= tempThreshold) AND (surfaceTemp <= dewPoint) AND (humidity >= humidityThreshold)`
4. **Time Window**: Night forecast window is **20:00 - 08:00** (covers all sleep hours)

**App Modes** (appMode=0 is car windshield, appMode=1 is garden plants):
- Car mode uses harsher surface cooling (glass cools more)
- Garden mode uses gentler cooling (plant leaves cool less)

## 🔄 Background Job System (WorkManager-Based)

### Frost Check Cycle
- **Recurring Job**: `FrostCheckWorker` runs hourly (configured in `FrostApplication.kt`)
- **Constraints**: Requires network connection
- **Logic Flow**:
  1. Get location (auto-detect or manual from settings)
  2. Fetch weather from Open-Meteo API
  3. Apply `WeatherCalculations.hasFrostRisk()` with user thresholds
  4. Save `TemperatureRecord` to database
  5. Send notification if risk detected (only between `alertStartHour` and `alertEndHour`)
  6. Update Glance widget via `FrostGlanceWidget().updateAll(context)`

### Notification Actions
- **"Ignoruj dziś"** (Ignore Today): Sets `ignoreUntil` timestamp to 23:59 today
- **"Zastosowałem matę"** (Applied mat): Snoozes until 08:00 next day
- **"Przypomnij za 2h"** (Remind in 2h): Enqueues new one-time worker via `NotificationActionReceiver`
- All actions are processed in `NotificationActionReceiver` and update settings via `SettingsDataStore`

### Special Triggers
- **Car Mode Alarm**: `CarModeReceiver` schedules exact alarm at user's set hour, triggers worker
- **Manual Widget Refresh**: `RefreshActionCallback` in Glance widget enqueues immediate `FrostCheckWorker`

## 📊 Data Storage Pattern

### Room Database
- Single entity: `TemperatureRecord` with timestamp, minTemp, hasRisk
- Simple schema (version 1)
- Key queries in `TemperatureDao`:
  - `getRecentRecords()`: Last 30 records (reactive Flow for UI)
  - `getAllRecords()`: Full history for stats
  - `getAbsoluteMinTemp()`: Lowest temperature ever recorded

### SettingsDataStore (Preferences)
- Uses `DataStore<Preferences>` not Proto (easier for migrations)
- `UserPreferences` data class contains 20+ settings:
  - Risk thresholds: `tempThreshold`, `humidityThreshold`, `precipitationThreshold`, `sensitivity`
  - Time ranges: `alertStartHour`, `alertEndHour`, `carModeHour`
  - Feature flags: `isAutoModeEnabled`, `isMataOptionEnabled`, `isProForced`
  - Location: `manualLatitude`, `manualLongitude`, `manualLocationName`

### Reactive Flow Strategy
- All data access returns `Flow<T>` or `StateFlow<T>` for reactive updates
- ViewModels use `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)` for lifecycle-safe collection

## 🌐 External Dependencies & Integration Points

### APIs
- **Open-Meteo Weather API** (`api.open-meteo.com`): Free, no authentication. Fetches current + 48h hourly forecast
- **Google Play Services Location**: For GPS-based auto-location detection
- **Google Mobile Ads**: Initialized in app onCreate but not actively used in displayed code

### Compose & Material
- **Material 3** with dynamic colors based on system wallpaper
- **Jetpack Glance** for widget UI (separate from main Compose screens)
- **yCharts** library for line graphs in History screen (displays temperature trends)

### Testing Dependencies
- JUnit 4, Mockito-kotlin for unit tests
- Espresso for UI tests
- Test dispatcher pattern: `StandardTestDispatcher` + `runTest` for coroutine testing

## 🛠️ Key Developer Workflows

### Running & Building
```bash
# Build debug
./gradlew assembleDebug

# Build release (with proguard)
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Sync gradle
File → Sync Now (or ./gradlew help)
```

### Debug Features
- **DebugScreen** (UI in dev build): Toggleable overlay with:
  - Frost risk simulator (adjust temp, humidity, wind)
  - Generate fake 14-day history
  - Force PRO mode
  - Test notification buttons
  - Clear all database tables

### Testing Patterns Observed
- **Unit Tests**: Mock repositories, use `runTest` with dispatcher override
- **ViewModel Tests**: Inject mocks, verify repository calls
- **Physics Tests**: Calculate dew points, verify frost risk logic with edge cases (wind >15 km/h cancels risk)

## 📋 Project-Specific Conventions

### Naming & Package Structure
- `data/local/`: Database and DataStore (Room, preferences)
- `data/remote/`: API calls (only Open-Meteo client)
- `data/repository/`: Abstraction layer (single responsibility)
- `utils/`: Utility objects (WeatherCalculations, NotificationHelper, AppResult sealed class)
- `worker/`: WorkManager tasks (FrostCheckWorker)
- `receiver/`: BroadcastReceivers (notification actions, alarms)
- `widget/`: Jetpack Glance widget (FrostGlanceWidget, FrostWidgetProvider)

### Error Handling
- **AppResult<T>**: Sealed class with `Success(data)` and `Error(appError)`
- **AppError**: Type-safe error variants (NetworkError, DatabaseError, LocationError)
- Pattern: `when (result) { is AppResult.Success -> ..., is AppResult.Error -> ... }`

### Sealed Classes for UI State
- `HomeUiState`: Loading, Success (with weather data), Error
- `HistoryUiState`: Data class with isLoading, monthlyStats, errorMessage
- UI renders based on state, not imperative callbacks

### Localization
- Polish (pl) and English (en) supported
- String resources in `res/values/strings.xml` and `res/values-en/`
- All UI text externalized (no hardcoded strings in Compose)

### Kotlin-Specific Practices
- Extension functions for DataStore: `Context.dataStore` delegation pattern
- Object singletons for APIs and calculations: `OpenMeteoApi`, `WeatherCalculations`
- KSP-based code generation: Hilt DI, Room DAOs, Kotlin Serialization
- Serialization uses `@SerialName` for API field mapping

## 🔌 Integration Touchpoints for New Features

### Adding New API Endpoint
1. Create data class in `data/remote/` with `@Serializable` annotation
2. Add suspend function to `OpenMeteoApi` object returning `AppResult<T>`
3. Inject into appropriate repository/ViewModel
4. Test with mock response in unit test

### Adding New Setting/Preference
1. Add field to `UserPreferences` data class in `SettingsDataStore.kt`
2. Create new `PreferencesKey` in companion object
3. Implement get/update methods in `SettingsDataStore`
4. Add corresponding UI toggle in `SettingsScreen` with ViewModel binding

### Adding Background Task
1. Create new `CoroutineWorker` subclass with `@HiltWorker` annotation
2. Define work request in caller (app init or on-demand)
3. Use `WorkManager.getInstance(context).enqueue(request)` or `enqueueUniquePeriodicWork()`
4. Return `Result.success()`, `Result.retry()`, or `Result.failure()`

### Expanding Database
1. Add entity class with `@Entity` and `@PrimaryKey` annotations
2. Add corresponding DAO interface with `@Query`, `@Insert` methods
3. Add entity to `@Database` annotation in `FrostDatabase`
4. Create migration if version change needed (not yet needed)

## ⚠️ Critical Gotchas

- **Permissions**: App requires SCHEDULE_EXACT_ALARM and ACCESS_BACKGROUND_LOCATION (check AndroidManifest)
- **Frost Window**: Hard-coded 20:00-08:00 in `WeatherCalculations.getNightMinTemp()` — change only if you understand impact on all screens
- **Glance Widget**: Updates via `FrostGlanceWidget().updateAll(context)` after data changes, not automatic
- **Settings Cache**: `SettingsDataStore` is singleton; changes propagate via Flow, no manual refresh needed
- **Test Data**: DebugScreen generates fake records with fixed timestamps; real history uses system time

## 📚 Key Files at a Glance

| File | Purpose |
|------|---------|
| `WeatherCalculations.kt` | Physics engine for frost risk (dew point, surface cooling) |
| `FrostCheckWorker.kt` | Hourly background job (fetch weather, check risk, notify) |
| `HomeViewModel.kt` | Main screen state (weather data, refresh control) |
| `HistoryViewModel.kt` | Stats screen state (monthly aggregations, trends) |
| `NotificationHelper.kt` | Notification creation + action setup |
| `LocationRepository.kt` | GPS + manual location fallback |
| `SettingsDataStore.kt` | User preferences persistence (non-database) |
| `OpenMeteoApi.kt` | HTTP client for weather forecast |
| `FrostGlanceWidget.kt` | Modern glance widget (Material 3) |

## 🚀 Quick Start for Contributors

1. Clone → `git clone && cd FrostAlert`
2. Open in Android Studio Ladybug+
3. Gradle Sync (auto-downloads libs)
4. Run DebugScreen to understand UI/logic without API calls
5. Check `app/build.gradle.kts` for all dependencies
6. Tests: `./gradlew test`
7. Add feature: Follow package structure above + dependency injection pattern

