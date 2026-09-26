package moe.shimmerfly.shimmerpatch.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["appPkgName", "modulePkgName"],
    foreignKeys = [ForeignKey(entity = LoadedModule::class, parentColumns = ["pkgName"], childColumns = ["modulePkgName"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("modulePkgName")]
)
data class Scope(
    val appPkgName: String,
    val modulePkgName: String
)
