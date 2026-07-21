package top.nkbe.npatch.manager

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.github.libxposed.service.HookedProcess
import io.github.libxposed.service.IHotReloadCallback
import io.github.libxposed.service.IXposedService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.runBlocking
import nkbe.util.ModuleMetadataReader
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.IHotReloadTarget
import top.nkbe.npatch.config.ConfigManager
import top.nkbe.npatch.lspApp

/** Process-safe registry that connects patched targets with their module app's API 102 service. */
object HotReloadRegistry {
    private const val TAG = "HotReloadRegistry"

    private val nextTargetId = AtomicLong(1)
    private val targets = ConcurrentHashMap<Long, TargetInfo>()

    private class TargetInfo(
        val id: Long,
        val modulePackageName: String,
        val targetPackageName: String,
        val uid: Int,
        val pid: Int,
        val processName: String,
        @Volatile var loadedVersionCode: Long,
        val target: IHotReloadTarget,
    ) : IBinder.DeathRecipient {
        @Volatile var state = HookedProcess.TARGET_STATE_UP_TO_DATE

        override fun binderDied() {
            targets.remove(id, this)
            runCatching { target.asBinder().unlinkToDeath(this, 0) }
        }
    }

    fun register(
        modulePackageName: String,
        loadedVersionCode: Long,
        targetPackageName: String,
        uid: Int,
        pid: Int,
        processName: String,
        target: IHotReloadTarget,
    ): Long {
        if (!isScoped(modulePackageName, targetPackageName)) {
            throw SecurityException(
                "Module $modulePackageName is not enabled for $targetPackageName"
            )
        }
        targets.values
            .firstOrNull {
                it.modulePackageName == modulePackageName &&
                    it.uid == uid &&
                    it.pid == pid &&
                    it.target.asBinder().isBinderAlive &&
                    it.target.asBinder() == target.asBinder()
            }
            ?.let {
                it.loadedVersionCode = loadedVersionCode
                it.state = HookedProcess.TARGET_STATE_UP_TO_DATE
                return it.id
            }

        val id = nextTargetId.getAndIncrement()
        val info =
            TargetInfo(
                id,
                modulePackageName,
                targetPackageName,
                uid,
                pid,
                processName,
                loadedVersionCode,
                target,
            )
        targets[id] = info
        runCatching { target.asBinder().linkToDeath(info, 0) }
            .onFailure { targets.remove(id, info) }
            .getOrThrow()
        return id
    }

    fun getRunningTargets(modulePackageName: String): List<HookedProcess> {
        val currentVersionCode = latestModule(modulePackageName)?.versionCode
        return targets.values
            .filter { it.modulePackageName == modulePackageName && it.target.asBinder().isBinderAlive }
            .map { info ->
                HookedProcess().apply {
                    targetId = info.id
                    uid = info.uid
                    pid = info.pid
                    processName = info.processName
                    state =
                        if (
                            info.state == HookedProcess.TARGET_STATE_UP_TO_DATE &&
                                currentVersionCode != null &&
                                info.loadedVersionCode != currentVersionCode
                        ) {
                            HookedProcess.TARGET_STATE_STALE
                        } else {
                            info.state
                        }
                    loadedVersionCode = info.loadedVersionCode
                }
            }
    }

    fun hotReload(
        modulePackageName: String,
        targetId: Long,
        data: Bundle?,
        callback: IHotReloadCallback?,
    ) {
        val info =
            targets[targetId]
                ?: throw SecurityException("Invalid hot reload target: $targetId")
        if (info.modulePackageName != modulePackageName) {
            throw SecurityException("Target $targetId does not belong to $modulePackageName")
        }
        val module =
            latestModule(modulePackageName)
                ?: throw UnsupportedOperationException("Module $modulePackageName is unavailable")
        if (module.file?.moduleClassNames?.size != 1) {
            throw UnsupportedOperationException("Hot reload requires exactly one Java entry class")
        }
        if (!beginReload(info)) {
            callback?.onHotReloadResult(
                IXposedService.HOT_RELOAD_IN_PROGRESS,
                "Target $targetId is already reloading",
            )
            return
        }

        val failure = performHotReload(info, module, data)
        if (failure == null) {
            runCatching { callback?.onHotReloadResult(IXposedService.HOT_RELOAD_SUCCEEDED, null) }
            return
        }
        runCatching { callback?.onHotReloadResult(mapFailureStatus(info, failure), failure.message) }
    }

