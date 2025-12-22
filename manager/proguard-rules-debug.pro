-dontobfuscate
-keep class com.beust.jcommander.** { *; }
-keep class org.lsposed.npatch.Patcher$Options { *; }
-keep class org.lsposed.npatch.share.LSPConfig { *; }
-keep class org.lsposed.npatch.share.PatchConfig { *; }
-keep class org.lsposed.lspd.nativebridge.** { *; }
-keep class org.lsposed.npatch.loader.SigBypass { *; }
-keepclassmembers class org.lsposed.patch.NPatch {
    private <fields>;
}
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
