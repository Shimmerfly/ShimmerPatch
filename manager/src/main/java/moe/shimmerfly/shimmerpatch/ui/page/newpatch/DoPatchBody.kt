package moe.shimmerfly.shimmerpatch.ui.page.newpatch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager.AppInfo
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.lspApp
import androidx.compose.foundation.shape.RoundedCornerShape
import moe.shimmerfly.shimmerpatch.ui.component.m3.CornerRadius
import moe.shimmerfly.shimmerpatch.ui.component.LoadingDialog
import moe.shimmerfly.shimmerpatch.ui.component.m3.SettingsDialog
import moe.shimmerfly.shimmerpatch.ui.page.Navigator
import moe.shimmerfly.shimmerpatch.ui.util.LocalSnackbarHost
import moe.shimmerfly.shimmerpatch.ui.util.checkIsApkFixedByLSP
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.PatchState
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.ViewAction

private const val TAG = "NewPatchPage"

private data class InstallAttempt(val id: Long, val method: NewPatchViewModel.InstallMethod)

/** Status and progress follow InstallerX-Revived's Material 3 InstallingDialog. */
@Composable
fun DoPatchBody(modifier: Modifier, navigator: Navigator) {
    val viewModel = viewModel<NewPatchViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.home_info_copied)
    val installSucceededMessage = stringResource(R.string.patch_install_successfully)
    val context = LocalContext.current
    val logState = rememberLazyListState()
    val isDragging by logState.interactionSource.collectIsDraggedAsState()
    var followLogs by remember { mutableStateOf(true) }
    var installation by remember { mutableStateOf<InstallAttempt?>(null) }

    fun copyLogs() {
        val text = viewModel.logs.joinToString("\n") { it.second }
        if (text.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ShimmerPatch Log", text))
        scope.launch { snackbarHost.showSnackbar(copiedMessage) }
    }

    LaunchedEffect(Unit) {
        if (viewModel.logs.isEmpty()) viewModel.dispatch(ViewAction.LaunchPatch)
    }
    LaunchedEffect(isDragging, logState.isScrollInProgress) {
        if (isDragging) followLogs = false
        else if (!logState.isScrollInProgress) followLogs = !logState.canScrollForward
    }
    LaunchedEffect(viewModel.logs.size) {
        if (followLogs && !isDragging && viewModel.logs.isNotEmpty()) {
            logState.scrollToItem(viewModel.logs.lastIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                // The app's own container corner, not Material's extra-large one.
                // Narrower than the screen: this banner is a status line, not a full card.
                modifier = Modifier.widthIn(max = 320.dp),
                shape = RoundedCornerShape(CornerRadius),
                color = if (viewModel.patchState == PatchState.ERROR) {
                    MaterialTheme.colorScheme.errorContainer
                } else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    ) {
                        // Every state carries a mark, the running one included.
                        Icon(
                            imageVector = when (viewModel.patchState) {
                                PatchState.FINISHED -> Icons.Outlined.CheckCircle
                                PatchState.ERROR -> Icons.Outlined.ErrorOutline
                                else -> Icons.Outlined.AutoFixHigh
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(when (viewModel.patchState) {
                                    PatchState.FINISHED -> R.string.patch_ui_finished
                                    PatchState.ERROR -> R.string.patch_ui_failed
                                    else -> R.string.patch_ui_running
                                }),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = viewModel.patchApp.app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (viewModel.patchState == PatchState.PATCHING) {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                // The app's own container corner, not Material's extra-large one.
                shape = RoundedCornerShape(CornerRadius),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.patch_ui_log),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = ::copyLogs, enabled = viewModel.logs.isNotEmpty()) {
                            Icon(Icons.Outlined.ContentCopy, stringResource(R.string.patch_ui_copy_log))
                        }
                    }
                    SelectionContainer {
                        LazyColumn(
                            state = logState,
                            modifier = Modifier.fillMaxSize(),
                            // Room at the end for the button floating over this card.
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                top = 12.dp,
                                end = 20.dp,
                                bottom = 88.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(viewModel.logs, key = { index, _ -> index }) { _, log ->
                                Text(
                                    text = log.second,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (log.first == Log.ERROR) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            }

        // A finished patch is one step from done, so the step floats over the log instead of
        // sitting under it: the log is what is left to read, and the button never drifts out of
        // reach while scrolling it.
        val finished = viewModel.patchState == PatchState.FINISHED
        val failed = viewModel.patchState == PatchState.ERROR
        if (finished || failed) {
            ExtendedFloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                onClick = {
                    if (finished) {
                        installation = InstallAttempt(
                            id = System.nanoTime(),
                            method = if (ShizukuApi.isReady) NewPatchViewModel.InstallMethod.SHIZUKU
                                else NewPatchViewModel.InstallMethod.SYSTEM,
                        )
                    } else {
                        copyLogs()
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (finished) Icons.Outlined.Download else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(if (finished) R.string.install else R.string.copy_error)) },
            )
        }
    }

    val installFailed = stringResource(R.string.patch_install_failed)
    val copyError = stringResource(R.string.copy_error)
    val onFinish: (Int, String?) -> Unit = { status, message ->
        scope.launch {
            installation = null
            when {
                status == PackageInstaller.STATUS_SUCCESS -> {
                    navigator.pop()
                    Toast.makeText(context.applicationContext, installSucceededMessage, Toast.LENGTH_SHORT).show()
                }
                status != PackageInstaller.STATUS_PENDING_USER_ACTION && status != NeoPackageManager.STATUS_USER_CANCELLED -> {
                    if (snackbarHost.showSnackbar(installFailed, copyError) == SnackbarResult.ActionPerformed) {
                        val clipboard = lspApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ShimmerPatch", message))
                    }
                }
            }
        }
    }
    RetainedPatchDialog(installation) { attempt, visible ->
        key(attempt.id) {
            InstallDialog(
                patchApp = viewModel.patchApp,
                method = if (attempt.method == NewPatchViewModel.InstallMethod.SHIZUKU) {
                    NeoPackageManager.InstallMethod.SHIZUKU
                } else NeoPackageManager.InstallMethod.SYSTEM,
                onFinish = onFinish,
                visible = visible,
            )
        }
    }
}

@Composable
fun UninstallConfirmationDialog(
    show: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onIgnoreAndInstall: () -> Unit,
) {
    // The same always-composed dialog host is used by settings and patching.
    SettingsDialog(
        show = show,
        title = stringResource(R.string.uninstall),
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
                TextButton(onClick = onIgnoreAndInstall) { Text(stringResource(R.string.patch_ignore_risk_install)) }
            }
        },
    ) {
        Text(stringResource(R.string.patch_uninstall_text))
    }
}

@Composable
fun InstallDialog(
    patchApp: AppInfo,
    method: NeoPackageManager.InstallMethod,
    onFinish: (Int, String?) -> Unit,
    visible: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var uninstallFirst by remember(method, patchApp.app.packageName) {
        mutableStateOf(
            if (method == NeoPackageManager.InstallMethod.SHIZUKU) {
                ShizukuApi.isPackageInstalledWithoutPatch(patchApp.app.packageName)
            } else {
                checkIsApkFixedByLSP(context, patchApp.app.packageName)
            },
        )
    }
    var installing by remember { mutableIntStateOf(0) }
    var awaitingUninstall by remember { mutableStateOf(false) }
    var installStarted by remember { mutableStateOf(false) }
    suspend fun doInstall() {
        Log.i(TAG, "Installing ${patchApp.app.packageName} with $method")
        installStarted = true
        installing = 1
        val outcome = NeoPackageManager.install(method)
        installing = 0
        Log.i(TAG, "Installation end: $outcome")
        when (outcome) {
            is NeoPackageManager.InstallOutcome.Completed ->
                onFinish(outcome.status, outcome.message)

            NeoPackageManager.InstallOutcome.PermissionRequired -> {
                installStarted = false
                onFinish(
                    NeoPackageManager.STATUS_USER_CANCELLED,
                    "Package install permission is required; retry after granting it",
                )
            }
        }
    }

    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var checkCount = 0
            var stillInstalled = true
            while (checkCount < 10) {
                if (!checkIsApkFixedByLSP(context, patchApp.app.packageName)) {
                    stillInstalled = false
                    break
                }
                kotlinx.coroutines.delay(300)
                checkCount++
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (stillInstalled) {
                    onFinish(PackageInstaller.STATUS_FAILURE, "Original application was not uninstalled")
                } else {
                    uninstallFirst = false
                    if (!installStarted) {
                        doInstall()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!uninstallFirst && !installStarted) {
            doInstall()
        }
    }

    UninstallConfirmationDialog(
        show = visible && uninstallFirst && !awaitingUninstall && installing == 0,
        onDismiss = { onFinish(NeoPackageManager.STATUS_USER_CANCELLED, "User cancelled") },
        onIgnoreAndInstall = {
            uninstallFirst = false
            if (!installStarted) {
                scope.launch {
                    doInstall()
                }
            }
        },
        onConfirm = {
            awaitingUninstall = true
            if (method == NeoPackageManager.InstallMethod.SHIZUKU) {
                scope.launch {
                    Log.i(TAG, "Uninstalling app ${patchApp.app.packageName}")
                    installing = 2
                    val (status, message) = NeoPackageManager.uninstall(patchApp.app.packageName)
                    installing = 0
                    Log.i(TAG, "Uninstallation end: $status, $message")
                    if (status == PackageInstaller.STATUS_SUCCESS) {
                        uninstallFirst = false
                        if (!installStarted) {
                            doInstall()
                        }
                    } else {
                        uninstallLauncher.launch(
                            Intent(Intent.ACTION_DELETE).apply {
                                data = "package:${patchApp.app.packageName}".toUri()
                            },
                        )
                    }
                }
            } else {
                uninstallLauncher.launch(
                    Intent(Intent.ACTION_DELETE).apply {
                        data = "package:${patchApp.app.packageName}".toUri()
                    },
                )
            }
        }
    )

    LoadingDialog(
        visible = visible && installing != 0,
        title = stringResource(if (installing == 1) R.string.installing else R.string.uninstalling),
    )
}
