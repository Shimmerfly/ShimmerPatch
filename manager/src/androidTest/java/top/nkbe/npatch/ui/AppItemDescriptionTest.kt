package top.nkbe.npatch.ui

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.ui.component.AppItem

class AppItemDescriptionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortDescriptionHasNoSeparateActionAndKeepsCardGestures() {
        var clicks = 0
        var longClicks = 0
        compose.setContent {
            MaterialExpressiveTheme {
                AppItem(
                    icon = {},
                    label = "Module",
                    packageName = "org.example.module",
                    description = "Short description",
                    onClick = { clicks++ },
                    onLongPress = { longClicks++ },
                )
            }
        }

        val description = compose.onNodeWithText("Short description", useUnmergedTree = true)
        description.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        description.performTouchInput { click() }
        description.performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(1, longClicks)
        }
    }

    @Test fun longDescriptionExpandsAndCollapsesWithoutClickingCard() {
        var clicks = 0
        val text = "First line\nSecond line\nThird line"
        compose.setContent {
            MaterialExpressiveTheme {
                AppItem(
                    icon = {},
                    label = "Module",
                    packageName = "org.example.module",
                    description = text,
                    onClick = { clicks++ },
                )
            }
        }

        val description = compose.onNodeWithText(text, useUnmergedTree = true)
        description.assertHasClickAction()
        assertEquals(2, description.lineCount())
        description.performClick()
        assertEquals(3, description.lineCount())
        description.assertHasClickAction().performClick()
        assertEquals(2, description.lineCount())
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test fun changedDescriptionRecalculatesWhetherExpansionIsAvailable() {
        val longText = "First line\nSecond line\nThird line"
        val text = mutableStateOf(longText)
        compose.setContent {
            MaterialExpressiveTheme {
                AppItem(
                    icon = {},
                    label = "Module",
                    packageName = "org.example.module",
                    description = text.value,
                )
            }
        }

        compose.onNodeWithText(longText, useUnmergedTree = true).performClick()
        compose.runOnIdle { text.value = "Short description" }
        compose.onNodeWithText("Short description", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.runOnIdle { text.value = longText }
        val description = compose.onNodeWithText(longText, useUnmergedTree = true)
        description.assertHasClickAction()
        assertEquals(2, description.lineCount())
    }

    private fun SemanticsNodeInteraction.lineCount(): Int {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single().lineCount
    }
}
