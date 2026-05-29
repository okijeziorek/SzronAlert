# SzronAlert iOS

Natywna aplikacja SwiftUI korzystająca ze wspólnego modułu Kotlin Multiplatform (`shared/`).

## Architektura

```
iosApp/
├── iosApp.xcodeproj/        # Xcode project (otwieraj .xcworkspace po pod install)
└── iosApp/
    ├── App.swift             # @main + TabView root
    ├── HomeView.swift        # Główny ekran ryzyka szronu
    ├── HistoryView.swift     # Historia alertów
    ├── SettingsView.swift    # Ustawienia + StoreKit 2
    ├── FrostRiskViewModel.swift  # ViewModel konsumujący FrostCore z KMP
    ├── Models.swift          # Swiftowe modele pogody (Codable)
    └── services/
        ├── CoreLocationService.swift        # CoreLocation → LocationService
        ├── LocalNotificationService.swift   # UNUserNotificationCenter → NotificationService
        └── StoreKitSubscriptionService.swift # StoreKit 2 → SubscriptionService
```

### Przepływ danych

```
CoreLocationService (CoreLocation)
        ↓
FrostRiskViewModel
        ├── URLSession → Open-Meteo API → WeatherResponse (Codable)
        └── FrostCore.shared.hasFrostRisk(...) ← shared KMP framework
                ↓
        HomeView (SwiftUI)
```

## Wymagania

- macOS 14+ z Xcode 15+
- iOS 16.0+ na urządzeniu lub symulatorze
- Ruby + CocoaPods (`gem install cocoapods`)
- JDK 17 (do budowania modułu `shared`)

## Konfiguracja środowiska

### 1. Zbuduj framework KMP

```bash
# z katalogu głównego projektu
./gradlew :shared:generateDummyFramework
```

### 2. Zainstaluj zależności CocoaPods

```bash
cd iosApp
pod install
```

### 3. Otwórz workspace w Xcode

```bash
open iosApp.xcworkspace
```

> **Uwaga:** Zawsze otwieraj `.xcworkspace`, NIE `.xcodeproj`, po uruchomieniu `pod install`.

### 4. Ustaw Team i Bundle ID

W Xcode → Target `iosApp` → Signing & Capabilities:
- Ustaw swój Apple Developer Team
- Bundle Identifier: `pl.oki.frostalert.ios` (lub własny)

### 5. Uruchom na symulatorze lub urządzeniu

`Cmd+R` w Xcode.

## MVP – zaimplementowane funkcje

Zgodnie z `IosFeatureMatrix.mvpEnabled` z modułu `shared`:

| Funkcja | Status |
|---|---|
| Główny ekran ryzyka szronu (`MAIN_RISK_SCREEN`) | ✅ `HomeView` |
| Historia alertów (`HISTORY`) | ✅ `HistoryView` (UserDefaults) |
| Ustawienia (`SETTINGS`) | ✅ `SettingsView` |
| Powiadomienia lokalne (`NOTIFICATIONS`) | ✅ `LocalNotificationService` |
| Lokalizacja (`LOCATION`) | ✅ `CoreLocationService` |
| Subskrypcje PRO (`SUBSCRIPTIONS`) | ✅ `StoreKitSubscriptionService` |

## Post-MVP (IosFeatureMatrix.postMvpDeferred)

| Funkcja | Uwagi |
|---|---|
| Widgety (`WIDGETS`) | WidgetKit + WidgetExtension target |
| Geofencing (`GEOFENCING`) | CLLocationManager regionMonitoring |
| Smart Home (`SMART_HOME`) | HomeKit / webhook |
| Narzędzia debugowania (`DEBUG_TOOLS`) | Dodatkowy tab w debug build |
| Extended PRO (`EXTENDED_PRO`) | Dodatkowe tiery StoreKit 2 |

## Produkty StoreKit 2

Zarejestruj w App Store Connect → Monetyzacja:

| ID | Typ | Opis |
|---|---|---|
| `pl.oki.frostalert.ios.pro.monthly` | Auto-Renewable Subscription | Miesięczna PRO |
| `pl.oki.frostalert.ios.pro.annual`  | Auto-Renewable Subscription | Roczna PRO |
| `pl.oki.frostalert.ios.pro.lifetime` | Non-Consumable | Dożywotnia PRO |

## Jak działa integracja z KMP

`FrostCore` z modułu `shared` jest skompilowany do frameworka Objective-C/Swift przez KMP.
Wywołanie z poziomu Swift:

```swift
import shared

let probability = Int(FrostCore.shared.calculateFrostProbability(
    temp: -1.0,
    humidity: 85.0,
    precip: 0.0,
    weatherCode: 0,         // Int32 w ObjC bridge
    tempThreshold: 1.0,
    humidityThreshold: 80.0,
    precipitationThreshold: 1.0,
    sensitivity: 1.0,
    windSpeed: 0.0,
    appMode: 0              // Int32 w ObjC bridge
))
```

Ten sam algorytm działa na Androidzie przez `WeatherCalculations.kt`, który deleguje do `FrostCore`.
Zmiana logiki w jednym miejscu (`shared/FrostCore.kt`) propaguje się na obie platformy.
