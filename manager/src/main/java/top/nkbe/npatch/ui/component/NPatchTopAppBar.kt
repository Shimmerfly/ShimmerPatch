package top.nkbe.npatch.ui.component

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun NPatchTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    largeTitle: String = title,
    largeTitleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = 0.dp,
    navigationIconPadding: Dp = 0.dp,
    actionIconPadding: Dp = 0.dp,
    bottomContent: @Composable () -> Unit = {},
) {
    Column(modifier) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        title,
                        color = titleColor,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                    if (subtitle.isNotEmpty()) Text(subtitle, color = subtitleColor, style = MaterialTheme.typography.labelSmall)
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
            windowInsets = if (defaultWindowInsetsPadding) TopAppBarDefaults.windowInsets else WindowInsets(0),
        )
        bottomContent()
    }
}
