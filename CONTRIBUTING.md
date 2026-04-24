# 🤝 Wkład w projekt (Contributing)

Dziękujemy za zainteresowanie rozwijaniem SzronAlert! Ten przewodnik pomoże Ci sprawnie wdrożyć się w projekt.

---

## Spis treści

1. [Konfiguracja środowiska](#-konfiguracja-środowiska)
2. [Workflow pracy z kodem](#-workflow-pracy-z-kodem)
3. [Standardy kodowania](#-standardy-kodowania)
4. [Dodawanie nowych funkcji](#-dodawanie-nowych-funkcji)
5. [Testy](#-testy)
6. [Zgłaszanie błędów i propozycji](#-zgłaszanie-błędów-i-propozycji)

---

## 🛠️ Konfiguracja środowiska

### Wymagania wstępne

| Narzędzie | Minimalna wersja |
|---|---|
| Android Studio | Ladybug (2024.2.1) |
| JDK | 17 |
| Android SDK | compileSdk 36, minSdk 26 |
| Kotlin | 2.x (przez plugin Compose) |

### Pierwsze kroki

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/okijeziorek/SzronAlert.git
cd SzronAlert

# 2. Otwórz w Android Studio i uruchom Gradle Sync

# 3. Wybierz wariant debug i uruchom aplikację

# 4. Opcjonalnie: uruchom testy
./gradlew :app:testDebugUnitTest
```

> **Uwaga**: Aplikacja korzysta z darmowego API [Open-Meteo](https://open-meteo.com) – nie są potrzebne żadne klucze.

---

## 🔄 Workflow pracy z kodem

1. Utwórz nową gałąź od `main`:
   ```bash
   git checkout -b feature/nazwa-funkcji
   ```
2. Dokonaj zmian, trzymając się [standardów kodowania](#-standardy-kodowania).
3. Uruchom testy: `./gradlew :app:testDebugUnitTest`
4. Otwórz Pull Request z opisem zmian.

---

## 📐 Standardy kodowania

### Architektura (obowiązkowe)

- **MVVM**: UI → ViewModel → Repository → Data. Nigdy nie otwieraj dostępu z UI bezpośrednio do danych.
- **StateFlow**: ViewModel emituje stan przez `StateFlow` zebrany w UI przez `collectAsState()`.
- **AppResult**: wszystkie operacje sieciowe/DB zwracają `AppResult<T>` (nie rzucaj wyjątków).
- **Hilt DI**: wstrzykuj zależności przez konstruktor; nie twórz singletonów ręcznie.

### Baza danych

- Każda zmiana schematu Room wymaga **jawnej migracji** (`MIGRATION_X_Y` w `FrostDatabase.kt`).
- Następna migracja to **`MIGRATION_8_9`** (bieżąca wersja DB: **8**).
- Dodawaj testy migracji dla każdej zmiany schematu.

### Ustawienia

- Nowe preferencje dodawaj do `UserPreferences` (data class) + odpowiedni `PreferencesKey` w `SettingsDataStore`.
- Implementuj getter i setter w `SettingsDataStore` (wzorzec `updateXxx()`).
- Pamiętaj o UI toggle w `SettingsScreen` z bindingiem do ViewModel.

### Widgety

- Po każdym zapisie danych aktualizuj **oba widgety**:
  ```kotlin
  FrostGlanceWidget().updateAll(context)
  WidgetSyncHelper.syncAll(context)   // lub FrostWidgetProvider bezpośrednio
  ```
- Glance nie aktualizuje się automatycznie.

### WorkManager / Workery

- Workerzy używają `@HiltWorker` i `@Assisted` w konstruktorze.
- Nie twórz workerów bez obsługi sieci (sprawdzaj `NetworkMonitor`).
- Zawsze zwracaj `Result.success()`, `Result.retry()` lub `Result.failure()` (nie null).

### Lokalizacja / i18n

- **Zero hardcoded stringów** w UI – wszystko przez `res/values/strings.xml` (PL) i `res/values-en/strings.xml` (EN).
- Dodając nowy tekst, dodaj oba tłumaczenia.

### Kotlin

- Używaj `suspend fun` dla operacji asynchronicznych (nie callback-ów).
- Preferuj `object` dla klas bezstanowych (np. `WeatherCalculations`).
- Używaj `data class` dla encji danych.

---

## ➕ Dodawanie nowych funkcji

### Nowy endpoint API

1. Utwórz `@Serializable` data class w `data/remote/`.
2. Dodaj `suspend fun` do `OpenMeteoApi` zwracający `AppResult<T>`.
3. Wstrzyknij przez repozytorium / ViewModel.
4. Przetestuj mock'iem w teście jednostkowym.

### Nowe ustawienie użytkownika

1. Dodaj pole do `UserPreferences` w `SettingsDataStore.kt`.
2. Utwórz `PreferencesKey` w companion object.
3. Implementuj `getXxx()` (Flow) i `updateXxx()` w `SettingsDataStore`.
4. Dodaj UI toggle w `SettingsScreen`.

### Nowe zadanie w tle

1. Utwórz `CoroutineWorker` z adnotacjami `@HiltWorker` i `@Assisted`.
2. Zarejestruj go przez `WorkManager.enqueueUniquePeriodicWork()` lub `enqueue()`.
3. Obsłuż błędy i zwróć odpowiedni `Result`.
4. Napisz test z `TestListenableWorkerBuilder`.

### Rozszerzenie bazy danych

1. Utwórz encję `@Entity` z `@PrimaryKey`.
2. Utwórz `@Dao` interface z metodami `@Query`/`@Insert`.
3. Dodaj encję i DAO do `@Database` w `FrostDatabase.kt`.
4. Napisz migrację `MIGRATION_N_(N+1)` i dodaj ją do listy `migrations`.

### Nowy ekran

1. Utwórz `*Screen.kt` (Composable) i `*ViewModel.kt` (z `@HiltViewModel`).
2. Dodaj ekran do `MainScreen.kt` (lista zakładek + `HorizontalPager`).
3. Dodaj ciągi do `strings.xml` (PL + EN).
4. Przetestuj ViewModel jednostkowo.

---

## 🧪 Testy

### Uruchamianie testów

```bash
# Testy jednostkowe (szybkie, bez urządzenia)
./gradlew :app:testDebugUnitTest

# Testy instrumentalne (wymaga urządzenia/emulatora)
./gradlew connectedAndroidTest
```

### Wzorce testowania

```kotlin
// ViewModel test
@Test
fun `should emit error state when API fails`() = runTest {
    val repo = mock<WeatherRepository> {
        onBlocking { getWeather(any()) } doReturn AppResult.Error(AppError.NetworkError)
    }
    val vm = HomeViewModel(repo, settingsRepo)
    vm.refresh()
    val state = vm.uiState.value
    assertTrue(state is HomeUiState.Error)
}

// Worker test (Robolectric + @Config(sdk=[33]))
@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class FrostCheckWorkerTest {
    @Test
    fun `worker returns success when weather fetched`() = runTest { ... }
}
```

### Wymagania dotyczące testów

- Nowe metody obliczeniowe (fizyka, trend) → testy edge-case.
- Nowe ViewModels → testy stanu (`Success`, `Error`, `Loading`).
- Zmiany w `WeatherCalculations` → testy z granicznymi wartościami.
- Migracje DB → testy migracji Room.

---

## 🐛 Zgłaszanie błędów i propozycji

Używaj **GitHub Issues** z odpowiednim szablonem:

- **Bug Report**: opisz kroki reprodukcji, oczekiwane i rzeczywiste zachowanie, wersję Androida.
- **Feature Request**: opisz problem, który rozwiązujesz, i proponowane rozwiązanie.

---

## ⚠️ Najczęstsze pułapki

| Problem | Rozwiązanie |
|---|---|
| Widget nie aktualizuje się | Wywołaj `FrostGlanceWidget().updateAll()` + `WidgetSyncHelper.syncAll()` po zapisie |
| Worker nie jest wstrzykiwany | Użyj `@HiltWorker` + `@Assisted` i zarejestruj w `HiltWorkerFactory` |
| Brak migracji Room | Dodaj `MIGRATION_N_(N+1)` i umieść w `FrostDatabase.migrations` |
| String hardcoded w UI | Przenieś do `strings.xml` w PL i EN |
| Geofence nie odpowiada | Sprawdź format ID: `geofence:<lat>:<lon>` i receiver w manifeście |
| Lint crashuje w CI | Lint jest domyślnie wyłączony; włącz przez `-PlintEnabled=true` |
| Testy Robolectric skipowane | Dodaj `@Config(sdk = [33])` – SDK 36 nie jest wspierane przez Robolectric 4.10.3 |
