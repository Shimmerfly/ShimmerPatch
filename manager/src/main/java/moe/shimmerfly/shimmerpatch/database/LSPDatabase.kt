package moe.shimmerfly.shimmerpatch.database

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.shimmerfly.shimmerpatch.database.dao.ModuleDao
import moe.shimmerfly.shimmerpatch.database.dao.ScopeDao

import moe.shimmerfly.shimmerpatch.database.entity.LoadedModule
import moe.shimmerfly.shimmerpatch.database.entity.Scope

@Database(entities = [LoadedModule::class, Scope::class], version = 1, exportSchema = false)
abstract class LSPDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun scopeDao(): ScopeDao
}
