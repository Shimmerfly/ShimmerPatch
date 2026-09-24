-dontobfuscate
-keep class com.beust.jcommander.** { *; }
-keep class moe.shimmerfly.shimmerpatch.Patcher$Options { *; }
-keep class moe.shimmerfly.shimmerpatch.share.LSPConfig { *; }
-keep class moe.shimmerfly.shimmerpatch.share.PatchConfig { *; }
-keep class org.lsposed.lspd.nativebridge.** { *; }
-keep class moe.shimmerfly.shimmerpatch.loader.SigBypass { *; }
-keepclassmembers class org.lsposed.patch.NPatch {
    private <fields>;
}
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
