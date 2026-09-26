// SPDX-License-Identifier: GPL-3.0-only
// Home layout / StatCard adapted from InstallerX-Revived HomePage.kt.
// Copyright (C) 2026 InstallerX Revived contributors. See docs/UI_SOURCES.md.
package moe.shimmerfly.shimmerpatch.ui.page

import androidx.activity.compose.LocalActivity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.share.LSPConfig
import moe.shimmerfly.shimmerpatch.ui.component.*
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn
import moe.shimmerfly.shimmerpatch.ui.util.*
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.AppManageViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.manage.ModuleManageViewModel

@Composable
fun HomeScreen(navigator: Navigator, onManageShortcut: (Int) -> Unit = {}, contentPadding: PaddingValues = PaddingValues()) {
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(LocalFloatingGlassBottomBarBlur.current)
    val activity = LocalActivity.current
    var handledIntent by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val intent = activity?.intent
        if (!handledIntent && intent?.action == Intent.ACTION_VIEW && intent.hasCategory(Intent.CATEGORY_DEFAULT) && intent.type == "application/vnd.android.package-archive") {
            handledIntent = true
            intent.data?.let { navigator.navigate(Route.NewPatch(ACTION_INTENT_INSTALL, it.toString())) }
        }
    }
    DisposableEffect(Unit) {
        val listener: (Int, Int) -> Unit = { _, _ ->
            ShizukuApi.refreshState()
        }
        ShizukuApi.refreshState()
        ShizukuApi.addRequestPermissionResultListener(listener)
        onDispose { ShizukuApi.removeRequestPermissionResultListener(listener) }
    }
    val apps = viewModel<AppManageViewModel>().patchedAppCount
    val modules = viewModel<ModuleManageViewModel>().appList.size
    ShimmerPatchScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShimmerPatchTopAppBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
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
                bottom = maxOf(padding.calculateBottomPadding(), contentPadding.calculateBottomPadding()) + 16.dp,
            ),
        ) {
            item {
                ShizukuStatusCard(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(Modifier.weight(1f).fillMaxHeight(), stringResource(R.string.apps), apps.toString(), backgroundAwareColor(MaterialTheme.colorScheme.surfaceBright)) { onManageShortcut(0) }
                    StatCard(Modifier.weight(1f).fillMaxHeight(), stringResource(R.string.modules), modules.toString(), backgroundAwareColor(MaterialTheme.colorScheme.surfaceBright)) { onManageShortcut(1) }
                }
            }
            item { DeviceInformation() }
            item {
                SegmentedColumn {
                    item {
                        BaseWidget(
                            title = stringResource(R.string.home_about),
                            description = stringResource(R.string.home_description),
                            icon = Icons.Outlined.Info,
                            onClick = { navigator.navigate(Route.About) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShizukuStatusCard(modifier: Modifier = Modifier) {
    val active = ShizukuApi.isPermissionGranted
    val container = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    ElevatedCard(
        onClick = { if (ShizukuApi.isBinderAvailable && !active) ShizukuApi.requestPermission() },
        modifier = modifier.fillMaxWidth(),
        colors = backgroundAwareCardColors(container, content),
    ) {
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (active) Icons.Outlined.CheckCircle else Icons.Outlined.Warning, null, Modifier.size(32.dp))
            Column(Modifier.padding(start = 20.dp)) {
                Text(stringResource(if (active) R.string.shizuku_available else R.string.shizuku_unavailable), style = MaterialTheme.typography.titleMediumEmphasized)
                Text(ShizukuApi.getVersionOrNull()?.let { "API $it" } ?: stringResource(R.string.home_shizuku_warning), style = MaterialTheme.typography.bodyMedium)
                if (!active) Text(stringResource(R.string.home_shizuku_optional_summary), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun DeviceInformation() {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.home_info_copied)
    val system = if (Build.VERSION.PREVIEW_SDK_INT != 0) "${Build.VERSION.CODENAME} Preview (API ${Build.VERSION.PREVIEW_SDK_INT})" else "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val device = buildString {
        append(Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
        if (Build.BRAND != Build.MANUFACTURER) append(" " + Build.BRAND.replaceFirstChar { it.uppercase() })
        append(" " + Build.MODEL)
    }
    val fields = listOf(
        Triple(stringResource(R.string.home_api_version), "${LSPConfig.instance.API_CODE}", Icons.Outlined.Code),
        Triple(stringResource(R.string.home_shimmerpatch_version), "${LSPConfig.instance.VERSION_NAME} (${LSPConfig.instance.VERSION_CODE})", Icons.Outlined.Tag),
        Triple(stringResource(R.string.home_framework_version), "${LSPConfig.instance.CORE_VERSION_NAME} (${LSPConfig.instance.CORE_VERSION_CODE})", Icons.Outlined.Layers),
        Triple(stringResource(R.string.home_system_version), system, Icons.Outlined.Android),
        Triple(stringResource(R.string.home_device), device, Icons.Outlined.Smartphone),
        Triple(stringResource(R.string.home_system_abi), Build.SUPPORTED_ABIS.joinToString(), Icons.Outlined.DeveloperBoard),
    )
    SegmentedColumn {
        fields.forEach { (title, value, icon) ->
            item {
                BaseWidget(title = title, description = value, icon = icon, onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ShimmerPatch Info", fields.joinToString("\n") { "${it.first}: ${it.second}" }))
                    scope.launch { snackbar.showSnackbar(copied) }
                })
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
