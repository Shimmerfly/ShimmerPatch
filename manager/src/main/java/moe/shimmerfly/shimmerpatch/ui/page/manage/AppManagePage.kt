// App rows, native menus and refresh follow InstallerX-Revived ApplyPage/ApplyItemWidget.
package moe.shimmerfly.shimmerpatch.ui.page.manage

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.BuildConfig
import moe.shimmerfly.shimmerpatch.config.ConfigManager
import moe.shimmerfly.shimmerpatch.config.Configs
import moe.shimmerfly.shimmerpatch.database.entity.LoadedModule
import moe.shimmerfly.shimmerpatch.manager.ModuleScopeSyncStore
import moe.shimmerfly.shimmerpatch.manager.DiagnosticLogExporter
import moe.shimmerfly.shimmerpatch.share.Constants
import moe.shimmerfly.shimmerpatch.share.LSPConfig

import moe.shimmerfly.shimmerpatch.ui.component.m3.DropdownAction
import moe.shimmerfly.shimmerpatch.ui.component.m3.ExpressiveActionDropdown
import moe.shimmerfly.shimmerpatch.ui.component.AppItem
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn
import moe.shimmerfly.shimmerpatch.ui.component.m3.topShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.middleShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.bottomShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.singleShape
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchPullToRefresh
import moe.shimmerfly.shimmerpatch.ui.page.ACTION_APPLIST
import moe.shimmerfly.shimmerpatch.ui.page.ACTION_STORAGE
import moe.shimmerfly.shimmerpatch.ui.page.Navigator
import moe.shimmerfly.shimmerpatch.ui.page.Route
import moe.shimmerfly.shimmerpatch.ui.page.SelectAppsResult
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.AppManageViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.ModuleManageViewModel
import moe.shimmerfly.shimmerpatch.ui.viewstate.ProcessingState
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import java.io.IOException

private const val TAG = "AppManagePage"

