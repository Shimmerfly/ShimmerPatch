package moe.shimmerfly.shimmerpatch.ui

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.ConfiguringFab

class PatchAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun startPatchHasAnAccessibleLabelButtonActionAndFocus() {
        var label = ""
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            MaterialExpressiveTheme {
                val localizedLabel = stringResource(R.string.patch_start)
                val inputMode = LocalInputModeManager.current
                SideEffect { label = localizedLabel; inputModeManager = inputMode }
                ConfiguringFab()
            }
        }
        compose.waitForIdle()
        // Native buttons take keyboard focus in keyboard mode; touch-mode focus belongs
        // to editable controls. Accessibility label/action assertions apply in both modes.
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        // Exercise the actual FAB without invoking SubmitPatch or touching any APK.
        val button = compose.onNode(hasText(label) or hasContentDescription(label))
        button.assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus -> requestFocus() }
            .assertIsFocused()
    }
}
