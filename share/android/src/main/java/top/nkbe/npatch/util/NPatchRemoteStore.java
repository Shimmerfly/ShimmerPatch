package top.nkbe.npatch.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.service.IRemotePreferenceCallback;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical storage backend for NPatch remote preferences and files.
 *
 * <p>All API generations and the libxposed compatibility service delegate to this class so they
 * observe the same data and validation rules.</p>
 */
public final class NPatchRemoteStore {
    public static final long CAP_REMOTE = 1L << 1;

    private static final Map<String, NPatchRemoteStore> INSTANCES = new ConcurrentHashMap<>();

    private static final class CallbackState {
        final IRemotePreferenceCallback callback;
        Map<String, Object> lastSnapshot;

        CallbackState(IRemotePreferenceCallback callback, Map<String, Object> lastSnapshot) {
            this.callback = callback;
            this.lastSnapshot = lastSnapshot;
        }
    }

    private final Context context;
    private final String modulePackageName;
    private final Map<String, PreferenceGroupState> preferenceGroups = new ConcurrentHashMap<>();

    private final class PreferenceGroupState {
        final SharedPreferences preferences;
        final Map<IBinder, CallbackState> callbacks = new ConcurrentHashMap<>();
        final SharedPreferences.OnSharedPreferenceChangeListener listener;

