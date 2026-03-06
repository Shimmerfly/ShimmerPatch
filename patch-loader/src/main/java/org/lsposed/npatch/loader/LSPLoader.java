package org.lsposed.npatch.loader;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LSPLoader {
    private static final String TAG = "NPatch";

    public static void initModules(LoadedApk loadedApk) {
        XposedInit.loadedPackagesInProcess.add(loadedApk.getPackageName());
        setPackageNameForResDir(loadedApk.getPackageName(), loadedApk.getResDir());
        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
        lpparam.packageName = loadedApk.getPackageName();
        lpparam.processName = ActivityThread.currentProcessName();
        lpparam.classLoader = loadedApk.getClassLoader();
        lpparam.appInfo = loadedApk.getApplicationInfo();
        lpparam.isFirstApplication = true;
        XC_LoadPackage.callAll(lpparam);
    }

    private static void setPackageNameForResDir(String packageName, String resDir) {
        try {
            // Use reflection to avoid direct type reference to android.content.res.XResources
            // which fails class resolution on Android 16+ due to strict boot classloader
            // namespace delegation for the android.content.res.* package.
            ClassLoader cl = LSPLoader.class.getClassLoader();
            Class<?> xResourcesClass = cl.loadClass("android.content.res.XResources");
            Method setMethod = xResourcesClass.getMethod("setPackageNameForResDir", String.class, String.class);
            setMethod.invoke(null, packageName, resDir);
        } catch (Throwable e) {
            Log.w(TAG, "XResources.setPackageNameForResDir not available, skipping resource dir setup", e);
        }
    }
}