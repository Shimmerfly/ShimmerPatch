package moe.shimmerfly.shimmerpatch.ui.page

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.LSPApplication
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.config.*
import moe.shimmerfly.shimmerpatch.install.DiscoveredInstaller
import moe.shimmerfly.shimmerpatch.install.InstallNotificationHelper
import moe.shimmerfly.shimmerpatch.install.ThirdPartyPackageInstaller
import moe.shimmerfly.shimmerpatch.manager.ManagerCacheCleaner
import moe.shimmerfly.shimmerpatch.manager.ManagerLogger
import moe.shimmerfly.shimmerpatch.network.DnsProvider
import moe.shimmerfly.shimmerpatch.network.NetworkDns
import moe.shimmerfly.shimmerpatch.ui.activity.MainActivity
import moe.shimmerfly.shimmerpatch.ui.component.NPatchScaffold
import moe.shimmerfly.shimmerpatch.ui.component.NPatchTopAppBar
import moe.shimmerfly.shimmerpatch.ui.component.rememberMaterial3BlurBackdrop
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarBlur
import moe.shimmerfly.shimmerpatch.ui.component.m3AppBarColor
import moe.shimmerfly.shimmerpatch.ui.component.m3BackdropLayer
import moe.shimmerfly.shimmerpatch.ui.component.m3.*
import moe.shimmerfly.shimmerpatch.ui.util.BackgroundImageStorage
import moe.shimmerfly.shimmerpatch.ui.util.LocalSnackbarHost
import moe.shimmerfly.shimmerpatch.ui.util.LocalThemeSettings
import moe.shimmerfly.shimmerpatch.ui.util.LocalFloatingGlassBottomBarBlur
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import kotlin.math.roundToInt

private const val TAG = "SettingsScreen"

/** Settings groups and item shapes are copied from WeKit's Material 3 settings UI. */
@Composable
fun SettingsScreen(contentPadding: PaddingValues = PaddingValues()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val backdrop = rememberMaterial3BlurBackdrop(enabled = LocalFloatingGlassBottomBarBlur.current)
    NPatchScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NPatchTopAppBar(
                title = stringResource(R.string.screen_settings),
                modifier = Modifier.m3AppBarBlur(backdrop),
                color = backdrop.m3AppBarColor(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize()
                .m3BackdropLayer(backdrop)
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(contentPadding.calculateBottomPadding(), innerPadding.calculateBottomPadding()) + 24.dp,
                ),
        ) {
            AppearanceSettings()
            InstallationSettings()
            SegmentedColumn(title = stringResource(R.string.settings_network)) {
                item(key = "dns") { DnsPreference() }
            }
            SegmentedColumn(title = stringResource(R.string.settings_other_settings)) {
                item(key = "language") { LanguagePreference() }
                item(key = "keystore") { KeyStorePreference() }
                item(key = "patch_logs") { DetailPatchLogs() }
                item(key = "full_logs") { OutputFullLog() }
                item(key = "welcome") { WelcomeGuide() }
                item(key = "storage") { StorageDirectory() }
                item(key = "cache") { ClearManagerCache() }
            }
        }
    }
}

