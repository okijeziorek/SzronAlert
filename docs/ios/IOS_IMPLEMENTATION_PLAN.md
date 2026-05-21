# iOS v1 Implementation (Execution Baseline)

Ten dokument wdraża plan przejścia do iOS jako roadmapę wykonawczą z realnym zakresem MVP i mapowaniem na kod.

## 1) Zakres iOS v1 (MVP)

### MVP (włączone)
- Ekran główny ryzyka szronu
- Historia
- Ustawienia
- Powiadomienia
- Lokalizacja
- Subskrypcje

### Po MVP (odroczone)
- Widgety
- Geofencing
- Smart Home
- Narzędzia debug
- Rozszerzone funkcje PRO

Źródło prawdy w kodzie: `shared/src/commonMain/.../platform/IosFeatureFlags.kt`

## 2) Decyzja technologiczna

Przyjęto kierunek: **Kotlin Multiplatform + natywny iOS UI (SwiftUI)**.

Zaimplementowano moduł `:shared` z targetami:
- `androidTarget()`
- `iosX64()`
- `iosArm64()`
- `iosSimulatorArm64()`

## 3) Wydzielenie shared core

Dodane:
- `shared/core/FrostCore.kt`
- `shared/core/SummerCore.kt`
- `shared/model/WeatherModels.kt`
- `shared/network/WeatherClient.kt`

Android został podpięty do `:shared`, a logika w:
- `app/.../WeatherCalculations.kt`
- `app/.../SummerCalculations.kt`

deleguje do shared core w obszarach platform-agnostic.

## 4) Warstwa iOS systemowa (kontrakty)

Dodano kontrakty do implementacji po stronie iOS:
- `LocationService`
- `NotificationService`
- `BackgroundTaskScheduler`
- `LocalStorage`
- `SubscriptionService`
- `SecurityService`

Plik: `shared/platform/SystemServices.kt`

## 5) Monetyzacja (StoreKit 2)

Przygotowano kontrakt przeniesienia subskrypcji:
- `SubscriptionService`
- `SubscriptionProduct`

Implementacja StoreKit 2 zostanie dostarczona po stronie iOS app.

## 6) Bezpieczeństwo i prywatność

Przygotowano miejsce na runtime checks i raport:
- `SecurityService`
- `SecurityCheckResult`

Wymagana finalizacja przed publikacją:
- polityka prywatności pod iOS
- App Store Connect privacy labels
- export compliance / tracking declarations

## 7) Jakość

Dodano testy shared core:
- `shared/src/commonTest/.../FrostCoreTest.kt`
- `shared/src/commonTest/.../SummerCoreTest.kt`

## 8) Release operacyjny (do wykonania w iOS app)

- Apple Developer Account + certyfikaty/provisioning
- Bundle ID i konfiguracje dev/prod
- CI/CD dla TestFlight

## 9) Listing App Store (do wykonania)

- Nazwa, opisy, screenshoty, słowa kluczowe
- Lokalizacje językowe
- Formularze zgodności

## 10) Etapowanie rolloutu

- Etap A: TestFlight (zamknięta beta)
- Etap B: publikacja iOS v1 (MVP)
- Etap C: widget/geofencing/smart-home/rozszerzony PRO
