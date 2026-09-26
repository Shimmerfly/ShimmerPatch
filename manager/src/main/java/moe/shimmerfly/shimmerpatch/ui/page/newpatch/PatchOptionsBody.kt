package moe.shimmerfly.shimmerpatch.ui.page.newpatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.config.Configs
import moe.shimmerfly.shimmerpatch.config.KeystorePreset
import moe.shimmerfly.shimmerpatch.share.Constants
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseItemContainer
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.DropDownMenuWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.DropdownOption
import moe.shimmerfly.shimmerpatch.ui.component.m3.RadioButtonWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn
import moe.shimmerfly.shimmerpatch.ui.component.m3.SwitchWidget
import moe.shimmerfly.shimmerpatch.ui.component.settings.CustomKeystoreDialog
import moe.shimmerfly.shimmerpatch.ui.component.settings.SettingsEditor
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.ViewAction

@Composable
fun ConfiguringFab() {
    val viewModel = viewModel<NewPatchViewModel>()
    val label = stringResource(R.string.patch_start)
    ExtendedFloatingActionButton(
        onClick = { viewModel.dispatch(ViewAction.SubmitPatch) },
        // Material 3 hides the animated text from semantics and uses the icon label.
        icon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = label) },
        text = { Text(label) },
    )
}

@Composable
fun sigBypassLvTitle(level: Int): String = stringResource(
    when (level) {
        0 -> R.string.patch_sigbypasslv0
        1 -> R.string.patch_sigbypasslv1
        2 -> R.string.patch_sigbypasslv2
        3 -> R.string.patch_sigbypasslv3
        else -> error("Invalid sigBypassLv: $level")
    }
)

@Composable
fun sigBypassLvDesc(level: Int): String = stringResource(
    when (level) {
        0 -> R.string.patch_sigbypasslv0_desc
        1 -> R.string.patch_sigbypasslv1_desc
        2 -> R.string.patch_sigbypasslv2_desc
        3 -> R.string.patch_sigbypasslv3_desc
        else -> error("Invalid sigBypassLv: $level")
    }
)

/**
 * The complete segmented settings layout and animated expandable items are reused from
 * WeKit ui/content/m3 (ported from InstallerX-Revived's Material 3 settings widgets).
 */
