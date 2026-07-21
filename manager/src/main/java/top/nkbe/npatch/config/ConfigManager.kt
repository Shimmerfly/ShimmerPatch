package top.nkbe.npatch.config

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.Room
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.lsposed.lspd.models.Module as LoadedModule
import top.nkbe.npatch.database.LSPDatabase
import top.nkbe.npatch.database.entity.Module
import top.nkbe.npatch.database.entity.Scope
import top.nkbe.npatch.lspApp
import top.nkbe.npatch.util.LocalInjectedModuleService
import top.nkbe.npatch.util.ModuleLoader

object ConfigManager {

    private const val TAG = "ConfigManager"

    @OptIn(ExperimentalCoroutinesApi::class)
    private val writeDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val readDispatcher = Dispatchers.IO

    private val db: LSPDatabase by lazy {
        Room.databaseBuilder(
            lspApp,
            LSPDatabase::class.java,
            "modules_config.db",
        ).build()
    }

    private val moduleDao
        get() = db.moduleDao()
    private val scopeDao
        get() = db.scopeDao()

    private val loadedModules = ConcurrentHashMap<String, LoadedModule>()

    suspend fun updateModules(newModules: Map<String, String>) =
        withContext(writeDispatcher) {
            for (module in moduleDao.getAll()) {
                val apkPath = newModules[module.pkgName]
                if (apkPath == null) {
                    moduleDao.delete(module)
                    loadedModules.remove(module.pkgName)
                } else if (module.apkPath != apkPath) {
                    module.apkPath = apkPath
                    moduleDao.update(module)
                    loadedModules.remove(module.pkgName)
                }
            }
            for ((pkgName, apkPath) in newModules) {
                moduleDao.insert(Module(pkgName, apkPath))
            }
        }

    suspend fun activateModule(pkgName: String, module: Module) =
        withContext(writeDispatcher) {
            moduleDao.insert(module)
            scopeDao.insert(Scope(appPkgName = pkgName, modulePkgName = module.pkgName))
        }

    suspend fun deactivateModule(pkgName: String, module: Module) =
        withContext(writeDispatcher) {
            scopeDao.delete(Scope(appPkgName = pkgName, modulePkgName = module.pkgName))
        }

    suspend fun replaceModulesForApp(pkgName: String, modules: List<Module>) =
        withContext(writeDispatcher) {
            scopeDao.replaceForApp(pkgName, modules)
        }

    suspend fun getModulesForApp(pkgName: String): List<Module> =
        withContext(readDispatcher) {
            scopeDao.getModulesForApp(pkgName)
        }

    suspend fun getAppsForModule(pkgName: String): List<String> =
        withContext(readDispatcher) {
            scopeDao.getAppsForModule(pkgName)
        }

    suspend fun getScopedModulePackageNames(): Set<String> =
        withContext(readDispatcher) {
            scopeDao.getScopedModulePackageNames().toSet()
        }

    suspend fun clearRuntimeCache() =
        withContext(writeDispatcher) {
            loadedModules.clear()
        }

    suspend fun getModuleFilesForApp(pkgName: String): List<LoadedModule> =
        withContext(readDispatcher) {
            val modules = scopeDao.getModulesForApp(pkgName)
            modules.mapNotNull { moduleRecord ->
                val appInfo =
                    runCatching {
                        lspApp.packageManager.getApplicationInfo(
                            moduleRecord.pkgName,
                            PackageManager.GET_META_DATA,
                        )
                    }.getOrElse { error ->
                        if (error is PackageManager.NameNotFoundException) {
                            loadedModules.remove(moduleRecord.pkgName)
                            moduleDao.delete(moduleRecord)
                            Log.w(TAG, "Module may be uninstalled: ${moduleRecord.pkgName}")
                        }
                        return@mapNotNull null
                    }

                val installedApkPath = appInfo.sourceDir?.takeIf { File(it).exists() }
                val resolvedApkPath = installedApkPath ?: moduleRecord.apkPath
                if (!File(resolvedApkPath).exists()) {
                    loadedModules.remove(moduleRecord.pkgName)
                    Log.w(TAG, "Module apk is missing: ${moduleRecord.pkgName}")
                    return@mapNotNull null
                }
                if (moduleRecord.apkPath != resolvedApkPath) {
                    moduleRecord.apkPath = resolvedApkPath
                    moduleDao.update(moduleRecord)
                    loadedModules.remove(moduleRecord.pkgName)
                }

                val versionCode = readVersionCode(moduleRecord.pkgName, resolvedApkPath)
                val cachedModule = loadedModules[moduleRecord.pkgName]
                if (
                    cachedModule != null &&
                    cachedModule.apkPath == resolvedApkPath &&
                    cachedModule.versionCode == versionCode
                ) {
                    return@mapNotNull cachedModule
                }

                val preLoadedApk =
                    ModuleLoader.loadModule(
                        resolvedApkPath,
                        readLegacyMinApiVersion(appInfo),
                    ) ?: return@mapNotNull null

                LoadedModule().apply {
                    packageName = moduleRecord.pkgName
                    apkPath = resolvedApkPath
                    file = preLoadedApk
                    applicationInfo = appInfo
                    appId = appInfo.uid
                    this.versionCode = versionCode
                    service = LocalInjectedModuleService(lspApp, moduleRecord.pkgName)
                }.also { module ->
                    loadedModules[moduleRecord.pkgName] = module
                }
            }
        }

    suspend fun getModuleFile(pkgName: String): LoadedModule? =
        withContext(readDispatcher) {
            val record = moduleDao.getModule(pkgName) ?: return@withContext null
            val appInfo =
                runCatching {
                    lspApp.packageManager.getApplicationInfo(
                        pkgName,
                        PackageManager.GET_META_DATA,
                    )
                }.getOrNull()
            val apkPath = appInfo?.sourceDir?.takeIf { File(it).exists() } ?: record.apkPath
            if (!File(apkPath).exists()) return@withContext null
            if (record.apkPath != apkPath) {
                record.apkPath = apkPath
                moduleDao.update(record)
            }
            val preLoadedApk =
                ModuleLoader.loadModule(apkPath, readLegacyMinApiVersion(appInfo))
                    ?: return@withContext null
            LoadedModule().apply {
                packageName = pkgName
                this.apkPath = apkPath
                file = preLoadedApk
                applicationInfo = appInfo
                appId = appInfo?.uid ?: -1
                versionCode = readVersionCode(pkgName, apkPath)
                service = LocalInjectedModuleService(lspApp, pkgName)
            }
        }

    private fun readVersionCode(packageName: String, apkPath: String): Long {
        val packageInfo =
            runCatching { lspApp.packageManager.getPackageInfo(packageName, 0) }.getOrNull()
                ?: runCatching { lspApp.packageManager.getPackageArchiveInfo(apkPath, 0) }.getOrNull()
        return packageInfo?.let(PackageInfoCompat::getLongVersionCode) ?: 0L
    }

    private fun readLegacyMinApiVersion(appInfo: android.content.pm.ApplicationInfo?): Int {
        val metadata = appInfo?.metaData ?: return 0
        if (!metadata.containsKey("xposedminversion")) {
            return 0
        }

        val intValue = metadata.getInt("xposedminversion", Int.MIN_VALUE)
        if (intValue != Int.MIN_VALUE) {
            return intValue
        }

        return parseLeadingInt(metadata.getString("xposedminversion")) ?: 0
    }

    private fun parseLeadingInt(value: String?): Int? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }
        val digits = trimmed.takeWhile(Char::isDigit)
        if (digits.isEmpty()) {
            return null
        }
        return digits.toIntOrNull()
    }
}
