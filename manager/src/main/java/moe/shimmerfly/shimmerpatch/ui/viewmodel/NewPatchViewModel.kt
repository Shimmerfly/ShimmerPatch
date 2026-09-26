package moe.shimmerfly.shimmerpatch.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.Patcher
import moe.shimmerfly.shimmerpatch.lspApp
import moe.shimmerfly.shimmerpatch.share.PatchConfig
import moe.shimmerfly.shimmerpatch.patch.util.ManifestParser
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager.AppInfo
import moe.shimmerfly.shimmerpatch.patch.util.Logger
import moe.shimmerfly.shimmerpatch.share.Constants

class NewPatchViewModel : ViewModel() {

    companion object {
        private const val TAG = "NewPatchViewModel"

        /**
         * Completes a permission written the short way, so the list shows exactly what the patcher
         * will declare: `INTERNET` and `android.permission.INTERNET` are the same entry.
         */
        fun normalizePermission(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.contains('.')) return trimmed
            return "android.permission." + trimmed.uppercase(Locale.ROOT)
        }
    }

    enum class PatchState {
        INIT, SELECTING, CONFIGURING, PATCHING, FINISHED, ERROR
    }

    enum class InstallMethod {
        SYSTEM, SHIZUKU
    }

    sealed class ViewAction {
        object DoneInit : ViewAction()
        data class ConfigurePatch(val app: AppInfo) : ViewAction()
        object SubmitPatch : ViewAction()
        object LaunchPatch : ViewAction()
    }

    var patchState by mutableStateOf(PatchState.INIT)
        private set

    // Patch Configuration
    @set:JvmName("_setUseManager")
    var useManager by mutableStateOf(true)
        private set
    var newPackageName by mutableStateOf("")
    var debuggable by mutableStateOf(false)
    var overrideVersionCode by mutableStateOf(false)
    var overrideVersionCodeValue by mutableStateOf("1")
    var overrideTargetSdk by mutableStateOf(false)
    var overrideTargetSdkValue by mutableStateOf("28")
    var sigBypassLevel by mutableIntStateOf(2)
    /** Replaces the launcher name of the patched app; blank keeps the original one. */
    var overrideLabel by mutableStateOf("")
    /** Forces the installer to unpack the app's native libraries. */
    var extractNativeLibs by mutableStateOf(false)
    /** Hides ART and the sensitive system libraries from the app's own environment checks. */
    var hideLibs by mutableStateOf(false)
    /** Whether the extra-permission editor is open. */
    var permissionsExpanded by mutableStateOf(false)
    /** The permission currently being typed, before it is added to the list. */
    var permissionInput by mutableStateOf("")
    val addedPermissions = mutableStateListOf<String>()
    var injectProvider by mutableStateOf(false)
    var useMicroG by mutableStateOf(false)
    var outputLog by mutableStateOf(true)
    var usesCleartextTraffic by mutableStateOf(false)
    var injectDex by mutableStateOf(false)
    var hasSubProcesses by mutableStateOf(false)
    var subProcessCount by mutableIntStateOf(0)
    var subProcesses by mutableStateOf<List<String>>(emptyList())
    /**
     * A module that will be embedded into the patched APK: one the user picked from the installed
     * modules, or an APK they picked from storage, which has no installed app behind it.
     */
    data class EmbeddedModule(
        val packageName: String,
        val label: String,
        val appInfo: AppInfo?,
        val apkPaths: List<String>,
    )

    /** Records the permission being typed, completing a short name the way the patcher does. */
    fun addPermission() {
        val permission = normalizePermission(permissionInput)
        if (permission.isEmpty()) return
        if (!addedPermissions.contains(permission)) addedPermissions.add(permission)
        permissionInput = ""
    }

    fun removePermission(permission: String) {
        addedPermissions.remove(permission)
    }

    var embeddedModules by mutableStateOf<List<EmbeddedModule>>(emptyList())
        private set
    var hasExecutedIntent by mutableStateOf(false)

    lateinit var patchApp: AppInfo
        private set
    lateinit var patchOptions: Patcher.Options
        private set

    val logs = mutableStateListOf<Pair<Int, String>>()
    private val logger = object : Logger() {
        override fun d(msg: String) {
            if (verbose) {
                Log.d(TAG, msg)
                logs += Log.DEBUG to msg
            }
        }

        override fun i(msg: String) {
            Log.i(TAG, msg)
            logs += Log.INFO to msg
        }

        override fun e(msg: String) {
            Log.e(TAG, msg)
            logs += Log.ERROR to msg
        }
    }

    fun dispatch(action: ViewAction) {
        viewModelScope.launch {
            when (action) {
                is ViewAction.DoneInit -> doneInit()
                is ViewAction.ConfigurePatch -> configurePatch(action.app)
                is ViewAction.SubmitPatch -> submitPatch()
                is ViewAction.LaunchPatch -> launchPatch()
            }
        }
    }

    fun reset() {
        patchState = PatchState.INIT
        useManager = true
        newPackageName = ""
        debuggable = false
        overrideVersionCode = false
        overrideVersionCodeValue = "1"
        overrideTargetSdk = false
        overrideTargetSdkValue = "28"
        sigBypassLevel = 2
        injectProvider = false
        useMicroG = false
        outputLog = true
        usesCleartextTraffic = false
        injectDex = false
        hasSubProcesses = false
        subProcessCount = 0
        subProcesses = emptyList()
        embeddedModules = emptyList()
        logs.clear()
        hasExecutedIntent = false
    }

    fun setUseManager(value: Boolean) {
        useManager = value
        if (!value && sigBypassLevel > Constants.SIGBYPASS_EXTREME) {
            sigBypassLevel = Constants.SIGBYPASS_EXTREME
        }
    }

    private fun doneInit() {
        patchState = PatchState.SELECTING
    }

    private fun configurePatch(app: AppInfo) {
        Log.d(TAG, "Configuring patch for ${app.app.packageName}")
        patchApp = app
        patchState = PatchState.CONFIGURING
        newPackageName = app.app.packageName
        try {
            val pair = ManifestParser.parseManifestFile(app.app.sourceDir)
            if (pair != null) {
                hasSubProcesses = pair.hasIsolatedOrMultiProcessComponents()
                subProcessCount = pair.getIsolatedOrMultiProcessCount()
                subProcesses = pair.getIsolatedOrMultiProcessComponents()
            } else {
                hasSubProcesses = false
                subProcessCount = 0
                subProcesses = emptyList()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to inspect manifest for subprocess components", t)
            hasSubProcesses = false
            subProcessCount = 0
            subProcesses = emptyList()
        }
    }

    private fun submitPatch() {
        Log.d(TAG, "Submit Patch")
        if (useManager) embeddedModules = emptyList()
        val patchSigBypassLevel = if (useManager) sigBypassLevel else sigBypassLevel.coerceAtMost(Constants.SIGBYPASS_EXTREME)
        val patchVersionCode = overrideVersionCodeValue.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val patchTargetSdk = overrideTargetSdkValue.toIntOrNull()?.takeIf { it > 0 } ?: 28
        sigBypassLevel = patchSigBypassLevel
        overrideVersionCodeValue = patchVersionCode.toString()
        overrideTargetSdkValue = patchTargetSdk.toString()
        val config = PatchConfig(
            useManager,
            debuggable,
            overrideVersionCode,
            patchVersionCode,
            patchSigBypassLevel,
            null,
            null,
            injectProvider,
            outputLog,
            newPackageName,
            useMicroG,
            hideLibs,
            usesCleartextTraffic,
            overrideTargetSdk,
            patchTargetSdk
        )
        patchOptions = Patcher.Options(
            newPackageName = newPackageName,
            config = config,
            apkPaths = listOf(patchApp.app.sourceDir) + (patchApp.app.splitSourceDirs ?: emptyArray()),
            embeddedModules = embeddedModules.flatMap { it.apkPaths },
            embeddedModulePackages = embeddedModules.map { it.packageName },
            injectDex = injectDex,
            labelOverride = overrideLabel.trim().ifEmpty { null },
            extractNativeLibs = extractNativeLibs,
            addedPermissions = addedPermissions.toList(),
        )
        patchState = PatchState.PATCHING
    }

    /** Replaces the selection with the installed modules the picker returned. */
    fun applyEmbeddedModuleSelection(apps: List<AppInfo>) {
        embeddedModules = apps.map { app ->
            EmbeddedModule(
                packageName = app.app.packageName,
                label = app.label,
                appInfo = app,
                apkPaths = (listOf(app.app.sourceDir) + (app.app.splitSourceDirs ?: emptyArray()).toList())
                    .filterNotNull()
                    .filter { it.isNotEmpty() },
            )
        }
    }

    fun removeEmbeddedModule(packageName: String) {
        embeddedModules = embeddedModules.filterNot { it.packageName == packageName }
    }

    /**
     * Adds a module APK the user picked from storage. It is copied into the cache first, because the
     * patcher reads plain paths and the document provider only hands out a stream, and its package
     * name and label are read from that copy.
     */
    suspend fun addEmbeddedModuleFromStorage(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(lspApp.cacheDir, "embedded_modules").apply { mkdirs() }
            val target = File(directory, "${System.currentTimeMillis()}.apk")
            val input = lspApp.contentResolver.openInputStream(uri)
                ?: error("The selected module could not be opened")
            input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }

            val info = lspApp.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
                ?: error("The selected file is not an APK")
            val packageName = info.packageName ?: error("The selected APK has no package name")
            val application = info.applicationInfo?.apply { sourceDir = target.absolutePath }
            val label = application
                ?.let { lspApp.packageManager.getApplicationLabel(it).toString() }
                ?: packageName

            val entry = EmbeddedModule(packageName, label, null, listOf(target.absolutePath))
            embeddedModules = embeddedModules.filterNot { it.packageName == packageName } + entry
        }
    }

    private suspend fun launchPatch() {
        logger.i("Launch Patch")
        patchState = try {
            Patcher.patch(logger, patchOptions)
            PatchState.FINISHED
        } catch (t: Throwable) {
            logger.e(t.message.orEmpty())
            logger.e(t.stackTraceToString())
            PatchState.ERROR
        }
    }
}
