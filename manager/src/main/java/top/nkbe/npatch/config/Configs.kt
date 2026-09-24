package top.nkbe.npatch.config

import androidx.core.content.edit
import top.nkbe.npatch.lspApp
import top.nkbe.npatch.ui.util.delegateStateOf
import top.nkbe.npatch.ui.util.getValue
import top.nkbe.npatch.ui.util.setValue
import java.io.File

object Configs {

    private const val PREFS_KEYSTORE_PRESET = "keystore_preset"
    private const val PREFS_KEYSTORE_PASSWORD = "keystore_password"
    private const val PREFS_KEYSTORE_ALIAS = "keystore_alias"
    private const val PREFS_KEYSTORE_ALIAS_PASSWORD = "keystore_alias_password"
    private const val PREFS_STORAGE_DIRECTORY = "storage_directory"
    private const val PREFS_DETAIL_PATCH_LOGS = "detail_patch_logs"
    private const val PREFS_LANGUAGE = "language"
    private const val PREFS_WEL_SKIP = "WEL_SKIP"

    private fun defaultKeyStorePreset(): KeystorePreset {
        return if (File(lspApp.filesDir, "keystore.bks").exists()) {
            KeystorePreset.CUSTOM
        } else {
            KeystorePreset.NPATCH
        }
    }

    var keyStorePreset by delegateStateOf(
        KeystorePreset.fromPrefValue(
            lspApp.prefs.getString(PREFS_KEYSTORE_PRESET, null),
            defaultKeyStorePreset()
        )
    ) {
        lspApp.prefs.edit { putString(PREFS_KEYSTORE_PRESET, it.prefValue) }
    }

    var language by delegateStateOf(lspApp.prefs.getString(PREFS_LANGUAGE, "")!!) {
        lspApp.prefs.edit { putString(PREFS_LANGUAGE, it) }
    }

    var keyStorePassword by delegateStateOf(lspApp.prefs.getString(PREFS_KEYSTORE_PASSWORD, "123456")!!) {
        lspApp.prefs.edit { putString(PREFS_KEYSTORE_PASSWORD, it) }
    }

    var keyStoreAlias by delegateStateOf(lspApp.prefs.getString(PREFS_KEYSTORE_ALIAS, "key0")!!) {
        lspApp.prefs.edit { putString(PREFS_KEYSTORE_ALIAS, it) }
    }

    var keyStoreAliasPassword by delegateStateOf(lspApp.prefs.getString(PREFS_KEYSTORE_ALIAS_PASSWORD, "123456")!!) {
        lspApp.prefs.edit { putString(PREFS_KEYSTORE_ALIAS_PASSWORD, it) }
    }

    var storageDirectory by delegateStateOf(lspApp.prefs.getString(PREFS_STORAGE_DIRECTORY, null)) {
        lspApp.prefs.edit { putString(PREFS_STORAGE_DIRECTORY, it) }
    }

    var detailPatchLogs by delegateStateOf(lspApp.prefs.getBoolean(PREFS_DETAIL_PATCH_LOGS, true)) {
        lspApp.prefs.edit { putBoolean(PREFS_DETAIL_PATCH_LOGS, it) }
    }

    var welcomeSeen by delegateStateOf(lspApp.prefs.getBoolean(PREFS_WEL_SKIP, false)) {
        lspApp.prefs.edit { putBoolean(PREFS_WEL_SKIP, it) }
    }

    private const val PREFS_OUTPUT_FULL_LOG = "output_full_log"

    var outputFullLog by delegateStateOf(lspApp.prefs.getBoolean(PREFS_OUTPUT_FULL_LOG, false)) {
        lspApp.prefs.edit { putBoolean(PREFS_OUTPUT_FULL_LOG, it) }
    }

    private const val PREFS_INSTALL_NOTIFICATION = "install_notification_enabled"
    var installNotificationEnabled by delegateStateOf(lspApp.prefs.getBoolean(PREFS_INSTALL_NOTIFICATION, false)) {
        lspApp.prefs.edit { putBoolean(PREFS_INSTALL_NOTIFICATION, it) }
    }

    private const val PREFS_INSTALL_ALL_USERS = "install_all_users"
    var installAllUsers by delegateStateOf(lspApp.prefs.getBoolean(PREFS_INSTALL_ALL_USERS, false)) {
        lspApp.prefs.edit { putBoolean(PREFS_INSTALL_ALL_USERS, it) }
    }

    private const val PREFS_THIRD_PARTY_INSTALLER = "third_party_installer_package"
    var thirdPartyInstallerPackage by delegateStateOf(lspApp.prefs.getString(PREFS_THIRD_PARTY_INSTALLER, "") ?: "") {
        lspApp.prefs.edit { putString(PREFS_THIRD_PARTY_INSTALLER, it) }
    }

    private const val PREFS_CUSTOM_INSTALLER = "custom_installer_package"
    var customInstallerPackage by delegateStateOf(lspApp.prefs.getString(PREFS_CUSTOM_INSTALLER, "") ?: "") {
        lspApp.prefs.edit { putString(PREFS_CUSTOM_INSTALLER, it) }
    }
}
