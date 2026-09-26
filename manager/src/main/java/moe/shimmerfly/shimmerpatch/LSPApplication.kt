package moe.shimmerfly.shimmerpatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.content.edit
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import moe.shimmerfly.shimmerpatch.manager.AppBroadcastReceiver
import moe.shimmerfly.shimmerpatch.manager.ManagerLogger
import moe.shimmerfly.shimmerpatch.manager.ManagerIntegrity
import moe.shimmerfly.shimmerpatch.manager.ModuleScopeSyncStore
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.ShizukuApi
import java.io.File

lateinit var lspApp: LSPApplication

class LSPApplication : Application() {

    lateinit var prefs: SharedPreferences
    lateinit var tmpApkDir: File

    var targetApkFiles: ArrayList<File>? = null
    val globalScope = CoroutineScope(Dispatchers.Default)


    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val rawLanguage = prefs.getString("language", "") ?: ""
        val language = normalizeLanguageTag(rawLanguage)
        if (language != rawLanguage) {
            prefs.edit { putString("language", language) }
        }
        super.attachBaseContext(applyLocale(base, language))
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
            .onFailure { it.printStackTrace() }
        ManagerIntegrity.verifyOnStartup(this)

        try {
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lspApp = this
        System.setProperty("java.io.tmpdir", cacheDir.absolutePath)
        filesDir.mkdir()
        tmpApkDir = noBackupFilesDir.resolve("apk").also { it.mkdirs() }
        prefs = lspApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
        ManagerLogger.init()
        ShizukuApi.init()
        ShizukuApi.addOnReadyListener {
            globalScope.launch {
                NeoPackageManager.fetchAppList()
            }
            globalScope.launch {
                ModuleScopeSyncStore.syncTrackedModuleScopes()
            }
        }
        // Coil builds its loader on the first request, which used to be the first frame of the
        // about page: that frame paid for OkHttp, the disk cache and the dispatchers at once and
        // the page visibly hitched. Build it here, on a frame nobody is waiting on.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .crossfade(true)
                .build()
        )
        AppBroadcastReceiver.register(this)
        if (!ShizukuApi.isReady) {
            globalScope.launch {
                NeoPackageManager.fetchAppList()
            }
        }
    }

    companion object {
        private const val LEGACY_NYA_LANGUAGE_TAG = "zh-x-nya"
        private const val NYA_LANGUAGE_TAG = "zh-MO"

        fun normalizeLanguageTag(languageTag: String): String {
            return when (languageTag) {
                LEGACY_NYA_LANGUAGE_TAG -> NYA_LANGUAGE_TAG
                else -> languageTag
            }
        }

        fun applyLocale(context: Context, languageTag: String): Context {
            val normalizedLanguageTag = normalizeLanguageTag(languageTag)
            if (normalizedLanguageTag.isEmpty()) return context
            val locale = Locale.forLanguageTag(normalizedLanguageTag)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        }
    }
}
