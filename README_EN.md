# NPatch Remote API

English · [简体中文](README.md)

NPatch Remote API is a lightweight Android SDK for the Xposed module settings UI. It lets a module app securely connect to NPatch Manager in Local mode and read and write Remote Preferences and Files in the NPatch Remote Store, partitioned by module package.

## What it does

- A module app obtains the standard libxposed API 102 `IXposedService` through an authenticated ContentProvider.
- Manager isolates Preferences and Files by module package and binds the service to the Binder calling UID.
- Injected target processes keep using the read-only Remote API supplied by libxposed; this SDK does not expose the private target-side AIDL.
- The SDK provides a time-bounded synchronous connect, `connectAsync`, provider availability checks, and support for a custom Manager authority.

## Relationship to libxposed

NPatch Remote API is **not another hooking API and does not replace libxposed**. libxposed API 102 defines the standard capabilities — `IXposedService`, Remote Preferences, Remote Files, and more. This SDK only solves the Local-mode connection gap for a normal module app that cannot receive the injected-process callback; the returned Binder still follows the standard API 102 contract.

| Capability | libxposed | NPatch Remote API |
| --- | --- | --- |
| Hooks and module lifecycle | Standard entry point | Not provided |
| Getting the service in an injected process | Framework callback | Not involved |
| Module settings app connection | No unified Local-mode entry | Authenticated Manager Provider entry |
| Remote Preferences / Files | API 102 Binder contract | Same contract |

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
