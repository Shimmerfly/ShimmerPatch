package moe.shimmerfly.shimmerpatch.ui.page

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import moe.shimmerfly.shimmerpatch.BuildConfig
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.config.Configs
import moe.shimmerfly.shimmerpatch.ui.component.ExpressiveBackButton
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchScaffold
import moe.shimmerfly.shimmerpatch.ui.component.ShimmerPatchTopAppBar
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseItemContainer
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn

private val welcomeShizukuListener: (Int, Int) -> Unit = { _, _ ->
    ShizukuApi.refreshState()
}

/** Uses the same WeKit / InstallerX Material 3 segmented widget family as Settings. */
@Composable
fun WelcomeScreen(reviewMode: Boolean, onFinish: () -> Unit, onReturn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var storageGranted by remember { mutableStateOf(context.hasStorageAccess()) }
    var appListGranted by remember { mutableStateOf(context.hasAppListAccessDeclaration()) }

    val legacyStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        storageGranted = context.hasStorageAccess()
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        storageGranted = context.hasStorageAccess()
        appListGranted = context.hasAppListAccessDeclaration()
    }
    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
            }
            settingsLauncher.launch(
                if (appIntent.resolveActivity(context.packageManager) != null) appIntent
                else Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            )
        } else {
            legacyStorageLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    fun completeWelcome() {
        if (reviewMode) onReturn() else {
            Configs.welcomeSeen = true
            onFinish()
        }
    }

    DisposableEffect(Unit) {
        ShizukuApi.refreshState()
        ShizukuApi.addRequestPermissionResultListener(welcomeShizukuListener)
        onDispose { ShizukuApi.removeRequestPermissionResultListener(welcomeShizukuListener) }
    }

    ShimmerPatchScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShimmerPatchTopAppBar(
                title = stringResource(when (pagerState.currentPage) {
                    1 -> R.string.welcome_permission_title
                    2 -> R.string.welcome_disclaimer_title
                    else -> R.string.app_name
                }),
                navigationIcon = {
                    if (reviewMode) ExpressiveBackButton(
                        contentDescription = stringResource(R.string.welcome_btn_return),
                        onClick = onReturn,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { (pagerState.currentPage + 1) / 3f },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                    Text(
                        stringResource(R.string.welcome_ui_step, pagerState.currentPage + 1, 3),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!reviewMode) {
                            TextButton(onClick = ::completeWelcome) {
                                Text(stringResource(R.string.welcome_btn_skip))
                            }
                        }
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                                Text(stringResource(R.string.nav_back))
                            }
                        }
                        Button(
                            enabled = pagerState.currentPage != 1 || storageGranted && appListGranted,
                            onClick = {
                                if (pagerState.currentPage == 2) completeWelcome()
                                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            },
                        ) {
                            Text(stringResource(if (pagerState.currentPage == 2) R.string.welcome_btn_finish else R.string.welcome_btn_next))
                        }
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 2,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            when (page) {
                0 -> WelcomeIntroPage()
                1 -> WelcomePermissionPage(
                    storageGranted = storageGranted,
                    appListGranted = appListGranted,
                    onStorageClick = ::requestStorageAccess,
                    onAppListClick = {
                        settingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${context.packageName}".toUri()
                        })
                    },
                )
                else -> WelcomeDisclaimerPage()
            }
        }
    }
}

@Composable
private fun WelcomeIntroPage() {
    WelcomePageContainer {
        item(key = "intro") {
            SegmentedColumn {
                item {
                    BaseItemContainer {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_artwork),
                                contentDescription = null,
                                modifier = Modifier.size(88.dp).clip(CircleShape),
                            )
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.displaySmall,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                stringResource(R.string.welcome_version, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.welcome_intro_content),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                stringResource(R.string.welcome_intro_detail),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item(key = "language") {
            SegmentedColumn { item { LanguagePreference() } }
        }
    }
}

@Composable
private fun WelcomePermissionPage(
    storageGranted: Boolean,
    appListGranted: Boolean,
    onStorageClick: () -> Unit,
    onAppListClick: () -> Unit,
) {
    WelcomePageContainer {
        item(key = "permissions") {
            SegmentedColumn(title = stringResource(R.string.welcome_permission_summary)) {
                item {
                    PermissionStatusItem(
                        icon = Icons.Outlined.Folder,
                        title = stringResource(R.string.welcome_permission_storage_title),
                        summary = stringResource(R.string.welcome_permission_storage_summary),
                        granted = storageGranted,
                        onClick = onStorageClick,
                    )
                }
                item {
                    PermissionStatusItem(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.welcome_permission_applist_title),
                        summary = stringResource(R.string.welcome_permission_applist_summary),
                        granted = appListGranted,
                        onClick = onAppListClick,
                    )
                }
            }
        }
        item(key = "shizuku") {
            SegmentedColumn(title = stringResource(R.string.welcome_optional_title)) {
                item {
                    val isGranted = ShizukuApi.isPermissionGranted
                    val apiVersion = ShizukuApi.getVersionOrNull()
                    BaseWidget(
                        icon = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                        title = stringResource(if (isGranted) R.string.shizuku_available else R.string.shizuku_unavailable),
                        description = buildString {
                            append(apiVersion?.let { "API $it" } ?: stringResource(R.string.home_shizuku_warning))
                            append("\n")
                            append(stringResource(R.string.welcome_optional_summary))
                        },
                        onClick = if (ShizukuApi.isBinderAvailable && !isGranted) {
                            { ShizukuApi.requestPermission() }
                        } else null,
                    )
                }
            }
        }
        item(key = "appearance") { AppearanceSettings() }
        item(key = "storage") {
            SegmentedColumn(title = stringResource(R.string.welcome_basic_settings_title)) {
                item { StorageDirectory() }
            }
        }
    }
}

@Composable
private fun WelcomeDisclaimerPage() {
    WelcomePageContainer {
        item {
            SegmentedColumn {
                item {
                    BaseWidget(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.welcome_disclaimer_summary),
                        description = stringResource(R.string.welcome_disclaimer_content),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePageContainer(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        content = content,
    )
}

@Composable
private fun PermissionStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    BaseWidget(
        icon = icon,
        title = title,
        description = summary + "\n" + stringResource(
            if (granted) R.string.welcome_permission_granted else R.string.welcome_permission_authorize
        ),
        onClick = if (granted) null else onClick,
    ) {
        if (granted) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

private fun Context.hasStorageAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    Environment.isExternalStorageManager()
} else {
    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

@android.annotation.SuppressLint("InlinedApi")
private fun Context.hasAppListAccessDeclaration(): Boolean = Manifest.permission.QUERY_ALL_PACKAGES in packageManager
    .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
