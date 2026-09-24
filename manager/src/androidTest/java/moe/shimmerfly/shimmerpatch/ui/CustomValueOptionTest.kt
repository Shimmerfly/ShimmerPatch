package moe.shimmerfly.shimmerpatch.ui

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import moe.shimmerfly.shimmerpatch.ui.component.m3.CustomValueOption

class CustomValueOptionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editingAndSelectingAreSeparateAndAnEmptyOptionCannotBeSelected() {
        val value = mutableStateOf("")
        val selected = mutableStateOf(false)
        var edits = 0
        var selections = 0
        compose.setContent {
            MaterialExpressiveTheme {
                CustomValueOption(
                    title = "Custom",
                    value = value.value,
                    selected = selected.value,
                    onEdit = { edits++ },
                    onSelect = { selections++; selected.value = true },
                )
            }
        }

        val radio = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        radio.assertIsNotEnabled().assertContentDescriptionEquals("Custom")
        compose.onNodeWithText("Custom")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        compose.runOnIdle {
            assertEquals(1, edits)
            assertEquals(0, selections)
            value.value = "https://dns.example/dns-query"
        }

        radio.assertIsEnabled().performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals(1, edits)
            assertEquals(1, selections)
        }
        compose.onNodeWithText("Custom").performClick()
        compose.runOnIdle {
            assertEquals(2, edits)
            assertEquals(1, selections)
        }
    }
}
