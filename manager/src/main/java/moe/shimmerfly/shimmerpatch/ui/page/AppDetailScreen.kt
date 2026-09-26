// SPDX-License-Identifier: GPL-3.0-only
// Patched-app page built from this repository's Material 3 Expressive widgets; see docs/UI_SOURCES.md.
package moe.shimmerfly.shimmerpatch.ui.page

import android.app.Activity
import android.widget.Toast
import android.content.ClipData
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.BuildConfig
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.lspApp
import moe.shimmerfly.shimmerpatch.config.ConfigManager
import moe.shimmerfly.shimmerpatch.database.entity.LoadedModule
import moe.shimmerfly.shimmerpatch.manager.DiagnosticLogExporter
import moe.shimmerfly.shimmerpatch.manager.ModuleScopeSyncStore
import moe.shimmerfly.shimmerpatch.share.Constants
import moe.shimmerfly.shimmerpatch.share.LSPConfig
import moe.shimmerfly.shimmerpatch.ui.component.ExpressiveBackButton
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchScaffold
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchTopAppBar
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.DropdownAction
import moe.shimmerfly.shimmerpatch.ui.component.m3.ExpressiveActionDropdown
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarBlur
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarColor
import moe.shimmerfly.shimmerpatch.ui.component.m3BackdropLayer
import moe.shimmerfly.shimmerpatch.ui.component.rememberMaterial3BlurBackdrop
import moe.shimmerfly.shimmerpatch.ui.util.LocalFloatingGlassBottomBarBlur
import moe.shimmerfly.shimmerpatch.ui.util.LocalSnackbarHost
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.AppDetailViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.AppManageViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.ModuleManageViewModel
import moe.shimmerfly.shimmerpatch.ui.viewstate.ProcessingState
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.PatchConfigReader
import moe.shimmerfly.shimmerpatch.util.ShizukuApi

