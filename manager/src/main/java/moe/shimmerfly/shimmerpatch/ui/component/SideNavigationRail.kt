// Follows weishu/KernelSU's navigation rail
// (manager/app/src/main/java/me/weishu/kernelsu/ui/component/bottombar/NavigationRailMaterial.kt).
package moe.shimmerfly.shimmerpatch.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.config.Configs
import moe.shimmerfly.shimmerpatch.ui.page.MainTab

/**
 * The destinations of the main screen as a permanent rail, for windows wide enough to keep the
 * navigation beside the content instead of below it.
 *
 * <p>Its width follows the expressive rail: collapsed it shows an icon above each label, expanded
 * it also shows them side by side. The header collapses and expands it, and the choice outlives
 * the process.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SideNavigationRail(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberWideNavigationRailState(
        initialValue = if (Configs.navigationRailExpanded) {
            WideNavigationRailValue.Expanded
        } else {
            WideNavigationRailValue.Collapsed
        },
    )
    val expanded = state.targetValue == WideNavigationRailValue.Expanded
    LaunchedEffect(state.targetValue) {
        Configs.navigationRailExpanded = state.targetValue == WideNavigationRailValue.Expanded
    }
    val scope = rememberCoroutineScope()

    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = state,
        colors = WideNavigationRailDefaults.colors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        // The rail owns the start edge, so it takes the cutout and system bar insets there.
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
        contentPadding = PaddingValues(vertical = 20.dp),
        header = {
            IconButton(
                modifier = Modifier.padding(start = 24.dp),
                onClick = { scope.launch { if (expanded) state.collapse() else state.expand() } },
            ) {
                Icon(
                    if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = stringResource(
                        if (expanded) R.string.nav_rail_collapse else R.string.nav_rail_expand
                    ),
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selected,
                onClick = { if (!selected) onSelected(index) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
