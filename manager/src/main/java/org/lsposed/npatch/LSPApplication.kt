package org.lsposed.npatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.lsposed.npatch.manager.AppBroadcastReceiver
import nkbe.util.NPackageManager
import nkbe.util.ShizukuApi
import java.io.File

lateinit var lspApp: LSPApplication

class LSPApplication : Application() {

    lateinit var prefs: SharedPreferences
    lateinit var tmpApkDir: File

    var targetApkFiles: ArrayList<File>? = null
    val globalScope = CoroutineScope(Dispatchers.Default)

    companion object {
        init {
            try {
                System.loadLibrary("verify")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        verifySignature()
        try {
            nativeVerify()
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        HiddenApiBypass.addHiddenApiExemptions("")
        lspApp = this
        filesDir.mkdir()
        tmpApkDir = cacheDir.resolve("apk").also { it.mkdir() }
        prefs = lspApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
        ShizukuApi.init()
        AppBroadcastReceiver.register(this)
        globalScope.launch { NPackageManager.fetchAppList() }
    }

    private fun verifySignature() {
        try {
            val flags = PackageManager.GET_SIGNING_CERTIFICATES
            val packageInfo = packageManager.getPackageInfo(packageName, flags)
            val signingInfo = packageInfo.signingInfo
            val signatures = signingInfo?.apkContentsSigners

            if (signatures != null && signatures.isNotEmpty()) {
                val currentHash = signatures[0].hashCode()
                val targetHash = 0x0293FA43
                if (currentHash != targetHash) {
                    killApp()
                }
            } else {
                killApp()
            }
        } catch (e: Exception) {
            killApp()
        }
    }
}
