package moe.shimmerfly.shimmerpatch.ui

import android.content.pm.ApplicationInfo
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager.AppInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import moe.shimmerfly.shimmerpatch.lspApp
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.PatchState
import moe.shimmerfly.shimmerpatch.ui.viewmodel.NewPatchViewModel.ViewAction

@RunWith(AndroidJUnit4::class)
class NewPatchInputSourceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val importedFiles = mutableListOf<File>()
    private lateinit var fixtureDirectory: File
    private lateinit var selectedBase: File
    private lateinit var viewModel: NewPatchViewModel

    @Before
    fun setUp() {
        // External files are selectable sources outside the old data/cache path heuristic.
        fixtureDirectory = File(
            checkNotNull(context.getExternalFilesDir(null)),
            "patch-input-test-${UUID.randomUUID()}",
        ).also { check(it.mkdirs()) }
        selectedBase = File(fixtureDirectory, "selected-${UUID.randomUUID()}.apk")
        File(context.applicationInfo.sourceDir).copyTo(selectedBase)
        instrumentation.runOnMainSync { viewModel = NewPatchViewModel() }
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            instrumentation.runOnMainSync { viewModel.viewModelScope.cancel() }
        }
        importedFiles.forEach(File::delete)
        if (::fixtureDirectory.isInitialized) fixtureDirectory.deleteRecursively()
    }

    @Test
    fun documentImportAndSubmitKeepTheImportedApkForAnInstalledPackage() {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            selectedBase,
        )
        // This is the same import entry point used by both the document picker and VIEW intents.
        val selected = runBlocking {
            NeoPackageManager.getAppInfoFromApks(listOf(uri)).getOrThrow().single()
        }
        val imported = File(selected.app.sourceDir).also(importedFiles::add)
        assertEquals(context.packageName, selected.app.packageName)
        assertEquals(lspApp.tmpApkDir.canonicalFile, imported.parentFile?.canonicalFile)
        assertEquals(selectedBase.name, imported.name)
        assertEquals(selectedBase.length(), imported.length())
        assertNotEquals(context.applicationInfo.sourceDir, imported.absolutePath)

        configureAndSubmit(selected)

        assertEquals(listOf(imported.absolutePath), viewModel.patchOptions.resolveActualApkPaths())
        assertEquals(imported.absolutePath, viewModel.patchOptions.toStringArray().last())
    }

    @Test
    fun submitKeepsExplicitExternalBaseAndSplitPathsForAnInstalledPackage() {
        val selectedSplits = listOf("split_config.en.apk", "split_config.arm64_v8a.apk")
            .map { name -> File(fixtureDirectory, name).also { selectedBase.copyTo(it) } }
        // The ViewModel consumes ApplicationInfo's source list; installation is not part of this test.
        val selectedInfo = ApplicationInfo(context.applicationInfo).apply {
            sourceDir = selectedBase.absolutePath
            publicSourceDir = selectedBase.absolutePath
            splitSourceDirs = selectedSplits.map(File::getAbsolutePath).toTypedArray()
        }
        val selected = AppInfo(selectedInfo, "Selected local APK", "fixture", 1L)
        val expectedPaths = listOf(selectedBase.absolutePath) + selectedSplits.map(File::getAbsolutePath)
        val installed = context.packageManager.getApplicationInfo(selectedInfo.packageName, 0)
        assertTrue(File(installed.sourceDir).isFile)
        assertNotEquals(installed.sourceDir, selectedBase.absolutePath)

        configureAndSubmit(selected)

        // With targetPackageName passed by submitPatch, these became the installed manager's APKs.
        assertEquals(expectedPaths, viewModel.patchOptions.resolveActualApkPaths())
        assertEquals(expectedPaths, viewModel.patchOptions.toStringArray().takeLast(expectedPaths.size))
    }

    private fun configureAndSubmit(selected: AppInfo) {
        instrumentation.runOnMainSync {
            viewModel.dispatch(ViewAction.ConfigurePatch(selected))
            assertEquals(PatchState.CONFIGURING, viewModel.patchState)
            assertSame(selected, viewModel.patchApp)
            viewModel.dispatch(ViewAction.SubmitPatch)
            assertEquals(PatchState.PATCHING, viewModel.patchState)
        }
    }
}