    fun triggerAutoHotReload(modulePackageName: String) {
        if (!isAutoHotReloadEnabled(modulePackageName)) {
            return
        }
        val module =
            latestModule(modulePackageName)
                ?: run {
                    Log.w(TAG, "Skipping auto hot reload for $modulePackageName: module unavailable")
                    return
                }
        if (module.file?.moduleClassNames?.size != 1) {
            Log.i(TAG, "Skipping auto hot reload for $modulePackageName: requires exactly one Java entry class")
            return
        }

        targets.values
            .filter { it.modulePackageName == modulePackageName && it.loadedVersionCode != module.versionCode }
            .forEach { info ->
                if (!beginReload(info)) {
                    Log.d(TAG, "Skipping auto hot reload for ${info.processName}: reload already in progress")
                    return@forEach
                }
                val failure = performHotReload(info, module, null)
                if (failure == null) {
                    Log.i(TAG, "Auto hot reload succeeded for $modulePackageName in ${info.processName}")
                } else {
                    Log.w(
                        TAG,
                        "Auto hot reload failed for $modulePackageName in ${info.processName}: ${failure.message}",
                        failure,
                    )
                }
            }
    }

    private fun beginReload(info: TargetInfo): Boolean {
        synchronized(info) {
            if (info.state == HookedProcess.TARGET_STATE_RELOADING) {
                return false
            }
            info.state = HookedProcess.TARGET_STATE_RELOADING
            return true
        }
    }

    private fun performHotReload(
        info: TargetInfo,
        module: Module,
        data: Bundle?,
    ): Throwable? {
        val failure =
            runCatching {
                if (!info.target.asBinder().isBinderAlive) {
                    error("Target process died before hot reload")
                }
                if (!isScoped(info.modulePackageName, info.targetPackageName)) {
                    throw UnsupportedOperationException(
                        "Module ${info.modulePackageName} is no longer enabled for ${info.targetPackageName}"
                    )
                }
                info.target.hotReloadModule(module, data)
                info.loadedVersionCode = module.versionCode
                info.state = HookedProcess.TARGET_STATE_UP_TO_DATE
            }
            .exceptionOrNull()
        if (failure == null) return null

        info.state = HookedProcess.TARGET_STATE_FAILED
        if (!info.target.asBinder().isBinderAlive) {
            targets.remove(info.id, info)
        }
        return failure
    }

    private fun mapFailureStatus(info: TargetInfo, failure: Throwable): Int {
        return when {
            !info.target.asBinder().isBinderAlive -> IXposedService.HOT_RELOAD_PROCESS_DIED
            failure is UnsupportedOperationException -> IXposedService.HOT_RELOAD_UNSUPPORTED
            else -> IXposedService.HOT_RELOAD_FAILED
        }
    }

    private fun isAutoHotReloadEnabled(modulePackageName: String): Boolean {
        return runCatching {
            val packageInfo =
                lspApp.packageManager.getPackageInfo(
                    modulePackageName,
                    PackageManager.GET_META_DATA,
                )
            ModuleMetadataReader.read(packageInfo, lspApp.packageManager)?.autoHotReload == true
        }.getOrElse { throwable ->
            Log.w(TAG, "Failed to inspect autoHotReload for $modulePackageName", throwable)
            false
        }
    }

    private fun latestModule(modulePackageName: String) =
        runBlocking { ConfigManager.getModuleFile(modulePackageName) }

    private fun isScoped(modulePackageName: String, targetPackageName: String) =
        runBlocking {
            ConfigManager.getModulesForApp(targetPackageName).any { it.pkgName == modulePackageName }
        }
}
