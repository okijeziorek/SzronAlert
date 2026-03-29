# Claude Instructions (FrostAlert)

Podstawowe zrodla prawdy:
- `AGENTS.md` (architektura, wzorce, gotchas)
- `DEVELOPMENT_PLAN.md` (fazy wdrozen)

## Szybkie reguly
- Zachowuj MVVM i przeplyw przez repository.
- Nie dodawaj hardcoded stringow do UI; uzywaj `res/values`.
- Traktuj widget Glance i AppWidget jako dwa rownorzedne pathy aktualizacji.
- Dla zmian w tle uwzgledniaj ograniczenia sieci i retry w WorkManager.

