package top.nkbe.npatch.ui

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.ui.page.LocalNavigator
import top.nkbe.npatch.ui.page.Navigator
import top.nkbe.npatch.ui.page.Route
import top.nkbe.npatch.ui.page.SelectAppsResult
import top.nkbe.npatch.ui.page.SelectAppsScreen

class SelectAppsScopeResultTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmationPreservesSelectedPackageMissingFromTheAppList() {
        val hiddenPackage = "top.nkbe.npatch.test.unavailable.scoped.module"
        val initialSelected = listOf(hiddenPackage)
        val navigator = Navigator(mutableListOf(Route.Main()))
        val result = mutableStateOf<SelectAppsResult?>(null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        compose.setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalNavigator provides navigator) {
                    LaunchedEffect(Unit) {
                        result.value = navigator.navigateForResult<SelectAppsResult>(
                            Route.SelectApps(multiSelect = true, initialSelected = initialSelected),
                        )
                    }
                    SelectAppsScreen(multiSelect = true, initialSelected = initialSelected)
                }
            }
        }

        val confirmLabel = context.getString(android.R.string.ok)
        compose.onNode((hasText(confirmLabel) or hasContentDescription(confirmLabel)) and hasClickAction())
            .performClick()
        compose.waitUntil { result.value != null }
        compose.runOnIdle {
            val selection = result.value as SelectAppsResult.MultipleApps
            assertEquals(initialSelected, selection.selectedPackageNames)
            assertTrue(selection.selected.isEmpty())
            assertEquals(listOf(Route.Main()), navigator.backStack)
        }
    }
}
