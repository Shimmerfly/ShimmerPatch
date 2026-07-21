package top.nkbe.npatch.config

import top.nkbe.npatch.lspApp

object Configs {

    private const val PREFS_KEYSTORE_PASSWORD = "keystore_password"
    private const val PREFS_KEYSTORE_ALIAS = "keystore_alias"
    private const val PREFS_KEYSTORE_ALIAS_PASSWORD = "keystore_alias_password"
    private const val PREFS_STORAGE_DIRECTORY = "storage_directory"
    private const val PREFS_DETAIL_PATCH_LOGS = "detail_patch_logs"
    private const val PREFS_LANGUAGE = "language"
    private const val PREFS_WEL_SKIP = "WEL_SKIP"
    private const val PREFS_OUTPUT_FULL_LOG = "output_full_log"

    var language: String
        get() = lspApp.prefs.getString(PREFS_LANGUAGE, "") ?: ""
        set(value) {
            lspApp.prefs.edit().putString(PREFS_LANGUAGE, value).apply()
        }

    var keyStorePassword: String
        get() = lspApp.prefs.getString(PREFS_KEYSTORE_PASSWORD, "123456") ?: "123456"
        set(value) {
            lspApp.prefs.edit().putString(PREFS_KEYSTORE_PASSWORD, value).apply()
        }

    var keyStoreAlias: String
        get() = lspApp.prefs.getString(PREFS_KEYSTORE_ALIAS, "key0") ?: "key0"
        set(value) {
            lspApp.prefs.edit().putString(PREFS_KEYSTORE_ALIAS, value).apply()
        }

    var keyStoreAliasPassword: String
        get() = lspApp.prefs.getString(PREFS_KEYSTORE_ALIAS_PASSWORD, "123456") ?: "123456"
        set(value) {
            lspApp.prefs.edit().putString(PREFS_KEYSTORE_ALIAS_PASSWORD, value).apply()
        }

    var storageDirectory: String?
        get() = lspApp.prefs.getString(PREFS_STORAGE_DIRECTORY, null)
        set(value) {
            lspApp.prefs.edit().putString(PREFS_STORAGE_DIRECTORY, value).apply()
        }

    var detailPatchLogs: Boolean
        get() = lspApp.prefs.getBoolean(PREFS_DETAIL_PATCH_LOGS, true)
        set(value) {
            lspApp.prefs.edit().putBoolean(PREFS_DETAIL_PATCH_LOGS, value).apply()
        }

    var welcomeSeen: Boolean
        get() = lspApp.prefs.getBoolean(PREFS_WEL_SKIP, false)
        set(value) {
            lspApp.prefs.edit().putBoolean(PREFS_WEL_SKIP, value).apply()
        }

    var outputFullLog: Boolean
        get() = lspApp.prefs.getBoolean(PREFS_OUTPUT_FULL_LOG, false)
        set(value) {
            lspApp.prefs.edit().putBoolean(PREFS_OUTPUT_FULL_LOG, value).apply()
        }
}
