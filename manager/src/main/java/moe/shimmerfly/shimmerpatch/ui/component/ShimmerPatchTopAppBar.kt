package moe.shimmerfly.shimmerpatch.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Shared title motion is copied from WeKit's ExpressiveCollapsingTopAppBar. */
@Composable
fun ShimmerPatchTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    bottomContent: @Composable () -> Unit = {},
) {
    Column(modifier) {
        ExpressiveCollapsingTopAppBar(
            title = title,
            scrollBehavior = scrollBehavior ?: TopAppBarDefaults.pinnedScrollBehavior(),
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = color,
                scrolledContainerColor = color,
                titleContentColor = titleColor,
            ),
        )
        bottomContent()
    }
}
