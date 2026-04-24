# Google Play Console - Instrukcja Konfiguracji Produktów

## Krok 1: Przygotowanie Konta

1. **Zaloguj się do Google Play Console**
   - URL: https://play.google.com/console/
   - Wybierz aplikację "Szron Alert" (pl.oki.frostalert)

2. **Zweryfikuj status konta deweloperskiego**
   - Upewnij się, że konto jest w pełni zweryfikowane
   - Wypełnione dane podatkowe i płatnicze
   - Zaakceptowane wszystkie umowy i regulaminy

## Krok 2: Konfiguracja Produktów In-App (Lifetime)

1. Przejdź do: **Monetyzacja > Produkty in-app**
2. Kliknij **"Utwórz produkt"**

### Produkt: PRO Lifetime

**Identyfikator produktu:** `frostalert_pro_lifetime`

**Dane podstawowe:**
- Nazwa: `FrostAlert PRO - Dożywotni`
- Opis: `Jednorazowy zakup. Wszystkie funkcje PRO na zawsze bez reklam.`
- Status: `Aktywny`
- Typ: `Zarządzany`

**Ceny:**
```
Polska (PLN):           199,99 zł
Czechy (CZK):          1499,00 Kč
Węgry (HUF):          21999,00 Ft
Rumunia (RON):          249,99 lei
Niemcy/Austria/Belgia:   59,99 €
Francja:                 59,99 €
Włochy/Hiszpania:        59,99 €
Wielka Brytania:         54,99 £
USA:                     64,99 $
Kanada:                  89,99 CA$
Australia:               99,99 A$
```

**Opcje zaawansowane:**
- Kwalifikowalność: Wszyscy użytkownicy
- Dostępność: Wszystkie kraje

---

## Krok 3: Konfiguracja Subskrypcji

1. Przejdź do: **Monetyzacja > Subskrypcje**
2. Kliknij **"Utwórz subskrypcję"**

### Subskrypcja 1: PRO Monthly

**Identyfikator subskrypcji:** `frostalert_pro_monthly`

**Dane podstawowe:**
- Nazwa: `FrostAlert PRO - Miesięcznie`
- Opis: `Subskrypcja miesięczna z automatycznym odnowieniem. Anuluj w każdej chwili.`
- Status: `Aktywny`

**Base Plan ID:** `monthly-base`
- Okres odnowienia: `1 miesiąc`
- Grace period: `7 dni` ✅ WŁĄCZ
- Account hold: `Włączone` ✅

**Faza cenowa 1 (Recurring):**
```
Okres rozliczeniowy: Co miesiąc
Ceny:
  Polska (PLN):           9,99 zł
  Czechy (CZK):          89,00 Kč
  Węgry (HUF):         1299,00 Ft
  Rumunia (RON):         14,99 lei
  Strefa Euro:            2,99 €
  Wielka Brytania:        2,99 £
  USA:                    3,49 $
  Kanada:                 4,49 CA$
  Australia:              4,99 A$
```

**Próbny okres (opcjonalnie):**
- ✅ **Zalecane:** 3-dniowy trial
- Cena: 0,00 (gratis)
- Po zakończeniu: Automatyczne przejście na płatną subskrypcję

---

### Subskrypcja 2: PRO Yearly

**Identyfikator subskrypcji:** `frostalert_pro_yearly`

**Dane podstawowe:**
- Nazwa: `FrostAlert PRO - Rocznie`
- Opis: `Subskrypcja roczna z najlepszą ceną. Oszczędź do 33% w porównaniu do miesięcznej!`
- Status: `Aktywny`

**Base Plan ID:** `yearly-base`
- Okres odnowienia: `1 rok (365 dni)`
- Grace period: `7 dni` ✅ WŁĄCZ
- Account hold: `Włączone` ✅

