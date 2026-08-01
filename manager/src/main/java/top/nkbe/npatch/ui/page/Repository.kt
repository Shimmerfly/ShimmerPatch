package org.lsposed.manager.ui.compose.repository

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.*

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import top.nkbe.npatch.R
import top.nkbe.npatch.repo.RepoLoader
import top.nkbe.npatch.ui.component.AccessibleMenuItem
import top.nkbe.npatch.ui.component.NPatchScaffold
import top.nkbe.npatch.ui.component.NPatchPullToRefresh
import top.nkbe.npatch.ui.component.NPatchTopAppBar
import top.nkbe.npatch.ui.component.floatingGlassBottomBarContentPadding
import top.nkbe.npatch.ui.component.PanelHeader
import top.nkbe.npatch.ui.component.SearchField
import top.nkbe.npatch.ui.component.SearchBarFake
import top.nkbe.npatch.ui.component.SearchBox
import top.nkbe.npatch.ui.component.SearchPager
import top.nkbe.npatch.ui.component.SearchStatus
import top.nkbe.npatch.ui.page.Navigator
import top.nkbe.npatch.ui.page.Route
import top.nkbe.npatch.ui.util.backgroundAwareCardColors
import top.nkbe.npatch.ui.util.backgroundAwareHazeStyle
import top.nkbe.npatch.ui.util.LocalFloatingGlassBottomBar
import top.nkbe.npatch.ui.viewmodel.RepoSort
import top.nkbe.npatch.ui.viewmodel.RepoUiModel
import top.nkbe.npatch.ui.viewmodel.RepositoryViewModel
import top.nkbe.npatch.ui.component.compat.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import top.nkbe.npatch.ui.component.compat.ListPopupColumn
import androidx.compose.material3.TopAppBarDefaults
import top.nkbe.npatch.ui.component.compat.PopupPositionProvider
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import top.nkbe.npatch.ui.component.compat.OverlayListPopup
import androidx.compose.material3.MaterialTheme
import top.nkbe.npatch.ui.component.compat.PopupHost
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private val RepoHorizontalPadding = 12.dp

@Composable
fun RepositoryScreen(
    navigator: Navigator,
    viewModel: RepositoryViewModel = viewModel()
) {
    val uiModels by viewModel.uiModels.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isUpgradableFirst by viewModel.upgradableFirst.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val selectedScopeTarget by viewModel.selectedScopeTarget.collectAsStateWithLifecycle()
    val useFloatingGlassBottomBar = LocalFloatingGlassBottomBar.current

    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = rememberHazeState()
    var query by rememberSaveable { mutableStateOf("") }

    val showSortMenu = remember { mutableStateOf(false) }
    val sortOptions = listOf(
        stringResource(R.string.sort_by_update_time),
        stringResource(R.string.sort_by_install_time),
        stringResource(R.string.sort_by_name)
    )
    val scopeFilterTitle = stringResource(R.string.repo_filter_scope_title)

    LaunchedEffect(query) {
        viewModel.onSearchQueryChanged(query)
    }

    val refreshTexts = listOf(
        stringResource(R.string.refresh_pulling),
        stringResource(R.string.refresh_release),
        stringResource(R.string.refresh_refresh),
        stringResource(R.string.refresh_complete),
    )

    NPatchScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            PanelHeader(
                title = stringResource(R.string.module_repo),
                description = {
                    if (uiModels.isNotEmpty()) {
                        Text(
                            text = uiModels.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                search = {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(android.R.string.search_go),
                        trailing = {
                            Box {
                            IconButton(onClick = { showSortMenu.value = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = stringResource(R.string.accessibility_sort)
                                )
                            }
                            OverlayListPopup(
                                show = showSortMenu.value,
                                alignment = PopupPositionProvider.Align.TopEnd,
                                onDismissRequest = { showSortMenu.value = false }
                            ) {
                                ListPopupColumn {
                                    AccessibleMenuItem(
                                        text = scopeFilterTitle,
                                        summary = selectedScopeTarget?.label ?: stringResource(R.string.off),
                                        selected = selectedScopeTarget != null,
                                        onClick = {
                                            showSortMenu.value = false
                                            navigator.navigate(Route.RepoScopeFilter(selectedScopeTarget?.packageName))
                                        }
                                    )
                                    AccessibleMenuItem(
                                        text = stringResource(R.string.sort_upgradable_first),
                                        selected = isUpgradableFirst,
                                        onClick = {
                                            viewModel.toggleUpgradableFirst()
                                            showSortMenu.value = false
                                        }
                                    )
                                    sortOptions.forEachIndexed { index, text ->
                                        val targetSort = when (index) {
                                            0 -> RepoSort.UPDATED
                                            1 -> RepoSort.CREATED
                                            else -> RepoSort.NAME
                                        }
                                        AccessibleMenuItem(
                                            text = text,
                                            selected = currentSort == targetSort,
                                            onClick = {
                                                viewModel.setSortOrder(targetSort)
                                                showSortMenu.value = false
                                            }
                                        )
                                    }
                                }
                            }
                            }
                        },
                    )
                },
            )
            val padding = PaddingValues(
                top = 4.dp,
                bottom = if (useFloatingGlassBottomBar) {
                    floatingGlassBottomBarContentPadding()
                } else {
                    20.dp
                },
            )
            NPatchPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                pullToRefreshState = pullToRefreshState,
                topAppBarScrollBehavior = scrollBehavior,
                refreshTexts = refreshTexts,
                contentPadding = padding,
            ) {
                RepoListContent(
                    uiModels = uiModels,
                    onRepoClick = { navigator.navigate(Route.RepoDetail(it)) },
                    contentPadding = padding,
                    hazeState = hazeState,
                    scrollBehavior = scrollBehavior,
                    isInitialLoading = isInitialLoading,
                    loadError = loadError,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoListContent(
    uiModels: List<RepoUiModel>,
    onRepoClick: (String) -> Unit,
    isInitialLoading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    contentPadding: PaddingValues,
    hazeState: HazeState,
    scrollBehavior: TopAppBarScrollBehavior
) {
    if (isInitialLoading && uiModels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.repo_loading_mirror),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (uiModels.isEmpty() && loadError != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.repo_load_failed, loadError),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.repo_retry))
                }
            }
        }
    } else if (uiModels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .hazeSource(state = hazeState)
        ) {
            if (loadError != null) {
                item(key = "repo-load-error") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.repo_load_failed, loadError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = onRetry) { Text(stringResource(R.string.repo_retry)) }
                    }
                }
            }
            items(
                items = uiModels,
                key = { it.module.name ?: it.hashCode() }
            ) { item ->
                Box(modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessLow))) {
                    RepositoryItem(item = item, onClick = {
                        item.module.name?.let(onRepoClick)
                    })
                }
            }
        }
    }
}

