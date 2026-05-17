# 🗺️ SzronAlert – Roadmap Rozwoju i Strategia Pozyskiwania Użytkowników

> Dokument opisuje kierunki dalszego rozwoju aplikacji po zamknięciu faz stabilizacyjnych (P0/P1 z `DEVELOPMENT_PLAN.md`), propozycje nowych funkcji oraz strategie wzrostu użytkowników.

---

## 📍 Gdzie jesteśmy (stan aktualny v1.3)

Aplikacja posiada solidny rdzeń:

| Obszar | Stan |
|---|---|
| Algorytm fizyczny szronu (punkt rosy, wychłodzenie radiacyjne) | ✅ Gotowy |
| Tło (WorkManager, powiadomienia, retry) | ✅ Gotowy |
| Tryb samochodowy i ogrodowy | ✅ Gotowy |
| Widgety (Glance + AppWidget) | ✅ Gotowy |
| Geofencing i wiele lokalizacji | ✅ Gotowy |
| Smart Home (webhook / IFTTT) | ✅ Gotowy |
| Mapa szronu (OSMDroid, PRO) | ✅ Gotowy |
| Monetyzacja (subskrypcja miesięczna / roczna / lifetime) | 🔄 W toku |
| Telemetria i analityka | ❌ Brak |
| Wersja iOS | ❌ Brak |

---

## 🚀 Fazy rozwoju po stabilizacji

### Faza 6 – Telemetria i Pętla Jakości (P0)

Bez danych niemożliwy jest świadomy rozwój. Priorytet: wdrożyć lekką, prywatność-respektującą analitykę.

**Zakres:**
- Firebase Analytics (anonimowe zdarzenia: `worker_success`, `widget_refresh`, `purchase_attempt`, `frost_alert_sent`)
- Firebase Crashlytics (zastąpienie lub uzupełnienie bieżącego logowania)
- Dashboard wewnętrzny: DAU/MAU, retention D1/D7/D30, konwersja Free → PRO
- A/B testing cenowy (79,99 PLN vs 99,99 PLN subskrypcja roczna)

**Definition of Done:**
- Zdarzenia wdrożone i widoczne w Firebase Console
- Crash-free sessions >= 99,7%
- Baseline retention D7 zmierzony po 2 tygodniach

---

### Faza 7 – Jakość Prognozowania (P1)

Dokładność to główny argument przy zakupie PRO i recenzjach 5★.