/** Row and chevron copied from InstallerX-Revived's NavigationItemWidget. */
@Composable
private fun SettingsAction(
    title: String,
    icon: ImageVector,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    BaseWidget(title = title, icon = icon, description = description, enabled = enabled, onClick = onClick) {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SettingsChoice(
    title: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var show by rememberSaveable { mutableStateOf(false) }
    SettingsAction(title, icon, options.getOrNull(selectedIndex), onClick = { show = true })
    SettingsDialog(show = show, title = title, onDismissRequest = { show = false }, scrollable = false) {
        // Radio controls use the same full-row selection contract as InstallerX's dialogs.
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().selectableGroup()) {
            lazySegmentedItems(options.indices.toList(), key = { it }) { index ->
                RadioButtonWidget(
                    title = options[index],
                    selected = index == selectedIndex,
                    onSelect = {
                        show = false
                        onSelected(index)
                    },
                )
            }
        }
    }
}

@Composable
fun AppearanceSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    // The Activity owns this state. Re-entering this page never renders default DataStore values.
    val theme = LocalThemeSettings.current
    val unknownError = stringResource(R.string.error_unknown)
    val alphaTitle = stringResource(R.string.settings_card_background_alpha)
    var alpha by remember(theme.cardBackgroundAlphaPercent) {
        mutableFloatStateOf(theme.cardBackgroundAlphaPercent.toFloat())
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val storedPath = BackgroundImageStorage.persistFromUri(context, uri)
                context.dataStore.edit { it[ThemeConfig.BG_IMAGE_URI] = storedPath }
            }.onFailure {
                Log.e(TAG, "Failed to persist background image", it)
                snackbarHost.showSnackbar(unknownError)
            }
        }
    }
    val themeModes = listOf(
        DropdownOption(0, stringResource(R.string.settings_theme_mode_system), Icons.Outlined.SettingsBrightness),
        DropdownOption(1, stringResource(R.string.settings_theme_mode_light), Icons.Outlined.LightMode),
        DropdownOption(2, stringResource(R.string.settings_theme_mode_dark), Icons.Outlined.DarkMode),
    )
    SegmentedColumn(title = stringResource(R.string.settings_appearance_theme)) {
        item(key = "theme_mode") {
            DropDownMenuWidget(
                title = stringResource(R.string.settings_theme_mode),
                icon = Icons.Outlined.SettingsBrightness,
                options = themeModes,
                value = theme.themeMode.value,
                onValueChange = { index -> scope.launch { context.dataStore.edit { it[ThemeConfig.THEME_MODE] = index } } },
            )
        }
        item(key = "monet") {
            SwitchWidget(
                title = stringResource(R.string.settings_monet_dynamic_color),
                description = stringResource(R.string.settings_monet_dynamic_color_summary),
                icon = Icons.Outlined.Palette,
                checked = theme.useMonet,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = { checked -> scope.launch { context.dataStore.edit { it[ThemeConfig.USE_MONET] = checked } } },
            )
        }
        item(key = "floating_navigation") {
            SwitchWidget(
                title = stringResource(R.string.settings_floating_glass_bottom_bar),
                description = stringResource(R.string.settings_floating_glass_bottom_bar_summary),
                icon = Icons.Outlined.Dashboard,
                checked = theme.useFloatingGlassBottomBar,
                onCheckedChange = { checked -> scope.launch { context.dataStore.edit { it[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR] = checked } } },
            )
        }
        item(key = "navigation_blur", animatedVisibility = theme.useFloatingGlassBottomBar) {
            SwitchWidget(
                title = stringResource(R.string.settings_floating_glass_bottom_bar_blur),
                description = stringResource(R.string.settings_floating_glass_bottom_bar_blur_summary),
                icon = Icons.Outlined.BlurCircular,
                checked = theme.useFloatingGlassBottomBarBlur,
                enabled = ThemeConfig.isFloatingGlassBottomBarBlurSupported(),
                onCheckedChange = { checked -> scope.launch { context.dataStore.edit { it[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR_BLUR] = checked } } },
            )
        }
        item(key = "background") {
            BaseWidget(
                title = stringResource(R.string.settings_custom_background_image),
                icon = Icons.Outlined.Image,
                onClick = { imagePicker.launch(arrayOf("image/*")) },
            ) {
                if (theme.backgroundImageUri.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching {
                                BackgroundImageStorage.clear(context)
                                context.dataStore.edit { it[ThemeConfig.BG_IMAGE_URI] = "" }
                            }.onFailure { snackbarHost.showSnackbar(unknownError) }
                        }
                    }) { Text(stringResource(R.string.settings_clear)) }
                } else {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        item(key = "card_opacity") {
            BaseItemContainer {
                BaseWidget(
                    title = alphaTitle,
                    description = stringResource(R.string.settings_card_background_alpha_summary),
                    icon = Icons.Outlined.Palette,
                ) { Text("${alpha.roundToInt()}%", style = MaterialTheme.typography.labelLarge) }
                val alphaSliderState = remember {
                    SliderState(
                        value = alpha,
                        steps = CARD_BACKGROUND_ALPHA_MAX - CARD_BACKGROUND_ALPHA_MIN - 1,
                        trackRange = CARD_BACKGROUND_ALPHA_MIN.toFloat()..CARD_BACKGROUND_ALPHA_MAX.toFloat(),
                    )
                }
                alphaSliderState.value = alpha
                Slider(
                    state = alphaSliderState,
                    onValueChange = { alpha = it.roundToInt().toFloat() },
                    track = { sliderState ->
                        SliderDefaults.Track(sliderState = sliderState, drawTick = { _, _ -> })
                    },
                    onValueChangeFinished = {
                        val percent = alpha.roundToInt().coerceIn(CARD_BACKGROUND_ALPHA_MIN, CARD_BACKGROUND_ALPHA_MAX)
                        scope.launch { context.dataStore.edit { it[ThemeConfig.CARD_BACKGROUND_ALPHA_PERCENT] = percent } }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp)
                        .semantics {
                            contentDescription = alphaTitle
                            stateDescription = "${alpha.roundToInt()}%"
                        },
                )
            }
        }
        item(key = "palette", animatedVisibility = !theme.useMonet) {
            ThemePalette(theme.customColor) { color ->
                scope.launch { context.dataStore.edit { it[ThemeConfig.CUSTOM_COLOR] = color } }
            }
        }
    }
}