@Composable
fun RepositoryItem(item: RepoUiModel, onClick: () -> Unit) {
    val context = LocalContext.current
    val repoLoader = remember { RepoLoader.getInstance() }
    val module = item.module
    val appName = module.description ?: module.name ?: "Unknown"
    val packageName = module.name ?: ""

    val updatedTime = remember(item.module.latestReleaseTime) {
        getRelativeTime(context, item.module.latestReleaseTime)
    }

    val showMenu = remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu.value = true },
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = appName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (item.isUpgradable) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.need_update),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (item.isInstalled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.installed),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Column {
                    Text(
                        text = "${stringResource(R.string.package_name)}: $packageName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val author = module.collaborators.firstOrNull()?.name
                        ?: module.collaborators.firstOrNull()?.login
                    if (author != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${stringResource(R.string.author)}: $author",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 1.dp),
                            fontWeight = FontWeight(550),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!module.summary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = module.summary!!,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = stringResource(R.string.sort_by_update_time),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = updatedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            OverlayListPopup(
                show = showMenu.value,
                alignment = PopupPositionProvider.Align.End,
                onDismissRequest = { showMenu.value = false }
            ) {
                ListPopupColumn {
                    AccessibleMenuItem(
                        text = stringResource(R.string.menu_open_in_browser),
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                repoLoader.getModulePageUrl(packageName).toUri()
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            showMenu.value = false
                        }
                    )
                    AccessibleMenuItem(
                        text = stringResource(R.string.download_latest_version),
                        onClick = {
                            val url = repoLoader.getReleases(packageName)
                                .firstOrNull()
                                ?.releaseAssets
                                ?.firstOrNull()
                                ?.downloadUrl
                                ?: repoLoader.getModulePageUrl(packageName)
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            showMenu.value = false
                        }
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

fun getRelativeTime(context: Context, timeString: String?): String {
    if (timeString == null) return "N/A"
    return try {
        val instant = java.time.Instant.parse(timeString)
        val time = instant.toEpochMilli()
        val now = System.currentTimeMillis()
        val diff = now - time
        val oneYearMillis = 365L * 24 * 60 * 60 * 1000

        when {
            diff < 60 * 1000 -> context.getString(R.string.time_just_now)
            diff < 60 * 60 * 1000 -> context.getString(R.string.time_minutes_ago, diff / (60 * 1000))
            diff < 24 * 60 * 60 * 1000 -> context.getString(R.string.time_hours_ago, diff / (60 * 60 * 1000))
            diff < 30L * 24 * 60 * 60 * 1000 -> context.getString(R.string.time_days_ago, diff / (24 * 60 * 60 * 1000))
            diff >= oneYearMillis -> {
                val date = Date(time)
                val sdf = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                sdf.format(date)
            }

            else -> {
                val date = Date(time)
                val sdf = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                sdf.format(date)
            }
        }
    } catch (_: Exception) {
        "N/A"
    }
}
