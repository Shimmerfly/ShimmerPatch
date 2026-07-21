# Neo LSPatch Framework

[![Java](https://img.shields.io/badge/Java-ED8B00?logo=OpenJDK&logoColor=white&label=)](https://openjdk.org/) [![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=Kotlin&logoColor=white&label=)](https://kotlinlang.org/) [![Download](https://img.shields.io/github/v/release/7723mod/NPatch?color=orange&logoColor=white&label=&logo=DocuSign)](https://github.com/7723mod/NPatch/releases/latest) [![Total](https://shields.io/github/downloads/7723mod/NPatch/total?logo=Bookmeter&label=Counts&logoColor=yellow&color=yellow)](https://github.com/7723mod/NPatch/releases)

## Introduction

> 中文见 [README-ZH.md](README-ZH.md)

NPatch is a rootless implementation of the LSPosed/LSPatch-style framework that injects dex and native libraries into the target APK to provide Xposed API support inside the app process.

Official website: [npatch.nkbe.top](https://npatch.nkbe.top)

For the most up-to-date guides, architecture notes, usage details, and release-related explanations, please refer to the official website first. The website is the main source of truth for day-to-day usage and documentation updates.

Key points:

- NPatch is designed around in-process injection rather than a system-wide Xposed service.
- The Manager does not need to stay in the foreground all the time.
- However, the Manager still must be installed and accessible to the system when you want to add or remove modules, or when you want to sync the latest configuration.
- Module scope and configuration are still managed through the Manager data flow.
- For local mode and embedded mode differences, please check the official documentation on the website.

Supported Android versions:

- Minimum: Android 9
- Maximum: In theory, the same as [JingMatrix/LSPosed](https://github.com/JingMatrix/LSPosed#supported-versions)

Download:

- Stable releases: [GitHub Releases](https://github.com/7723mod/NPatch/releases)
- Canary builds: [GitHub Actions](https://github.com/7723mod/NPatch/actions)
- Debug builds are only available through the @ONPatch Telegram channel

Usage:

- Jar
  - Download `npatch.jar`
  - Run `java -jar npatch.jar`
- Manager
  - Install `manager.apk` on an Android device
  - Follow the instructions in the manager app

Translation:

You can contribute translations through [Crowdin](https://crowdin.com/project/lspatch_jingmatrix).

Credits:

- [LSPosed](https://github.com/JingMatrix/LSPosed): Core framework
- [Xpatch](https://github.com/WindySha/Xpatch): Fork source
- [Apkzlib](https://android.googlesource.com/platform/tools/apkzlib): Repacking tool
