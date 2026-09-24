// Tab controller and floating navigation follow InstallerX-Revived / WeKit; see docs/UI_SOURCES.md.
package moe.shimmerfly.shimmerpatch.ui.page

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.distinctUntilChanged
import moe.shimmerfly.shimmerpatch.ui.component.*
import moe.shimmerfly.shimmerpatch.ui.util.*
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

@Composable
fun MainScreen(
    navigator: Navigator,
    selectedTab: Int = MainTab.Home.ordinal,
    selectedManageTab: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    onSelectedManageTabChange: (Int) -> Unit = {},
    onNavigationBarHeightChanged: (Dp) -> Unit = {},
) {
    val tabs = MainTab.entries
    val pager = rememberPagerState(initialPage = selectedTab.coerceIn(tabs.indices), pageCount = { tabs.size })
    val controller = rememberMainPagerState(pager)
    val managePager = rememberPagerState(initialPage = selectedManageTab.coerceIn(0, 1), pageCount = { 2 })
    val manageController = rememberMainPagerState(managePager)
    val onTabSettled by rememberUpdatedState(onSelectedTabChange)
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val focusManager = LocalFocusManager.current
    val selectTab: (Int) -> Unit = { index ->
        focusManager.clearFocus()
        onSelectedTabChange(index)
    }
    val floating = LocalFloatingGlassBottomBar.current
    val blur = LocalFloatingGlassBottomBarBlur.current
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val backdrop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isRenderEffectSupported()) rememberLayerBackdrop { drawRect(surface); drawContent() } else null

    // Only the controller writes PagerState. Clicks and shortcuts submit a destination.
    LaunchedEffect(selectedTab) { controller.animateToPage(selectedTab.coerceIn(tabs.indices)) }
    LaunchedEffect(pager, controller) {
        snapshotFlow { Triple(pager.settledPage, pager.isScrollInProgress, controller.isNavigating) }
            .distinctUntilChanged().collect { (page, scrolling, navigating) ->
                if (!scrolling && !navigating) {
                    if (page != currentSelectedTab) focusManager.clearFocus()
                    controller.syncPage()
                    onTabSettled(page)
                }
            }
    }

    val navigationItems: @Composable RowScope.() -> Unit = {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = controller.selectedPage == index,
                onClick = { selectTab(index) },
                icon = { Icon(if (controller.selectedPage == index) tab.selectedIcon else tab.unselectedIcon, null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }

    NPatchScaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (floating) {
                Box(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (backdrop != null) FloatingBottomBar(
                        items = tabs,
                        selectedIndex = { controller.selectedPage },
                        onSelected = selectTab,
                        backdrop = backdrop,
                        mode = if (blur) FloatingBottomBarMode.LiquidGlass else FloatingBottomBarMode.None,
                        iconContent = { tab, _, selected ->
                            Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = null)
                        },
                        labelContent = { tab, _ ->
                            Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium)
                        },
                    ) else {
                        // Same floating placement on older Android, using an opaque M3 surface.
                        NavigationBar(
                            modifier = Modifier.clip(CircleShape),
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            content = navigationItems,
                        )
                    }
                }
            } else {
                NavigationBar(content = navigationItems)
            }
        },
    ) { chromePadding ->
        // Keep all three page compositions and their scroll state alive. Content can pass behind
        // the floating bar; each list gets the measured chrome inset as scrollable end padding.
        val navigationHeight = chromePadding.calculateBottomPadding()
        SideEffect { onNavigationBarHeightChanged(navigationHeight) }
        val contentPadding = PaddingValues(bottom = navigationHeight)
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop),
            key = { tabs[it].name },
            beyondViewportPageCount = tabs.lastIndex,
        ) { page ->
            when (tabs[page]) {
                MainTab.Home -> HomeScreen(
                    navigator = navigator,
                    contentPadding = contentPadding,
                    onManageShortcut = { manageTab ->
                        manageController.snapToPage(manageTab) {
                            onSelectedManageTabChange(manageTab)
                            onSelectedTabChange(MainTab.Manage.ordinal)
                        }
                    },
                )
                MainTab.Manage -> ManageScreen(
                    navigator = navigator,
                    controller = manageController,
                    selectedPage = selectedManageTab,
                    onSelectedPageChange = onSelectedManageTabChange,
                    contentPadding = contentPadding,
                )
                MainTab.Settings -> SettingsScreen(contentPadding = contentPadding)
            }
        }
    }
}
