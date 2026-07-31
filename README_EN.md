# NPatch Remote API

English · [简体中文](README.md)

NPatch Remote API is a lightweight Android SDK for the Xposed module settings UI. It lets a module app securely connect to NPatch Manager in Local mode and read and write Remote Preferences and Files in the NPatch Remote Store, partitioned by module package.

## What it does

- A module app obtains the standard libxposed API 102 `IXposedService` through an authenticated ContentProvider.
- Manager isolates Preferences and Files by module package and binds the service to the Binder calling UID.
- Injected target processes keep using the read-only Remote API supplied by libxposed; this SDK does not expose the private target-side AIDL.
- The SDK provides a time-bounded synchronous connect, `connectAsync`, provider availability checks, and support for a custom Manager authority.

## Three channels: `XposedInterface` and `XposedService` are different

`XposedInterface` and `XposedService` belong to different processes, lifecycles, and responsibilities. NPatch Remote API does not participate in target-process injection and does not provide `XposedInterface`; it only adds a module-app entry point for obtaining the standard `XposedService` contract in NPatch Local mode.

| Channel | Process | Purpose | Delivery |
| --- | --- | --- | --- |
| `XposedInterface` | Injected target app | Module entry, hooks, and target-process lifecycle | libxposed module lifecycle callbacks such as `XposedModule.attachFramework(...)` |
| `XposedService` | Module or settings app | Scope, Remote Preferences, Remote Files, and hot reload | The module registers `<module-package>.XposedService`; `XposedServiceHelper.registerListener(...)` receives the Binder |
| `NPatchRemoteClient` | Module settings app | Explicit/fallback connection in NPatch Local mode | Authenticated NPatch Manager ContentProvider, returning the same API 102 `IXposedService` contract |

Modules should prefer the standard libxposed `XposedServiceHelper.registerListener(...)` path. Use `NPatchRemoteClient` only when standard service delivery is unavailable, is not triggered, or the module explicitly needs to connect to NPatch Local Manager. It does not replace `XposedInterface` or define another hooking API.

## Add the SDK

Download the AAR from [Releases](https://github.com/7723mod/NPatch-Remote-API/releases), copy it into the module app's `libs/` directory, then add the dependency:

```kotlin
dependencies {
    implementation(files("libs/npatch-remote-api-v1.0.0-release.aar"))
    implementation("io.github.libxposed:interface:102.0.0")
}
```

The SDK requires Android 9 (API 28) or higher. Building this repository requires JDK 21, Android SDK 37, and the bundled Gradle Wrapper.

## Quick start

Connecting may start the NPatch Manager process. Call the synchronous API from a worker thread, or use the asynchronous API from the UI; do not block the main thread on connection.

```java
NPatchRemoteClient.connectAsync(getApplicationContext())
        .thenAccept(client -> {
            SharedPreferences preferences =
                    client.getRemotePreferences("settings");
            preferences.edit().putBoolean("enabled", true).apply();
        })
        .exceptionally(error -> {
            Log.e("Module", "NPatch Remote unavailable", error);
            return null;
        });
```

If you build a Manager with a custom application ID, pass the module package name and its authority:

```java
NPatchRemoteClient client = NPatchRemoteClient.connect(
        context,
        context.getPackageName(),
        "your.manager.application.id.remote"
);
```

For the complete integration guide, API behavior, and security boundary, see the NPatch website:

- [NPatch Remote API Developer Guide](https://npatch.nkbe.top/en/guide/remote-api.html)

## Build

```bash
./gradlew assembleRelease
```

The AAR is written to `build/outputs/aar/`. You can also run `publishReleasePublicationToMavenLocal` to publish it to the local Maven repository.

## Compatibility

- SDK: `1.0.0`
- libxposed interface: `102.0.0`
- NPatch: `1.0.7` or newer
- Android: API 28+ (Android 9 or higher)

## License

Apache License 2.0. See [LICENSE](LICENSE).
