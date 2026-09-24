// App selection, grouped rows and refresh ported from InstallerX-Revived
// ui/page/main/settings/config/apply/{ApplyPage,ApplyItemWidget}.kt.
package top.nkbe.npatch.ui.page

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.parcelize.Parcelize
import nkbe.util.NeoPackageManager
import nkbe.util.NeoPackageManager.AppInfo
import top.nkbe.npatch.R
import top.nkbe.npatch.ui.component.AppItem
import top.nkbe.npatch.ui.component.ExpressiveBackButton
import top.nkbe.npatch.ui.component.m3.topShape
import top.nkbe.npatch.ui.component.m3.middleShape
import top.nkbe.npatch.ui.component.m3.bottomShape
import top.nkbe.npatch.ui.component.m3.singleShape
import top.nkbe.npatch.ui.component.NPatchPullToRefresh
import top.nkbe.npatch.ui.component.NPatchScaffold
import top.nkbe.npatch.ui.component.NPatchTopAppBar
import top.nkbe.npatch.ui.component.SearchBar
import top.nkbe.npatch.ui.component.m3AppBarBlur
import top.nkbe.npatch.ui.component.m3AppBarColor
import top.nkbe.npatch.ui.component.m3BackdropLayer
import top.nkbe.npatch.ui.component.rememberMaterial3BlurBackdrop
import top.nkbe.npatch.ui.viewmodel.SelectAppsViewModel

@Parcelize
sealed class SelectAppsResult : Parcelable {
    data class SingleApp(val selected: AppInfo) : SelectAppsResult()
    data class MultipleApps(
        val selected: List<AppInfo>,
        val selectedPackageNames: List<String> = selected.map { it.app.packageName },
    ) : SelectAppsResult()
}

@Composable
fun SelectAppsScreen(multiSelect: Boolean, initialSelected: List<String>?) {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SelectAppsViewModel>()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPackages by rememberSaveable(multiSelect, initialSelected) {
        mutableStateOf(initialSelected.orEmpty())
    }
    val appFilter: (AppInfo) -> Boolean = remember(multiSelect) {
        { app -> if (multiSelect) app.isXposedModule else app.app.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
    }
    val visibleApps = remember(viewModel.filteredList, searchQuery) {
        viewModel.filteredList.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues().calculateBottomPadding()
    val title = stringResource(if (multiSelect) R.string.screen_select_modules else R.string.screen_select_apps)

    LaunchedEffect(multiSelect) { viewModel.filterAppList(false, appFilter) }

    NPatchScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            NPatchTopAppBar(
                modifier = Modifier.m3AppBarBlur(backdrop),
                color = backdrop.m3AppBarColor(),
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    ExpressiveBackButton(onClick = { navigator.pop() })
                },
                bottomContent = {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                },
            )
        },
        floatingActionButton = {
            if (multiSelect) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(bottom = (imeBottom - systemBottom).coerceAtLeast(0.dp)),
                    onClick = {
                        val selected = NeoPackageManager.appList.filter { it.app.packageName in selectedPackages }
                        navigator.setResultAndBack(
                            SelectAppsResult.MultipleApps(
                                selected = selected,
                                selectedPackageNames = selectedPackages,
                            ),
                        )
                    },
                    icon = { Icon(Icons.Outlined.Done, contentDescription = stringResource(android.R.string.ok)) },
                    text = { Text(stringResource(android.R.string.ok)) },
                )
            }
        },
    ) { innerPadding ->
        val listPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = maxOf(innerPadding.calculateBottomPadding(), imeBottom) + if (multiSelect) 96.dp else 16.dp,
        )
        NPatchPullToRefresh(
            isRefreshing = viewModel.isRefreshing,
            scrollBehavior = scrollBehavior,
            onRefresh = { viewModel.filterAppList(true, appFilter) },
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop),
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (visibleApps.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            if (viewModel.isRefreshing) ContainedLoadingIndicator()
                            else Text(stringResource(R.string.manage_no_search_results))
                        }
                    }
                }
                itemsIndexed(visibleApps, key = { _, app -> app.app.packageName }) { index, app ->
                    val checked = app.app.packageName in selectedPackages
                    AppItem(
                        modifier = Modifier.animateItem(),
                        icon = {
                            Image(
                                bitmap = NeoPackageManager.getIcon(app),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            )
                        },
                        label = app.label,
                        packageName = app.app.packageName,
                        shape = when {
                            visibleApps.size == 1 -> singleShape
                            index == 0 -> topShape
                            index == visibleApps.lastIndex -> bottomShape
                            else -> middleShape
                        },
                        checked = if (multiSelect) checked else null,
                        onClick = {
                            if (multiSelect) {
                                selectedPackages = if (checked) selectedPackages - app.app.packageName
                                    else selectedPackages + app.app.packageName
                            } else navigator.setResultAndBack(SelectAppsResult.SingleApp(app))
                        },
                        trailingContent = if (multiSelect) {
                            { Checkbox(checked = checked, onCheckedChange = null) }
                        } else if (app.isPatched) {
                            {
                                Text(
                                    stringResource(R.string.patch_target_already_patched),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    )
                }
            }
        }
    }
}
