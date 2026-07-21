package top.nkbe.npatch.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import top.nkbe.npatch.database.entity.Module
import top.nkbe.npatch.database.entity.Scope

@Dao
interface ScopeDao {

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM module INNER JOIN scope ON module.pkgName = scope.modulePkgName WHERE scope.appPkgName = :appPkgName")
    suspend fun getModulesForApp(appPkgName: String): List<Module>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scope: Scope)

    @Query("DELETE FROM scope WHERE appPkgName = :appPkgName")
    suspend fun deleteForApp(appPkgName: String)

    @Transaction
    suspend fun replaceForApp(appPkgName: String, modules: List<Module>) {
        deleteForApp(appPkgName)
        modules.forEach { module ->
            insertModule(module)
            insert(Scope(appPkgName = appPkgName, modulePkgName = module.pkgName))
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModule(module: Module)

    @Delete
    suspend fun delete(scope: Scope)

    @Query("SELECT appPkgName FROM scope WHERE modulePkgName = :modulePkgName")
    suspend fun getAppsForModule(modulePkgName: String): List<String>

    @Query("SELECT DISTINCT modulePkgName FROM scope")
    suspend fun getScopedModulePackageNames(): List<String>
}
