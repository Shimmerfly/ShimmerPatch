package org.lsposed.npatch.ui.page

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import org.lsposed.npatch.R
import org.lsposed.npatch.ui.page.destinations.*

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {
    Home(HomeScreenDestination, R.string.screen_home, Icons.Filled.Home, Icons.Outlined.Home),
    Manage(ManageScreenDestination, R.string.screen_manage, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Repo(RepoScreenDestination, R.string.screen_repo, Icons.Filled.GetApp, Icons.Outlined.GetApp),
    // Logs(LogsScreenDestination, R.string.screen_logs, Icons.Filled.Assignment, Icons.Outlined.Assignment),
    Settings(SettingsScreenDestination, R.string.screen_settings, Icons.Filled.Settings, Icons.Outlined.Settings);
}
