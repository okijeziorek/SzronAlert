# Copilot Instructions (FrostAlert)

Ten plik jest skrotem dla Copilot. Pelne zasady i kontekst projektu sa w:
- `AGENTS.md`
- `DEVELOPMENT_PLAN.md`

## Priorytety pracy
1. Najpierw stabilnosc (`FrostCheckWorker`, migracje Room, widgety).
2. Potem niezawodnosc geofencingu i trendow 7-dniowych.
3. Dopiero potem rozszerzenia i monetyzacja.

## Zasady implementacyjne
- Trzymaj architekture: UI -> ViewModel -> Repository -> Data.
- Nie omijaj `SettingsDataStore` i `AppResult`.
- Po zapisie danych aktualizuj oba widgety (`FrostGlanceWidget` i `FrostWidgetProvider`).
- Dla WorkManager z Hilt trzymaj `@HiltWorker` i poprawny konstruktor `@Assisted`.

## Praktyka dla zmian
- Przy zmianach DB: dodaj migracje i test migracji.
- Przy zmianach trendu: dopisz testy edge-case (`UP/DOWN/STABLE`).
- Przy zmianach geofence: testuj zmiane promienia i lokalizacji manualnej.

