package top.nkbe.npatch.ui.page

import android.os.Build
import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.launch
import top.nkbe.npatch.R
import top.nkbe.npatch.LSPApplication
import top.nkbe.npatch.config.Configs
import top.nkbe.npatch.config.DEFAULT_CUSTOM_COLOR
import top.nkbe.npatch.config.ThemeConfig
import top.nkbe.npatch.config.ThemeMode
import top.nkbe.npatch.config.dataStore
import top.nkbe.npatch.ui.theme.LocalizedOverlay
import top.nkbe.npatch.ui.component.ambience.AmbienceKind
import top.nkbe.npatch.ui.activity.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppearanceSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val scope = rememberCoroutineScope()
    val mode = ThemeMode.fromValue(prefs[ThemeConfig.THEME_MODE] ?: ThemeMode.SYSTEM.value)
    val dynamic = prefs[ThemeConfig.USE_MONET] ?: false
    val seed = prefs[ThemeConfig.CUSTOM_COLOR] ?: DEFAULT_CUSTOM_COLOR
    val amoled = prefs[ThemeConfig.AMOLED_BLACK] ?: false
    val ambience = prefs[ThemeConfig.HEADER_AMBIENCE] ?: "circuit"
    val glass = prefs[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR] ?: false
    val blur = prefs[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR_BLUR]
        ?: ThemeConfig.isFloatingGlassBottomBarBlurSupported()
    fun update(block: androidx.datastore.preferences.core.MutablePreferences.() -> Unit) {
        scope.launch { context.dataStore.edit(block) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        LocalizedOverlay {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                SheetHeading(stringResource(R.string.home_appearance_theme), Icons.Rounded.Palette)
                SingleChoiceSegmentedButtonRow(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to Icons.Rounded.BrightnessAuto,
                        ThemeMode.LIGHT to Icons.Rounded.LightMode,
                        ThemeMode.DARK to Icons.Rounded.DarkMode,
                    )
                    modes.forEachIndexed { index, (item, icon) ->
                        SegmentedButton(
                            selected = mode == item,
                            onClick = { update { this[ThemeConfig.THEME_MODE] = item.value } },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            icon = {},
                            label = { Icon(icon, contentDescription = null, Modifier.size(20.dp)) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SheetHeading(stringResource(R.string.home_appearance_color), Icons.Rounded.Palette)
                ToggleRow(
                    stringResource(R.string.settings_monet_dynamic_color),
                    stringResource(R.string.settings_monet_dynamic_color_summary),
                    dynamic,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                ) { value -> update { this[ThemeConfig.USE_MONET] = value } }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HOME_SEEDS.forEach { color ->
                        val selected = !dynamic && seed == color
                        Box(
                            Modifier.size(44.dp)
                                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .padding(4.dp).clip(CircleShape).background(Color(color))
                                .clickable {
                                    update {
                                        this[ThemeConfig.USE_MONET] = false
                                        this[ThemeConfig.CUSTOM_COLOR] = color
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                ToggleRow(
                    stringResource(R.string.home_appearance_amoled),
                    stringResource(R.string.home_appearance_amoled_summary),
                    amoled,
                ) { value -> update { this[ThemeConfig.AMOLED_BLACK] = value } }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SheetHeading(stringResource(R.string.home_appearance_ambience), Icons.Rounded.Waves)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AmbienceKind.entries.forEach { kind ->
                        FilterChip(
                            selected = AmbienceKind.from(ambience) == kind,
                            onClick = { update { this[ThemeConfig.HEADER_AMBIENCE] = kind.key } },
                            label = { Text(stringResource(kind.labelRes)) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SheetHeading(stringResource(R.string.home_appearance_navigation), Icons.Rounded.Waves)
                ToggleRow(
                    stringResource(R.string.settings_floating_glass_bottom_bar),
                    stringResource(R.string.settings_floating_glass_bottom_bar_summary),
                    glass,
                ) { value -> update { this[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR] = value } }
                if (glass) {
                    ToggleRow(
                        stringResource(R.string.settings_floating_glass_bottom_bar_blur),
                        stringResource(R.string.settings_floating_glass_bottom_bar_blur_summary),
                        blur,
                        enabled = ThemeConfig.isFloatingGlassBottomBarBlurSupported(),
                    ) { value -> update { this[ThemeConfig.USE_FLOATING_GLASS_BOTTOM_BAR_BLUR] = value } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLanguageSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val systemLabel = stringResource(R.string.settings_language_system)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        LocalizedOverlay {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                SheetHeading(stringResource(R.string.settings_language), Icons.Rounded.Language)
                LANGUAGE_ENTRIES.forEach { (rawTag, rawLabel) ->
                    val tag = LSPApplication.normalizeLanguageTag(rawTag)
                    val label = if (rawLabel == "settings_language_system") systemLabel else rawLabel
                    val selected = LSPApplication.normalizeLanguageTag(Configs.language) == tag
                    ListItem(
                        headlineContent = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingContent = {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                            else Spacer(Modifier.size(24.dp))
                        },
                        modifier = Modifier.clickable {
                            if (!selected) {
                                Configs.language = tag
                                onDismiss()
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeading(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

private val HOME_SEEDS = listOf(
    0xFF6ABFCF.toInt(), 0xFFF27297.toInt(), 0xFF4CAF78.toInt(),
    0xFF8B6FC0.toInt(), 0xFFE27845.toInt(), 0xFF3F7FC4.toInt(),
)
