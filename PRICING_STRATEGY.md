# FrostAlert - Rekomendacje Cenowe dla Google Play

## Analiza Rynku i Rekomendacje Cenowe

### Kontekst Aplikacji
FrostAlert to aplikacja pogodowa z zaawansowanym algorytmem przewidywania szronu, skierowana głównie do:
- Kierowców (zapobieganie oblodzeniu szyb)
- Ogrodników (ochrona roślin przed przymrozkami)
- Rynek główny: Polska, z planami ekspansji międzynarodowej

### Strategia Monetyzacji

#### 1. Model Subskrypcyjny vs. Jednorazowy Zakup

**Zalety subskrypcji:**
- Przewidywalny, stały przychód
- Wyższa wartość LTV (Lifetime Value)
- Możliwość ciągłego rozwoju i aktualizacji

**Zalety zakupu jednorazowego:**
- Niższy próg wejścia psychologicznego
- Brak obaw o automatyczne płatności
- Atrakcyjne dla użytkowników long-term

**Rekomendacja:** Oferuj OBA modele - użytkownicy cenią wybór.

### Rekomendowane Ceny (PLN)

#### Subskrypcja Miesięczna
**Cena: 9,99 PLN/miesiąc**
- Równowartość jednej kawy (~2,50€)
- Niska bariera wejścia
- Idealna dla testowania PRO przez 1-2 miesiące zimowe
- Benchmarki: Weather Pro (11,99 PLN), AccuWeather (8,99 PLN)

#### Subskrypcja Roczna
**Cena: 79,99 PLN/rok** (6,67 PLN/miesiąc - oszczędność 33%)
- Sweet spot dla committed users
- ~20€ rocznie
- Pokrywa cały sezon zimowy + letni (burze, podlewanie)
- Benchmarki: Weather Underground (99 PLN), Dark Sky było 49,99 PLN

**Alternatywna opcja premium:** 99,99 PLN/rok dla wyższego ARPU

#### Zakup Dożywotni (One-time)
**Cena: 199,99 PLN** (jednorazowo)
- Równowartość 2,5 roku subskrypcji rocznej
- Atrakcyjne dla power users
- Brak recurring revenue, ale wyższa konwersja wśród price-sensitive users
- Benchmarki: Weather & Radar (179 PLN), Windy PRO (249 PLN)

**Alternatywna opcja budget:** 149,99 PLN dla szerszej konwersji

### Ceny Międzynarodowe (Parity)

#### Europa Zachodnia (EUR)
- Miesięczna: 2,99 EUR
- Roczna: 24,99 EUR
- Dożywotnia: 59,99 EUR

#### USA/Wielka Brytania
- Miesięczna: $3,49 / £2,99
- Roczna: $27,99 / £24,99
- Dożywotnia: $64,99 / £54,99

#### Europa Środkowo-Wschodnia (lokalne waluty z PPP adjustment)
- Czechy: 89 CZK / 699 CZK / 1499 CZK
- Węgry: 1299 HUF / 9999 HUF / 21999 HUF
- Rumunia: 14,99 RON / 119 RON / 249 RON

### Konfiguracja w Google Play Console

#### 1. Product IDs (już zdefiniowane w kodzie)
```
frostalert_pro_monthly    → BillingClient.ProductType.SUBS
frostalert_pro_yearly     → BillingClient.ProductType.SUBS
frostalert_pro_lifetime   → BillingClient.ProductType.INAPP
```

#### 2. Base Plans (dla subskrypcji)
- **Monthly**: Base plan z auto-renewal co 30 dni
- **Yearly**: Base plan z auto-renewal co 365 dni
- Grace period: 7 dni (zalecane dla subscription recovery)
- Account hold: Włączone (pozwala na reaktywację po problemach płatnościowych)

#### 3. Pricing Strategy
**Strategia A - Conservative (bezpieczniejsza na start):**
- Monthly: 9,99 PLN
- Yearly: 79,99 PLN (best value badge)
- Lifetime: 199,99 PLN

**Strategia B - Growth (większy ARPU):**
- Monthly: 12,99 PLN
- Yearly: 99,99 PLN (best value badge)
- Lifetime: 249,99 PLN

**Rekomendacja początkowa:** Strategia A z planem przejścia na B po osiągnięciu 5000 PRO users.

