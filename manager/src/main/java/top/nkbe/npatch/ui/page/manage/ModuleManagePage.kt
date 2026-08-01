package top.nkbe.npatch.ui.page.manage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import nkbe.util.NeoPackageManager
import top.nkbe.npatch.R
import top.nkbe.npatch.ui.component.AccessibleMenuItem
import top.nkbe.npatch.ui.component.AppItem
import top.nkbe.npatch.ui.component.NPatchPullToRefresh
import top.nkbe.npatch.ui.viewmodel.manage.ModuleManageViewModel
import top.nkbe.npatch.ui.util.ensureVisibleByMix
import top.nkbe.npatch.ui.util.relativeLuminance
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import top.nkbe.npatch.ui.component.compat.ListPopupColumn
import top.nkbe.npatch.ui.component.compat.PopupPositionProvider
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import top.nkbe.npatch.ui.component.compat.OverlayListPopup
import androidx.compose.material3.MaterialTheme

private data class ModuleBadgeColors(
    val container: Color,
    val content: Color
)

@Composable
private fun rememberModuleBadgeColors(
    isModern: Boolean,
    isLegacy: Boolean
): ModuleBadgeColors {
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceIsDark = relativeLuminance(surfaceArgb) < 0.5
    fun boostedContainer(candidate: Color): Color {
        val mixed = ensureVisibleByMix(
            original = surfaceArgb,
            candidate = candidate.toArgb(),
            minRatio = 2.8,
            mixWithWhiteIfLighter = surfaceIsDark
        )
        return Color(mixed)
    }

    return when {
        isModern -> ModuleBadgeColors(
            container = boostedContainer(MaterialTheme.colorScheme.primaryContainer),
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )

        isLegacy -> ModuleBadgeColors(
            container = boostedContainer(MaterialTheme.colorScheme.secondaryContainer),
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )

        else -> ModuleBadgeColors(
            container = MaterialTheme.colorScheme.error,
            content = MaterialTheme.colorScheme.onError
        )
    }
}

@Composable
fun ModuleManageBody(
    searchQuery: String = "",
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollBehavior: TopAppBarScrollBehavior,
    hazeState: HazeState,
    viewModel: ModuleManageViewModel = viewModel()
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    val hapticFeedback = LocalHapticFeedback.current

    val filteredList = remember(viewModel.appList, searchQuery) {
        if (searchQuery.isEmpty()) viewModel.appList
        else viewModel.appList.filter {
            it.appInfo.label.contains(searchQuery, true) ||
                    it.appInfo.app.packageName.contains(searchQuery, true) ||
                    it.metadata.displayName.contains(searchQuery, true)
        }
    }
    val sortedList = remember(filteredList) {
        filteredList.sortedByDescending { it.activationEnabled }
    }

    NPatchPullToRefresh(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh() },
        pullToRefreshState = pullToRefreshState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)


                .hazeSource(state = hazeState),
            contentPadding = contentPadding,
            overscrollEffect = null
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (NeoPackageManager.appList.isEmpty()) {
                                LinearProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.manage_loading),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) stringResource(R.string.manage_no_search_results) else stringResource(R.string.manage_no_modules),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = sortedList,
                    key = { _, item -> item.appInfo.app.packageName }
                ) { index, item ->
                    Column {
                        if (index == 0 || sortedList[index - 1].activationEnabled != item.activationEnabled) {
                            ModuleSectionHeader(
                                title = stringResource(
                                    if (item.activationEnabled) R.string.manage_running
                                    else R.string.manage_not_running
                                ),
                                count = sortedList.count { it.activationEnabled == item.activationEnabled },
                            )
                        }
                    val showDropdown = remember { mutableStateOf(false) }
                    val settingsIntent = remember { NeoPackageManager.getSettingsIntent(item.appInfo.app.packageName) }
                    val apiBadgeColors = rememberModuleBadgeColors(
                        isModern = item.metadata.isModern,
                        isLegacy = item.metadata.isLegacy
                    )
                    val pipelineBadgeColors = rememberModuleBadgeColors(
                        isModern = item.metadata.isModern,
                        isLegacy = item.metadata.isLegacy
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        AppItem(
                            icon = {
                                Image(
                                    bitmap = NeoPackageManager.getIcon(item.appInfo),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                )
                            },
                            label = item.metadata.displayName.ifEmpty { item.appInfo.label },
                            packageName = item.appInfo.app.packageName,
                            labelTrailingContent = {
                                if (item.activationEnabled) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            summaryRow = {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = apiBadgeColors.container
                                ) {
                                    Text(
                                        text = when {
                                            item.metadata.isModern -> stringResource(R.string.manage_module_api_version, item.metadata.targetApiVersion)
                                            item.metadata.isLegacy -> stringResource(R.string.manage_module_api_version, item.metadata.minApiVersion)
                                            else -> stringResource(R.string.manage_module_api_unsupported, item.metadata.minApiVersion)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = apiBadgeColors.content,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (item.metadata.version.isNotEmpty()) {
                                    Text(
                                        text = item.metadata.version,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            topRightContent = {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = pipelineBadgeColors.container
                                ) {
                                    Text(
                                        text = when {
                                            item.metadata.isModern -> stringResource(R.string.manage_module_pipeline_modern)
                                            item.metadata.isLegacy -> stringResource(R.string.manage_module_pipeline_legacy)
                                            else -> stringResource(R.string.manage_module_pipeline_unsupported)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = pipelineBadgeColors.content,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            description = item.metadata.description,
                            warningText = if (item.metadata.isUnsupported) {
                                stringResource(R.string.manage_module_unsupported_warning, item.metadata.minApiVersion)
                            } else null,
                            onClick = {
                                showDropdown.value = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            },
                            onLongPress = {
                                showDropdown.value = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                        )

                        OverlayListPopup(
                            show = showDropdown.value,
                            alignment = PopupPositionProvider.Align.End,
                            onDismissRequest = { showDropdown.value = false }
                        ) {
                            val actions = mutableListOf<Pair<String, () -> Unit>>()

                            if (settingsIntent != null) {
                                actions.add(stringResource(R.string.manage_module_settings) to {
                                    context.startActivity(settingsIntent)
                                })
                            }
                            actions.add(stringResource(R.string.manage_app_info) to {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", item.appInfo.app.packageName, null)
                                )
                                context.startActivity(intent)
                            })

                            ListPopupColumn {
                                actions.forEachIndexed { index, (text, action) ->
                                    AccessibleMenuItem(
                                        text = text,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                            showDropdown.value = false
                                            action()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
