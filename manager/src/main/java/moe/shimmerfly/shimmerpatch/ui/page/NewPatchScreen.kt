package moe.shimmerfly.shimmerpatch.ui.page

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import moe.shimmerfly.shimmerpatch.ui.component.ExpressiveBackButton
import moe.shimmerfly.shimmerpatch.ui.component.LoadingDialog
import moe.shimmerfly.shimmerpatch.ui.component.m3.SettingsDialog
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.RetainedPatchDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.ConfiguringFab
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.DoPatchBody
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.PatchOptionsBody
import moe.shimmerfly.shimmerpatch.ui.page.newpatch.PatchFlowViewModel
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchScaffold
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchTopAppBar
import moe.shimmerfly.shimmerpatch.ui.util.LocalSnackbarHost
import moe.shimmerfly.shimmerpatch.ui.viewmodel.MainViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.PatchState
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.ViewAction

const val ACTION_STORAGE = 0
const val ACTION_APPLIST = 1
const val ACTION_INTENT_INSTALL = 2

@Composable
fun NewPatchScreen(
    id: Int,
    data: String? = null
) {
    val navigator = LocalNavigator.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val patchViewModel = viewModel<NewPatchViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val activity = LocalActivity.current as ComponentActivity
    val activityScope = viewModel<MainViewModel>(viewModelStoreOwner = activity).viewModelScope
    val flowViewModel = viewModel<PatchFlowViewModel>()
    val scope = flowViewModel.viewModelScope
    val errorUnknown = stringResource(R.string.error_unknown)
    // Modules can also be added from storage; the document is copied and read by the view model.
    val moduleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                patchViewModel.addEmbeddedModuleFromStorage(uri).onFailure {
                    Toast.makeText(activity, R.string.patch_embed_add_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var pendingPatchedApp by flowViewModel.pendingPatchedApp
    var pendingPatchedType by flowViewModel.pendingPatchedType
    var isExtracting by flowViewModel.isExtracting
    var missingOriginalDialog by flowViewModel.missingOriginalDialog
    var packageMismatchDialog by flowViewModel.packageMismatchDialog

    fun handleAppSelected(app: NeoPackageManager.AppInfo) {
        scope.launch {
            val patchedType = withContext(Dispatchers.IO) {
                if (app.patchedType == NeoPackageManager.PatchedType.NONE) {
                    NeoPackageManager.detectPatchedTypeDeep(app)
                } else app.patchedType
            }
            if (patchedType != NeoPackageManager.PatchedType.NONE) {
                pendingPatchedType = patchedType
                pendingPatchedApp = app
            } else {
                patchViewModel.dispatch(ViewAction.ConfigurePatch(app))
            }
        }
    }

    fun showReadError(error: Throwable) {
        activityScope.launch {
            snackbarHost.showSnackbar(error.localizedMessage ?: error.message ?: errorUnknown)
        }
        navigator.pop()
    }

    val apkMimeTypes = arrayOf(
        "application/vnd.android.package-archive",
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
    )
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { apks ->
        if (apks.isEmpty()) {
            navigator.pop()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            NeoPackageManager.getAppInfoFromApks(apks)
                .onSuccess { handleAppSelected(it.first()) }
                .onFailure(::showReadError)
        }
    }

    LaunchedEffect(flowViewModel.requestStorage.value) {
        if (flowViewModel.requestStorage.value) {
            flowViewModel.requestStorage.value = false
            storageLauncher.launch(apkMimeTypes)
        }
    }

    LaunchedEffect(Unit) {
        if (patchViewModel.hasExecutedIntent) return@LaunchedEffect
        patchViewModel.hasExecutedIntent = true
        // This work belongs to the entry, so opening SelectApps does not cancel its result consumer.
        scope.launch {
            NeoPackageManager.cleanTmpApkDir()
            patchViewModel.dispatch(ViewAction.DoneInit)
            when (id) {
                ACTION_STORAGE -> flowViewModel.requestStorage.value = true
                ACTION_APPLIST -> {
                    val targetApp = data?.takeIf { it.isNotEmpty() }?.let { packageName ->
                        NeoPackageManager.appList.firstOrNull { it.app.packageName == packageName }
                    }
                    if (targetApp != null) {
                        handleAppSelected(targetApp)
                    } else {
                        val result = navigator.navigateForResult<SelectAppsResult>(Route.SelectApps(false, null))
                        if (result == null) navigator.pop()
                        else handleAppSelected((result as SelectAppsResult.SingleApp).selected)
                    }
                }
                ACTION_INTENT_INSTALL -> {
                    if (data.isNullOrEmpty()) {
                        showReadError(IllegalArgumentException(errorUnknown))
                    } else {
                        NeoPackageManager.getAppInfoFromApks(listOf(data.toUri()))
                            .onSuccess { handleAppSelected(it.first()) }
                            .onFailure(::showReadError)
                    }
                }
            }
        }
    }

    // Only a running patch blocks back. The destination owner retains its content throughout
    // predictive pop; ordinary back is handled by the same NavDisplay as the other pages.
    BackHandler(enabled = patchViewModel.patchState == PatchState.PATCHING || isExtracting) {}

    ShimmerPatchScaffold(
        modifier = Modifier.imePadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShimmerPatchTopAppBar(
                title = stringResource(R.string.screen_new_patch),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (patchViewModel.patchState != PatchState.PATCHING && !isExtracting) {
                        ExpressiveBackButton(onClick = { navigator.pop() })
                    }
                },
            )
        },
        floatingActionButton = {
            // Editing keeps the full field clear; the action returns when the IME closes.
            if (patchViewModel.patchState == PatchState.CONFIGURING && !isImeVisible) ConfiguringFab()
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (patchViewModel.patchState) {
                PatchState.CONFIGURING -> PatchOptionsBody(
                    modifier = Modifier,
                    onAddEmbed = {
                        scope.launch {
                            val result = navigator.navigateForResult<SelectAppsResult>(
                                Route.SelectApps(true, patchViewModel.embeddedModules.mapTo(ArrayList()) { it.packageName })
                            )
                            if (result is SelectAppsResult.MultipleApps) {
                                patchViewModel.applyEmbeddedModuleSelection(result.selected)
                            }
                        }
                    },
                    onAddFromStorage = {
                        moduleFilePicker.launch(
                            arrayOf("application/vnd.android.package-archive", "application/octet-stream")
                        )
                    },
                )
                PatchState.PATCHING, PatchState.FINISHED, PatchState.ERROR -> {
                    DoPatchBody(modifier = Modifier, navigator = navigator)
                }
                else -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.manage_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    RetainedPatchDialog(pendingPatchedApp?.takeUnless { isExtracting }) { app, show ->
        val patchedType = pendingPatchedType
        val typeName = when (patchedType) {
            NeoPackageManager.PatchedType.EMBEDDED -> stringResource(R.string.patch_type_embedded_apk)
            NeoPackageManager.PatchedType.NONE -> "ShimmerPatch/LSPatch/FPA"
            else -> patchedType.displayName
        }
        val dismiss = {
            pendingPatchedApp = null
            navigator.pop()
            Unit
        }
        SettingsDialog(
            show = show,
            title = stringResource(R.string.patch_extract_original_title, typeName),
            content = { Text(stringResource(R.string.patch_extract_original_text, typeName)) },
            onDismissRequest = dismiss,
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        isExtracting = true
                        scope.launch {
                            when (val result = NeoPackageManager.extractOriginalApk(app)) {
                                is NeoPackageManager.ExtractResult.Success -> {
                                    pendingPatchedApp = null
                                    patchViewModel.dispatch(ViewAction.ConfigurePatch(result.originalAppInfo))
                                }
                                is NeoPackageManager.ExtractResult.NoOriginalApk -> {
                                    pendingPatchedApp = null
                                    missingOriginalDialog = app
                                }
                                is NeoPackageManager.ExtractResult.PackageMismatch -> {
                                    pendingPatchedApp = null
                                    packageMismatchDialog = app to result
                                }
                                is NeoPackageManager.ExtractResult.Corrupted -> {
                                    pendingPatchedApp = null
                                    snackbarHost.showSnackbar(result.message)
                                    navigator.pop()
                                }
                                is NeoPackageManager.ExtractResult.Error -> {
                                    pendingPatchedApp = null
                                    snackbarHost.showSnackbar(result.message)
                                    navigator.pop()
                                }
                            }
                            isExtracting = false
                        }
                    }) { Text(stringResource(R.string.patch_extract_original_confirm)) }
                    TextButton(onClick = {
                        pendingPatchedApp = null
                        patchViewModel.dispatch(ViewAction.ConfigurePatch(app))
                    }) { Text(stringResource(R.string.patch_extract_original_direct)) }
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    LoadingDialog(visible = isExtracting, title = stringResource(R.string.patch_extract_original_extracting))

    RetainedPatchDialog(missingOriginalDialog) { app, show ->
        val dismiss = {
            missingOriginalDialog = null
            navigator.pop()
            Unit
        }
        SettingsDialog(
            show = show,
            title = stringResource(R.string.patch_extract_original_title, app.label),
            content = { Text(stringResource(R.string.patch_extract_original_missing)) },
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(onClick = {
                    missingOriginalDialog = null
                    patchViewModel.dispatch(ViewAction.ConfigurePatch(app))
                }) { Text(stringResource(R.string.patch_extract_original_direct)) }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    RetainedPatchDialog(packageMismatchDialog) { (app, mismatch), show ->
        val dismiss = {
            packageMismatchDialog = null
            navigator.pop()
            Unit
        }
        SettingsDialog(
            show = show,
            title = stringResource(R.string.patch_extract_original_title, app.label),
            content = { Text(stringResource(R.string.patch_extract_package_mismatch, mismatch.outerPkg, mismatch.innerPkg)) },
            onDismissRequest = dismiss,
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        packageMismatchDialog = null
                        patchViewModel.dispatch(ViewAction.ConfigurePatch(mismatch.originalAppInfo))
                    }) { Text(stringResource(R.string.patch_extract_original_confirm)) }
                    TextButton(onClick = {
                        packageMismatchDialog = null
                        patchViewModel.dispatch(ViewAction.ConfigurePatch(app))
                    }) { Text(stringResource(R.string.patch_extract_original_direct)) }
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}
