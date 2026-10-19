package za.co.giantautoupholstery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import za.co.giantautoupholstery.core.ui.EmptyState
import za.co.giantautoupholstery.core.ui.GiantTheme
import za.co.giantautoupholstery.core.ui.PasswordField
import za.co.giantautoupholstery.core.ui.ServiceCard
import za.co.giantautoupholstery.domain.Service
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithContentDescription

class ServiceCardUiTest {
    @get:Rule val rule = createComposeRule()
    private val service = Service("1", "Dining Chair Recovering", "Chairs", 35_000, "1–2 days", "New fabric on seats", null)

    @Test fun serviceCard_showsNamePriceAndHandlesClick() {
        var clicked = false
        rule.setContent { GiantTheme(false) { ServiceCard(service, onClick = { clicked = true }) } }
        rule.onNodeWithText("Dining Chair Recovering").assertIsDisplayed()
        rule.onNodeWithText("From R350").assertIsDisplayed()
        rule.onNodeWithText("Dining Chair Recovering").performClick()
        assertTrue(clicked)
    }

    @Test fun emptyState_displaysMessage() {
        rule.setContent { GiantTheme(true) { EmptyState("No matching services", "Try a different search or category.") } }
        rule.onNodeWithText("No matching services").assertIsDisplayed()
    }

    @Test fun passwordField_hasAccessibleVisibilityToggle() {
        val value = mutableStateOf("Secret123")
        rule.setContent { GiantTheme(false) { PasswordField(value.value, { value.value = it }, "Password") } }
        rule.onNodeWithContentDescription("Show password").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }
}
