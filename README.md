# ❄️ FrostAlert - Inteligentny Asystent Przymrozków

**FrostAlert** to zaawansowana aplikacja na system Android, która pomaga kierowcom i ogrodnikom przewidzieć ryzyko wystąpienia szronu oraz oblodzenia. W przeciwieństwie do zwykłych prognoz pogody, FrostAlert analizuje specyficzne warunki fizyczne, aby dać Ci znać, czy rano czeka Cię skrobanie szyb.

---

## 🚀 Główne Funkcje (Features)

### 🧠 Inteligentny Algorytm
*   **Analiza Punktu Rosy**: Obliczamy dokładną temperaturę, w której para wodna zamienia się w szron.
*   **Wychłodzenie Radiacyjne**: Uwzględniamy zachmurzenie – przy bezchmurnym niebie szyby auta wychładzają się nawet o 4.5°C bardziej niż temperatura powietrza!
*   **Precyzyjna Prognoza Nocna**: Skupiamy się na oknie czasowym 20:00 - 08:00, aby dostarczyć najbardziej istotne dane.

### 📱 Nowoczesny Interfejs (UI/UX)
*   **Material 3 & Dynamic Colors**: Aplikacja automatycznie dopasowuje kolory do tapety Twojego telefonu.
*   **Prognoza Godzinowa**: Czytelny, przewijany pasek z ikonami pogody, temperaturą i wilgotnością na najbliższe 24h.
*   **Splash Screen API**: Płynny i profesjonalny start aplikacji.

### 📊 Historia i Statystyki
*   **Trend Temperatury**: Wykres liniowy pokazujący zmiany minimalnych temperatur z ostatnich nocy.
*   **Eksport CSV**: Możliwość pobrania historii danych do pliku Excel.
*   **Statystyki Sezonu**: Licznik nocy z przymrozkami oraz średnia temperatura minimalna.

### 🛠️ Integracja Systemowa
*   **Glance Widget**: Nowoczesny widget na pulpit z przyciskiem natychmiastowego odświeżania.
*   **App Shortcuts**: Szybki dostęp do Historii bezpośrednio z ikony aplikacji.
*   **Powiadomienia Inteligentne**: Akcje typu "Ignoruj dziś" dostępne bezpośrednio w powiadomieniu.
*   **Monitor Połączenia**: Automatyczne wykrywanie braku internetu.

### 🌍 Gotowość Globalna
*   **Pełna Lokalizacja**: Aplikacja dostępna w języku Polskim i Angielskim.
*   **Jednostki**: Możliwość przełączenia między stopniami Celsjusza (°C) a Fahrenheita (°F).

---

## 🛠️ Stos Technologiczny (Tech Stack)

*   **Język**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **Architektura**: MVVM (ViewModel, Repository)
*   **Baza Danych**: Room Database
*   **Przechowywanie Ustawień**: Jetpack DataStore
*   **Praca w Tle**: WorkManager
*   **Sieć**: Ktor Client + Kotlinx Serialization
*   **Widgety**: Jetpack Glance
*   **Wykresy**: yCharts
*   **Lokalizacja**: Google Play Services Location

---

## ⚙️ Konfiguracja i Instalacja

1.  **Klonowanie repozytorium**:
    ```bash
    git clone https://github.com/okijeziorek/FrostAlert.git
    ```
2.  **Otwarcie w Android Studio**: Zalecana wersja Ladybug lub nowsza.
3.  **Klucze API**: Aplikacja korzysta z darmowego API Open-Meteo (nie wymaga klucza).
4.  **Budowanie**:
    *   Uruchom `Gradle Sync`.
    *   Wybierz wariant `debug` lub `release`.

---

## 🛡️ Uprawnienia (Permissions)

*   `ACCESS_FINE_LOCATION`: Do automatycznego pobierania prognozy dla Twojej lokalizacji.
*   `ACCESS_BACKGROUND_LOCATION`: Aby sprawdzać pogodę w nocy, gdy telefon jest zablokowany.
*   `POST_NOTIFICATIONS`: Do wysyłania ostrzeżeń o szronie.
*   `SCHEDULE_EXACT_ALARM`: Aby powiadomienia przychodziły punktualnie.

---

## 👨‍💻 Autor
Stworzone z myślą o mroźnych porankach. ❄️