@Composable
private fun ThemePalette(selectedColor: Int, onSelected: (Int) -> Unit) {
    val palettes = listOf(
        DEFAULT_CUSTOM_COLOR to R.string.settings_color_cherry_blossom,
        0xFF007AFF.toInt() to R.string.settings_color_default_blue,
        0xFF34C759.toInt() to R.string.settings_color_fresh_green,
        0xFFAF52DE.toInt() to R.string.settings_color_elegant_purple,
        0xFFFF9500.toInt() to R.string.settings_color_vibrant_orange,
        0xFF00BCD4.toInt() to R.string.settings_color_cyan,
        0xFF81C784.toInt() to R.string.settings_color_mint_green,
        0xFFF06292.toInt() to R.string.settings_color_pink,
        0xFFD81B60.toInt() to R.string.settings_color_deep_pink,
        0xFF64B5F6.toInt() to R.string.settings_color_ice_blue,
        0xFFE91E63.toInt() to R.string.settings_color_rose,
    )
    BaseItemContainer {
        BaseWidget(title = stringResource(R.string.settings_builtin_theme_color), icon = Icons.Outlined.Palette)
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEach { (value, name) ->
                val label = stringResource(name)
                Column(
                    modifier = Modifier.width(80.dp).clip(MaterialTheme.shapes.medium)
                        .selectable(selected = selectedColor == value, role = Role.RadioButton, onClick = { onSelected(value) })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(value)), contentAlignment = Alignment.Center) {
                        if (selectedColor == value) Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = if (Color(value).luminance() > 0.5f) Color.Black else Color.White,
                        )
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun InstallationSettings() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Configs.installNotificationEnabled = granted
    }
    SegmentedColumn(title = stringResource(R.string.settings_installation_category)) {
        item(key = "notifications") {
            SwitchWidget(
                title = stringResource(R.string.settings_install_notification),
                description = stringResource(R.string.settings_install_notification_summary),
                icon = Icons.Outlined.ArrowUpward,
                checked = Configs.installNotificationEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !InstallNotificationHelper.hasNotificationPermission(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Configs.installNotificationEnabled = enabled
                    }
                },
            )
        }
        item(key = "all_users") {
            SwitchWidget(
                title = stringResource(R.string.settings_install_all_users),
                description = stringResource(R.string.settings_install_all_users_summary),
                icon = Icons.Outlined.Person,
                checked = Configs.installAllUsers,
                onCheckedChange = { Configs.installAllUsers = it },
            )
        }
        item(key = "installer") { InstallerPreference() }
    }
}

