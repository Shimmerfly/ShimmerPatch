package top.nkbe.npatch.ui

import androidx.activity.compose.BackHandler
import android.view.KeyEvent
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.state.ToggleableState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.ui.component.AppItem
import top.nkbe.npatch.ui.component.SearchBar
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import top.nkbe.npatch.ui.component.m3.DropdownAction
import top.nkbe.npatch.ui.component.m3.ExpressiveActionDropdown
import top.nkbe.npatch.ui.component.m3.BaseWidget
import top.nkbe.npatch.ui.component.m3.RadioButtonWidget
import top.nkbe.npatch.ui.component.m3.SettingsDialog
import top.nkbe.npatch.ui.component.m3.SwitchWidget

class SettingsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinaryActionDoesNotAnnounceAnUnselectedState() {
        var clicks = 0
        compose.setContent {
            MaterialExpressiveTheme {
                BaseWidget(title = "Clear cache", onClick = { clicks++ })
            }
        }
        compose.onNodeWithText("Clear cache")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun switchHasOneAccessibleStateAndTogglesFromItsWholeRow() {
        val enabled = mutableStateOf(false)
        compose.setContent {
            MaterialExpressiveTheme {
                SwitchWidget(title = "Installation notifications", checked = enabled.value, onCheckedChange = { enabled.value = it })
            }
        }
        compose.onNodeWithText("Installation notifications").assertIsOff().performClick()
        compose.onNodeWithText("Installation notifications").assertIsOn()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertCountEquals(1)
    }

    @Test fun radioSelectionExposesOnlyOneSelectedNode() {
        val selected = mutableStateOf(false)
        compose.setContent {
            MaterialExpressiveTheme {
                RadioButtonWidget(title = "System installer", selected = selected.value, onSelect = { selected.value = true })
            }
        }
        compose.onNodeWithText("System installer").performClick().assertIsSelected()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertCountEquals(1)
    }

    @Test fun searchPlaceholderFocusesThePersistentInput() {
        val query = mutableStateOf("")
        compose.setContent {
            MaterialExpressiveTheme {
                SearchBar(query = query.value, onQueryChange = { query.value = it }, label = "Search apps")
            }
        }
        compose.onNodeWithText("Search apps").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.mainClock.advanceTimeByFrame()
        compose.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test fun appSelectionAnnouncesItsCheckedStateFromTheRow() {
        val checked = mutableStateOf(false)
        compose.setContent {
            MaterialExpressiveTheme {
                AppItem(
                    icon = {}, label = "Example app", packageName = "org.example.app",
                    checked = checked.value, onClick = { checked.value = !checked.value },
                    trailingContent = { Checkbox(checked = checked.value, onCheckedChange = null) },
                )
            }
        }
        compose.onNodeWithText("Example app").assertIsOff().performClick().assertIsOn()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertCountEquals(1)
    }

    @Test fun ordinaryMenuActionDoesNotExposeSelectionState() {
        var clicks = 0
        compose.setContent {
            MaterialExpressiveTheme {
                Box {
                    ExpressiveActionDropdown(
                        expanded = true,
                        groups = listOf(listOf(DropdownAction("Optimize", Icons.Outlined.Speed) { clicks++ })),
                        onDismissRequest = {},
                        onAction = { it.onClick() },
                    )
                }
            }
        }
        compose.onNodeWithText("Optimize")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun dialogBackDismissesOnlyTheDialog() {
        val visible = mutableStateOf(true)
        var pageBacks = 0
        var dialogDismissals = 0
        var dialogView: View? = null
        compose.setContent {
            MaterialExpressiveTheme {
                BackHandler { pageBacks++ }
                SettingsDialog(
                    show = visible.value,
                    title = "Dialog back test",
                    onDismissRequest = { dialogDismissals++; visible.value = false },
                ) {
                    val view = LocalView.current
                    SideEffect { dialogView = view }
                    Text("Back should close this dialog")
                }
            }
        }
        compose.onNodeWithText("Dialog back test").assertExists()
        // Compose idleness alone does not mean the separate Android window has focus.
        compose.waitUntil(timeoutMillis = 5_000) { dialogView?.hasWindowFocus() == true }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        instrumentation.waitForIdleSync()
        compose.onNodeWithText("Dialog back test").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, dialogDismissals)
            assertEquals(0, pageBacks)
        }
    }

    @Test fun dialogIsRemovedWhenDismissed() {
        val visible = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialExpressiveTheme {
                SettingsDialog(
                    show = visible.value,
                    title = "Settings confirmation",
                    onDismissRequest = { visible.value = false },
                    dismissButton = {},
                    confirmButton = {
                        TextButton(onClick = { visible.value = false }) { Text("Confirm") }
                    },
                ) { Text("Dialog body") }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Dialog body").assertExists()
        compose.onNodeWithText("Confirm").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Dialog body").assertDoesNotExist()
    }
}