@Composable
fun AppManageBody(
    navigator: Navigator,
    scrollBehavior: TopAppBarScrollBehavior,
    searchQuery: String = "",
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val viewModel = viewModel<AppManageViewModel>()
    val moduleManageViewModel = viewModel<ModuleManageViewModel>()
    val diagnosticsChooser = stringResource(R.string.manage_export_diagnostics_chooser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val hapticFeedback = LocalHapticFeedback.current

    val filteredList = remember(viewModel.appList, searchQuery) {
        if (searchQuery.isEmpty()) viewModel.appList
        else viewModel.appList.filter {
            it.first.label.contains(searchQuery, true) ||
                    it.first.app.packageName.contains(searchQuery, true)
        }
    }

    // Patcher managers ship the patcher rather than being patched themselves, so they are listed in
    // their own group under the apps instead of mixing into them.
    val appRows = filteredList.filterNot { it.first.isPatcherManager }
    val managerRows = filteredList.filter { it.first.isPatcherManager }

    val uninstallSuccessfully = stringResource(R.string.manage_uninstall_successfully)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                Toast.makeText(context, uninstallSuccessfully, Toast.LENGTH_SHORT).show()
                viewModel.dispatch(AppManageViewModel.ViewAction.Refresh)
            }
        }
    }

    val isProcessing = viewModel.updateLoaderState is ProcessingState.Processing 
            || viewModel.optimizeState is ProcessingState.Processing
            || viewModel.forceStopState is ProcessingState.Processing
            || viewModel.forceRestartState is ProcessingState.Processing
    if (isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.manage_loading)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ContainedLoadingIndicator() } },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        )
    }

    when (viewModel.updateLoaderState) {
        is ProcessingState.Idle -> Unit
        is ProcessingState.Processing -> Unit
        is ProcessingState.Done -> {
            val it = viewModel.updateLoaderState as ProcessingState.Done
            val updateSuccessfully = stringResource(R.string.manage_update_loader_successfully)
            val updateFailed = stringResource(R.string.manage_update_loader_failed)
            LaunchedEffect(Unit) {
                it.result.onSuccess {
                    Toast.makeText(context, updateSuccessfully, Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, updateFailed, Toast.LENGTH_SHORT).show()
                }
                viewModel.dispatch(AppManageViewModel.ViewAction.ClearUpdateLoaderResult)
            }
        }
    }

    when (viewModel.optimizeState) {
        is ProcessingState.Idle -> Unit
        is ProcessingState.Processing -> Unit
        is ProcessingState.Done -> {
            val it = viewModel.optimizeState as ProcessingState.Done
            val optimizeSucceed = stringResource(R.string.manage_optimize_successfully)
            val optimizeFailed = stringResource(R.string.manage_optimize_failed)
            LaunchedEffect(Unit) {
                Toast.makeText(context, if (it.result) optimizeSucceed else optimizeFailed, Toast.LENGTH_SHORT).show()
                viewModel.dispatch(AppManageViewModel.ViewAction.ClearOptimizeResult)
            }
        }
    }

    when (viewModel.forceStopState) {
        is ProcessingState.Idle -> Unit
        is ProcessingState.Processing -> Unit
        is ProcessingState.Done -> {
            val it = viewModel.forceStopState as ProcessingState.Done
            val forceStopSucceed = stringResource(R.string.manage_force_stop_successfully)
            val forceStopFailed = stringResource(R.string.manage_force_stop_failed)
            LaunchedEffect(Unit) {
                Toast.makeText(context, if (it.result) forceStopSucceed else forceStopFailed, Toast.LENGTH_SHORT).show()
                viewModel.dispatch(AppManageViewModel.ViewAction.ClearForceStopResult)
            }
        }
    }

    when (viewModel.forceRestartState) {
        is ProcessingState.Idle -> Unit
        is ProcessingState.Processing -> Unit
        is ProcessingState.Done -> {
            val it = viewModel.forceRestartState as ProcessingState.Done
            val forceRestartSucceed = stringResource(R.string.manage_force_restart_successfully)
            val forceRestartFailed = stringResource(R.string.manage_force_restart_failed)
            LaunchedEffect(Unit) {
                Toast.makeText(context, if (it.result) forceRestartSucceed else forceRestartFailed, Toast.LENGTH_SHORT).show()
                viewModel.dispatch(AppManageViewModel.ViewAction.ClearForceRestartResult)
            }
        }
    }

    ShimmerPatchPullToRefresh(
        isRefreshing = viewModel.isRefreshing,
        scrollBehavior = scrollBehavior,
        onRefresh = { viewModel.dispatch(AppManageViewModel.ViewAction.Refresh) },
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
            if (appRows.isEmpty() && managerRows.isEmpty()) {
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
                                    text = if (searchQuery.isNotEmpty()) stringResource(R.string.manage_no_search_results) else stringResource(R.string.manage_no_apps),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = appRows,
                    key = { _, item -> item.first.app.packageName },
                ) { index, (appInfo, patchConfig) ->
                    // A bundle another patcher produced carries no configuration of ours, so the row
                    // names that patcher and offers nothing that would manage our loader.
                    val isOurs = patchConfig != null
                    val isLocal = patchConfig?.useManager == true
                    val loaderVersion = patchConfig?.lspConfig?.VERSION_CODE
                    val managerVersion = LSPConfig.instance.VERSION_CODE

                    val loaderOutdated = loaderVersion != null && if (isLocal) {
                        loaderVersion < Constants.MIN_ROLLING_VERSION_CODE
                    } else {
                        loaderVersion != managerVersion
                    }
                    val managerPackageMismatch = patchConfig != null && !isLocal &&
                        patchConfig.managerPackageName != BuildConfig.APPLICATION_ID

                    val versionText = if (loaderOutdated || managerPackageMismatch) loaderVersion?.toString() else null
                    val canUpdateLoader = loaderOutdated || managerPackageMismatch

                    val showDropdown = remember { mutableStateOf(false) }
                    var pressPosition by remember { mutableStateOf(Offset.Zero) }
                    val scopeUpdatedText = stringResource(R.string.manage_module_scope_updated)
                    val openScope: () -> Unit = {
                        viewModel.viewModelScope.launch {
                            val targetAppPkg = appInfo.app.packageName
                            val activated = withContext(Dispatchers.IO) {
                                ConfigManager.getModulesForApp(targetAppPkg).map { it.pkgName }.toSet()
                            }

                            val result = navigator.navigateForResult<SelectAppsResult>(
                                Route.SelectApps(true, activated.toList())
                            )
                            if (result is SelectAppsResult.MultipleApps) {
                                withContext(Dispatchers.IO) {
                                    val affectedPackages = ConfigManager.saveModuleSelection(
                                        appPkgName = targetAppPkg,
                                        initialPackageNames = activated,
                                        selectedPackageNames = result.selectedPackageNames.toSet(),
                                        availableModules = result.selected.map {
                                            LoadedModule(it.app.packageName, it.app.sourceDir)
                                        },
                                    )
                                    if (ShizukuApi.isReady) {
                                        // Notify both removed and newly added modules so they do not
                                        // keep stale scope state after a scope edit.
                                        ModuleScopeSyncStore.syncModuleScopes(affectedPackages)
                                    }
                                }
                                moduleManageViewModel.refreshScopedActivationState()
                                Toast.makeText(context, scopeUpdatedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    val openAppInfo: () -> Unit = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:${appInfo.app.packageName}".toUri()
                            }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        awaitEachGesture {
                            pressPosition = awaitFirstDown(requireUnconsumed = false).position
                        }
                    }) {
                        AppItem(
                            modifier = Modifier.animateItem(),
                            shape = when {
                                appRows.size == 1 -> singleShape
                                index == 0 -> topShape
                                index == appRows.lastIndex -> bottomShape
                                else -> middleShape
                            },
                            icon = {
                                Image(
                                    bitmap = NeoPackageManager.getIcon(appInfo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                                )
                            },
                            label = appInfo.label,
                            packageName = appInfo.app.packageName,
                            summaryRow = {
                                val patchColor = if (isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Which patcher produced the bundle comes first; only our own patches
                                    // can also say how their loader was built.
                                    Text(
                                        text = appInfo.patchedType.displayName,
                                        color = patchColor,
                                        style = MaterialTheme.typography.labelMedium,
                                    )

                                    if (isOurs) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val modeLabel = if (isLocal) {
                                            "${stringResource(R.string.patch_local)} ${stringResource(R.string.manage_rolling)}"
                                        } else {
                                            stringResource(R.string.patch_integrated)
                                        }
                                        Text(
                                            text = modeLabel,
                                            color = patchColor,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }

                                    versionText?.let { version ->
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = version,
                                            color = patchColor,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }

                                    if (canUpdateLoader) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        with(LocalDensity.current) {
                                            val size = 16.sp * 1.2
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardCapslock,
                                                contentDescription = null,
                                                modifier = Modifier.size(size.toDp()),
                                                tint = patchColor
                                            )
                                        }
                                    }
                                }
                            },
                            // The row opens the app's own page. Which patcher produced the bundle,
                            // what it embeds and which actions apply all belong there, rather than
                            // behind the module picker this used to open.
                            onClick = { navigator.navigate(Route.AppDetail(appInfo.app.packageName)) },
                            onLongPress = {
                                showDropdown.value = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                        )

                        val actions = buildList {

                            patchConfig?.let { config ->
                                if (canUpdateLoader || BuildConfig.DEBUG) {
                                    add(DropdownAction(stringResource(R.string.manage_update_loader), Icons.Outlined.SystemUpdate) {
                                        scope.launch { viewModel.dispatch(AppManageViewModel.ViewAction.UpdateLoader(appInfo, config)) }
                                    })
                                }
                            }
                            if (isLocal) {
                                add(DropdownAction(stringResource(R.string.manage_module_scope), Icons.Outlined.Extension) {
                                    openScope()
                                })
                            }
                            add(DropdownAction(stringResource(R.string.manage_export_diagnostics), Icons.Outlined.FileUpload) {
                                scope.launch {
                                    runCatching {
                                        val result = DiagnosticLogExporter.export(
                                            context,
                                            appInfo.app.packageName,
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            result.file,
                                        )
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            clipData = ClipData.newUri(
                                                context.contentResolver,
                                                result.file.name,
                                                uri,
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }.let { shareIntent ->
                                            context.startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    diagnosticsChooser,
                                                ),
                                            )
                                        }
                                    }.onFailure {
                                        Log.e(TAG, "Failed to export diagnostics for ${appInfo.app.packageName}", it)
                                        Toast.makeText(
                                            context,
                                            R.string.manage_export_diagnostics_failed,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            })
                            add(DropdownAction(stringResource(R.string.manage_repatch), Icons.Outlined.Build) {
                                navigator.navigate(
                                    Route.NewPatch(
                                        id = ACTION_APPLIST,
                                        data = appInfo.app.packageName,
                                    )
                                )
                            })
                            val shizukuUnavailable = stringResource(R.string.shizuku_unavailable)
                            add(DropdownAction(stringResource(R.string.manage_optimize), Icons.Outlined.Speed) {
                                scope.launch {
                                    if (!ShizukuApi.isReady) {
                                        Toast.makeText(context, shizukuUnavailable, Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.dispatch(AppManageViewModel.ViewAction.PerformOptimize(appInfo))
                                    }
                                }
                            })
                            add(DropdownAction(stringResource(R.string.manage_force_stop), Icons.Outlined.StopCircle) {
                                if (ShizukuApi.isReady) {
                                    scope.launch { viewModel.dispatch(AppManageViewModel.ViewAction.PerformForceStop(appInfo)) }
                                } else {
                                    openAppInfo()
                                }
                            })
                            add(DropdownAction(stringResource(R.string.manage_force_restart), Icons.Outlined.RestartAlt) {
                                if (ShizukuApi.isReady) {
                                    scope.launch {
                                        viewModel.dispatch(AppManageViewModel.ViewAction.PerformForceRestart(appInfo))
                                    }
                                } else {
                                    openAppInfo()
                                }
                            })
                            add(DropdownAction(stringResource(R.string.manage_app_info), Icons.Outlined.Info, onClick = openAppInfo))
                            add(DropdownAction(stringResource(R.string.uninstall), Icons.Outlined.Delete, isDestructive = true) {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = "package:${appInfo.app.packageName}".toUri()
                                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                }
                                launcher.launch(intent)
                            })
                        }
                        // Same zero-size pointer anchor as DropDownMenuWidget.
                        Box(Modifier.offset {
                            IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt())
                        }) {
                            ExpressiveActionDropdown(
                                expanded = showDropdown.value,
                                groups = listOf(actions.dropLast(1), actions.takeLast(1)),
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

                // The tools themselves, under the apps they produced. Each row opens the same detail
                // page, which is where their version and archive live.
                if (managerRows.isNotEmpty()) {
                    item(key = "managers") {
                        SegmentedColumn(title = stringResource(R.string.manage_managers)) {
                            managerRows.forEach { (managerInfo, _) ->
                                item(key = managerInfo.app.packageName) {
                                    BaseWidget(
                                        iconContent = {
                                            Image(
                                                bitmap = NeoPackageManager.getIcon(managerInfo),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
                                            )
                                        },
                                        title = managerInfo.label,
                                        description = "${managerInfo.app.packageName} · ${managerInfo.patcherManagerName}",
                                        onClick = {
                                            navigator.navigate(Route.AppDetail(managerInfo.app.packageName))
                                        },
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

@Composable
fun AppManageFab(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shouldSelectDirectory = remember { mutableStateOf(false) }
    val showNewPatchDialog = remember { mutableStateOf(false) }

    val errorText = stringResource(R.string.patch_select_dir_error)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        try {
            if (it.resultCode == Activity.RESULT_CANCELED) return@rememberLauncherForActivityResult
            val uri = it.data?.data ?: throw IOException("No data")
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            Configs.storageDirectory = uri.toString()
            Log.i(TAG, "Storage directory: ${uri.path}")
            showNewPatchDialog.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error when requesting saving directory", e)
            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
        }
    }

    if (shouldSelectDirectory.value) {
        AlertDialog(
            title = { Text(stringResource(R.string.patch_select_dir_title)) },
            text = { Text(stringResource(R.string.patch_select_dir_text)) },
            onDismissRequest = { shouldSelectDirectory.value = false },
            dismissButton = {
                TextButton(onClick = { shouldSelectDirectory.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                    shouldSelectDirectory.value = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    if (showNewPatchDialog.value) {
        AlertDialog(
            title = { Text(stringResource(R.string.screen_new_patch)) },
            onDismissRequest = { showNewPatchDialog.value = false },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showNewPatchDialog.value = false
                            navigator.navigate(Route.NewPatch(id = ACTION_STORAGE))
                        },
                    ) { Text(stringResource(R.string.patch_from_storage)) }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showNewPatchDialog.value = false
                            navigator.navigate(Route.NewPatch(id = ACTION_APPLIST))
                        },
                    ) { Text(stringResource(R.string.patch_from_applist)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewPatchDialog.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = {
            val uri = Configs.storageDirectory?.toUri()
            if (uri == null) {
                shouldSelectDirectory.value = true
            } else {
                runCatching {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    if (DocumentFile.fromTreeUri(context, uri)?.exists() == false) throw IOException("Storage directory was deleted")
                }.onSuccess {
                    showNewPatchDialog.value = true
                }.onFailure {
                    Log.w(TAG, "Failed to take persistable permission for saved uri", it)
                    Configs.storageDirectory = null
                    shouldSelectDirectory.value = true
                }
            }
        }
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.screen_new_patch))
    }
}
