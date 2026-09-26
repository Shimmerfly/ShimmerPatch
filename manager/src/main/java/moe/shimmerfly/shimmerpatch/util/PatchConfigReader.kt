package moe.shimmerfly.shimmerpatch.util

import android.content.pm.ApplicationInfo
import android.util.Base64
import com.google.gson.Gson
import moe.shimmerfly.shimmerpatch.share.PatchConfig

/**
 * Reads the configuration our patcher stores in a patched app's manifest.
 *
 * The metadata key was renamed with the brand, so both spellings are read: a bundle this project
 * produced before the rename is still ours, and its configuration still parses into the same shape.
 * A bundle another patcher produced carries neither key, and its configuration is not ours to read.
 */
object PatchConfigReader {

    fun read(app: ApplicationInfo): PatchConfig? = runCatching {
        listOf(NeoPackageManager.META_DATA_SHIMMERPATCH, NeoPackageManager.META_DATA_NPATCH)
            .firstNotNullOfOrNull { key -> app.metaData?.getString(key) }
            ?.let { encoded ->
                val json = Base64.decode(encoded, Base64.DEFAULT).toString(Charsets.UTF_8)
                Gson().fromJson(json, PatchConfig::class.java)?.takeIf { config -> config.lspConfig != null }
            }
    }.getOrNull()
}
