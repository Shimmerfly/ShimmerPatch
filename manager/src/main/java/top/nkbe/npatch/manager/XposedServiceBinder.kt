package top.nkbe.npatch.manager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import io.github.libxposed.service.HookedProcess
import io.github.libxposed.service.IHotReloadCallback
import io.github.libxposed.service.IXposedScopeCallback
import io.github.libxposed.service.IXposedService
import kotlinx.coroutines.runBlocking
import top.nkbe.npatch.BuildConfig
import top.nkbe.npatch.config.ConfigManager
import top.nkbe.npatch.lspApp
import top.nkbe.npatch.util.NPatchRemoteStore

class XposedServiceBinder(private val packageName: String) : IXposedService.Stub() {

    private val remoteStore by lazy { NPatchRemoteStore.get(lspApp, packageName) }

    override fun getApiVersion(): Int = IXposedService.LIB_API

    override fun getFrameworkName(): String = "NPatch"

    override fun getFrameworkVersion(): String = BuildConfig.VERSION_NAME

    override fun getFrameworkVersionCode(): Long = BuildConfig.VERSION_CODE.toLong()

    override fun getFrameworkProperties(): Long {
        return NPatchRemoteStore.CAP_REMOTE
    }

    override fun getScope(): List<String> {
        return runBlocking { ConfigManager.getAppsForModule(packageName) }
    }

    override fun requestScope(packages: List<String>, callback: IXposedScopeCallback) {
        // 免 Root 下暫時自動允許，或可以加入一個彈窗確認
        runBlocking {
            packages.forEach { appPkg ->
                ConfigManager.activateModule(appPkg, top.nkbe.npatch.database.entity.Module(packageName, ""))
            }
            callback.onScopeRequestApproved(packages)
        }
    }

    override fun removeScope(packages: List<String>) {
        runBlocking {
            packages.forEach { appPkg ->
                ConfigManager.deactivateModule(appPkg, top.nkbe.npatch.database.entity.Module(packageName, ""))
            }
        }
    }

    override fun getRunningTargets(): List<HookedProcess> {
        return HotReloadRegistry.getRunningTargets(packageName)
    }

    override fun hotReloadModule(
        targetId: Long,
        data: Bundle?,
        callback: IHotReloadCallback?,
    ) {
        HotReloadRegistry.hotReload(packageName, targetId, data, callback)
    }

    override fun requestRemotePreferences(group: String): Bundle {
        return remoteStore.requestPreferences(group, null)
    }

    override fun updateRemotePreferences(group: String, diff: Bundle) {
        remoteStore.updatePreferences(group, diff)
    }

    override fun deleteRemotePreferences(group: String) {
        remoteStore.deletePreferences(group)
    }

    override fun listRemoteFiles(): Array<String> {
        return remoteStore.listFiles()
    }

    override fun openRemoteFile(name: String): ParcelFileDescriptor {
        return remoteStore.openFile(name, true)
    }

    override fun deleteRemoteFile(name: String): Boolean {
        return remoteStore.deleteFile(name)
    }
}
