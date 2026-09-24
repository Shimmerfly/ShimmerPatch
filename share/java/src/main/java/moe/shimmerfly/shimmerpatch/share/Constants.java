package moe.shimmerfly.shimmerpatch.share;

public class Constants {

    final static public String CONFIG_ASSET_PATH = "assets/shimmerpatch/config.json";
    final static public String LOADER_DEX_ASSET_PATH = "assets/shimmerpatch/loader.bin";
    final static public String META_LOADER_DEX_ASSET_PATH = "assets/shimmerpatch/metaloader.dex";
    final static public String PROVIDER_DEX_ASSET_PATH = "assets/shimmerpatch/mtprovider.dex";
    final static public String ORIGINAL_APK_ASSET_PATH = "assets/shimmerpatch/origin.apk";
    final static public String EMBEDDED_MODULES_ASSET_PATH = "assets/shimmerpatch/modules/";

    final static public String PATCH_FILE_SUFFIX = "-shimmerpatched.apk";
    final static public String PATCH_ARCHIVE_SUFFIX = "-shimmerpatched.apks";
    final static public String PROXY_APP_COMPONENT_FACTORY = "moe.shimmerfly.shimmerpatch.metaloader.LSPAppComponentFactoryStub";
    final static public String MANAGER_PACKAGE_NAME = "moe.shimmerfly.shimmerpatch";
    final static public String REAL_GMS_PACKAGE_NAME = "com.google.android.gms";
    final static public int MIN_ROLLING_VERSION_CODE = 750;

    public static final int SIGBYPASS_NONE = 0;
    public static final int SIGBYPASS_BASIC = 1;
    public static final int SIGBYPASS_HIGH = 2;
    public static final int SIGBYPASS_EXTREME = 3;
    public static final int SIGBYPASS_SECCOMP = 4;
    public static final int SIGBYPASS_STEALTH = 5;
}
