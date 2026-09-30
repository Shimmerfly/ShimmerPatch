package moe.shimmerfly.shimmerpatch.config

/**
 * Which zip and signing engine a patch is built with.
 *
 * Both engines are compiled into the manager, so this only decides which one runs while patching:
 * apkzlib is the one patching has always used, and NeoApk is the engine the patcher moved to
 * upstream. The stored value is the name, so a preference written by a newer build still reads
 * back on one that has never heard of it.
 */
enum class PatchEngine(val prefValue: String) {
    APKZLIB("apkzlib"),
    NEOAPK("neoapk"),
    ;

    companion object {
        fun fromPrefValue(value: String?): PatchEngine =
            entries.firstOrNull { it.prefValue == value } ?: APKZLIB
    }
}
