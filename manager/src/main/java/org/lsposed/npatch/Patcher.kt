package org.lsposed.npatch

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.npatch.config.Configs
import org.lsposed.npatch.config.MyKeyStore
import org.lsposed.npatch.share.Constants
import org.lsposed.npatch.share.PatchConfig
import org.lsposed.patch.NPatch
import org.lsposed.patch.util.Logger
import java.io.File
import java.io.IOException

object Patcher {

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    class Options(
        val newPackageName: String,
        private val injectDex: Boolean,
        private val config: PatchConfig,
        private val apkPaths: List<String>,
        private val embeddedModules: List<String>?
    ) {
        fun toStringArray(): Array<String> {
            return buildList {
                add("-o"); add(lspApp.tmpApkDir.absolutePath)
                add("-p"); add(config.newPackage)
                if (config.debuggable) add("-d")
                add("-l"); add(config.sigBypassLevel.toString())
                if (config.useManager) add("--manager")
                if (config.overrideVersionCode) add("-r")
                if (Configs.detailPatchLogs) add("-v")
                embeddedModules?.forEach {
                    add("-m"); add(it)
                }
                if (config.injectProvider) add("--provider")
                if(injectDex) add("--injectdex")
                if (config.useMicroG) add("--useMicroG")
                if (!MyKeyStore.useDefault) {
                    addAll(arrayOf("-k", MyKeyStore.file.path, Configs.keyStorePassword, Configs.keyStoreAlias, Configs.keyStoreAliasPassword))
                }
                addAll(apkPaths)
            }.toTypedArray()
        }
    }

    suspend fun patch(logger: Logger, options: Options) {
        withContext(Dispatchers.IO) {
            cleanupPatchedArtifacts()
            NPatch(logger, *options.toStringArray()).doCommandLine()

            val uri = Configs.storageDirectory?.toUri()
                ?: throw IOException("Uri is null")
            val root = DocumentFile.fromTreeUri(lspApp, uri)
                ?: throw IOException("DocumentFile is null")
            deletePatchedApks(root)
            lspApp.targetApkFiles?.clear()
            val apkFileList = collectPatchedApks()
            if (apkFileList.isEmpty()) {
                throw IOException("No patched APK files found in ${lspApp.tmpApkDir.absolutePath}")
            }
            apkFileList.forEach { cachedApkFile ->
                val existingFile = root.findFile(cachedApkFile.name)
                if (existingFile?.delete() == false) {
                    throw IOException("Unable to replace output file: ${cachedApkFile.name}")
                }
                val finalFile = root.createFile(APK_MIME_TYPE, cachedApkFile.name)
                    ?: throw IOException("無法建立輸出檔案： ${cachedApkFile.name}")
                lspApp.contentResolver.openOutputStream(finalFile.uri, "wt")?.use { output ->
                    cachedApkFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Unable to open an output stream: ${finalFile.uri}")
            }
            lspApp.targetApkFiles = apkFileList
            logger.i("Patched files are saved to ${root.uri.lastPathSegment}")
        }
    }

    private fun cleanupPatchedArtifacts() {
        deletePatchedApks(lspApp.tmpApkDir)
        lspApp.externalCacheDir?.let { deletePatchedApks(it) }
    }

    private fun collectPatchedApks(): ArrayList<File> {
        val externalCacheDir = lspApp.externalCacheDir
            ?: throw IOException("External cache directory is unavailable")
        return lspApp.tmpApkDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(Constants.PATCH_FILE_SUFFIX) }
            .sortedBy { it.name }
            .mapTo(arrayListOf()) { tempApkFile ->
                val cachedApkFile = externalCacheDir.resolve(tempApkFile.name)
                if (cachedApkFile.exists() && !cachedApkFile.delete()) {
                    throw IOException("Unable to clear cached APK: ${cachedApkFile.absolutePath}")
                }
                if (!tempApkFile.renameTo(cachedApkFile)) {
                    tempApkFile.copyTo(cachedApkFile, overwrite = true)
                    if (!tempApkFile.delete()) {
                        throw IOException("Unable to remove temp APK: ${tempApkFile.absolutePath}")
                    }
                }
                cachedApkFile
            }
    }

    private fun deletePatchedApks(directory: File) {
        if (!directory.exists()) return
        directory.walkBottomUp()
            .filter { it.isFile && it.name.endsWith(Constants.PATCH_FILE_SUFFIX) }
            .forEach {
                if (!it.delete()) {
                    throw IOException("Unable to delete stale APK: ${it.absolutePath}")
                }
            }
    }

    private fun deletePatchedApks(root: DocumentFile) {
        root.listFiles().forEach {
            if (it.name?.endsWith(Constants.PATCH_FILE_SUFFIX) == true && !it.delete()) {
                throw IOException("Unable to delete stale output file: ${it.name}")
            }
        }
    }
}
