package moe.shimmerfly.shimmerpatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import moe.shimmerfly.shimmerpatch.ui.component.NPatchPullToRefresh

/** Exercises the production refresh/app-bar nesting with one uninterrupted pointer gesture. */
class NestedRefreshTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var appBar: TopAppBarState
    private lateinit var refresh: PullToRefreshState
    private lateinit var list: LazyListState
    private var density = 1f
    private var refreshes = 0

    private fun showList(collapsed: Boolean = true, firstItem: Int = 0) {
        compose.setContent {
            MaterialExpressiveTheme {
                density = LocalDensity.current.density
                appBar = rememberTopAppBarState(
                    initialHeightOffsetLimit = -px(120f),
                    initialHeightOffset = if (collapsed) -px(120f) else 0f,
                )
                val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    state = appBar,
                    snapAnimationSpec = null,
                    flingAnimationSpec = null,
                )
                refresh = rememberPullToRefreshState()
                list = rememberLazyListState(initialFirstVisibleItemIndex = firstItem)
                var refreshing by remember { mutableStateOf(false) }
                NPatchPullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = { refreshes++; refreshing = true },
                    scrollBehavior = behavior,
                    pullToRefreshState = refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("list")) {
                        items(40) { Box(Modifier.fillMaxWidth().height(80.dp)) }
                    }
                }
            }
        }
    }

    @Test fun continuousPullExpandsAppBarThenPullsRefreshAndRefreshesOnlyOnRelease() {
        showList()
        compose.onNodeWithTag("list").performTouchInput {
            down(Offset(center.x, px(40f)))
            moveBy(Offset(0f, px(60f)))
        }
        compose.runOnIdle {
            assertTrue(appBar.heightOffset > appBar.heightOffsetLimit)
            assertTrue(appBar.heightOffset < 0f)
            assertEquals(0f, refresh.distanceFraction, 0.001f)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, px(90f))) }
        compose.runOnIdle {
            assertEquals(0f, appBar.heightOffset, 0.001f)
            assertTrue(refresh.distanceFraction > 0f)
            assertTrue(refresh.distanceFraction < 1f)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, px(180f))) }
        compose.runOnIdle {
            assertTrue(refresh.distanceFraction > 1f)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithTag("list").performTouchInput { up() }
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun scrolledListConsumesDownwardDragBeforeAppBarOrRefresh() {
        showList(firstItem = 3)
        compose.onNodeWithTag("list").performTouchInput {
            down(Offset(center.x, px(40f)))
            moveBy(Offset(0f, px(60f)))
        }
        compose.runOnIdle {
            assertTrue(list.firstVisibleItemIndex < 3)
            assertTrue(list.canScrollBackward)
            assertEquals(appBar.heightOffsetLimit, appBar.heightOffset, 0.001f)
            assertEquals(0f, refresh.distanceFraction, 0.001f)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithTag("list").performTouchInput { up() }
    }

    @Test fun reversingPullRetractsRefreshBeforeCollapsingAppBar() {
        showList(collapsed = false)
        compose.onNodeWithTag("list").performTouchInput {
            down(Offset(center.x, px(40f)))
            moveBy(Offset(0f, px(100f)))
        }
        var initialFraction = 0f
        compose.runOnIdle {
            initialFraction = refresh.distanceFraction
            assertTrue(initialFraction > 0f)
        }
        compose.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, -px(30f))) }
        compose.runOnIdle {
            assertTrue(refresh.distanceFraction > 0f)
            assertTrue(refresh.distanceFraction < initialFraction)
            assertEquals(0f, appBar.heightOffset, 0.001f)
        }
        compose.onNodeWithTag("list").performTouchInput { moveBy(Offset(0f, -px(80f))) }
        compose.runOnIdle {
            assertEquals(0f, refresh.distanceFraction, 0.001f)
            assertTrue(appBar.heightOffset < 0f)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithTag("list").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, refreshes) }
    }

    private fun px(dp: Float) = dp * density
}
