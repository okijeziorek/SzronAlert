# FrostAlert Development Plan (Closed Testing)

Ten plan porządkuje dalszy rozwój aplikacji na etapy z jasnym zakresem, Definition of Done i metrykami.

## Faza 1 (P0): Stabilność tła i danych

### Zakres
- Utwardzenie `FrostCheckWorker` (retry/backoff, scenariusze offline, odporne logowanie błędów).
- Domknięcie migracji Room (brak utraty danych po aktualizacjach).
- Ograniczenie ciężkiej pracy na wątku głównym przy starcie i odświeżaniu.

### Definition of Done
- Worker nie wywala procesu przy błędach zewnętrznych API/DB.
- Wszystkie migracje między wspieranymi wersjami przechodzą testy.
- Brak regresji w zapisie `TemperatureRecord` i historii.

### Metryki sukcesu
- Crash-free users >= 99.5%
- ANR <= 0.47%
- Skuteczność cyklu workera >= 95%

## Faza 2 (P0): Niezawodność widgetu i geofencingu

### Zakres
- Synchronizacja update path: `FrostGlanceWidget` + `FrostWidgetProvider`.
- Rejestracja geofence reaktywna na zmiany ustawień i lokalizacji.
- Stabilizacja `GeofenceManager` dla ENTER/DWELL/EXIT i poprawnego radius.

### Definition of Done
- Widgety pokazują te same dane po każdym odświeżeniu.
- Geofence aktualizuje się po zmianie lokalizacji ręcznej i promienia.
- Brak crashy przy przejściach geofence.

### Metryki sukcesu
- Stale widget rate < 2%
- Geofence false-positive < 10%
- Skargi na baterię < 5% odpowiedzi ankietowych

## Faza 3 (P1): Jakość trendu 7-dniowego

### Zakres
- Dopracowanie `TrendCalculations` i mapowania danych do wykresu.
- Poprawa czytelności wykresów i podpisów osi.
- Obsługa edge-case: mało danych, skoki, STABLE/UP/DOWN.

### Definition of Done
- Wynik trendu deterministyczny dla tych samych danych.
- Wykres czytelny na różnych rozmiarach ekranu i skalach czcionki.
- Testy trendów pokrywają edge-case'y.

### Metryki sukcesu
- Zgłoszenia "dziwny trend" < 5%
- Pokrycie testów modułu trendu >= 85%

## Faza 4 (P0): Monetyzacja (AdMob + PRO)

### Zakres
- Ustabilizowanie integracji `BillingClientWrapper` i flow zakupu PRO.
- Uporządkowanie warunków wyświetlania reklam (baner/natywne) vs PRO.
- Przegląd placementu reklam pod UX i wydajność.

### Definition of Done
- Zakup, restore, cancel i błąd są poprawnie obsługiwane.
- Użytkownik PRO nie widzi reklam.
- Wersje testowe i produkcyjne mają poprawne ID AdMob.

### Metryki sukcesu
- Purchase success rate >= 90% (attempt -> ack)
- Błędy refund/debug purchase = 0
- Brak wzrostu ANR po włączeniu reklam

## Faza 5 (P1): Gotowość do rozszerzonych testów

### Zakres
- Telemetria kluczowych zdarzeń (worker, geofence, widget, billing).
- Rozszerzenie regresji: unit + androidTest + smoke checklist release.
- Stopniowy powrót do sensownych gate'ów lint.

### Definition of Done
- Proces release jest powtarzalny: build -> test -> rollout -> analiza.
- Najważniejsze scenariusze przechodzą automaty i checklisty manualne.
- Brak blokerów po rundzie closed testing.

### Metryki sukcesu
- Crash-free sessions >= 99.7%
- Retention D7 >= 25%
- Blokery przed open testing = 0

