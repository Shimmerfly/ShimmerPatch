package top.nkbe.npatch.ui.page

import top.nkbe.npatch.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 主页面底部导航标签枚举。
 */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(R.string.screen_home, Icons.Rounded.Home, Icons.Rounded.Home),
    Manage(R.string.screen_manage, Icons.Rounded.Extension, Icons.Rounded.Extension),
    Repo(R.string.screen_repo, Icons.Rounded.CloudDownload, Icons.Rounded.CloudDownload),
    Settings(R.string.screen_settings, Icons.Rounded.Settings, Icons.Rounded.Settings)
}
