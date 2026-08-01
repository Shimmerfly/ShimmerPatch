package top.nkbe.npatch.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SB {
    private static final long EXIT_DELAY_MS = 300L;

    private SB() {
    }

    public static boolean hasConflict(Context context) {
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        return check(appContext);
    }

    public static void triggerConflict(Context context) {
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(appContext, getConflictMsg(), Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Process.killProcess(Process.myPid());
                System.exit(0);
            }, EXIT_DELAY_MS);
        });
    }

    private static boolean check(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            List<PackageInfo> packages = pm.getInstalledPackages(
                    PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES);
            if (packages == null) return false;
            for (PackageInfo pi : packages) {
                if (hasHkpManifestMarker(pi) && verifySignature(pi)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasHkpManifestMarker(PackageInfo pi) {
        if (pi == null || pi.applicationInfo == null) return false;
        if (apkHasHkpManifestMarker(pi.applicationInfo.sourceDir)) return true;
        String[] splitSourceDirs = pi.applicationInfo.splitSourceDirs;
        if (splitSourceDirs == null) return false;
        for (String splitSourceDir : splitSourceDirs) {
            if (apkHasHkpManifestMarker(splitSourceDir)) return true;
        }
        return false;
    }

    private static boolean apkHasHkpManifestMarker(String apkPath) {
        if (apkPath == null) return false;
        try (ZipFile zip = new ZipFile(apkPath)) {
            ZipEntry manifest = zip.getEntry(getManifestXml());
            return manifestContains(zip, manifest, getHkpActivityMarker());
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean manifestContains(ZipFile zip, ZipEntry manifest, String marker) {
        if (zip == null || manifest == null || marker == null) return false;
        try (var is = zip.getInputStream(manifest)) {
            byte[] data = is.readAllBytes();
            return containsUtf8(data, marker) || containsUtf16Le(data, marker);
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean containsUtf8(byte[] data, String marker) {
        if (data == null || marker == null) return false;
        byte[] needle = marker.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return indexOf(data, needle);
    }

    private static boolean containsUtf16Le(byte[] data, String marker) {
        if (data == null || marker == null) return false;
        byte[] needle = marker.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        return indexOf(data, needle);
    }

    private static boolean indexOf(byte[] data, byte[] needle) {
        if (data == null || needle == null || needle.length == 0 || data.length < needle.length) {
            return false;
        }
        for (int i = 0; i <= data.length - needle.length; i++) {
            int j = 0;
            while (j < needle.length && data[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) return true;
        }
        return false;
    }

    private static boolean verifySignature(PackageInfo pi) {
        if (pi == null) return false;
        try {
            String target = getHkpHash();
            if (pi.signingInfo != null) {
                if (matchesSignature(target, pi.signingInfo.getApkContentsSigners())) return true;
                if (matchesSignature(target, pi.signingInfo.getSigningCertificateHistory())) return true;
            }
            return matchesSignature(target, pi.signatures);
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean matchesSignature(String target, Signature[] sigs) {
        if (target == null || sigs == null) return false;
        for (Signature sig : sigs) {
            if (sig != null && target.equalsIgnoreCase(sha256(sig.toByteArray()))) return true;
        }
        return false;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    private static String getConflictMsg() {
        return r("TSsbUwERxD82PwQ9dyRzbMwoQig6AB");
    }

    private static String getHkpActivityMarker() {
        return r("MQMf5wYiQgCg3wLA8iegdAkssN");
    }

    private static String getManifestXml() {
        return r("CyksVdkxbeCpoFfVGuQzeaM8ZZ");
    }

    private static String getHkpHash() {
        return r("z4qgJ7sznm5Xmexvm6t1biHSecAjiAv12MEkrs5S4rre2Aa1Z25TU7tYGDHbvVepCUJ8jVqqve9DWJ3QTssE8pg");
    }

    private static String r(String value) {
        final String alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        java.math.BigInteger n = java.math.BigInteger.ZERO;
        for (int i = 0; i < value.length(); i++) {
            int digit = alphabet.indexOf(value.charAt(i));
            if (digit < 0) return "";
            n = n.multiply(java.math.BigInteger.valueOf(58)).add(java.math.BigInteger.valueOf(digit));
        }
        byte[] bytes = n.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        int zeros = 0;
        while (zeros < value.length() && value.charAt(zeros) == alphabet.charAt(0)) zeros++;
        if (zeros > 0) {
            byte[] padded = new byte[zeros + bytes.length];
            System.arraycopy(bytes, 0, padded, zeros, bytes.length);
            bytes = padded;
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
