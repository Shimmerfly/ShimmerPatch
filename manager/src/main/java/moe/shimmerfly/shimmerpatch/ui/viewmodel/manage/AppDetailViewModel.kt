package moe.shimmerfly.shimmerpatch.ui.viewmodel.manage

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.lspApp
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager.AppInfo
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * State for one patched app's page: what the bundle embeds and the archive-level actions.
 *
 * Loader and scope actions stay in AppManageViewModel, which already implements them, so this page
 * dispatches those there instead of growing a second copy.
 */
class AppDetailViewModel : ViewModel() {

    companion object {
        private const val TAG = "AppDetailViewModel"

        /**
         * Where each patcher keeps the modules it embedded. All of them are tried, because a bundle
         * can have been produced by any of them and the prefix is what identifies the patcher's own
         * layout rather than the app that carries it.
         */
        private val MODULE_PREFIXES = listOf(
            "assets/shimmerpatch/modules/",
            "assets/npatch/modules/",
            "assets/lspatch/modules/",
        )

        private const val APK_MIME = "application/vnd.android.package-archive"
    }

    /** One module embedded in the patched archive. */
    data class EmbeddedModule(
        /** The module's package name, which is how the patcher names the embedded file. */
        val packageName: String,
        /** The installed module this entry refers to, when it is installed, for a label and icon. */
        val installed: AppInfo?,
    )

    var modules by mutableStateOf<List<EmbeddedModule>>(emptyList())
        private set

    var modulesLoaded by mutableStateOf(false)
        private set

    var modulesUnreadable by mutableStateOf(false)
        private set

    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    sealed interface ExportState {
        data object Idle : ExportState
        data object Running : ExportState
        data class Done(val files: Int) : ExportState
        data object Failed : ExportState
    }

    /** The scanned app this page describes. The scan keeps [NeoPackageManager.appList] current. */
    fun appInfo(packageName: String): AppInfo? =
        NeoPackageManager.appList.firstOrNull { it.app.packageName == packageName }

    fun clearExportState() {
        exportState = ExportState.Idle
    }

    /** Reads the embedded module list out of the archive, which is why this is suspending. */
    suspend fun loadModules(appInfo: AppInfo) {
        modulesLoaded = false
        modulesUnreadable = false
        val sourceDir = appInfo.app.sourceDir
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val sourceFile = File(sourceDir ?: return@runCatching emptyList<String>())
                if (!sourceFile.isFile) return@runCatching emptyList<String>()
                ZipFile(sourceFile).use { zip ->
                    zip.entries().asSequence()
                        .map { it.name }
                        .filter { name -> MODULE_PREFIXES.any { prefix -> name.startsWith(prefix) } }
                        .filter { name -> name.endsWith(".apk", ignoreCase = true) }
                        .map { name -> name.substringAfterLast('/').removeSuffix(".apk") }
                        .distinct()
                        .sorted()
                        .toList()
                }
            }
        }
        modules = result.fold(
            { packageNames ->
                // Resolve each entry against the installed apps, so a known module shows its name.
                val installed = NeoPackageManager.appList.associateBy { it.app.packageName }
                packageNames.map { pkg -> EmbeddedModule(pkg, installed[pkg]) }
            },
            { error ->
                Log.w(TAG, "Could not read embedded modules of ${appInfo.app.packageName}", error)
                modulesUnreadable = true
                emptyList()
            },
        )
        modulesLoaded = true
    }

    /**
     * Copies the installed APK set - the base plus any splits - into a folder the user picked, so the
     * patched bundle can be kept or moved to another device.
     */
    suspend fun exportApks(directory: Uri, appInfo: AppInfo) {
        exportState = ExportState.Running
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val target = DocumentFile.fromTreeUri(lspApp, directory)
                    ?: throw IOException("The selected folder is unavailable")
                val sources = buildList {
                    appInfo.app.sourceDir?.let { add(File(it)) }
                    appInfo.app.splitSourceDirs?.forEach { add(File(it)) }
                }.filter { it.isFile }
                if (sources.isEmpty()) throw IOException("No APK files to export")

                var written = 0
                for (source in sources) {
                    // Drop an earlier copy of the same name, so repeated exports do not pile up.
                    target.findFile(source.name)?.delete()
                    val created = target.createFile(APK_MIME, source.name)
                        ?: throw IOException("Could not create ${source.name}")
                    val output = lspApp.contentResolver.openOutputStream(created.uri, "w")
                        ?: throw IOException("Could not open ${source.name} for writing")
                    output.use { sink ->
                        source.inputStream().use { input -> input.copyTo(sink) }
                    }
                    written++
                }
                written
            }
        }
        exportState = result.fold(
            { ExportState.Done(it) },
            { error ->
                Log.e(TAG, "Failed to export the APK set of ${appInfo.app.packageName}", error)
                ExportState.Failed
            },
        )
    }
}
