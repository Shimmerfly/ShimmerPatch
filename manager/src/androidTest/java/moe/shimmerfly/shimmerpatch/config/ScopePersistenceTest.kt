package moe.shimmerfly.shimmerpatch.config

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import moe.shimmerfly.shimmerpatch.database.LSPDatabase
import moe.shimmerfly.shimmerpatch.database.entity.LoadedModule

@RunWith(AndroidJUnit4::class)
class ScopePersistenceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val fixtureId = UUID.randomUUID().toString().replace("-", "")
    private val targetPackage = "${context.packageName}.scope_test.target_$fixtureId"
    private val fixtureModules = mutableListOf<LoadedModule>()
    private lateinit var database: LSPDatabase

    @Before
    fun setUp() {
        database = Room.databaseBuilder(context, LSPDatabase::class.java, "modules_config.db").build()
    }

    @After
    fun tearDown() {
        if (!::database.isInitialized) return
        try {
            runBlocking {
                database.scopeDao().deleteForApp(targetPackage)
                fixtureModules.forEach { module ->
                    database.moduleDao().delete(module)
                    snapshotFile(module).delete()
                }
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun incompleteScanKeepsScopeForUnchangedInstalledPackage() = runBlocking {
        val installed = instrumentation.context.applicationInfo
        val module = LoadedModule(installed.packageName, installed.sourceDir)
        assertTrue(File(module.apkPath).isFile)
        activateFixture(module)
        val snapshot = snapshotFile(module).readText()

        ConfigManager.updateModules(emptyMap())
        assertScopeAndSnapshot(module, snapshot)
        assertEquals(module.apkPath, instrumentation.context.packageManager.getApplicationInfo(module.pkgName, 0).sourceDir)

        ConfigManager.updateModules(mapOf(module.pkgName to module.apkPath))
        assertScopeAndSnapshot(module, snapshot)
        assertEquals(module.apkPath, instrumentation.context.packageManager.getApplicationInfo(module.pkgName, 0).sourceDir)
    }

    @Test
    fun unavailableApkDoesNotDeleteSavedScopeWhileLoading() = runBlocking {
        val module = LoadedModule(
            "${context.packageName}.scope_test.missing_$fixtureId",
            File(context.cacheDir, "missing-module-$fixtureId.apk").absolutePath,
        )
        assertFalse(File(module.apkPath).exists())
        activateFixture(module)
        val snapshot = snapshotFile(module).readText()

        assertNull(ConfigManager.getModuleFile(module.pkgName))
        assertScopeAndSnapshot(module, snapshot)
    }

    @Test
    fun scopeEditKeepsInvisibleSelectionAndRemovesExplicitlyDeselectedModule() = runBlocking {
        val apkPath = instrumentation.context.applicationInfo.sourceDir
        val visible = LoadedModule("${context.packageName}.scope_test.visible_$fixtureId", apkPath)
        val hidden = LoadedModule("${context.packageName}.scope_test.hidden_$fixtureId", apkPath)
        activateFixture(visible)
        activateFixture(hidden)
        val visibleSnapshot = snapshotFile(visible).readText()
        val hiddenSnapshot = snapshotFile(hidden).readText()

        val initialSelection = setOf(visible.pkgName, hidden.pkgName)
        ConfigManager.saveModuleSelection(
            appPkgName = targetPackage,
            initialPackageNames = initialSelection,
            selectedPackageNames = initialSelection,
            availableModules = listOf(visible),
        )

        assertEquals(setOf(visible, hidden), ConfigManager.getModulesForApp(targetPackage).toSet())
        assertEquals(listOf(targetPackage), ConfigManager.getAppsForModule(visible.pkgName))
        assertEquals(listOf(targetPackage), ConfigManager.getAppsForModule(hidden.pkgName))
        assertEquals(visibleSnapshot, snapshotFile(visible).readText())
        assertEquals(hiddenSnapshot, snapshotFile(hidden).readText())

        ConfigManager.saveModuleSelection(targetPackage, initialSelection, setOf(hidden.pkgName), listOf(visible))

        assertTrue(ConfigManager.getAppsForModule(visible.pkgName).isEmpty())
        assertEquals(0, JSONObject(snapshotFile(visible).readText()).getJSONArray("scope").length())
        assertScopeAndSnapshot(hidden, hiddenSnapshot)
    }

    @Test
    fun unchangedEditorDoesNotOverwriteConcurrentScopeChanges() = runBlocking {
        val apkPath = instrumentation.context.applicationInfo.sourceDir
        val original = LoadedModule("${context.packageName}.scope_test.original_$fixtureId", apkPath)
        val addedElsewhere = LoadedModule("${context.packageName}.scope_test.added_$fixtureId", apkPath)
        activateFixture(original)
        val initialSelection = setOf(original.pkgName)
        activateFixture(addedElsewhere)
        ConfigManager.deactivateModule(targetPackage, original)

        ConfigManager.saveModuleSelection(targetPackage, initialSelection, initialSelection, listOf(original))

        assertEquals(listOf(addedElsewhere), ConfigManager.getModulesForApp(targetPackage))
        assertTrue(ConfigManager.getAppsForModule(original.pkgName).isEmpty())
        assertEquals(listOf(targetPackage), ConfigManager.getAppsForModule(addedElsewhere.pkgName))
    }

    private suspend fun activateFixture(module: LoadedModule) {
        // These tests own only their fixture records, never existing module configuration.
        assertNull(database.moduleDao().getModule(module.pkgName))
        assertFalse(snapshotFile(module).exists())
        fixtureModules += module
        ConfigManager.activateModule(targetPackage, module)
        assertTrue(snapshotFile(module).isFile)
    }

    private suspend fun assertScopeAndSnapshot(module: LoadedModule, snapshot: String) {
        assertEquals(listOf(module), ConfigManager.getModulesForApp(targetPackage))
        assertEquals(listOf(targetPackage), ConfigManager.getAppsForModule(module.pkgName))
        assertEquals(module, database.moduleDao().getModule(module.pkgName))
        assertTrue(snapshotFile(module).isFile)
        assertEquals(snapshot, snapshotFile(module).readText())
    }

    private fun snapshotFile(module: LoadedModule): File =
        File(context.filesDir, "module_scope_snapshots/${module.pkgName}.json")
}
