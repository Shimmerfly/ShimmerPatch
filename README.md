# NPatch Remote API

[English](README_EN.md) · 简体中文

NPatch Remote API 是给 Xposed 模块设置界面（模块 App）使用的轻量级 Android SDK。它让模块 App 在本地修补模式下安全连接 NPatch Manager，读写 NPatch Remote Store 中的 Preferences 和 Files，数据按模块包名隔离。

## 功能特性

- 模块 App 通过经过身份验证的 ContentProvider 取得标准 libxposed API 102 的 `IXposedService`。
- Manager 按模块包名隔离 Preferences 与 Files，并把服务绑定到 Binder 调用 UID。
- 被注入的目标进程继续使用 libxposed 提供的只读 Remote API；本 SDK 不暴露目标进程侧的私有 AIDL。
- 支持带超时的同步连接、`connectAsync`、Provider 可用性探测以及自定义 Manager authority。

## 与 libxposed 的关系

NPatch Remote API **不是另一套 Hook API，也不取代 libxposed**。libxposed API 102 定义了 `IXposedService`、Remote Preferences、Remote Files 等标准能力；本 SDK 只解决本地修补模式下，普通模块 App 无法像被注入进程那样通过框架 callback 取得 service 的问题。取得 Binder 之后，仍然使用标准 API 102 合约。

| 能力 | libxposed | NPatch Remote API |
| --- | --- | --- |
| Hook 与模块生命周期 | 标准入口 | 不提供 |
| 被注入进程取得 service | 框架 callback | 不参与 |
| 模块设置 App 取得 service | 没有统一的本地模式入口 | 通过 Manager Provider 验证后取得 |
| Remote Preferences / Files | API 102 Binder 合约 | 使用同一份合约 |

## 引入 SDK

从 [Releases](https://github.com/7723mod/NPatch-Remote-API/releases) 下载 AAR，复制到模块 App 的 `libs/` 目录，然后添加依赖：

```kotlin
dependencies {
    implementation(files("libs/npatch-remote-api-v1.0.0-release.aar"))
    implementation("io.github.libxposed:interface:102.0.0")
}
```

SDK 最低支持 Android 9（API 28）或更高版本。独立构建本仓库需要 JDK 21、Android SDK 37 和自带的 Gradle Wrapper。

## 快速开始

连接可能需要启动 NPatch Manager 进程。请在工作线程调用同步 API，或在 UI 中使用异步 API；不要在主线程上阻塞等待连接。

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

若自行编译的 Manager 使用了自定义 application ID，可同时传入模块包名与对应的 authority：

```java
NPatchRemoteClient client = NPatchRemoteClient.connect(
        context,
        context.getPackageName(),
        "your.manager.application.id.remote"
);
```

完整接入方式、API 行为与安全边界请参阅 NPatch 官网开发指南：

- [NPatch Remote API 开发指南](https://npatch.nkbe.top/guide/remote-api.html)

## 构建

```bash
./gradlew assembleRelease
```

AAR 输出在 `build/outputs/aar/`。也可以运行 `publishReleasePublicationToMavenLocal` 发布到本机 Maven 仓库。

## 兼容性

- SDK：`1.0.0`
- libxposed interface：`102.0.0`
- NPatch：`1.0.7` 或更高
- Android：API 28+（Android 9 或更高）

## 许可证

Apache License 2.0。参见 [LICENSE](LICENSE)。
