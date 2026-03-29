# FrostAlert Development Plan (Closed Testing)

Ten plan porzadkuje dalszy rozwoj aplikacji na etapy z jasnym zakresem, Definition of Done i metrykami.

## Faza 1 (P0): Stabilnosc tla i danych

### Zakres
- Utwardzenie `FrostCheckWorker` (retry/backoff, scenariusze offline, odporne logowanie bledow).
- Domkniecie migracji Room (brak utraty danych po aktualizacjach).
- Ograniczenie ciezkiej pracy na watku glownym przy starcie i odswiezaniu.

### Definition of Done
- Worker nie wywala procesu przy bledach zewnetrznych API/DB.
- Wszystkie migracje miedzy wspieranymi wersjami przechodza testy.
- Brak regresji w zapisie `TemperatureRecord` i historii.

### Metryki sukcesu
- Crash-free users >= 99.5%
- ANR <= 0.47%
- Skutecznosc cyklu workera >= 95%

## Faza 2 (P0): Niezawodnosc widgetu i geofencingu

### Zakres
- Synchronizacja update path: `FrostGlanceWidget` + `FrostWidgetProvider`.
- Rejestracja geofence reaktywna na zmiany ustawien i lokalizacji.
- Stabilizacja `GeofenceManager` dla ENTER/DWELL/EXIT i poprawnego radius.

### Definition of Done
- Widgety pokazuja te same dane po kazdym odswiezeniu.
- Geofence aktualizuje sie po zmianie lokalizacji recznej i promienia.
- Brak crashy przy przejsciach geofence.

### Metryki sukcesu
- Stale widget rate < 2%
- Geofence false-positive < 10%
- Skargi na baterie < 5% odpowiedzi ankietowych

## Faza 3 (P1): Jakosc trendu 7-dniowego

### Zakres
- Dopracowanie `TrendCalculations` i mapowania danych do wykresu.
- Poprawa czytelnosci wykresow i podpisow osi.
- Obsluga edge-case: malo danych, skoki, STABLE/UP/DOWN.

### Definition of Done
- Wynik trendu deterministyczny dla tych samych danych.
- Wykres czytelny na roznych rozmiarach ekranu i skalach czcionki.
- Testy trendow pokrywaja edge-case'y.

### Metryki sukcesu
- Zgloszenia "dziwny trend" < 5%
- Pokrycie testow modulu trendu >= 85%

## Faza 4 (P0): Monetyzacja (AdMob + PRO)

### Zakres
- Ustabilizowanie integracji `BillingClientWrapper` i flow zakupu PRO.
- Uporzadkowanie warunkow wyswietlania reklam (baner/natywne) vs PRO.
- Przeglad placementu reklam pod UX i wydajnosc.

### Definition of Done
- Zakup, restore, cancel i blad sa poprawnie obslugiwane.
- Uzytkownik PRO nie widzi reklam.
- Wersje testowe i produkcyjne maja poprawne ID AdMob.

### Metryki sukcesu
- Purchase success rate >= 90% (attempt -> ack)
- Bledy refund/debug purchase = 0
- Brak wzrostu ANR po wlaczeniu reklam

## Faza 5 (P1): Gotowosc do rozszerzonych testow

### Zakres
- Telemetria kluczowych zdarzen (worker, geofence, widget, billing).
- Rozszerzenie regresji: unit + androidTest + smoke checklist release.
- Stopniowy powrot do sensownych gate'ow lint.

### Definition of Done
- Proces release jest powtarzalny: build -> test -> rollout -> analiza.
- Najwazniejsze scenariusze przechodza automaty i checklisty manualne.
- Brak blockerow po rundzie closed testing.

### Metryki sukcesu
- Crash-free sessions >= 99.7%
- Retention D7 >= 25%
- Blockery przed open testing = 0