@Composable
private fun InstallerPreference() {
    val context = LocalContext.current
    val packageName = Configs.thirdPartyInstallerPackage
    val defaultLabel = stringResource(R.string.settings_third_party_installer_system_default)
    var show by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(packageName) }
    var customPackage by rememberSaveable { mutableStateOf(Configs.customInstallerPackage) }
    var customSelected by rememberSaveable { mutableStateOf(false) }
    var discovered by remember { mutableStateOf<List<DiscoveredInstaller>>(emptyList()) }
    var discovering by remember { mutableStateOf(false) }
    val currentSummary by produceState(initialValue = packageName.ifBlank { defaultLabel }, packageName, defaultLabel) {
        value = if (packageName.isBlank()) defaultLabel else withContext(Dispatchers.IO) {
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                "${context.packageManager.getApplicationLabel(info)} ($packageName)"
            }.getOrDefault(packageName)
        }
    }
    LaunchedEffect(show) {
        if (show) {
            discovering = true
            try {
                discovered = withContext(Dispatchers.IO) {
                    runCatching { ThirdPartyPackageInstaller.getDiscoveredInstallers(context) }
                        .onFailure { Log.e(TAG, "Failed to discover package installers", it) }
                        .getOrDefault(emptyList())
                }
                if (draft.isNotBlank() && discovered.none { it.packageName == draft }) {
                    customPackage = draft
                    customSelected = true
                }
            } finally {
                discovering = false
            }
        }
    }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    SettingsAction(
        title = stringResource(R.string.settings_third_party_installer),
        description = currentSummary,
        icon = Icons.Outlined.Android,
        onClick = {
            draft = packageName
            customPackage = Configs.customInstallerPackage
            customSelected = packageName.isNotBlank() && packageName == customPackage
            showEditor = false
            show = true
        },
    )
    SettingsDialog(
        show = show,
        title = stringResource(R.string.settings_third_party_installer_dialog_title),
        onDismissRequest = { if (!saving) show = false },
        scrollable = false,
        confirmButton = {
            TextButton(enabled = !saving && !discovering, onClick = {
                val selectedPackage = if (customSelected) customPackage else draft
                saving = true
                scope.launch {
                    try {
                        val valid = (!customSelected && selectedPackage.isBlank()) || withContext(Dispatchers.IO) {
                            ThirdPartyPackageInstaller.isInstallerValid(context, selectedPackage)
                        }
                        if (valid) {
                            Configs.customInstallerPackage = customPackage
                            Configs.thirdPartyInstallerPackage = selectedPackage
                            show = false
                        } else {
                            customPackage = selectedPackage
                            customSelected = true
                            showEditor = true
                        }
                    } finally { saving = false }
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
    ) {
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().selectableGroup()) {
            val customIndex = discovered.size + 1
            lazySegmentedItems(
                items = (0..customIndex).toList(),
                key = { index ->
                    when (index) {
                        0 -> "system"
                        customIndex -> "custom"
                        else -> discovered[index - 1].packageName
                    }
                },
            ) { index ->
                when (index) {
                    0 -> RadioButtonWidget(
                        title = defaultLabel,
                        selected = !customSelected && draft.isBlank(),
                        onSelect = { customSelected = false; draft = "" },
                    )
                    customIndex -> CustomValueOption(
                        title = stringResource(R.string.settings_third_party_installer_custom),
                        value = customPackage,
                        selected = customSelected,
                        onEdit = { showEditor = true },
                        onSelect = { customSelected = true },
                    )
                    else -> {
                        val installer = discovered[index - 1]
                        RadioButtonWidget(
                            title = installer.label,
                            description = installer.packageName,
                            selected = !customSelected && draft == installer.packageName,
                            onSelect = { customSelected = false; draft = installer.packageName },
                        )
                    }
                }
            }
        }
        if (discovering) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    CustomValueDialog(
        show = show && showEditor,
        title = stringResource(R.string.settings_third_party_installer_custom),
        label = stringResource(R.string.settings_third_party_installer_custom_hint),
        value = customPackage,
        errorText = stringResource(R.string.settings_third_party_installer_invalid_pkg),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        onDismissRequest = { showEditor = false },
        validate = { value -> withContext(Dispatchers.IO) { ThirdPartyPackageInstaller.isInstallerValid(context, value) } },
        onConfirm = { customPackage = it },
    )
}

@Composable
private fun DnsPreference() {
    var selected by remember { mutableStateOf(NetworkDns.selectedProvider()) }
    var show by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(selected) }
    var customUrl by rememberSaveable { mutableStateOf(NetworkDns.customUrl()) }
    val providers = DnsProvider.entries
    val labels = listOf(
        stringResource(R.string.settings_dns_tencent),
        stringResource(R.string.settings_dns_google),
        stringResource(R.string.settings_dns_cloudflare),
        stringResource(R.string.settings_dns_system),
        stringResource(R.string.settings_dns_custom),
    )
    SettingsAction(
        title = stringResource(R.string.settings_dns),
        description = "${labels[providers.indexOf(selected)]} · ${stringResource(R.string.settings_dns_summary)}",
        icon = Icons.Outlined.Language,
        onClick = { draft = selected; customUrl = NetworkDns.customUrl(); showEditor = false; show = true },
    )
    SettingsDialog(
        show = show,
        title = stringResource(R.string.settings_dns),
        onDismissRequest = { show = false },
        scrollable = false,
        confirmButton = {
            TextButton(onClick = {
                if (customUrl.isNotBlank()) {
                    if (!NetworkDns.setCustomUrl(customUrl, selectProvider = false)) {
                        showEditor = true
                        return@TextButton
                    }
                } else if (draft == DnsProvider.CUSTOM) {
                    showEditor = true
                    return@TextButton
                }
                NetworkDns.setProvider(draft)
                selected = draft
                show = false
            }) { Text(stringResource(android.R.string.ok)) }
        },
    ) {
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().selectableGroup()) {
            lazySegmentedItems(providers, key = { it.name }) { provider ->
                if (provider == DnsProvider.CUSTOM) {
                    CustomValueOption(
                        title = labels[provider.ordinal],
                        value = customUrl,
                        selected = draft == provider,
                        selectionEnabled = NetworkDns.isValidCustomUrl(customUrl),
                        onEdit = { showEditor = true },
                        onSelect = { draft = provider },
                    )
                } else {
                    RadioButtonWidget(
                        title = labels[provider.ordinal],
                        selected = draft == provider,
                        onSelect = { draft = provider },
                    )
                }
            }
        }
    }
    CustomValueDialog(
        show = show && showEditor,
        title = stringResource(R.string.settings_dns_custom),
        label = stringResource(R.string.settings_dns_custom_url),
        value = customUrl,
        errorText = stringResource(R.string.settings_dns_custom_invalid),
        description = stringResource(R.string.settings_dns_custom_summary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        onDismissRequest = { showEditor = false },
        validate = { NetworkDns.isValidCustomUrl(it) },
        onConfirm = { customUrl = it },
    )
}

private val LANGUAGE_ENTRIES = listOf(
    "" to "settings_language_system",
    "en" to "English",
    "zh-CN" to "中文 (简体)",
    "zh-MO" to "中文 (喵喵)",
    "zh-TW" to "中文 (繁體)",
    "zh-HK" to "中文 (香港)",
    "ja" to "日本語",
    "ko" to "한국어",
    "fr" to "Français",
    "de" to "Deutsch",
    "es" to "Español",
    "it" to "Italiano",
    "pt" to "Português",
    "pt-BR" to "Português (Brasil)",
    "ru" to "Русский",
    "ar" to "العربية",
    "tr" to "Türkçe",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "uk" to "Українська",
    "vi" to "Tiếng Việt",
    "th" to "ภาษาไทย",
    "hi" to "हिन्दी",
    "af" to "Afrikaans",
    "bg" to "Български",
    "bn" to "বাংলা",
    "ca" to "Català",
    "cs" to "Čeština",
    "da" to "Dansk",
    "el" to "Ελληνικά",
    "et" to "Eesti",
    "fa" to "فارسی",
    "fi" to "Suomi",
    "hr" to "Hrvatski",
    "hu" to "Magyar",
    "in" to "Bahasa Indonesia",
    "iw" to "עברית",
    "ku" to "Kurdî",
    "lt" to "Lietuvių",
    "no" to "Norsk",
    "ro" to "Română",
    "si" to "සිංහල",
    "sk" to "Slovenčina",
    "sv" to "Svenska",
    "ur" to "اردو",
)

@Composable
fun LanguagePreference() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val systemLabel = stringResource(R.string.settings_language_system)
    val labels = remember(systemLabel) {
        LANGUAGE_ENTRIES.map { (_, label) -> if (label == "settings_language_system") systemLabel else label }
    }
    val selectedIndex = LANGUAGE_ENTRIES.indexOfFirst {
        LSPApplication.normalizeLanguageTag(it.first) == LSPApplication.normalizeLanguageTag(Configs.language)
    }.coerceAtLeast(0)
    SettingsChoice(
        title = stringResource(R.string.settings_language), icon = Icons.Outlined.Language,
        options = labels, selectedIndex = selectedIndex,
        onSelected = { index ->
            if (index != selectedIndex) {
                Configs.language = LSPApplication.normalizeLanguageTag(LANGUAGE_ENTRIES[index].first)
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                activity?.finish()
            }
        },
    )
}

@Composable
private fun KeyStorePreference() {
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val unknownError = stringResource(R.string.error_unknown)
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val currentPreset = Configs.keyStorePreset
    DropDownMenuWidget(
        title = stringResource(R.string.settings_keystore), icon = Icons.Outlined.Key,
        options = listOf(
            DropdownOption(0, "NPatch"),
            DropdownOption(1, "FPA"),
            DropdownOption(2, stringResource(R.string.settings_keystore_custom)),
        ),
        value = currentPreset.ordinal,
        onValueChange = { index ->
            if (index == 2) showCustom = true else scope.launch {
                runCatching { if (index == 0) MyKeyStore.reset() else MyKeyStore.setBuiltinFpa() }
                    .onFailure { Log.e(TAG, "Failed to change keystore", it); snackbarHost.showSnackbar(unknownError) }
            }
        },
    )
    CustomKeystoreDialog(show = showCustom, onDismiss = { showCustom = false })
}

@Composable
private fun CustomKeystoreDialog(show: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var path by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var aliasPassword by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var previouslyShown by rememberSaveable { mutableStateOf(show) }
    LaunchedEffect(show) {
        if (show && !previouslyShown) {
            path = ""
            password = ""
            alias = ""
            aliasPassword = ""
            error = null
        }
        previouslyShown = show
    }
    // Keep the result launcher registered while its external picker is on screen.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("No keystore input")
                    input.use { source -> MyKeyStore.tmpFile.outputStream().use { source.copyTo(it) } }
                }
                path = uri.lastPathSegment.orEmpty()
                error = null
            } catch (failure: Exception) {
                Log.e(TAG, "Failed to read keystore", failure)
                error = R.string.settings_keystore_wrong_keystore
            } finally { busy = false }
        }
    }
    SettingsDialog(
        show = show,
        title = stringResource(R.string.settings_keystore_dialog_title),
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                error = null
                if (path.isBlank()) {
                    error = R.string.settings_keystore_wrong_keystore
                    return@TextButton
                }
                busy = true
                scope.launch {
                    try {
                        error = withContext(Dispatchers.IO) { validateKeystore(password, alias, aliasPassword) }
                        if (error == null) {
                            MyKeyStore.setCustom(password, alias, aliasPassword)
                            onDismiss()
                        }
                    } catch (failure: Exception) {
                        Log.e(TAG, "Failed to import keystore", failure)
                        error = R.string.settings_keystore_wrong_keystore
                    } finally { busy = false }
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
    ) {
        Text(stringResource(R.string.settings_keystore_desc), style = MaterialTheme.typography.bodyMedium)
        error?.let { SettingsErrorText(stringResource(it)) }
        // A real accessible button replaces the old read-only field / PressInteraction interception.
        OutlinedButton(enabled = !busy, onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(path.ifBlank { stringResource(R.string.settings_keystore_file) })
        }
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = alias, onValueChange = { alias = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_alias)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = aliasPassword, onValueChange = { aliasPassword = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_alias_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

private fun validateKeystore(password: String, alias: String, aliasPassword: String): Int? {
    val keystore = KeyStore.getInstance("BKS")
    try {
        MyKeyStore.tmpFile.inputStream().use { keystore.load(it, password.toCharArray()) }
    } catch (error: IOException) {
        return if (error.message == "KeyStore integrity check failed.") R.string.settings_keystore_wrong_password
        else R.string.settings_keystore_wrong_keystore
    }
    if (!keystore.containsAlias(alias)) return R.string.settings_keystore_wrong_alias
    try {
        if (keystore.getKey(alias, aliasPassword.toCharArray()) == null) return R.string.settings_keystore_wrong_alias
    } catch (_: GeneralSecurityException) {
        return R.string.settings_keystore_wrong_alias_password
    }
    return null
}

@Composable
private fun DetailPatchLogs() {
    SwitchWidget(
        title = stringResource(R.string.settings_detail_patch_logs), icon = Icons.Outlined.BugReport,
        checked = Configs.detailPatchLogs, onCheckedChange = { Configs.detailPatchLogs = it },
    )
}

@Composable
private fun OutputFullLog() {
    SwitchWidget(
        title = stringResource(R.string.settings_output_full_log),
        description = stringResource(R.string.settings_output_full_log_summary),
        icon = Icons.Outlined.Description, checked = Configs.outputFullLog,
        onCheckedChange = { Configs.outputFullLog = it; ManagerLogger.setEnabled(it) },
    )
}

@Composable
private fun WelcomeGuide() {
    val navigator = LocalNavigator.current
    SettingsAction(
        title = stringResource(R.string.settings_view_welcome),
        description = stringResource(R.string.settings_view_welcome_summary), icon = Icons.Outlined.Info,
        onClick = { navigator.push(Route.Welcome(reviewMode = true)) },
    )
}

@Composable
fun StorageDirectory() {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val errorText = stringResource(R.string.patch_select_dir_error)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                Configs.storageDirectory = uri.toString()
            } catch (failure: Exception) {
                Log.e(TAG, "Error when requesting saving directory", failure)
                scope.launch { snackbarHost.showSnackbar(errorText) }
            }
        }
    }
    SettingsAction(
        title = stringResource(R.string.settings_storage_directory), description = Configs.storageDirectory,
        icon = Icons.Outlined.Folder, onClick = { launcher.launch(null) },
    )
}

@Composable
fun ClearManagerCache() {
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val successText = stringResource(R.string.settings_manager_cache_success)
    val failedText = stringResource(R.string.settings_manager_cache_failed)
    var show by rememberSaveable { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    SettingsAction(
        title = stringResource(R.string.settings_manager_cache),
        description = stringResource(R.string.settings_manager_cache_summary),
        icon = Icons.Outlined.DeleteSweep, enabled = !clearing, onClick = { show = true },
    )
    SettingsDialog(
        show = show, title = stringResource(R.string.settings_manager_cache),
        onDismissRequest = { show = false },
        confirmButton = {
            TextButton(onClick = {
                show = false
                clearing = true
                scope.launch {
                    try {
                        ManagerCacheCleaner.clear()
                        snackbarHost.showSnackbar(successText)
                    } catch (failure: Exception) {
                        Log.e(TAG, "Failed to clear manager cache", failure)
                        snackbarHost.showSnackbar(failedText)
                    } finally { clearing = false }
                }
            }) { Text(stringResource(android.R.string.ok), color = MaterialTheme.colorScheme.error) }
        },
    ) { Text(stringResource(R.string.settings_manager_cache_dialog_text)) }
}
