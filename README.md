# NPatch Remote API

NPatch Remote API 是给 Xposed 模块伴生 App 使用的轻量 Android SDK。它让模块设置界面在本地修补模式下安全连接 NPatch Manager，共享 Remote Preferences 与 Remote Files。

The NPatch Remote API is a small Android SDK for Xposed module companion apps. It lets a module settings UI securely connect to NPatch Manager in Local mode and share Remote Preferences and Remote Files.

## 能做什么 / What it does

- 模块 App 通过已验证的 ContentProvider 获取标准 libxposed API 102 `IXposedService`。
- Manager 按模块包名隔离 Preferences 和 Files，并绑定 Binder 的 calling UID。
- 被注入目标进程继续使用 libxposed 提供的只读 Remote API；公共 SDK 不暴露目标进程的内部 AIDL。
- SDK 支持有界同步连接、`connectAsync`、Provider 可用性探测和自定义 Manager authority。

- A module app obtains the standard libxposed API 102 `IXposedService` through an authenticated ContentProvider.
- Manager isolates Preferences and Files by module package and binds the service to the original Binder calling UID.
- Injected target processes continue to use the read-only Remote API supplied by libxposed; this SDK does not expose the private target-side AIDL.
- The SDK provides bounded synchronous connection, `connectAsync`, provider availability checks, and custom Manager authorities.

## 与 libxposed 的关系 / Relationship to libxposed

NPatch Remote API 不是另一套 Hook API，也不取代 libxposed。它只解决本地模式下普通模块 App 无法通过 injected-process callback 获取 service 的问题；取得 Binder 后仍使用标准 API 102 合约。

This is not another hooking API and does not replace libxposed. It only solves the Local-mode connection gap for a normal module app that cannot receive the injected-process callback; the returned Binder still follows the standard API 102 contract.

| 能力 / Capability | libxposed | NPatch Remote API |
| --- | --- | --- |
| Hook 与模块生命周期 / Hooks and lifecycle | 标准入口 / Standard API | 不封装 / Not wrapped |
| 被注入进程取得 service / Service in an injected process | 框架 callback | 不参与 / Not involved |
| 模块设置 App 连接 / Module-app connection | 框架相关 / Framework-dependent | Manager Provider 验证入口 / Authenticated Manager entry |
| Remote Preferences / Files | API 102 Binder 合约 / API 102 Binder contract | 使用同一合约 / Same contract |

## 引入 / Add the SDK

从 [Releases](https://github.com/7723mod/NPatch-Remote-API/releases) 下载 AAR：

Download the AAR from [Releases](https://github.com/7723mod/NPatch-Remote-API/releases):

```kotlin
dependencies {
    implementation(files("libs/npatch-remote-api-v1.0.0-release.aar"))
    implementation("io.github.libxposed:interface:102.0.0")
}
```

SDK 的最低 Android 版本为 API 28，独立构建需要 JDK 21、Android SDK 37 和 Gradle Wrapper。

The minimum Android version is API 28. Building this repository requires JDK 21, Android SDK 37, and the included Gradle Wrapper.

## 快速开始 / Quick start

请在工作线程调用同步 API，或在 UI 中使用异步 API。连接结果不应阻塞主线程。

Call the synchronous API from a worker thread, or use the asynchronous API from a UI. Do not block the main thread on connection.

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

若 Manager 使用自定义 application ID，可传入对应 authority。For a custom Manager application ID, pass its authority:

```java
NPatchRemoteClient client = NPatchRemoteClient.connect(
        context,
        context.getPackageName(),
        "your.manager.application.id.remote"
);
```

完整接入、API 行为和安全边界请参阅：

For the complete integration guide, API behavior, and security boundary, see:

- [接入指南 / Getting started](docs/getting-started.md)
- [API 与行为 / API reference](docs/api-reference.md)
- [安全与架构 / Security and architecture](docs/architecture.md)

## 构建 / Build

```bash
./gradlew assembleRelease
```

输出位于 `build/outputs/aar/`，也可以执行 `publishReleasePublicationToMavenLocal` 发布到本机 Maven。

The AAR is written to `build/outputs/aar/`. You can also run `publishReleasePublicationToMavenLocal` to publish it to the local Maven repository.

## 兼容性 / Compatibility

- SDK API: `1.0.0`
- libxposed interface: `102.0.0`
- NPatch: `1.0.7` or newer
- Android: API 28+

## License

Apache License 2.0. See [LICENSE](LICENSE).
