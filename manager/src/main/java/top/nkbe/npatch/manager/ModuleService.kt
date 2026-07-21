package top.nkbe.npatch.manager

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import java.util.concurrent.ConcurrentHashMap
import android.util.Log

import kotlinx.coroutines.runBlocking
import top.nkbe.npatch.config.ConfigManager
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService
import org.lsposed.lspd.service.IHotReloadTarget

class ModuleService : Service() {

    companion object {
        private const val TAG = "ModuleService"
        private const val REGISTER_CLIENT_PACKAGE = 0x4E5041
    }

    override fun onBind(intent: Intent): IBinder? {
        val packageName = intent.getStringExtra("packageName") ?: return null

        /*
         * The binding transaction is dispatched by ActivityManager, so its Binder UID is not a
         * reliable identity for the target application. In particular, some apps reach here with
         * the manager/system UID and were rejected before their first module list could be read.
         * Keep the package supplied by the patched loader for the lifetime of this connection;
         * unscoped apps still receive an empty module list from ConfigManager.
         */
        Log.i(TAG, "$packageName requests binder from uid=${Binder.getCallingUid()}")
        return ScopedApplicationService(packageName).asBinder()
    }

    private inner class ScopedApplicationService(
        private val requestedPackageName: String,
    ) : ILSPApplicationService.Stub() {
        private val clientPackages = ConcurrentHashMap<Int, String>()

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == REGISTER_CLIENT_PACKAGE) {
                data.enforceInterface("org.lsposed.lspd.service.ILSPApplicationService")
                val packageName = data.readString()
                val callingUid = Binder.getCallingUid()
                val packages = packageManager.getPackagesForUid(callingUid).orEmpty()
                if (packageName != null && packageName in packages) {
                    clientPackages[Binder.getCallingPid()] = packageName
                    Log.i(TAG, "Registered client pid=${Binder.getCallingPid()} package=$packageName")
                } else {
                    Log.w(TAG, "Rejected client package registration: uid=$callingUid package=$packageName")
                }
                reply?.writeNoException()
                return true
            }
            return super.onTransact(code, data, reply, flags)
        }

        private fun targetPackageName(): String {
            clientPackages[Binder.getCallingPid()]?.let { return it }
            val uid = Binder.getCallingUid()
            val packages = packageManager.getPackagesForUid(uid).orEmpty()
            return when {
                requestedPackageName in packages -> requestedPackageName
                // Shared uid: prefer the package that actually has scoped modules.
                packages.size > 1 -> packages.firstOrNull { pkg ->
                    runCatching {
                        runBlocking { ConfigManager.getModulesForApp(pkg).isNotEmpty() }
                    }.getOrDefault(false)
                } ?: requestedPackageName
                // Isolated processes and service-binding callbacks do not have a package mapping.
                else -> requestedPackageName
            }
        }

        private fun modules(): List<Module> {
            return runBlocking { ConfigManager.getModuleFilesForApp(targetPackageName()) }
        }

        override fun isLogMuted(): Boolean = false

        override fun getLegacyModulesList(): List<Module> {
            val list = modules().filter { it.file?.legacy == true }
            Log.d(TAG, "${targetPackageName()} calls getLegacyModulesList: $list")
            return list
        }

        override fun getModulesList(): List<Module> {
            val list = modules().filter { it.file?.legacy == false }
            Log.d(TAG, "${targetPackageName()} calls getModulesList: $list")
            return list
        }

        override fun getPrefsPath(packageName: String): String {
            val userId =
                runCatching { packageManager.getApplicationInfo(packageName, 0).uid / 100000 }
                    .getOrDefault(0)
            return if (userId == 0) {
                "/data/data/$packageName/shared_prefs/"
            } else {
                "/data/user/$userId/$packageName/shared_prefs/"
            }
        }

        override fun registerHotReloadTarget(
            modulePackageName: String,
            loadedVersionCode: Long,
            target: IHotReloadTarget,
        ): Long {
            val packageName = targetPackageName()
            return HotReloadRegistry.register(
                modulePackageName,
                loadedVersionCode,
                packageName,
                Binder.getCallingUid(),
                Binder.getCallingPid(),
                packageName,
                target,
            )
        }

        override fun requestInjectedManagerBinder(
            binder: MutableList<IBinder>
        ): ParcelFileDescriptor? {
            val packageName = targetPackageName()
            Log.i(TAG, "$packageName requests injected manager binder")
            binder.add(XposedServiceBinder(packageName))
            return null
        }
    }
}
