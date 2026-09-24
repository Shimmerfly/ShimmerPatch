package top.nkbe.npatch.ui.component

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.lerp

/** Keeps Material's content-width indicator attached to the page throughout a drag or animation. */
fun Modifier.pagerTabIndicatorOffset(scope: TabIndicatorScope, pagerState: PagerState): Modifier =
    with(scope) {
        this@pagerTabIndicatorOffset.tabIndicatorLayout { measurable, constraints, tabPositions ->
            if (tabPositions.isEmpty()) return@tabIndicatorLayout layout(0, 0) {}

            val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, tabPositions.lastIndex.toFloat())
            val startIndex = position.toInt()
            val start = tabPositions[startIndex]
            val end = tabPositions[(startIndex + 1).coerceAtMost(tabPositions.lastIndex)]
            val fraction = position - startIndex
            val width = lerp(start.contentWidth, end.contentWidth, fraction).roundToPx()
            val offset = lerp(
                start.left + (start.width - start.contentWidth) / 2,
                end.left + (end.width - end.contentWidth) / 2,
                fraction,
            ).roundToPx()
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative(offset, 0)
            }
        }
    }
