package pl.oki.frostalert.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import pl.oki.frostalert.ui.theme.FrostAlertTheme

class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingSteps_clickingNext_changesContent() {
        var onFinishCalled = false
        composeTestRule.setContent {
            // Wyłączamy dynamicColor, aby uniknąć crashu w środowisku testowym
            FrostAlertTheme(dynamicColor = false) {
                OnboardingScreen(onFinish = { onFinishCalled = true })
            }
        }

        // Krok 0 - Witaj
        composeTestRule.onNodeWithText("Witaj we FrostAlert", substring = true).assertIsDisplayed()
        // Szukamy przycisku po tekście (może być "DALEJ" lub inny, sprawdzamy co jest w kodzie)
        composeTestRule.onNodeWithText("DALEJ", ignoreCase = true).performClick()

        // Krok 1 - Precyzyjny algorytm
        composeTestRule.onNodeWithText("Precyzyjny algorytm", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("DALEJ", ignoreCase = true).performClick()

        // Krok 2 - Alerty
        composeTestRule.onNodeWithText("Alerty w tle", substring = true).assertIsDisplayed()
        // Ostatni przycisk ma tekst z zasobów (ZACZYNAMY)
        composeTestRule.onNodeWithText("ZACZYNAMY", ignoreCase = true).performClick()

        // Weryfikacja zakończenia
        assert(onFinishCalled)
    }
}
