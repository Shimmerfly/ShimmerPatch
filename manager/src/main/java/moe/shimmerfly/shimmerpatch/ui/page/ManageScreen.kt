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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlin.math.max
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
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
import kotlinx.coroutines.channels.Channel
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
    /** The pager the bottom bar drives, which this screen's own pager hands its swipes over to. */
    screenPagerState: PagerState,
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
    // One continuous gesture across two pagers: the inner one keeps the drag while it still has a
    // page to reach, and the moment it is at an edge the leftover travels to the screen pager. So
    // swiping on 应用 stops at 模块, and only a swipe that starts from 模块 carries on to 设置.
    val snapRequests = remember { Channel<Int>(Channel.CONFLATED) }
    LaunchedEffect(screenPagerState, snapRequests) {
        // The move is animated from here rather than inside the gesture: a fling's own scope can
        // end with the finger, which used to cut the animation short and leave the pager parked
        // between two pages.
        for (target in snapRequests) {
            screenPagerState.animateScrollToPage(target.coerceIn(0, screenPagerState.pageCount - 1))
        }
    }
    // One continuous gesture across two pagers: this screen's pager keeps the drag while it still
    // has a page to reach, and the moment it is at an edge the leftover travels to the screen pager.
    // A hand-off that got anything at all always lands exactly one page away from where the gesture
    // started - short swipe or long, the page changes once.
    val handOff = remember(pagerState, screenPagerState, snapRequests) {
        object : NestedScrollConnection {
            // What this gesture handed to the screen pager so far, in its own scroll direction.
            private var handed = 0f
            private var startPage = -1

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.x == 0f) return Offset.Zero
                if (startPage < 0) startPage = screenPagerState.currentPage
                // Nested scroll reports where the finger went; dispatchRawDelta wants the scroll
                // that follows from it, which runs the other way. One gesture carries the screen
                // pager at most one page over, however far the finger travelled.
                val requested = -available.x
                val room = screenPagerState.layoutInfo.pageSize.toFloat() - abs(handed)
                val delta = requested.coerceIn(-max(room, 0f), max(room, 0f))
                if (delta == 0f) return Offset.Zero
                val taken = screenPagerState.dispatchRawDelta(delta)
                handed += taken
                return Offset(taken, 0f)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (handed != 0f && startPage >= 0) {
                    snapRequests.trySend(if (handed > 0f) startPage + 1 else startPage - 1)
                }
                handed = 0f
                startPage = -1
                return Velocity.Zero
            }
        }
    }
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
    // A fast flick can end without the hand-off's settle ever running, which leaves the screen
    // pager parked between two pages. Once this screen's own pager has stopped moving, finish the
    // move - the last page it asked for, or the nearest one.
    LaunchedEffect(pagerState, screenPagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling && !controller.isNavigating &&
                    !screenPagerState.isScrollInProgress &&
                    abs(screenPagerState.currentPageOffsetFraction) > 0.001f
                ) {
                    // Nothing asked for a page, so finish on the nearest one.
                    snapRequests.trySend(screenPagerState.currentPage)
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
            modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop).nestedScroll(handOff),
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
