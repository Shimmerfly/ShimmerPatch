// Ported from weishu/KernelSU (manager/app/src/main/java/me/weishu/kernelsu/ui/util/WindowSize.kt).
package moe.shimmerfly.shimmerpatch.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Whether the window is wide enough to keep a navigation rail beside the content instead of a
 * bottom bar.
 *
 * <p>A window is split once it reaches the expanded breakpoint, or once it is merely medium but
 * wider than it is tall, because a landscape window has room to spare on its sides.
 */
@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalResources.current.displayMetrics.density
    val widthDp = windowInfo.containerSize.width / density
    val heightDp = windowInfo.containerSize.height / density
    return widthDp >= 840f || (widthDp >= 600f && heightDp / widthDp < 1.2f)
}
