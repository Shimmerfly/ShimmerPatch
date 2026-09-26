// Tabs and retained pager ported from WeKit ui/agent/settings/PromptsScreen.kt.
package moe.shimmerfly.shimmerpatch.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchScaffold
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchTopAppBar
import moe.shimmerfly.shimmerpatch.ui.component.SearchBar
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarBlur
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarColor
import moe.shimmerfly.shimmerpatch.ui.component.m3BackdropLayer
import moe.shimmerfly.shimmerpatch.ui.component.pagerTabIndicatorOffset
import moe.shimmerfly.shimmerpatch.ui.component.rememberMaterial3BlurBackdrop
import moe.shimmerfly.shimmerpatch.ui.page.manage.AppManageBody
import moe.shimmerfly.shimmerpatch.ui.page.manage.AppManageFab
import moe.shimmerfly.shimmerpatch.ui.page.manage.ModuleManageBody
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.ModuleManageViewModel

@Composable
fun ManageScreen(
    navigator: Navigator,
    controller: MainPagerState,
    modifier: Modifier = Modifier,
    selectedPage: Int = 0,
    onSelectedPageChange: (Int) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val tabTitles = listOf(stringResource(R.string.apps), stringResource(R.string.modules))
    val safeSelectedPage = selectedPage.coerceIn(tabTitles.indices)
    val pagerState = controller.pagerState
    val onPageChanged by rememberUpdatedState(onSelectedPageChange)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val moduleManageViewModel = viewModel<ModuleManageViewModel>()
    val backdrop = rememberMaterial3BlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val bottomInset = maxOf(contentPadding.calculateBottomPadding(), WindowInsets.ime.asPaddingValues().calculateBottomPadding())

    // Share InstallerX's navigation controller with Main: clicks and Home shortcuts only
    // submit destinations, and user swipes are reported once the pager settles.
    LaunchedEffect(safeSelectedPage) { controller.animateToPage(safeSelectedPage) }
    LaunchedEffect(pagerState, controller) {
        snapshotFlow { Triple(pagerState.settledPage, pagerState.isScrollInProgress, controller.isNavigating) }
            .distinctUntilChanged()
            .collect { (page, scrolling, navigating) ->
                if (!scrolling && !navigating) {
                    controller.syncPage()
                    onPageChanged(page)
                }
            }
    }
    LaunchedEffect(pagerState.settledPage, ShizukuApi.isReady, moduleManageViewModel.enabledActivationPackagesKey) {
        if (pagerState.settledPage == 1) {
            moduleManageViewModel.refreshScopedActivationState()
            if (ShizukuApi.isReady) moduleManageViewModel.refreshEnabledActivations()
        }
    }

    ShimmerPatchScaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            ShimmerPatchTopAppBar(
                modifier = Modifier.m3AppBarBlur(backdrop),
                color = backdrop.m3AppBarColor(),
                title = stringResource(R.string.screen_manage),
                scrollBehavior = scrollBehavior,
                bottomContent = {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)).padding(horizontal = 12.dp).padding(bottom = 8.dp),
                        containerColor = Color.Transparent,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.pagerTabIndicatorOffset(this, pagerState),
                                width = Dp.Unspecified,
                            )
                        },
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { onPageChanged(index) },
                                text = { Text(title) },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = controller.selectedPage == 0,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                    scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                    scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
            ) {
                AppManageFab(navigator, Modifier.padding(bottom = bottomInset))
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop),
            beyondViewportPageCount = 1,
        ) { page ->
            val listPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + bottomInset +
                    if (page == 0) 96.dp else 16.dp,
            )
            when (page) {
                0 -> AppManageBody(navigator, scrollBehavior, searchQuery, listPadding)
                1 -> ModuleManageBody(scrollBehavior, searchQuery, listPadding, moduleManageViewModel)
            }
        }
    }
}
