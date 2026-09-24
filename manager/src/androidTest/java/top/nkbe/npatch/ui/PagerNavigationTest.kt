package top.nkbe.npatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.nkbe.npatch.ui.page.MainPagerState
import top.nkbe.npatch.ui.page.rememberMainPagerState

/** Regression: a second state writer used to snap/cancel tab animation; rapid input must settle. */
class PagerNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortcutSettlesInnerTabBeforeContinuingNavigation() {
        lateinit var controller: MainPagerState
        var pageAtContinuation: Int? = null
        var offsetAtContinuation: Float? = null
        compose.setContent {
            val pager = rememberPagerState(pageCount = { 2 })
            controller = rememberMainPagerState(pager)
            HorizontalPager(pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                Box(Modifier.fillMaxSize()) { Text("Page $page") }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            controller.snapToPage(1) {
                pageAtContinuation = controller.pagerState.currentPage
                offsetAtContinuation = controller.pagerState.currentPageOffsetFraction
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitUntil { pageAtContinuation != null }
        compose.runOnIdle {
            assertEquals(1, pageAtContinuation)
            assertEquals(0f, offsetAtContinuation)
            assertEquals(1, controller.selectedPage)
            assertEquals(false, controller.isNavigating)
        }
    }

    @Test fun rapidNavigationCancelsPreviousTargetAndReturnsToHome() {
        lateinit var controller: MainPagerState
        compose.setContent {
            val pager = rememberPagerState(pageCount = { 3 })
            controller = rememberMainPagerState(pager)
            HorizontalPager(pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 2) { page ->
                Box(Modifier.fillMaxSize()) { Text("Page $page") }
            }
        }
        compose.runOnIdle { controller.animateToPage(2) }
        compose.runOnIdle {
            assertEquals(2, controller.pagerState.settledPage)
            assertEquals(2, controller.selectedPage)
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { controller.animateToPage(0) }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { controller.animateToPage(1) }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { controller.animateToPage(0) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, controller.pagerState.settledPage)
            assertEquals(0, controller.selectedPage)
            assertEquals(false, controller.isNavigating)
        }
    }
}
