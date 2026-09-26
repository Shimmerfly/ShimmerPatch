package moe.shimmerfly.shimmerpatch.ui.page

import moe.shimmerfly.shimmerpatch.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 主页面底部导航标签枚举。
 */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(R.string.screen_home, Icons.Filled.Home, Icons.Outlined.Home),
    Manage(R.string.screen_manage, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Settings(R.string.screen_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}
