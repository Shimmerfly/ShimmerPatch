package top.nkbe.npatch.util;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import io.github.libxposed.service.HookedProcess;
import io.github.libxposed.service.IHotReloadCallback;
import io.github.libxposed.service.IXposedScopeCallback;
import org.lsposed.lspd.service.ILSPInjectedModuleService;
import org.lsposed.lspd.service.IRemotePreferenceCallback;

public final class LocalInjectedModuleService extends ILSPInjectedModuleService.Stub {
    private final NPatchRemoteStore store;
    private final boolean writableFiles;
    private final int allowedUid;

    public LocalInjectedModuleService(Context context, String packageName) {
        this(context, packageName, false, -1);
    }

    public LocalInjectedModuleService(Context context, String packageName, boolean writableFiles) {
        this(context, packageName, writableFiles, -1);
    }

    public LocalInjectedModuleService(
            Context context, String packageName, boolean writableFiles, int allowedUid) {
        store = NPatchRemoteStore.get(context, packageName);
        this.writableFiles = writableFiles;
        this.allowedUid = allowedUid;
    }

    @Override
    public long getFrameworkProperties() {
        enforceCaller();
        return NPatchRemoteStore.CAP_REMOTE;
    }

    @Override
    public java.util.List<String> getScope() {
        enforceCaller();
        return java.util.Collections.emptyList();
    }

    @Override
    public void requestScope(java.util.List<String> packages, IXposedScopeCallback callback) {
        enforceCaller();
        if (callback != null) {
            try {
                callback.onScopeRequestFailed("Scope requests are not supported by local mode");
            } catch (RemoteException ignored) {
            }
        }
    }

    @Override
    public void removeScope(java.util.List<String> packages) {
        enforceCaller();
    }

    @Override
    public java.util.List<HookedProcess> getRunningTargets() {
        enforceCaller();
        return java.util.Collections.emptyList();
    }

    @Override
    public void hotReloadModule(long targetId, Bundle data, IHotReloadCallback callback) {
        enforceCaller();
        if (callback != null) {
            try {
                callback.onHotReloadResult(
                        io.github.libxposed.service.IXposedService.HOT_RELOAD_UNSUPPORTED,
                        "Hot reload is not supported by local mode");
            } catch (RemoteException ignored) {
            }
        }
    }

    @Override
    public Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback) {
        enforceCaller();
        return store.requestPreferences(group, callback);
    }

    @Override
    public void updateRemotePreferences(String group, Bundle diff) throws RemoteException {
        enforceCaller();
        store.updatePreferences(group, diff);
    }

    @Override
    public void deleteRemotePreferences(String group) throws RemoteException {
        enforceCaller();
        store.deletePreferences(group);
    }

    @Override
    public ParcelFileDescriptor openRemoteFile(String path) throws RemoteException {
        enforceCaller();
        return store.openFile(path, writableFiles);
    }

    @Override
    public String[] getRemoteFileList() {
        enforceCaller();
        return store.listFiles();
    }

    @Override
    public boolean deleteRemoteFile(String path) {
        enforceCaller();
        return store.deleteFile(path);
    }

    private void enforceCaller() {
        if (allowedUid >= 0 && Binder.getCallingUid() != allowedUid) {
            throw new SecurityException("Remote service binder was passed to another UID");
        }
    }
}
