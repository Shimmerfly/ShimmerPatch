package top.nkbe.npatch.ui.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import org.lsposed.manager.ui.compose.repository.RepositoryScreen
import top.nkbe.npatch.ui.component.FloatingGlassBottomBar
import top.nkbe.npatch.ui.component.FloatingGlassBottomBarIcon
import top.nkbe.npatch.ui.component.FloatingGlassBottomBarItem
import top.nkbe.npatch.ui.component.FloatingGlassBottomBarLabel
import top.nkbe.npatch.ui.component.NPatchScaffold
import top.nkbe.npatch.ui.util.LocalFloatingGlassBottomBar
import top.nkbe.npatch.ui.util.LocalFloatingGlassBottomBarBlur
import top.nkbe.npatch.ui.util.backgroundAwareCardColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navigator: Navigator,
    selectedTab: Int = MainTab.Home.ordinal,
    selectedManageTab: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    onSelectedManageTabChange: (Int) -> Unit = {},
) {
    val tabs = MainTab.entries
    val safeSelectedTab = selectedTab.coerceIn(0, tabs.lastIndex)
    val stateHolder = rememberSaveableStateHolder()
    val useFloatingGlassBottomBar = LocalFloatingGlassBottomBar.current
    val useFloatingGlassBottomBarBlur = LocalFloatingGlassBottomBarBlur.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backdrop = if (useFloatingGlassBottomBarBlur) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }

    @Composable
    fun Page(page: Int) {
        when (tabs[page]) {
            MainTab.Home -> HomeScreen(
                navigator = navigator,
                onManageShortcut = { managePage ->
                    onSelectedManageTabChange(managePage)
                    onSelectedTabChange(MainTab.Manage.ordinal)
                },
                onRepoShortcut = { onSelectedTabChange(MainTab.Repo.ordinal) },
                onSettingsShortcut = { onSelectedTabChange(MainTab.Settings.ordinal) },
            )
            MainTab.Manage -> ManageScreen(
                navigator = navigator,
                selectedPage = selectedManageTab,
                onSelectedPageChange = onSelectedManageTabChange,
            )
            MainTab.Repo -> RepositoryScreen(navigator)
            MainTab.Settings -> SettingsScreen()
        }
    }

    @Composable
    fun CurrentPage() {
        stateHolder.SaveableStateProvider(tabs[safeSelectedTab].name) {
            Page(safeSelectedTab)
        }
    }

    if (!useFloatingGlassBottomBar) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                tabs.forEachIndexed { index, tab ->
                    val selected = safeSelectedTab == index
                    item(
                        selected = selected,
                        onClick = { onSelectedTabChange(index) },
                        icon = {
                            androidx.compose.material3.Icon(
                                if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { androidx.compose.material3.Text(stringResource(tab.labelRes)) },
                    )
                }
            },
        ) {
            CurrentPage()
        }
        return
    }

    NPatchScaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (useFloatingGlassBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 12.dp + WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding(),
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    FloatingGlassBottomBar(
                        selectedIndex = { safeSelectedTab },
                        onSelected = onSelectedTabChange,
                        tabsCount = tabs.size,
                        backdrop = backdrop,
                        isBlurEnabled = useFloatingGlassBottomBarBlur,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = safeSelectedTab == index
                            val label = stringResource(tab.labelRes)
                            FloatingGlassBottomBarItem(
                                onClick = { onSelectedTabChange(index) },
                                selected = isSelected,
                                label = label,
                            ) {
                                FloatingGlassBottomBarIcon(
                                    selected = isSelected,
                                    selectedIcon = tab.selectedIcon,
                                    unselectedIcon = tab.unselectedIcon,
                                )
                                FloatingGlassBottomBarLabel(label)
                            }
                        }
                    }
                }
            } else {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = safeSelectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { onSelectedTabChange(index) },
                            icon = {
                                androidx.compose.material3.Icon(
                                    if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { androidx.compose.material3.Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .then(
                    if (useFloatingGlassBottomBar &&
                        useFloatingGlassBottomBarBlur &&
                        backdrop != null
                    ) {
                        Modifier.layerBackdrop(backdrop)
                    } else {
                        Modifier
                    },
                )
                // Floating glass must sample page content behind itself. Applying Scaffold's
                // bottom padding here clips the recorded backdrop above the navigation bar.
                .then(
                    if (useFloatingGlassBottomBar) Modifier
                    else Modifier.padding(bottom = padding.calculateBottomPadding())
                )
                .fillMaxSize(),
        ) {
            CurrentPage()
        }
    }
}