@Composable
fun PatchOptionsBody(modifier: Modifier, onAddEmbed: () -> Unit, onAddFromStorage: () -> Unit) {
    val viewModel = viewModel<NewPatchViewModel>()
    val app = viewModel.patchApp
    val appIcon by produceState<ImageBitmap?>(null, app) {
        // The selected ApplicationInfo points to either the installed app or the imported APK.
        // Avoid the package-name cache, which could contain the icon of another installed build.
        value = withContext(Dispatchers.IO) { NeoPackageManager.loadIconBitmap(app.app) }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
        item(key = "app") {
            SegmentedColumn {
                item {
                    BaseWidget(
                        iconContent = {
                            val bitmap = appIcon
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
                                )
                            } else Icon(Icons.Outlined.Android, null, Modifier.size(40.dp))
                        },
                        title = viewModel.patchApp.label,
                        titleStyle = MaterialTheme.typography.headlineSmall,
                        description = viewModel.patchApp.app.packageName,
                    )
                }
            }
        }
        item(key = "mode") {
            SegmentedColumn(
                title = stringResource(R.string.patch_mode),
                modifier = Modifier.selectableGroup(),
            ) {
                item(key = "local") {
                    RadioButtonWidget(
                        title = stringResource(R.string.patch_local),
                        description = stringResource(R.string.patch_local_desc),
                        icon = Icons.Outlined.Api,
                        selected = viewModel.useManager,
                        onSelect = { viewModel.setUseManager(true) },
                    )
                }
                item(key = "integrated") {
                    RadioButtonWidget(
                        title = stringResource(R.string.patch_integrated),
                        description = stringResource(R.string.patch_integrated_desc),
                        icon = Icons.Outlined.WorkOutline,
                        selected = !viewModel.useManager,
                        onSelect = { viewModel.setUseManager(false) },
                        extraContent = {
                            // The embedded modules expand inside the selected mode, where they belong:
                            // what is listed, what can be added, and how to take one back out.
                            AnimatedVisibility(
                                visible = !viewModel.useManager,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    if (viewModel.embeddedModules.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.patch_embed_modules_empty),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                                        )
                                    } else {
                                        viewModel.embeddedModules.forEach { module ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                val bitmap = module.appInfo?.let { NeoPackageManager.getIcon(it) }
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)),
                                                    )
                                                } else {
                                                    Icon(Icons.Outlined.Extension, null, Modifier.size(32.dp))
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        text = module.label,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = module.packageName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.removeEmbeddedModule(module.packageName) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.patch_embed_remove))
                                                }
                                            }
                                        }
                                    }
                                    TextButton(onClick = onAddEmbed) {
                                        Icon(Icons.Outlined.Extension, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.patch_embed_add_installed))
                                    }
                                    TextButton(onClick = onAddFromStorage) {
                                        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.patch_embed_add_storage))
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
        if (viewModel.hasSubProcesses) {
            item(key = "subprocess") {
                SegmentedColumn {
                    item {
                        BaseWidget(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.patch_inject_dex),
                            description = pluralStringResource(
                                R.plurals.patch_subprocess_detected_hint,
                                viewModel.subProcessCount,
                                viewModel.subProcessCount,
                            ),
                        )
                    }
                }
            }
        }
        item(key = "advanced") {
            SegmentedColumn(title = stringResource(R.string.patch_advanced)) {
                item(key = "package") {
                    BaseItemContainer {
                        SettingsEditor(
                            label = stringResource(R.string.patch_new_package),
                            text = viewModel.newPackageName,
                            onValueChange = { viewModel.newPackageName = it },
                        )
                    }
                }
                item(key = "label") {
                    BaseItemContainer {
                        SettingsEditor(
                            label = stringResource(R.string.patch_override_label),
                            text = viewModel.overrideLabel,
                            onValueChange = { viewModel.overrideLabel = it },
                            placeholder = stringResource(R.string.patch_override_label_hint),
                        )
                    }
                }
                item(key = "debuggable") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_debuggable),
                        icon = Icons.Outlined.BugReport,
                        checked = viewModel.debuggable,
                        onCheckedChange = { viewModel.debuggable = it },
                    )
                }
                expandableItem(
                    expanded = viewModel.overrideVersionCode,
                    topContent = {
                        SwitchWidget(
                            title = stringResource(R.string.patch_override_version_code),
                            description = stringResource(R.string.patch_override_version_code_desc),
                            icon = Icons.Outlined.Layers,
                            checked = viewModel.overrideVersionCode,
                            onCheckedChange = { viewModel.overrideVersionCode = it },
                        )
                    },
                    bottomContent = {
                        BaseItemContainer {
                            SettingsEditor(
                                label = stringResource(R.string.patch_custom_version_code),
                                text = viewModel.overrideVersionCodeValue,
                                onValueChange = { value -> viewModel.overrideVersionCodeValue = value.filter { it in '0'..'9' } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    },
                )
                expandableItem(
                    expanded = viewModel.overrideTargetSdk,
                    topContent = {
                        SwitchWidget(
                            title = stringResource(R.string.patch_override_target_sdk),
                            description = stringResource(R.string.patch_override_target_sdk_desc),
                            icon = Icons.Outlined.Android,
                            checked = viewModel.overrideTargetSdk,
                            onCheckedChange = { viewModel.overrideTargetSdk = it },
                        )
                    },
                    bottomContent = {
                        BaseItemContainer {
                            SettingsEditor(
                                label = stringResource(R.string.patch_custom_target_sdk),
                                text = viewModel.overrideTargetSdkValue,
                                onValueChange = { value -> viewModel.overrideTargetSdkValue = value.filter { it in '0'..'9' } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    },
                )
                item(key = "extract_native_libs") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_extract_native_libs),
                        description = stringResource(R.string.patch_extract_native_libs_desc),
                        icon = Icons.Outlined.Unarchive,
                        checked = viewModel.extractNativeLibs,
                        onCheckedChange = { viewModel.extractNativeLibs = it },
                    )
                }
                item(key = "hide_libs") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_hide_libs),
                        description = stringResource(R.string.patch_hide_libs_desc),
                        icon = Icons.Outlined.VisibilityOff,
                        // The loader only hides them once the signature bypass runs at all.
                        enabled = viewModel.sigBypassLevel >= Constants.SIGBYPASS_BASIC,
                        checked = viewModel.hideLibs,
                        onCheckedChange = { viewModel.hideLibs = it },
                    )
                }
                expandableItem(
                    expanded = viewModel.permissionsExpanded,
                    topContent = {
                        BaseWidget(
                            icon = Icons.Outlined.Key,
                            title = stringResource(R.string.patch_add_permission),
                            description = stringResource(
                                R.string.patch_add_permission_count,
                                viewModel.addedPermissions.size,
                            ),
                            onClick = { viewModel.permissionsExpanded = !viewModel.permissionsExpanded },
                            trailingContent = {
                                Icon(
                                    imageVector = if (viewModel.permissionsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    },
                    bottomContent = {
                        BaseItemContainer {
                            Column {
                                SettingsEditor(
                                    label = stringResource(R.string.patch_add_permission_hint),
                                    text = viewModel.permissionInput,
                                    onValueChange = { viewModel.permissionInput = it },
                                )
                                Text(
                                    text = stringResource(R.string.patch_add_permission_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                                viewModel.addedPermissions.forEach { permission ->
                                    PermissionRow(
                                        permission = permission,
                                        onRemove = { viewModel.removePermission(permission) },
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.addPermission() },
                                    enabled = viewModel.permissionInput.isNotBlank(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.patch_permission_add))
                                }
                            }
                        }
                    },
                )
                item(key = "provider") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_inject_mt_provider),
                        description = stringResource(R.string.patch_inject_mt_provider_desc),
                        icon = Icons.Outlined.AddCard,
                        checked = viewModel.injectProvider,
                        onCheckedChange = { viewModel.injectProvider = it },
                    )
                }
                item(key = "dex") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_inject_dex),
                        description = stringResource(R.string.patch_inject_dex_desc),
                        icon = Icons.Outlined.AccountTree,
                        checked = viewModel.injectDex,
                        onCheckedChange = { viewModel.injectDex = it },
                    )
                }
                item(key = "microg") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_use_microg),
                        description = stringResource(R.string.patch_use_microg_desc),
                        icon = Icons.Outlined.CloudSync,
                        checked = viewModel.useMicroG,
                        onCheckedChange = { viewModel.useMicroG = it },
                    )
                }
                item(key = "log") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_output_log_to_media),
                        description = stringResource(R.string.patch_output_log_to_media_desc),
                        icon = Icons.Outlined.Output,
                        checked = viewModel.outputLog,
                        onCheckedChange = { viewModel.outputLog = it },
                    )
                }
                item(key = "cleartext") {
                    SwitchWidget(
                        title = stringResource(R.string.patch_cleartext_traffic),
                        description = stringResource(R.string.patch_cleartext_traffic_desc),
                        icon = Icons.Outlined.Http,
                        checked = viewModel.usesCleartextTraffic,
                        onCheckedChange = { viewModel.usesCleartextTraffic = it },
                    )
                }
            }
        }
        item(key = "keystore") {
            val customLabel = stringResource(R.string.settings_keystore_custom)
            val presetName = { preset: KeystorePreset ->
                when (preset) {
                    KeystorePreset.NPATCH -> "NPatch"
                    KeystorePreset.FPA -> "FPA"
                    KeystorePreset.CUSTOM -> customLabel
                }
            }
            val defaultName = presetName(Configs.keyStorePreset)
            SegmentedColumn(title = stringResource(R.string.settings_keystore)) {
                item {
                    DropDownMenuWidget(
                        icon = Icons.Outlined.Key,
                        title = stringResource(R.string.patch_keystore),
                        // The widget shows the description instead of the picked label, so the
                        // effective keystore has to be spelled out here.
                        description = stringResource(
                            R.string.patch_keystore_temp_only,
                            when (viewModel.keystoreOverride) {
                                null -> presetName(Configs.keyStorePreset)
                                KeystorePreset.CUSTOM -> stringResource(
                                    R.string.patch_keystore_custom_file,
                                    viewModel.tempKeystoreLabel.orEmpty(),
                                )
                                else -> presetName(viewModel.keystoreOverride ?: Configs.keyStorePreset)
                            },
                        ),
                        value = viewModel.keystoreOverride,
                        options = listOf(
                            DropdownOption<KeystorePreset?>(
                                value = null,
                                label = stringResource(R.string.patch_keystore_follow_setting, defaultName),
                            ),
                            DropdownOption<KeystorePreset?>(KeystorePreset.NPATCH, "NPatch"),
                            DropdownOption<KeystorePreset?>(KeystorePreset.FPA, "FPA"),
                            // Named for what tapping it does: this one opens the import dialog
                            // instead of applying a preset straight away.
                            DropdownOption<KeystorePreset?>(
                                KeystorePreset.CUSTOM,
                                stringResource(R.string.patch_keystore_pick_file),
                            ),
                        ),
                        onValueChange = { viewModel.chooseKeystore(it) },
                    )
                }
            }
        }
        item(key = "signature") {
            SegmentedColumn(
                title = stringResource(R.string.patch_sigbypass),
                modifier = Modifier.selectableGroup(),
            ) {
                for (level in Constants.SIGBYPASS_NONE..Constants.SIGBYPASS_EXTREME) {
                    item(key = level) {
                        RadioButtonWidget(
                            title = sigBypassLvTitle(level),
                            description = sigBypassLvDesc(level),
                            icon = Icons.Outlined.Security,
                            selected = viewModel.sigBypassLevel == level,
                            onSelect = { viewModel.sigBypassLevel = level },
                        )
                    }
                }
            }
        }
    }

    if (viewModel.showKeystoreDialog) {
        CustomKeystoreDialog(
            show = true,
            stageFile = viewModel.keystoreStageFile,
            onDismiss = { viewModel.showKeystoreDialog = false },
            onConfirm = { file, name, password, alias, aliasPassword ->
                viewModel.setTemporaryKeystore(file, name, password, alias, aliasPassword)
            },
        )
    }
}

/** One declared permission, removable again while the extra-permission editor is open. */
@Composable
private fun PermissionRow(permission: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = permission,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.patch_permission_remove),
            )
        }
    }
}
