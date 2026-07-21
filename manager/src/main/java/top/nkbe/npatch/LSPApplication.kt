package top.nkbe.npatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nkbe.util.NeoPackageManager
import nkbe.util.ShizukuApi
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.nkbe.npatch.manager.AppBroadcastReceiver

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
            prefs.edit().putString("language", language).apply()
        }
        super.attachBaseContext(applyLocale(base, language))
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        }

        lspApp = this
        filesDir.mkdir()
        tmpApkDir = cacheDir.resolve("apk").also { it.mkdir() }
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        ShizukuApi.init()
        AppBroadcastReceiver.register(this)
        globalScope.launch { NeoPackageManager.fetchAppList() }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            return context.createConfigurationContext(config)
        }
    }
}
