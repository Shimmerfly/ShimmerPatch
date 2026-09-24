# ShimmerPatch Remote API

[English](README_EN.md) · 简体中文

ShimmerPatch Remote API 是给 Xposed 模块介面 使用的轻量级 Android SDK。它让模块 App 在本地修补模式下安全连接 ShimmerPatch Manager，读写 ShimmerPatch Remote Store 中的 Preferences 和 Files，数据按模块包名隔离。

## 功能特性

- 模块 App 通过经过身份验证的 ContentProvider 取得标准 libxposed API 102 的 `IXposedService`。
- Manager 按模块包名隔离 Preferences 与 Files，并把服务绑定到 Binder 调用 UID。
- 被注入的目标进程继续使用 libxposed 提供的只读 Remote API；本 SDK 不暴露目标进程侧的私有 AIDL。
- 支持带超时的同步连接、`connectAsync`、Provider 可用性探测以及自定义 Manager authority。

## 与 libxposed 的关系

ShimmerPatch Remote API 不是另一套 Xposed API，也不取代 libxposed。

`XposedInterface` 和 `XposedService` 属于不同进程、不同生命周期和不同业务。ShimmerPatch Remote API 不参与注入目标进程，也不提供 `XposedInterface`；它只为模块本体/设置 App 在 ShimmerPatch Local 模式下补充一个取得标准 `XposedService` 的入口。

| 通道 | 所在进程 | 用途 | 获取方式 |
| --- | --- | --- | --- |
| `XposedInterface` | 被注入的目标 App | 模块入口、Hook、目标进程生命周期 | `XposedModule.attachFramework(...)` 等 libxposed 模块生命周期回调 |
| `XposedService` | 模块本体或设置 App | 作用域、Remote Preferences、Remote Files、热重载 | 模块注册 `<模块包名>.XposedService`，由 `XposedServiceHelper.registerListener(...)` 接收 Binder |
| `ShimmerPatchRemoteClient` | 模块设置 App | ShimmerPatch Local 模式的备用/显式连接 | 经过校验的 ShimmerPatch Manager ContentProvider，返回同一份 API 102 `IXposedService` 合约 |

模块应优先使用 libxposed 的 `XposedServiceHelper.registerListener(...)` 标准路径。只有标准服务投递不可用、未触发或模块需要显式连接 ShimmerPatch Local Manager 时，才使用 `ShimmerPatchRemoteClient`。它不会替代 `XposedInterface`，也不会定义另一套 Hook API。

## 引入 SDK

从 [Releases](https://github.com/7723mod/NPatch-Remote-API/releases) 下载 AAR，复制到模块 App 的 `libs/` 目录，然后添加依赖：

```kotlin
dependencies {
    implementation(files("libs/shimmerpatch-remote-api-v1.0.1-release.aar"))
    implementation("io.github.libxposed:interface:102.0.0")
}
```

SDK 最低支持 Android 9（API 28）或更高版本。独立构建本仓库需要 JDK 21、Android SDK 37 和自带的 Gradle Wrapper。

## 快速开始

连接可能需要启动 ShimmerPatch Manager 进程。请在工作线程调用同步 API，或在 UI 中使用异步 API；不要在主线程上阻塞等待连接。

```java
ShimmerPatchRemoteClient.connectAsync(getApplicationContext())
        .thenAccept(client -> {
            SharedPreferences preferences =
                    client.getRemotePreferences("settings");
            preferences.edit().putBoolean("enabled", true).apply();
        })
        .exceptionally(error -> {
            Log.e("Module", "ShimmerPatch Remote unavailable", error);
            return null;
        });
```

若自行编译的 Manager 使用了自定义 application ID，可同时传入模块包名与对应的 authority：

```java
ShimmerPatchRemoteClient client = ShimmerPatchRemoteClient.connect(
        context,
        context.getPackageName(),
        "your.manager.application.id.remote"
);
```

完整接入方式、API 行为与安全边界请参阅 ShimmerPatch 官网开发指南：

- [ShimmerPatch Remote API 开发指南](https://shimmerpatch.nkbe.top/guide/remote-api.html)

## 构建

```bash
./gradlew assembleRelease
```

AAR 输出在 `build/outputs/aar/`。也可以运行 `publishReleasePublicationToMavenLocal` 发布到本机 Maven 仓库。

## 兼容性

- SDK：`1.0.1`
- libxposed interface：`102.0.0`
- ShimmerPatch：`1.0.7` 或更高
- Android：API 28+（Android 9 或更高）

## 许可证

Apache License 2.0。参见 [LICENSE](LICENSE)。
