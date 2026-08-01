package top.nkbe.npatch.ui.page

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import nkbe.util.ShizukuApi
import top.nkbe.npatch.R
import top.nkbe.npatch.config.ThemeConfig
import top.nkbe.npatch.config.dataStore
import top.nkbe.npatch.share.LSPConfig
import top.nkbe.npatch.ui.component.VectorStatusHeader
import top.nkbe.npatch.ui.component.floatingGlassBottomBarContentPadding
import top.nkbe.npatch.ui.util.LocalFloatingGlassBottomBar

private const val REPO_URL = "https://github.com/7723mod/NPatch"
private const val TELEGRAM_URL = "https://t.me/NPatch"
private const val OFFICIAL_WEBSITE_URL = "https://www.nkbe.top"
private const val DISCUSSIONS_URL = "https://t.me/NPatch_HS"
private const val CANARY_URL = "https://t.me/ONPatch"

@SuppressLint("ContextCastToActivity")
@Composable
fun HomeScreen(
    navigator: Navigator,
    onManageShortcut: (Int) -> Unit = {},
    onRepoShortcut: () -> Unit = {},
    onSettingsShortcut: () -> Unit = {},
) {
    var isIntentLaunched by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as Activity
    val launchIntent = activity.intent
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    val prefs by activity.dataStore.data.collectAsState(initial = emptyPreferences())
    val ambience = prefs[ThemeConfig.HEADER_AMBIENCE] ?: "circuit"
    val useFloatingGlassBottomBar = LocalFloatingGlassBottomBar.current
    val bottomContentPadding = if (useFloatingGlassBottomBar) {
        floatingGlassBottomBarContentPadding()
    } else {
        24.dp
    }

    LaunchedEffect(Unit) {
        if (!isIntentLaunched && launchIntent.action == Intent.ACTION_VIEW &&
            launchIntent.hasCategory(Intent.CATEGORY_DEFAULT) &&
            launchIntent.type == "application/vnd.android.package-archive"
        ) {
            isIntentLaunched = true
            launchIntent.data?.let { navigator.navigate(Route.NewPatch(ACTION_INTENT_INSTALL, it.toString())) }
        }
        ShizukuApi.refreshState()
        ShizukuApi.addRequestPermissionResultListener(shizukuListener)
    }
    DisposableEffect(Unit) {
        onDispose { ShizukuApi.removeRequestPermissionResultListener(shizukuListener) }
    }

    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val collapse by remember {
        derivedStateOf {
            if (headerHeightPx == 0) 0f
            else if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / headerHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = with(density) { headerHeightPx.toDp() } + 16.dp,
                    bottom = bottomContentPadding,
                ),
            ) {
                item("take-part") {
                    TakePartSection(onOpen = activity::openUrl)
                    Spacer(Modifier.height(26.dp))
                }
                item("about") {
                    HomeAboutSection(
                        onOpenAbout = { navigator.navigate(Route.About) },
                        onOpenProject = { activity.openUrl(TELEGRAM_URL) },
                    )
                }
            }

            val available = ShizukuApi.isPermissionGranted
            VectorStatusHeader(
                active = available,
                status = stringResource(if (available) R.string.status_active else R.string.status_inactive),
                version = "${LSPConfig.instance.VERSION_NAME} (${LSPConfig.instance.VERSION_CODE})",
                apiVersion = LSPConfig.instance.API_CODE,
                onStatusClick = {
                    if (ShizukuApi.isBinderAvailable && !available) ShizukuApi.requestPermission()
                },
                onAppearanceClick = { showAppearance = true },
                onLanguageClick = { showLanguage = true },
                ambience = ambience,
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height }.graphicsLayer {
                    alpha = 1f - collapse
                    translationY = -collapse * headerHeightPx * 0.5f
                },
            )
        }
    }

    if (showAppearance) HomeAppearanceSheet { showAppearance = false }
    if (showLanguage) HomeLanguageSheet { showLanguage = false }
}

private val shizukuListener: (Int, Int) -> Unit = { _, grantResult ->
    ShizukuApi.isPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
    ShizukuApi.refreshState()
}

@Composable
private fun TakePartSection(onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_contribute), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProjectDoor(Icons.Rounded.RateReview, stringResource(R.string.home_review_change), Modifier.weight(1f).fillMaxHeight()) { onOpen(OFFICIAL_WEBSITE_URL) }
            ProjectDoor(Icons.Rounded.Forum, stringResource(R.string.home_discussions), Modifier.weight(1f).fillMaxHeight()) { onOpen(DISCUSSIONS_URL) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProjectDoor(Icons.Rounded.Science, stringResource(R.string.home_test_canary), Modifier.weight(1f).fillMaxHeight()) { onOpen(CANARY_URL) }
            ProjectDoor(Icons.Rounded.BugReport, stringResource(R.string.home_report_problem), Modifier.weight(1f).fillMaxHeight()) { onOpen("$REPO_URL/issues/new/choose") }
        }
    }
}

@Composable
private fun ProjectDoor(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Row(Modifier.fillMaxHeight().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HomeAboutSection(onOpenAbout: () -> Unit, onOpenProject: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_about), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.home_description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        InfoRow(stringResource(R.string.home_api_version), LSPConfig.instance.API_CODE.toString())
        InfoRow(stringResource(R.string.home_npatch_version), "${LSPConfig.instance.VERSION_NAME} (${LSPConfig.instance.VERSION_CODE})")
        InfoRow(stringResource(R.string.home_framework_version), "${LSPConfig.instance.CORE_VERSION_NAME} (${LSPConfig.instance.CORE_VERSION_CODE})")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProjectDoor(Icons.Rounded.Info, stringResource(R.string.home_about), Modifier.weight(1f).fillMaxHeight()) { onOpenAbout() }
            ProjectDoor(Icons.AutoMirrored.Rounded.Send, "Telegram", Modifier.weight(1f).fillMaxHeight()) { onOpenProject() }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun Activity.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