**Faza cenowa 1 (Recurring):**
```
Okres rozliczeniowy: Co rok
Ceny:
  Polska (PLN):          79,99 zł
  Czechy (CZK):         699,00 Kč
  Węgry (HUF):         9999,00 Ft
  Rumunia (RON):        119,99 lei
  Strefa Euro:           24,99 €
  Wielka Brytania:       24,99 £
  USA:                   27,99 $
  Kanada:                37,99 CA$
  Australia:             42,99 A$
```

**Próbny okres (opcjonalnie):**
- ✅ **Zalecane:** 7-dniowy trial dla rocznej
- Cena: 0,00 (gratis)
- Po zakończeniu: Automatyczne przejście na płatną subskrypcję roczną

---

## Krok 4: Konfiguracja Zaawansowana

### Grace Period (Prolongata)

Dla obu subskrypcji:
1. Włącz **Grace period**: 7 dni
2. Zachowanie: Użytkownik zachowuje dostęp do PRO przez 7 dni po nieudanej płatności
3. Powiadomienia: Google Play automatycznie przypomina o odnowieniu metody płatności

### Account Hold (Wstrzymanie Konta)

1. Włącz dla obu subskrypcji
2. Po grace period: Konto w trybie "hold" (user traci dostęp)
3. Po naprawieniu płatności: Automatyczna reaktywacja

### Anulowanie i Zwroty

**Polityka zwrotów:**
- 14 dni dla UE (wymóg prawny)
- Google Play automatycznie obsługuje zwroty zgodnie z prawem lokalnym

**Anulowanie subskrypcji:**
- Użytkownik może anulować w każdej chwili przez Play Store
- Dostęp PRO pozostaje do końca opłaconego okresu

---

## Krok 5: Testowanie z Test Accounts

### Dodanie testerów

1. Przejdź do: **Konfiguracja > Licencje testowe**
2. Dodaj adresy email testerów (konta Gmail)
3. Testerzy mogą testować zakupy bez faktycznych płatności

### Typy testów

**License Test Accounts:**
```
test@yourdomain.com
developer@yourdomain.com
```

**Zakupy testowe:**
- Nie są naliczane opłaty
- Pełna symulacja purchase flow
- Można testować refund/cancel

### Szybkie odnowienia (dla testów)

W testach subskrypcje odnowią się szybciej:
- Miesięczna → odnawia się co 5 minut
- Roczna → odnawia się co 30 minut

To pozwala przetestować cały cykl życia subskrypcji w krótkim czasie.

---

## Krok 6: Aktywacja Produktów

### Przed publikacją

1. **Sprawdź wszystkie produkty:**
   - Identyfikatory zgodne z kodem (nie ma literówek)
   - Ceny poprawnie ustawione dla wszystkich regionów
   - Opisy czytelne i zgodne z polityką

2. **Status produktów:**
   - Zmień status z "Wersja robocza" na **"Aktywny"**

3. **Publikacja:**
   - Produkty muszą być aktywne PRZED publikacją wersji aplikacji
   - Zalecane: Aktywuj produkty → Poczekaj 2-4h → Publikuj APK/AAB

---

## Krok 7: Weryfikacja w Aplikacji

### Checklist przed publikacją

- [ ] Wszystkie 3 produkty utworzone i aktywne
- [ ] Ceny ustawione dla wszystkich głównych regionów
- [ ] Grace period włączony (7 dni)
- [ ] Test accounts dodane i przetestowane
- [ ] Purchase flow działa w closed testing
- [ ] Restore purchases działa poprawnie
- [ ] Subscription management link działa
- [ ] Powiadomienia o odnowieniach działają (testowe)

### Test flow dla każdego produktu

**Monthly Subscription:**
1. Otwórz aplikację z test account
2. Kliknij "Odblokuj PRO"
3. Wybierz "PRO Miesięczny"
4. Potwierdź zakup (bez płatności - test account)
5. Sprawdź czy PRO jest aktywne
6. Poczekaj 5 minut → sprawdź czy odnowiło
7. Anuluj subskrypcję przez Play Store
8. Sprawdź czy nadal masz PRO do końca okresu

