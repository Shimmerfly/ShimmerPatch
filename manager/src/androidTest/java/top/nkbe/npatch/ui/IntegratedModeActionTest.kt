package top.nkbe.npatch.ui

import android.content.pm.ApplicationInfo
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import nkbe.util.NeoPackageManager.AppInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.R
import top.nkbe.npatch.ui.page.newpatch.PatchOptionsBody
import top.nkbe.npatch.ui.viewmodel.NewPatchViewModel
import top.nkbe.npatch.ui.viewmodel.NewPatchViewModel.PatchState
import top.nkbe.npatch.ui.viewmodel.NewPatchViewModel.ViewAction

class IntegratedModeActionTest {
    @get:Rule val compose = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    @After fun clearViewModels() {
        instrumentation.runOnMainSync { owner.viewModelStore.clear() }
    }

    @Test fun embeddedModuleActionExpandsInsideIntegratedModeAndUpdatesItsCount() {
        val context = instrumentation.targetContext
        val app = AppInfo(ApplicationInfo(context.applicationInfo), "Selected app", "fixture", 1L)
        lateinit var viewModel: NewPatchViewModel
        instrumentation.runOnMainSync {
            viewModel = ViewModelProvider.create(owner)[NewPatchViewModel::class]
            // ConfigurePatch runs on Main.immediate and only inspects the existing APK manifest.
            viewModel.dispatch(ViewAction.ConfigurePatch(app))
            assertEquals(PatchState.CONFIGURING, viewModel.patchState)
        }
        val localLabel = context.getString(R.string.patch_local)
        val integratedLabel = context.getString(R.string.patch_integrated)
        val embedLabel = context.getString(R.string.patch_embed_modules)
        var pickerRequests = 0
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                MaterialExpressiveTheme {
                    PatchOptionsBody(Modifier, onAddEmbed = { pickerRequests++ })
                }
            }
        }

        compose.onNodeWithText(localLabel).assertIsSelected()
        compose.onNodeWithText("$embedLabel (0)").assertDoesNotExist()
        compose.onNodeWithText(integratedLabel).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("$embedLabel (0)")
            .assertHasClickAction()
            .assert(hasAnyAncestor(hasText(integratedLabel)))
            .performClick()
        compose.runOnIdle {
            assertEquals(1, pickerRequests)
            viewModel.embeddedModules = listOf(app)
        }
        compose.onNodeWithText("$embedLabel (1)").assertHasClickAction()
        compose.onNodeWithText(localLabel).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("$embedLabel (1)").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(app), viewModel.embeddedModules)
            assertEquals(PatchState.CONFIGURING, viewModel.patchState)
        }
    }
}