**Zakres:**
- **Kalibracja osobista** – użytkownik porównuje prognozę z realnym szronem i feedbackuje; model adaptuje się przez `CalibrationFeedback` (encja już istnieje w DB)
- **Prognoza probabilistyczna** – wyświetlanie przedziału pewności (np. „70–85% szansa szronu") zamiast binarnego TAK/NIE
- **Mikroklimat PRO** – pobranie danych ze stacji pogodowych powiatu (integracja z IMGW XML API lub Synoptic API)
- **Korekta wysokościowa** – gradient temperatury z wysokością n.p.m. (dane elevation z Open-Elevation API)
- **Integracja z osobistymi stacjami meteo** (Weather Underground Personal Weather Station API lub Ecowitt API)

---

### Faza 8 – Platforma Wieloplatformowa (P2)

**Zakres:**
- **Tile/Complication dla Wear OS** – ryzyko szronu na tarczę zegarka
- **Android Auto** – głosowe powiadomienie o szronie podczas jazdy (MediaSession / AAOS template)
- **Widżet na lock screen** (Android 13+) – jednym spojrzeniem przed wyjściem
- **iOS (docelowo)** – port kluczowych funkcji przez Kotlin Multiplatform lub React Native; osobna decyzja biznesowa

---

### Faza 9 – Sieć Społeczności i UGC (P2)

Treść generowana przez użytkowników to najtańszy kanał pozyskiwania nowych użytkowników.

**Zakres:**
- **Mapa potwierdzeń szronu** – użytkownicy raportują „mam szron / nie mam szronu" w swoim rejonie; dane anonimowe, agregowane na serwer (prosta REST API)
- **Ranking trafności** – użytkownik ocenia trafność prognozy; zbiór danych do ulepszenia kalibracji
- **„Frost Streak"** – grywalizacja: seria dni z poprawnymi prognozami; udostępnianie w social media
- **Komentarze lokalne** – notatki przywiązane do geolokalizacji (np. „tu zawsze jest szron wcześniej niż prognoza")
- **Tryb Współdzielony** – jedno konto PRO dla 2 osób (para/rodzina), wspólne lokalizacje

---

## 💡 Pomysły na nowe funkcje (posortowane według wpływu na retencję)

### 🔴 Wysoki wpływ – dodaj jak najszybciej

#### 1. Tryb Zimowy Kierowcy 2.0
- Alarm głosowy TTS (już jest `TtsHelper`) z komunikatem: *„Uwaga, dziś w nocy prognozowany szron. Nastaw budzik 5 minut wcześniej."*
- Integracja z kalendarzem: jeśli użytkownik ma spotkanie rano, alert pojawia się automatycznie wieczorem
- Szacowanie czasu odmrażania szyby (wzór empiryczny wg grubości szronu)

#### 2. Dashboard Sezonowy
- Podsumowanie sezonu zimowego: ile nocy z szronem, najzimniejsza noc, ile razy alert był trafiony
- Udostępnianie statystyk jako grafika (share card) do social media
- „Rok w danych" – Spotify Wrapped style, generowany każdego 1 marca

#### 3. Alert Przymrozkowy dla Ogrodu 2.0
- Personalizowane progi dla każdej rośliny z bazy (`UserPlant`) – już istnieje DB, brakuje pełnego UI
- Push-notification per roślina: „Twoja róża jest zagrożona dziś w nocy (−2°C)"
- Integracja z fotograficznym dziennikiem ogrodu (już jest `PhotoDocumentationScreen`)

#### 4. Powiadomienia Bogate (Rich Notifications)
- Miniaturka wykresu temperatury w nocy bezpośrednio w powiadomieniu (NotificationCompat BigPictureStyle)
- Animowany wskaźnik ryzyka na powiadomieniu
- Przycisk „Sprawdź teraz" otwierający Quick Settings Tile

#### 5. Lokalne Porównanie Sąsiednich Miast
- Dla zalogowanych PRO: lista 3–5 sąsiednich miejscowości z ich ryzykiem szronu tej nocy
- Przydatne dla osób z domkiem letniskowym lub pendlerów

---

### 🟡 Średni wpływ – dobry kandydat na PRO

#### 6. Eksport i Raportowanie
- Raport PDF sezonowy (już jest `ReportGenerator`) – dodać więcej statystyk i wysyłkę mailem
- Eksport do CSV już istnieje; dodać eksport do Google Sheets przez Sheets API
- Integracja z Apple Health / Google Fit jako „aktywność: przygotowanie do zimy" (niszowe, ale unikalne)

#### 7. Automatyzacja Smart Home 2.0
- Gotowe przepisy IFTTT (linki wygenerowane przez app zamiast ręcznej konfiguracji webhooka)
- Natywna integracja Google Home (Matter/Home API) dla ogrzewania tarasu lub elektrycznych okiennic garażu
- Integracja Home Assistant przez local REST – webhook już jest, dodać szablon automatyzacji HA

#### 8. Prognoza Długoterminowa (14 dni)
- Już w planie jako funkcja PRO; Open-Meteo dostarcza do 16 dni
- Wizualizacja „kiedy następny szron w tym tygodniu" (timeline view)
- Widok miesięczny z zaznaczonymi nocami szronowymi (calendar heatmap)

#### 9. Tryb Rolniczy / Gospodarski
- Nowy `appMode=2` dla rolników: progi dla upraw (rzepak, pszenica, truskawka)
- Alerty agro: BBCH faza wzrostu + ryzyko przymrozku = krytyczny alert
- Współpraca z IUNG lub ARiMR jako kanał dystrybucji B2B

#### 10. Asystent AI (lokalny, prywatny)
- Mikro-model na urządzeniu (TensorFlow Lite / ONNX) dopasowujący kalibrację do zachowania użytkownika
- Chatbot pogodowy: „Czy muszę odkryć pomidory dziś wieczorem?" → odpowiedź na podstawie danych lokalnych
- Możliwość wdrożenia jako PRO+ tier w przyszłości

---

### 🟢 Niski próg wejścia – szybkie wdrożenie

#### 11. Widget Interaktywny (Android 12+)
- Checkbox „Użyłem maty" bezpośrednio z widgetu (bez otwierania app)
- Suwak czułości w widgecie
- Glance już jest, wystarczy rozszerzyć o ActionCallback

#### 12. Powiadomienie Poranne „Dzień Dobry"
- `MorningBriefWorker` już istnieje – rozszerzyć o: prognoza na dziś + najzimniejsza noc tygodnia + porada sezonowa
- Opcjonalne TTS czytanie poranka podczas ładowania telefonu

#### 13. Paleta Motywów
- Tryb „Zimowy" (niebiesko-biały), „Jesienny" (brązowo-pomarańczowy), „Letni" (zielony)
- Dynamic Colors już jest; dodać ręczne palety jako PRO customization
- Ikona aplikacyjna adaptacyjna zmieniająca kolor wg sezonu

#### 14. Tryb Offline z Cache
- Przechowuj ostatnią prognozę 48h i wyświetlaj ze znacznikiem „brak połączenia – dane sprzed Xh"
- `NetworkMonitor` już jest, brakuje UI komunikacji o braku sieci z fallbackiem na cache

#### 15. Skróty Siri / Google Assistant
- „Hej Google, czy dziś będzie szron?" → integracja z App Actions (actions.xml)
- Integracja z konwersacją głosową przez istniejący TtsHelper

---

## 📈 Strategia Pozyskiwania Użytkowników

### Kanał 1 – Google Play Store Optimization (ASO)

| Element | Działanie |
|---|---|
| **Tytuł** | „SzronAlert – Alarm Szronowy i Pogoda" |
| **Krótki opis** | Dodać słowa kluczowe: szron, przymrozki, ogród, samochód, oblodzenie, temperatura |
| **Screenshoty** | Pokazuj rzeczywiste alertu + widget w kontekście (smartfon na desce rozdzielczej auta) |
| **Film promocyjny** | 30-sekundowe wideo: poranek, szron na szybie, aplikacja mówi „masz 2 godziny" |
| **Oceny** | Po 3. trafnej prognozie wyświetlaj in-app prompt do oceny (In-App Review API) |
| **Lokalizacja** | Priorytet: DE, AT, CZ, SK – tam silna kultura samochodowa + chłodny klimat |

### Kanał 2 – Content Marketing

- **Blog / Substack**: „Dlaczego zwykła prognoza temperatury kłamie o szronie" – edukacja fizyki atmosferycznej
- **YouTube Shorts / TikTok**: nagrania poranne ze szronem + overlay z aplikacją
- **Reddit**: r/Android, r/gardening, r/Poland, r/motoPL – organiczne wpisy bez spamu
- **Twitter/X**: mapa pogodowa szronu przed każdym twardym mrozem (screenshoty z app)

### Kanał 3 – Partnerstwa i B2B

| Partner | Model |
|---|---|
| **Sklepy ogrodnicze** (Leroy Merlin, Castorama, OBI) | Promocja w aplikacji sklepu, link do SzronAlert na stronie produktu |
| **Stacje meteorologiczne hobbystyczne** (Ecowitt, Davis) | Integracja z API ich urządzeń → cross-promo |
| **Aplikacje rolnicze** (AgroNawigator, eAgro) | Licencja na algorytm szronowy jako API |
| **Ubezpieczyciele** (agrarne OC) | Alert przymrozkowy jako benefit dla posiadaczy polisy uprawowej |
| **Zarządcy dróg / gminy** | B2B API: dane szronowe dla służb drogowych |

### Kanał 4 – Programy Polecające

- **Invite-a-Friend**: 30-dniowy PRO trial dla zapraszającego i zaproszonego
- **„Sezon razem"**: kup roczny PRO, podaruj drugi komuś bliskiemu (gift subscription)
- **Influencerzy ogrodniczy**: kody rabatowe dla YouTuberów z kanałami ogrodniczymi (100k+ sub.)

### Kanał 5 – Sezonowość

Szron jest sezonowy – wykorzystaj to:

| Miesiąc | Działanie marketingowe |
|---|---|
| **Wrzesień** | „Przygotuj ogród na zimę" – kampania App/Play Ads |
| **Październik** | „Pierwszy szron w tym roku" – push do nieaktywnych użytkowników |
| **Listopad** | Black Friday: lifetime PRO –30% |
| **Marzec** | „Wiosenne przymrozki – największe zagrożenie dla ogrodu" – kampania |
| **Maj** | Lody na kiju – zimna Zośka, Pankracy, Serwacy (tradycyjne przymrozki PL) – posty |

---

## 🔑 Kluczowe Metryki Sukcesu (po Fazie 5)

| Metryka | Target 6 miesięcy | Target 12 miesięcy |
|---|---|---|
| Aktywni użytkownicy miesięcznie (MAU) | 5 000 | 25 000 |
| Retencja D7 | ≥ 25% | ≥ 35% |
| Retencja D30 | ≥ 10% | ≥ 18% |
| Konwersja Free → PRO | ≥ 2% | ≥ 5% |
| Ocena w Play Store | ≥ 4,3★ | ≥ 4,5★ |
| Crash-free sessions | ≥ 99,5% | ≥ 99,8% |
| Przychód miesięczny (MRR) | 500 PLN | 5 000 PLN |

---

## ⚙️ Dług Techniczny do Spłaty (priorytetyzacja)

| Dług | Priorytet | Opis |
|---|---|---|
| Włączenie szyfrowania SQLCipher | **P0** | Biblioteka obecna, brakuje `SupportFactory` w `FrostDatabase` |
| Migracja Room 8 → 9 | **P0** | Następna zmiana schematu DB wymaga `MIGRATION_8_9` |
| Telemetria Firebase | **P0** | Brak danych = brak możliwości podejmowania decyzji |
| Testy integracyjne widgetów | **P1** | Stale widget rate wymaga pokrycia testami |
| Lint gates w CI | **P1** | Przywrócić stopniowo po fazie 5 |
| Aktualizacja Robolectric do SDK 36 | **P2** | Aktualnie testy z `@Config(sdk = [33])` |
| Szyfrowanie transport (Certificate Pinning) | **P2** | Dla danych geofence i kalibracji na przyszły backend |

---

## 🗓️ Proponowany Harmonogram (po Fazie 5)

```
Q3 2025  │ Faza 6: Telemetria + In-App Review prompt
         │ Faza 7: Kalibracja osobista + prognoza probabilistyczna
─────────┤
Q4 2025  │ Sezon zimowy: kampania marketingowa + Black Friday PRO
         │ Wear OS tile + Android Auto (P2)
         │ Dashboard Sezonowy + share card
─────────┤
Q1 2026  │ Mapa potwierdzeń szronu (crowdsource)
         │ Partnerstwa B2B (sklepy ogrodnicze, stacje meteo)
         │ Tryb Rolniczy (appMode=2)
─────────┤
Q2 2026  │ Integracja Google Home / Home Assistant
         │ Tryb Offline z cache + Rich Notifications
         │ Ocena ekspansji: DE/AT/CZ jako priorytetowe rynki
```

---

## 📝 Priorytety dla Kolejnego Sprintu

1. ☐ Wdrożyć Firebase Analytics + Crashlytics (Faza 6)
2. ☐ Aktywować szyfrowanie SQLCipher (dług techniczny P0)
3. ☐ Dodać In-App Review prompt po 3 trafnych alertach
4. ☐ Rozbudować UI dla `UserPlant` (tryb ogrodowy 2.0 – retencja)
5. ☐ Prognoza probabilistyczna 0–100% na ekranie głównym
6. ☐ Widget interaktywny Android 12+ (checkbox „Użyłem maty")
7. ☐ Tryb Offline z fallbackiem na ostatnią prognozę w cache

---

*Dokument zaktualizowany: Maj 2026. Kolejna rewizja: przed otwarciem Open Testing.*
