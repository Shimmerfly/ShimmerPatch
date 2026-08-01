package top.nkbe.npatch.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nkbe.util.ShizukuApi
import top.nkbe.npatch.R
import top.nkbe.npatch.ui.component.NPatchScaffold
import top.nkbe.npatch.ui.component.floatingGlassBottomBarContentPadding
import top.nkbe.npatch.ui.component.PanelHeader
import top.nkbe.npatch.ui.component.SearchField
import top.nkbe.npatch.ui.component.compat.TabRow
import top.nkbe.npatch.ui.page.manage.AppManageBody
import top.nkbe.npatch.ui.page.manage.AppManageFab
import top.nkbe.npatch.ui.page.manage.ModuleManageBody
import top.nkbe.npatch.ui.util.LocalFloatingGlassBottomBar
import top.nkbe.npatch.ui.viewmodel.manage.ModuleManageViewModel
import top.nkbe.npatch.ui.viewmodel.manage.AppManageViewModel

/** Vector-style installed apps/modules panel. */
@Composable
fun ManageScreen(
    navigator: Navigator,
    selectedPage: Int = 0,
    onSelectedPageChange: (Int) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val tabTitles = listOf(stringResource(R.string.apps), stringResource(R.string.modules))
    val safeSelectedPage = selectedPage.coerceIn(0, tabTitles.lastIndex)
    val pagerState = rememberPagerState(safeSelectedPage) { tabTitles.size }
    val settledPage by remember(pagerState) { derivedStateOf { pagerState.settledPage } }
    var query by rememberSaveable { mutableStateOf("") }

    val moduleViewModel = viewModel<ModuleManageViewModel>()
    val appViewModel = viewModel<AppManageViewModel>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = rememberHazeState()
    val useFloatingGlassBottomBar = LocalFloatingGlassBottomBar.current

    LaunchedEffect(safeSelectedPage) {
        if (pagerState.currentPage != safeSelectedPage) pagerState.scrollToPage(safeSelectedPage)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect(onSelectedPageChange)
    }
    LaunchedEffect(
        settledPage,
        ShizukuApi.isReady,
        moduleViewModel.enabledActivationPackagesKey,
    ) {
        if (settledPage == 1) {
            moduleViewModel.refreshScopedActivationState()
            if (ShizukuApi.isReady) moduleViewModel.refreshEnabledActivations()
        }
    }

    NPatchScaffold(
        contentWindowInsets = WindowInsets.statusBars,
        floatingActionButton = {
            if (settledPage == 0) {
                AppManageFab(
                    navigator = navigator,
                    modifier = if (useFloatingGlassBottomBar) {
                        Modifier.padding(bottom = rememberFloatingBottomBarFabPadding())
                    } else Modifier,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            PanelHeader(
                title = tabTitles[settledPage],
                description = {
                    Text(
                        text = if (settledPage == 0) {
                            appViewModel.appList.size.toString()
                        } else {
                            val active = moduleViewModel.appList.count { it.activationEnabled }
                            stringResource(
                                R.string.manage_active_of,
                                active,
                                moduleViewModel.appList.size,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                search = {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.manage_search),
                    )
                },
            )

            TabRow(
                tabs = tabTitles,
                selectedTabIndex = settledPage,
                onTabSelected = { index ->
                    onSelectedPageChange(index)
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
            )

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = if (useFloatingGlassBottomBar) {
                        floatingGlassBottomBarContentPadding()
                    } else {
                        20.dp
                    },
                )
                when (page) {
                    0 -> AppManageBody(
                        navigator,
                        query,
                        contentPadding,
                        scrollBehavior,
                        hazeState,
                    )
                    1 -> ModuleManageBody(
                        query,
                        contentPadding,
                        scrollBehavior,
                        hazeState,
                        moduleViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFloatingBottomBarFabPadding() =
    68.dp + 12.dp + 8.dp + WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
