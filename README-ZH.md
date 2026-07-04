# Neo LSPatch Framework

[![Java](https://img.shields.io/badge/Java-ED8B00?logo=OpenJDK&logoColor=white&label=)](https://openjdk.org/) [![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=Kotlin&logoColor=white&label=)](https://kotlinlang.org/) [![Download](https://img.shields.io/github/v/release/7723mod/NPatch?color=orange&logoColor=white&label=&logo=DocuSign)](https://github.com/7723mod/NPatch/releases/latest) [![Total](https://shields.io/github/downloads/7723mod/NPatch/total?logo=Bookmeter&label=Counts&logoColor=yellow&color=yellow)](https://github.com/7723mod/NPatch/releases)

## 简介

> English: [README.md](README.md)

NPatch 是一个无需 root 的 LSPosed / LSPatch 风格框架，会把 dex 与原生库注入到目标 APK 中，让应用在自身进程里获得 Xposed API 支持。

官网：[npatch.nkbe.top](https://npatch.nkbe.top)

最新指南、架构说明、使用方式和版本相关信息，请优先以官网为准。官网是目前最完整也最及时的文档来源。

重点说明：

- 管理器不需要常驻前台。
- NPatch 的核心是进程内注入，不是系统级的全局 XposedService。
- 但是当你要新增或移除模块，或者同步最新配置时，管理器仍然必须已安装，并且能被系统正常访问。
- 模块作用域与配置仍然由管理器的数据流负责管理。
- 本地模式与内嵌模式的差异，请以官网文档为准。

支持版本：

- 最低版本：Android 9
- 最高版本：理论上与 [JingMatrix/LSPosed](https://github.com/JingMatrix/LSPosed#supported-versions) 相同

下载：

- 稳定版：[GitHub Releases](https://github.com/7723mod/NPatch/releases)
- 测试版只会通过 Telegram 频道 `@ONPatch` 发布

使用方式：

- Jar 模式
  - 下载 `npatch.jar`
  - 执行 `java -jar npatch.jar`
- 管理器模式
  - 在 Android 设备上安装 `manager.apk`
  - 按照管理器 App 的指引操作

致谢：

- [LSPosed](https://github.com/JingMatrix/LSPosed)：核心框架
- [Xpatch](https://github.com/WindySha/Xpatch)：分支来源
- [Apkzlib](https://android.googlesource.com/platform/tools/apkzlib)：重打包工具