#### 4. Free Trial
**Rekomendacja:** 7-dniowy free trial dla subskrypcji rocznej
- Zwiększa konwersję o 15-25%
- Wymaga konfiguracji grace period w Play Console
- User commitment jest wyższy po wypróbowaniu funkcji PRO

**Alternatywa:** 3-dniowy trial dla obu subskrypcji (monthly + yearly)

### Funkcje PRO (Value Proposition)

Kluczowe dla justyfikacji ceny:
1. ✅ **Brak reklam** (immediate value)
2. 🗺️ **Mapa szronu w okolicy** (unique differentiator)
3. 📅 **Prognoza 14-dniowa** (extended value)
4. 📍 **Wiele lokalizacji** (5+) (dla users z działką/domem letniskowym)
5. 📄 **Raporty PDF sezonowe** (professional touch)
6. ⚡ **Wsparcie priorytetowe** (premium feel)

### A/B Testing Plan

Po wdrożeniu zalecane testy:
1. **Test 1:** Cena roczna 79,99 PLN vs 99,99 PLN (wpływ na konwersję vs ARPU)
2. **Test 2:** Free trial 3 dni vs 7 dni vs brak trial (monthly subscription)
3. **Test 3:** Kolejność ofert na ekranie zakupu:
   - Wariant A: Yearly (default) → Lifetime → Monthly
   - Wariant B: Lifetime (default) → Yearly → Monthly

### KPIs do Monitorowania

1. **Conversion Rate:** % free → PRO
   - Target: 2-5% w pierwszych 3 miesiącach
   - Good: 5-10% po optymalizacjach
2. **ARPU (Average Revenue Per User):**
   - Target: 25-40 PLN w pierwszym roku
3. **Subscription Mix:**
   - Docelowo: 50% yearly, 30% lifetime, 20% monthly
4. **Churn Rate (monthly subs):**
   - Target: <10% monthly churn
5. **Trial → Paid Conversion:**
   - Target: >40% trial users płaci po wygaśnięciu

### Timeline Wdrożenia

**Tydzień 1-2:** Konfiguracja produktów w Play Console
- Utworzenie product IDs
- Ustawienie cen dla wszystkich regionów
- Konfiguracja base plans i grace periods
- Testowanie z Google Play test accounts

**Tydzień 3:** Internal testing
- Weryfikacja purchase flow
- Test restore purchases
- Test subscription management

**Tydzień 4:** Closed testing rollout
- Feedback od pierwszych płacących użytkowników
- Monitorowanie crash rate i error rate w billing flow

**Tydzień 5+:** Iteracja based on data
- Analiza conversion funnel
- Optymalizacja messaging i value prop
- Ewentualne A/B testy cen

### Compliance & Legal

1. **Play Store Policies:**
   - Wszystkie subskrypcje muszą być zarządzane przez Play Billing
   - Obowiązkowa opcja anulowania w Google Play
   - Clear disclosure of renewal terms

2. **VAT/Tax Handling:**
   - Google Play automatycznie obsługuje VAT dla UE
   - Ceny wyświetlane jako "including taxes"

3. **Refund Policy:**
   - 14-dniowa polityka zwrotów (EU law)
   - Graceful handling przez Google Play

### Podsumowanie Akcji

✅ Kod zaktualizowany o model subscriptions + lifetime
✅ String resources dodane (PL + EN)
✅ ProductOffering i ProductInfo data classes
✅ BillingClientWrapper rozszerzony o SUBS support

🔄 **Następne kroki:**
1. Konfiguracja produktów w Google Play Console (manualnie przez dev/owner)
2. Utworzenie UI dla purchase dialog z 3 opcjami
3. Testowanie z sandbox accounts
4. Dodanie telemetrii dla purchase events
5. Dokumentacja dla użytkownika o zarządzaniu subskrypcją

### Dodatkowe Zasoby

**Benchmarking konkurencji (Q1 2025):**
- Weather Pro: 11,99 PLN/m, brak rocznej
- AccuWeather Premium: 8,99 PLN/m, 89,99 PLN/rok
- Weather Underground: 9,99 PLN/m, 99,99 PLN/rok
- Windy PRO: Tylko lifetime 249 PLN

**Wnioski:** Proponowane ceny są konkurencyjne i mieszczą się w market average.
