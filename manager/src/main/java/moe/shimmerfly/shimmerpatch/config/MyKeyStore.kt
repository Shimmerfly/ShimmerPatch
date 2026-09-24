package moe.shimmerfly.shimmerpatch.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.lspApp
import java.io.File

enum class KeystorePreset(val prefValue: String) {
    SHIMMERPATCH("shimmerpatch"),
    FPA("fpa"),
    CUSTOM("custom");

    companion object {
        fun fromPrefValue(value: String?, fallback: KeystorePreset): KeystorePreset {
            return values().firstOrNull { it.prefValue == value } ?: fallback
        }
    }
}

object MyKeyStore {

    val file = File("${lspApp.filesDir}/keystore.bks")
    val tmpFile = File("${lspApp.filesDir}/keystore.bks.tmp")

    private suspend fun installBuiltin(assetName: String) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            lspApp.assets.open(assetName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tmpFile.delete()
        }
    }

    suspend fun reset() {
        installBuiltin("shimmerpatch.key")
        Configs.keyStorePassword = "123456"
        Configs.keyStoreAlias = "key0"
        Configs.keyStoreAliasPassword = "123456"
        Configs.keyStorePreset = KeystorePreset.SHIMMERPATCH
    }

    suspend fun setBuiltinFpa() {
        installBuiltin("fpa_app.key")
        Configs.keyStorePassword = "12345678"
        Configs.keyStoreAlias = "app"
        Configs.keyStoreAliasPassword = "12345678"
        Configs.keyStorePreset = KeystorePreset.FPA
    }

    suspend fun setCustom(password: String, alias: String, aliasPassword: String) {
        withContext(Dispatchers.IO) {
            file.delete()
            tmpFile.copyTo(file, overwrite = true)
            tmpFile.delete()
            Configs.keyStorePassword = password
            Configs.keyStoreAlias = alias
            Configs.keyStoreAliasPassword = aliasPassword
            Configs.keyStorePreset = KeystorePreset.CUSTOM
        }
    }
}