/**
 * One patched app: who produced the bundle, what it embeds, and everything that can be done about it.
 *
 * Tapping a row in the app list opens this page rather than a module picker, because what the row
 * can tell the user - which patcher produced it, how old its loader is - is exactly what decides
 * which actions apply.
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val snackbarHost = LocalSnackbarHost.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(LocalFloatingGlassBottomBarBlur.current)

    val detailViewModel = viewModel<AppDetailViewModel>()
    // Loader and scope actions are the ones the list page already drives, so they are not duplicated.
    val manageViewModel = viewModel<AppManageViewModel>()
    val moduleManageViewModel = viewModel<ModuleManageViewModel>()

    val appInfo = detailViewModel.appInfo(packageName)
    val patchConfig = remember(appInfo) { appInfo?.let { PatchConfigReader.read(it.app) } }
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
    val canUpdateLoader = loaderOutdated || managerPackageMismatch

    LaunchedEffect(appInfo) {
        appInfo?.let { detailViewModel.loadModules(it) }
    }

    // --- result plumbing, shared with the list page's actions --------------------------------

    val uninstallSuccessfully = stringResource(R.string.manage_uninstall_successfully)
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showToast(scope, uninstallSuccessfully)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { directory ->
        val target = appInfo
        if (directory != null && target != null) {
            scope.launch { detailViewModel.exportApks(directory, target) }
        }
    }

    val updateLoaderSuccessfully = stringResource(R.string.manage_update_loader_successfully)
    val updateLoaderFailed = stringResource(R.string.manage_update_loader_failed)
    when (val state = manageViewModel.updateLoaderState) {
        is ProcessingState.Done -> LaunchedEffect(state) {
            showToast(scope, if (state.result.isSuccess) updateLoaderSuccessfully else updateLoaderFailed)
            manageViewModel.dispatch(AppManageViewModel.ViewAction.ClearUpdateLoaderResult)
        }
        else -> Unit
    }

    val optimizeSucceed = stringResource(R.string.manage_optimize_successfully)
    val optimizeFailed = stringResource(R.string.manage_optimize_failed)
    when (val state = manageViewModel.optimizeState) {
        is ProcessingState.Done -> LaunchedEffect(state) {
            showToast(scope, if (state.result) optimizeSucceed else optimizeFailed)
            manageViewModel.dispatch(AppManageViewModel.ViewAction.ClearOptimizeResult)
        }
        else -> Unit
    }

    val forceStopSucceed = stringResource(R.string.manage_force_stop_successfully)
    val forceStopFailed = stringResource(R.string.manage_force_stop_failed)
    when (val state = manageViewModel.forceStopState) {
        is ProcessingState.Done -> LaunchedEffect(state) {
            showToast(scope, if (state.result) forceStopSucceed else forceStopFailed)
            manageViewModel.dispatch(AppManageViewModel.ViewAction.ClearForceStopResult)
        }
        else -> Unit
    }

    val forceRestartSucceed = stringResource(R.string.manage_force_restart_successfully)
    val forceRestartFailed = stringResource(R.string.manage_force_restart_failed)
    when (val state = manageViewModel.forceRestartState) {
        is ProcessingState.Done -> LaunchedEffect(state) {
            showToast(scope, if (state.result) forceRestartSucceed else forceRestartFailed)
            manageViewModel.dispatch(AppManageViewModel.ViewAction.ClearForceRestartResult)
        }
        else -> Unit
    }

    val exportDone = stringResource(R.string.app_detail_export_apk_done)
    val exportFailed = stringResource(R.string.app_detail_export_apk_failed)
    when (val state = detailViewModel.exportState) {
        is AppDetailViewModel.ExportState.Done -> {
            val message = stringResource(R.string.app_detail_export_apk_done, state.files)
            LaunchedEffect(state) {
                showToast(scope, message)
                detailViewModel.clearExportState()
            }
        }
        is AppDetailViewModel.ExportState.Failed -> LaunchedEffect(state) {
            showToast(scope, exportFailed)
            detailViewModel.clearExportState()
        }
        else -> Unit
    }

    // --- actions available to the page -------------------------------------------------------

    val openAppInfo: () -> Unit = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
        )
    }

    val scopeUpdatedText = stringResource(R.string.manage_module_scope_updated)

    val openScope: () -> Unit = {
        scope.launch {
            val activated = withContext(Dispatchers.IO) {
                ConfigManager.getModulesForApp(packageName).map { it.pkgName }.toSet()
            }
            val result = navigator.navigateForResult<SelectAppsResult>(
                Route.SelectApps(true, activated.toList())
            )
            if (result is SelectAppsResult.MultipleApps) {
                withContext(Dispatchers.IO) {
                    val affected = ConfigManager.saveModuleSelection(
                        appPkgName = packageName,
                        initialPackageNames = activated,
                        selectedPackageNames = result.selectedPackageNames.toSet(),
                        availableModules = result.selected.map {
                            LoadedModule(it.app.packageName, it.app.sourceDir)
                        },
                    )
                    if (ShizukuApi.isReady) ModuleScopeSyncStore.syncModuleScopes(affected)
                }
                moduleManageViewModel.refreshScopedActivationState()
                showToast(scope, scopeUpdatedText)
            }
        }
    }

    val exportDiagnostics: () -> Unit = {
        scope.launch {
            runCatching {
                val result = DiagnosticLogExporter.export(context, packageName)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    result.file,
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, result.file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.let { shareIntent ->
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.manage_export_diagnostics_chooser),
                        ),
                    )
                }
            }.onFailure {
                snackbarHost.showSnackbar(context.getString(R.string.manage_export_diagnostics_failed))
            }
        }
    }

    val shizukuUnavailable = stringResource(R.string.shizuku_unavailable)

    var menuExpanded by remember { mutableStateOf(false) }

    ShimmerPatchScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShimmerPatchTopAppBar(
                title = appInfo?.label ?: stringResource(R.string.app_detail_missing),
                scrollBehavior = scrollBehavior,
                navigationIcon = { ExpressiveBackButton(onClick = onBack) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                        ExpressiveActionDropdown(
                            expanded = menuExpanded,
                            groups = listOf(
                                listOf(
                                    DropdownAction(stringResource(R.string.manage_export_diagnostics), Icons.Outlined.FileUpload, onClick = exportDiagnostics),
                                    DropdownAction(stringResource(R.string.manage_app_info), Icons.Outlined.Info, onClick = openAppInfo),
                                ),
                                listOf(
                                    DropdownAction(stringResource(R.string.uninstall), Icons.Outlined.Delete, isDestructive = true) {
                                        uninstallLauncher.launch(
                                            Intent(Intent.ACTION_DELETE).apply {
                                                data = "package:$packageName".toUri()
                                                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                            }
                                        )
                                    },
                                ),
                            ),
                            onDismissRequest = { menuExpanded = false },
                            onAction = { action ->
                                menuExpanded = false
                                action.onClick()
                            },
                        )
                    }
                },
                modifier = Modifier.m3AppBarBlur(backdrop),
                color = backdrop.m3AppBarColor(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "header") {
                SegmentedColumn {
                    item {
                        BaseWidget(
                            iconContent = {
                                if (appInfo != null) {
                                    Image(
                                        bitmap = NeoPackageManager.getIcon(appInfo),
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
                                    )
                                } else {
                                    Icon(Icons.Outlined.Android, null, Modifier.size(44.dp))
                                }
                            },
                            title = appInfo?.label ?: stringResource(R.string.app_detail_missing),
                            titleStyle = MaterialTheme.typography.headlineSmall,
                            description = packageName,
                            // One row of chips says which patcher produced this bundle and, for our
                            // own, how its loader compares with this manager.
                            extraContent = {
                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    appInfo?.patchedType?.displayName?.takeIf { it.isNotEmpty() }?.let { patcher ->
                                        DetailChip(patcher, emphasized = isLocal)
                                    }
                                    if (isOurs) {
                                        DetailChip(
                                            if (isLocal) {
                                                "${stringResource(R.string.patch_local)} · ${stringResource(R.string.manage_rolling)}"
                                            } else {
                                                stringResource(R.string.patch_integrated)
                                            }
                                        )
                                    }
                                    if (isOurs && loaderVersion != null) {
                                        DetailChip("${stringResource(R.string.app_detail_loader_version)} $loaderVersion")
                                    }
                                }
                            },
                        )
                    }
                    appInfo?.let { info ->
                        val patcherName = info.patchedType.displayName
                        item {
                            BaseWidget(
                                icon = Icons.Outlined.Info,
                                title = if (isOurs) {
                                    stringResource(R.string.app_detail_modules_embedded, info.label)
                                } else {
                                    stringResource(R.string.app_detail_foreign_bundle, patcherName)
                                },
                                titleStyle = MaterialTheme.typography.bodyMedium,
                                enabled = false,
                            )
                        }
                    }
                }
            }

            item(key = "modules") {
                SegmentedColumn(title = stringResource(R.string.app_detail_modules)) {
                    val modules = detailViewModel.modules
                    if (modules.isEmpty()) {
                        item {
                            BaseWidget(
                                icon = Icons.Outlined.Extension,
                                title = if (detailViewModel.modulesUnreadable) {
                                    stringResource(R.string.app_detail_deep_scan_failed)
                                } else {
                                    stringResource(R.string.app_detail_modules_empty)
                                },
                                enabled = false,
                            )
                        }
                    } else {
                        modules.forEach { module ->
                            item(key = module.packageName) {
                                BaseWidget(
                                    iconContent = {
                                        val installed = module.installed
                                        if (installed != null) {
                                            Image(
                                                bitmap = NeoPackageManager.getIcon(installed),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Extension, null, Modifier.size(40.dp))
                                        }
                                    },
                                    title = module.installed?.label ?: module.packageName,
                                    description = if (module.installed == null) {
                                        "${module.packageName} · ${stringResource(R.string.app_detail_module_not_installed)}"
                                    } else {
                                        module.packageName
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "actions") {
                SegmentedColumn(title = stringResource(R.string.app_detail_actions)) {
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.Build,
                            title = stringResource(R.string.app_detail_repatch),
                            description = stringResource(R.string.app_detail_repatch_summary),
                            onClick = {
                                navigator.navigate(Route.NewPatch(id = ACTION_APPLIST, data = packageName))
                            },
                        )
                    }
                    if (isOurs) {
                        if (canUpdateLoader) {
                            item {
                                BaseWidget(
                                    icon = Icons.Outlined.SystemUpdate,
                                    title = stringResource(R.string.manage_update_loader),
                                    description = stringResource(R.string.app_detail_update_loader_summary),
                                    onClick = {
                                        detailViewModel.appInfo(packageName)?.let { info ->
                                            patchConfig?.let { config ->
                                                manageViewModel.dispatch(
                                                    AppManageViewModel.ViewAction.UpdateLoader(info, config)
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        item {
                            BaseWidget(
                                icon = Icons.Outlined.Extension,
                                title = stringResource(R.string.manage_module_scope),
                                description = stringResource(R.string.app_detail_module_scope_summary),
                                onClick = openScope,
                            )
                        }
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.FileDownload,
                            title = stringResource(R.string.app_detail_export_apk),
                            description = stringResource(R.string.app_detail_export_apk_summary),
                            enabled = appInfo != null,
                            onClick = { exportLauncher.launch(null) },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.Speed,
                            title = stringResource(R.string.manage_optimize),
                            description = stringResource(R.string.app_detail_optimize_summary),
                            onClick = {
                                if (!ShizukuApi.isReady) {
                                    showToast(scope, shizukuUnavailable)
                                } else {
                                    appInfo?.let {
                                        manageViewModel.dispatch(AppManageViewModel.ViewAction.PerformOptimize(it))
                                    }
                                }
                            },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.StopCircle,
                            title = stringResource(R.string.manage_force_stop),
                            description = stringResource(R.string.app_detail_force_stop_summary),
                            onClick = {
                                if (ShizukuApi.isReady) {
                                    appInfo?.let {
                                        manageViewModel.dispatch(AppManageViewModel.ViewAction.PerformForceStop(it))
                                    }
                                } else {
                                    openAppInfo()
                                }
                            },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.RestartAlt,
                            title = stringResource(R.string.manage_force_restart),
                            description = stringResource(R.string.app_detail_force_restart_summary),
                            onClick = {
                                if (ShizukuApi.isReady) {
                                    appInfo?.let {
                                        manageViewModel.dispatch(AppManageViewModel.ViewAction.PerformForceRestart(it))
                                    }
                                } else {
                                    openAppInfo()
                                }
                            },
                        )
                    }
                }
            }

            item(key = "details") {
                SegmentedColumn(title = stringResource(R.string.app_detail_details)) {
                    item {
                        BaseWidget(
                            title = stringResource(R.string.app_detail_patcher),
                            description = appInfo?.patchedType?.displayName?.takeIf { it.isNotEmpty() } ?: "-",
                            enabled = false,
                        )
                    }
                    if (isOurs) {
                        item {
                            BaseWidget(
                                title = stringResource(R.string.app_detail_mode),
                                description = if (isLocal) {
                                    "${stringResource(R.string.patch_local)} · ${stringResource(R.string.manage_rolling)}"
                                } else {
                                    stringResource(R.string.patch_integrated)
                                },
                                enabled = false,
                            )
                        }
                        item {
                            BaseWidget(
                                title = stringResource(R.string.app_detail_loader_version),
                                description = loaderVersion?.toString() ?: "-",
                                enabled = false,
                            )
                        }
                    }
                    item {
                        BaseWidget(
                            title = stringResource(R.string.app_detail_version),
                            description = appInfo?.let { "${it.versionName} (${it.versionCode})" } ?: "-",
                            enabled = false,
                        )
                    }
                    item {
                        BaseWidget(
                            title = stringResource(R.string.app_detail_package_name),
                            description = packageName,
                            enabled = false,
                        )
                    }
                    item {
                        BaseWidget(
                            title = stringResource(R.string.app_detail_install_path),
                            description = appInfo?.app?.sourceDir ?: "-",
                            descriptionStyle = MaterialTheme.typography.bodySmall,
                            enabled = false,
                        )
                    }
                }
            }
        }
    }
}

/** A short label chip, in the same rounded language as the grouped rows around it. */
@Composable
private fun DetailChip(text: String, emphasized: Boolean = false) {
    Surface(
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Shows a short message. The caller resolves the text, because only composition can. */
private fun showToast(scope: CoroutineScope, message: String) {
    scope.launch {
        Toast.makeText(lspApp, message, Toast.LENGTH_SHORT).show()
    }
}
