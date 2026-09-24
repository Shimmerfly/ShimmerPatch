# This DEX is the patch loader's parent; their obfuscated classes must not collide.
-repackageclasses top.nkbe.npatch.internal.metaloader

-keep class top.nkbe.npatch.metaloader.LSPAppComponentFactoryStub {
    public static byte[] dex;
    public static boolean hideLibs;
    <init>();
}
-keep class * extends androidx.room.Entity {
    <fields>;
}
-keep interface * extends androidx.room.Dao {
    <methods>;
}

-dontwarn androidx.annotation.NonNull
-dontwarn androidx.annotation.Nullable
-dontwarn androidx.annotation.VisibleForTesting
