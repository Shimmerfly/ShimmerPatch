package top.nkbe.npatch.ui

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.ui.component.m3.CustomValueDialog

class CustomValueDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalidInputStaysOpenValidInputIsTrimmedAndCancelDiscardsEdits() {
        val visible = mutableStateOf(true)
        val stored = mutableStateOf("org.example.initial")
        val saved = mutableListOf<String>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val confirmLabel = context.getString(android.R.string.ok)
        val cancelLabel = context.getString(android.R.string.cancel)

        compose.setContent {
            MaterialExpressiveTheme {
                CustomValueDialog(
                    show = visible.value,
                    title = "Custom installer",
                    label = "Package name",
                    value = stored.value,
                    errorText = "Enter an installed package",
                    onDismissRequest = { visible.value = false },
                    validate = { it == "org.example.installer" },
                    onConfirm = { saved += it; stored.value = it },
                )
            }
        }

        compose.onNode(hasSetTextAction()).performTextReplacement("invalid")
        compose.onNodeWithText(confirmLabel).performClick()
        compose.onNodeWithText("Custom installer").assertExists()
        compose.onNodeWithText("Enter an installed package").assertExists()
        compose.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assertTextContains("invalid")
        compose.runOnIdle {
            assertEquals(emptyList<String>(), saved)
            assertEquals("org.example.initial", stored.value)
        }

        compose.onNode(hasSetTextAction()).performTextReplacement("  org.example.installer  ")
        compose.onNodeWithText(confirmLabel).performClick()
        compose.onNodeWithText("Custom installer").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("org.example.installer"), saved)
            visible.value = true
        }

        compose.onNode(hasSetTextAction()).assertTextContains("org.example.installer")
            .performTextReplacement("discard this draft")
        compose.onNodeWithText(cancelLabel).performClick()
        compose.onNodeWithText("Custom installer").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("org.example.installer"), saved)
            assertEquals("org.example.installer", stored.value)
            visible.value = true
        }
        compose.onNode(hasSetTextAction()).assertTextContains("org.example.installer")
    }
}