**Yearly Subscription:**
1. Powtórz proces jak dla monthly
2. Testowe odnowienie nastąpi po 30 minutach

**Lifetime Purchase:**
1. Otwórz z test account
2. Wybierz "PRO Dożywotni"
3. Potwierdź zakup
4. Sprawdź czy PRO aktywne
5. Odinstaluj aplikację
6. Zainstaluj ponownie
7. Kliknij "Przywróć zakupy"
8. Sprawdź czy PRO nadal aktywne

---

## Krok 8: Monitorowanie po Publikacji

### Metryki do śledzenia

**Play Console > Monetyzacja > Podsumowanie:**
- Purchase conversion rate
- Trial conversion rate
- Churn rate (monthly vs yearly)
- Revenue per paying user (ARPPU)
- Subscriber retention (cohort analysis)

**Rekomendowane działania:**

1. **Tydzień 1-2:**
   - Monitoruj błędy płatności
   - Sprawdzaj purchase error rate
   - Odpowiadaj na feedback o cenach

2. **Miesiąc 1:**
   - Analiza conversion rate
   - A/B test cen (jeśli conversion < 2%)
   - Sprawdź trial → paid conversion

3. **Miesiąc 2-3:**
   - Obserwuj churn rate
   - Optymalizuj messaging w aplikacji
   - Rozważ dodanie ofert promocyjnych

---

## Rozwiązywanie Problemów

### Problem: "Produkt nie znaleziony"

**Przyczyna:** Produkt nieaktywny lub niepoprawny ID

**Rozwiązanie:**
1. Sprawdź status produktu w Play Console (musi być "Aktywny")
2. Zweryfikuj Product ID w kodzie vs Console (case-sensitive!)
3. Poczekaj 2-4h po aktywacji produktu
4. Wyczyść cache Google Play na urządzeniu testowym

### Problem: "Błąd płatności" w testach

**Przyczyna:** Account nie dodane jako license tester

**Rozwiązanie:**
1. Dodaj email do License Test Accounts
2. Wyloguj/zaloguj się na urządzeniu
3. Poczekaj 15 minut na propagację

### Problem: Restore purchases nie działa

**Przyczyna:** Kupiono na innym koncie Google

**Rozwiązanie:**
1. Purchases są przypisane do konta Google
2. User musi używać tego samego konta
3. Sprawdź czy `queryPurchasesAsync()` jest wywoływane poprawnie

### Problem: Subskrypcja anulowana ale user ma dostęp

**To jest poprawne zachowanie:**
- Użytkownik zachowuje dostęp do końca opłaconego okresu
- `isAutoRenewing = false` ale `purchaseState = PURCHASED`
- Sprawdź `subscriptionState.isAutoRenewing` w kodzie

---

## Checklist Finalna przed Produkcją

- [ ] Wszystkie produkty aktywne w Play Console
- [ ] Ceny zatwierdzone przez managera/właściciela
- [ ] Test purchases zakończone sukcesem (3+ testers)
- [ ] Restore purchases przetestowane
- [ ] Grace period i account hold skonfigurowane
- [ ] Polityka prywatności zaktualizowana (subscription handling)
- [ ] Regulations.txt w Play Console (terms of subscription)
- [ ] Support email skonfigurowany dla billing issues
- [ ] Monitoring/analytics na miejscu (Firebase/Amplitude)
- [ ] Plan komunikacji z userami o nowych planach cenowych

---

## Dodatkowe Zasoby

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Subscription Best Practices](https://developer.android.com/google/play/billing/subscriptions)
- [Testing In-app Billing](https://developer.android.com/google/play/billing/test)
- [Managing Subscriptions](https://support.google.com/googleplay/android-developer/answer/6112423)

**Kontakt do wsparcia:**
- Play Console Support: https://support.google.com/googleplay/android-developer
- Billing issues: W Play Console > Pomoc > Skontaktuj się z zespołem wsparcia
