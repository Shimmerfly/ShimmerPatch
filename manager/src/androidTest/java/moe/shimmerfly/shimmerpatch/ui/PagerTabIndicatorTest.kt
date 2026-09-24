package moe.shimmerfly.shimmerpatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import moe.shimmerfly.shimmerpatch.ui.component.pagerTabIndicatorOffset

class PagerTabIndicatorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun indicatorFollowsUnfinishedDrag() = checkDrag(LayoutDirection.Ltr)

    @Test fun indicatorFollowsUnfinishedRtlDrag() = checkDrag(LayoutDirection.Rtl)

    private fun checkDrag(direction: LayoutDirection) {
        lateinit var pager: PagerState
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MaterialExpressiveTheme {
                    pager = rememberPagerState(pageCount = { 2 })
                    Column(Modifier.fillMaxSize()) {
                        PrimaryTabRow(
                            selectedTabIndex = pager.currentPage,
                            indicator = {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier.pagerTabIndicatorOffset(this, pager).testTag("indicator"),
                                    width = Dp.Unspecified,
                                )
                            },
                        ) {
                            listOf("Apps", "Modules").forEachIndexed { index, title ->
                                Tab(
                                    selected = pager.currentPage == index,
                                    onClick = {},
                                    modifier = Modifier.testTag("tab$index"),
                                    text = { Text(title) },
                                )
                            }
                        }
                        HorizontalPager(pager, Modifier.weight(1f).testTag("pager")) {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        val startX = compose.onNodeWithTag("tab0").fetchSemanticsNode().boundsInRoot.center.x
        val endX = compose.onNodeWithTag("tab1").fetchSemanticsNode().boundsInRoot.center.x
        val dragDirection = if (direction == LayoutDirection.Ltr) -1 else 1
        compose.onNodeWithTag("pager").performTouchInput {
            down(Offset(width * if (dragDirection < 0) 0.8f else 0.2f, center.y))
            moveBy(Offset(width * 0.3f * dragDirection, 0f))
        }

        fun assertIndicatorTracksPage() {
            var position = 0f
            compose.runOnIdle {
                position = pager.currentPage + pager.currentPageOffsetFraction
                assertTrue(pager.isScrollInProgress)
                assertEquals(0, pager.settledPage)
                assertTrue(position in 0.1f..0.9f)
            }
            val indicatorX = compose.onNodeWithTag("indicator").fetchSemanticsNode().boundsInRoot.center.x
            assertEquals(startX + (endX - startX) * position, indicatorX, 2f)
        }

        assertIndicatorTracksPage()
        compose.onNodeWithTag("pager").performTouchInput {
            moveBy(Offset(width * 0.3f * dragDirection, 0f))
        }
        assertIndicatorTracksPage()
        compose.onNodeWithTag("pager").performTouchInput { cancel() }
    }
}