        PreferenceGroupState(String group) {
            preferences = context.getSharedPreferences(preferencesName(group), Context.MODE_PRIVATE);
            listener = (sharedPreferences, key) -> notifyPreferenceChanges(this);
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    private NPatchRemoteStore(Context context, String modulePackageName) {
        Context appContext = context.getApplicationContext();
        this.context = appContext == null ? context : appContext;
        this.modulePackageName = requireSafeIdentifier(modulePackageName, "module package name");
    }

    public static NPatchRemoteStore get(Context context, String modulePackageName) {
        Context appContext = context.getApplicationContext();
        Context storageContext = appContext == null ? context : appContext;
        String safePackageName = requireSafeIdentifier(modulePackageName, "module package name");
        String key = storageContext.getPackageName() + '@' + System.identityHashCode(storageContext)
                + ':' + safePackageName;
        return INSTANCES.computeIfAbsent(
                key, ignored -> new NPatchRemoteStore(storageContext, safePackageName));
    }

    public Bundle requestPreferences(String group, IRemotePreferenceCallback callback) {
        PreferenceGroupState state = preferenceGroup(group);
        HashMap<String, Object> snapshot = snapshotPreferences(state.preferences);
        if (callback != null) {
            IBinder binder = callback.asBinder();
            state.callbacks.put(binder, new CallbackState(callback, new HashMap<>(snapshot)));
            try {
                binder.linkToDeath(() -> state.callbacks.remove(binder), 0);
            } catch (RemoteException e) {
                state.callbacks.remove(binder);
            }
        }
        Bundle result = new Bundle();
        result.putSerializable("map", snapshot);
        result.putBoolean("managed", preferencesFile(group).isFile());
        return result;
    }

    @SuppressWarnings("deprecation")
    public void updatePreferences(String group, Bundle diff) throws RemoteException {
        Objects.requireNonNull(diff, "diff");
        SharedPreferences.Editor editor = preferenceGroup(group).preferences.edit();
        if (diff.getBoolean("clear", false)) {
            editor.clear();
        }

        Serializable deletes = diff.getSerializable("delete");
        if (deletes instanceof Set<?>) {
            for (Object key : (Set<?>) deletes) {
                if (key instanceof String) {
                    editor.remove((String) key);
                }
            }
        }

        Serializable puts = diff.getSerializable("put");
        if (puts instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) puts).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    continue;
                }
                putValue(editor, (String) entry.getKey(), entry.getValue());
            }
        }
        if (!editor.commit()) {
            throw new RemoteException("Failed to persist remote preferences");
        }
    }

    public void deletePreferences(String group) throws RemoteException {
        if (!preferenceGroup(group).preferences.edit().clear().commit()) {
            throw new RemoteException("Failed to delete remote preferences");
        }
    }

    public String[] listFiles() {
        String[] files = remoteFilesDir().list();
        return files == null ? new String[0] : files;
    }

    public ParcelFileDescriptor openFile(String path, boolean writable) throws RemoteException {
        File file = resolveRemoteFile(path);
        if (!writable && !file.isFile()) {
            return null;
        }
        if (writable) {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new RemoteException("Cannot create remote file directory");
            }
        }
        int mode = writable
                ? ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE
                : ParcelFileDescriptor.MODE_READ_ONLY;
        try {
            return ParcelFileDescriptor.open(file, mode);
        } catch (Throwable t) {
            RemoteException error = new RemoteException("Cannot open remote file: " + path);
            error.initCause(t);
            throw error;
        }
    }

    public boolean deleteFile(String path) {
        try {
            return resolveRemoteFile(path).delete();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private PreferenceGroupState preferenceGroup(String group) {
        String safeGroup = requireSafeIdentifier(group, "preference group");
        return preferenceGroups.computeIfAbsent(safeGroup, PreferenceGroupState::new);
    }

    private void notifyPreferenceChanges(PreferenceGroupState state) {
        HashMap<String, Object> current = snapshotPreferences(state.preferences);
        List<Map.Entry<IBinder, CallbackState>> callbacks =
                new ArrayList<>(state.callbacks.entrySet());
        for (Map.Entry<IBinder, CallbackState> entry : callbacks) {
            CallbackState callbackState = entry.getValue();
            Bundle diff = buildDiffBundle(callbackState.lastSnapshot, current);
            callbackState.lastSnapshot = new HashMap<>(current);
            if (diff.isEmpty()) {
                continue;
            }
            try {
                callbackState.callback.onUpdate(diff);
            } catch (RemoteException e) {
                state.callbacks.remove(entry.getKey());
            }
        }
    }

    private String preferencesName(String group) {
        return "npatch_remote_" + modulePackageName + '_' + group;
    }

    private File preferencesFile(String group) {
        return new File(
                new File(context.getApplicationInfo().dataDir, "shared_prefs"),
                preferencesName(requireSafeIdentifier(group, "preference group")) + ".xml");
    }

    private File remoteFilesDir() {
        return new File(context.getFilesDir(), "npatch/remote/" + modulePackageName);
    }

    private File resolveRemoteFile(String path) {
        String safePath = requireSafeIdentifier(path, "remote file name");
        return new File(remoteFilesDir(), safePath);
    }

    private static String requireSafeIdentifier(String value, String label) {
        if (value == null
                || value.isEmpty()
                || value.equals(".")
                || value.equals("..")
                || value.indexOf('/') >= 0
                || value.indexOf('\\') >= 0
                || !value.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid " + label);
        }
        return value;
    }

    private static void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Set<?>) {
            HashSet<String> strings = new HashSet<>();
            for (Object item : (Set<?>) value) {
                if (!(item instanceof String)) {
                    throw new IllegalArgumentException("Remote string set contains a non-string value");
                }
                strings.add((String) item);
            }
            editor.putStringSet(key, strings);
        } else {
            throw new IllegalArgumentException("Unsupported remote preference value");
        }
    }

    private static HashMap<String, Object> snapshotPreferences(SharedPreferences preferences) {
        HashMap<String, Object> snapshot = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Serializable) {
                snapshot.put(entry.getKey(), value);
            }
        }
        return snapshot;
    }

    private static Bundle buildDiffBundle(Map<String, Object> previous, Map<String, Object> current) {
        Set<String> deleted = new HashSet<>();
        HashMap<String, Object> updated = new HashMap<>();
        for (String key : previous.keySet()) {
            if (!current.containsKey(key)) {
                deleted.add(key);
            }
        }
        for (Map.Entry<String, Object> entry : current.entrySet()) {
            if (!Objects.equals(previous.get(entry.getKey()), entry.getValue())) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        Bundle diff = new Bundle();
        if (!deleted.isEmpty()) {
            diff.putSerializable("delete", new HashSet<>(deleted));
        }
        if (!updated.isEmpty()) {
            diff.putSerializable("put", updated);
        }
        return diff;
    }
}
