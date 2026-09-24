-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
 public static void check*(...);
 public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}
-assumenosideeffects public class kotlin.coroutines.jvm.internal.DebugMetadataKt {
   private static ** getDebugMetadataAnnotation(...) return null;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.beust.jcommander.** { *; }
-keep interface com.beust.jcommander.** { *; }
-keep class moe.shimmerfly.shimmerpatch.patch.NPatch { *; }
-keepclassmembers class moe.shimmerfly.shimmerpatch.patch.NPatch {
    @com.beust.jcommander.Parameter <fields>;
}

-keepclassmembers class moe.shimmerfly.shimmerpatch.database.dao.** { *; }
-keep class moe.shimmerfly.shimmerpatch.database.entity.** { *; }
-keep class moe.shimmerfly.shimmerpatch.manager.ConfigProvider { *; }
-keep class moe.shimmerfly.shimmerpatch.Patcher$Options { *; }
-keep class moe.shimmerfly.shimmerpatch.share.LSPConfig { *; }
-keep class moe.shimmerfly.shimmerpatch.share.PatchConfig { *; }
-keep class org.lsposed.lspd.nativebridge.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
-keep class moe.shimmerfly.shimmerpatch.loader.SigBypass { *; }
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
-dontwarn org.lsposed.hiddenapibypass.**
-dontwarn com.squareup.moshi.**
-dontwarn retrofit2.**
-dontwarn okio.**

-keep class moe.shimmerfly.shimmerpatch.util.NeoPackageManager$AppInfo { *; }
-keep class moe.shimmerfly.shimmerpatch.util.NeoPackageManager$PatchedType { *; }
-keep class moe.shimmerfly.shimmerpatch.util.ModuleMetadataSnapshot { *; }
-keep class moe.shimmerfly.shimmerpatch.util.ModulePipeline { *; }
-keep class moe.shimmerfly.shimmerpatch.config.KeystorePreset { *; }

# APK Signature & Patching engine reflection/ASN1 requirements
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-keep class com.android.tools.build.apkzlib.** { *; }
-dontwarn com.android.tools.build.apkzlib.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class com.wind.meditor.** { *; }
-dontwarn com.wind.meditor.**
-keep class pxb.android.axml.** { *; }
-dontwarn pxb.android.axml.**
-keep class moe.shimmerfly.shimmerpatch.patch.** { *; }

