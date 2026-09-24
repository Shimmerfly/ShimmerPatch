package moe.shimmerfly.shimmerpatch.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LoadedModule(
    @PrimaryKey val pkgName: String,
    var apkPath: String
)
