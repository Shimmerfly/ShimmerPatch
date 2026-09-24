// App rows, native menus and refresh follow InstallerX-Revived ApplyPage/ApplyItemWidget.
package moe.shimmerfly.shimmerpatch.ui.page.manage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.ui.component.m3.DropdownAction
import moe.shimmerfly.shimmerpatch.ui.component.m3.ExpressiveActionDropdown
import moe.shimmerfly.shimmerpatch.ui.component.AppItem
import moe.shimmerfly.shimmerpatch.ui.component.m3.topShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.middleShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.bottomShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.singleShape
import moe.shimmerfly.shimmerpatch.ui.component.NPatchPullToRefresh
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.ModuleManageViewModel

private data class ModuleBadgeColors(
    val container: Color,
    val content: Color
)

@Composable
private fun rememberModuleBadgeColors(
    isModern: Boolean,
    isLegacy: Boolean
): ModuleBadgeColors {
    return when {
        isModern -> ModuleBadgeColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )

        isLegacy -> ModuleBadgeColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
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
    scrollBehavior: TopAppBarScrollBehavior,
    searchQuery: String = "",
    contentPadding: PaddingValues = PaddingValues(0.dp),
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

    NPatchPullToRefresh(
        isRefreshing = viewModel.isRefreshing,
        scrollBehavior = scrollBehavior,
        onRefresh = { viewModel.refresh() },
        pullToRefreshState = pullToRefreshState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (NeoPackageManager.appList.isEmpty()) {
                                ContainedLoadingIndicator()
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
                    items = filteredList,
                    key = { _, item -> item.appInfo.app.packageName },
                ) { index, item ->
                    val showDropdown = remember { mutableStateOf(false) }
                    var pressPosition by remember { mutableStateOf(Offset.Zero) }
                    val settingsIntent = remember { NeoPackageManager.getSettingsIntent(item.appInfo.app.packageName) }
                    val apiBadgeColors = rememberModuleBadgeColors(
                        isModern = item.metadata.isModern,
                        isLegacy = item.metadata.isLegacy
                    )
                    val pipelineBadgeColors = rememberModuleBadgeColors(
                        isModern = item.metadata.isModern,
                        isLegacy = item.metadata.isLegacy
                    )

                    Box(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        awaitEachGesture {
                            pressPosition = awaitFirstDown(requireUnconsumed = false).position
                        }
                    }) {
                        AppItem(
                            modifier = Modifier.animateItem(),
                            shape = when {
                                filteredList.size == 1 -> singleShape
                                index == 0 -> topShape
                                index == filteredList.lastIndex -> bottomShape
                                else -> middleShape
                            },
                            icon = {
                                Image(
                                    bitmap = NeoPackageManager.getIcon(item.appInfo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            },
                            label = item.metadata.displayName.ifEmpty { item.appInfo.label },
                            packageName = item.appInfo.app.packageName,
                            labelTrailingContent = {
                                if (item.activationEnabled) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = stringResource(R.string.manage_module_activation_enabled),
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = apiBadgeColors.content,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (item.metadata.version.isNotEmpty()) {
                                    Text(
                                        text = item.metadata.version,
                                        style = MaterialTheme.typography.bodySmall,
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
                                        style = MaterialTheme.typography.labelSmall,
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

                        val actions = buildList {
                            if (settingsIntent != null) {
                                add(DropdownAction(stringResource(R.string.manage_module_settings), Icons.Outlined.Settings) {
                                    context.startActivity(settingsIntent)
                                })
                            }
                            add(DropdownAction(stringResource(R.string.manage_app_info), Icons.Outlined.Info) {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", item.appInfo.app.packageName, null)
                                )
                                context.startActivity(intent)
                            })
                        }
                        // Same zero-size pointer anchor as DropDownMenuWidget.
                        Box(Modifier.offset {
                            IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt())
                        }) {
                            ExpressiveActionDropdown(
                                expanded = showDropdown.value,
                                groups = listOf(actions),
                                onDismissRequest = { showDropdown.value = false },
                                onAction = { action ->
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showDropdown.value = false
                                    action.onClick()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
