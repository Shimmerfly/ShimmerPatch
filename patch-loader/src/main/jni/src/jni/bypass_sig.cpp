//
// Created by VIP on 2021/4/25.
// Modified  by HSSkyBoy on 2025/12/15
//

#include "bypass_sig.h"

#include "../src/native_api.h"
#include "elf_util.h"
#include "logging.h"
#include "native_util.h"
#include "patch_loader.h"
#include "utils/hook_helper.hpp"
#include "utils/jni_helper.hpp"
#include <unistd.h>
#include <string>
#include <cstring>
#include <memory>

using lsplant::operator""_sym;

namespace lspd {

    static std::string targetApkPath;
    static std::string redirectApkPath;
    static std::string currentPackageName;
    static void *openat_backup = nullptr;

    inline static constexpr const char* kLibCName = "libc.so";

    // 修改回傳型別以匹配 kImg 的實際型別
    std::unique_ptr<SandHook::ElfImg> &GetC(bool release = false) {
        static auto kImg = std::make_unique<SandHook::ElfImg>(kLibCName);
        if (release) {
            kImg.reset();
            kImg = nullptr;
        }
        return kImg;
    }

    // OpenAt Hook 邏輯
    inline static auto __openat_ =
            "__openat"_sym.hook->*[]<lsplant::Backup auto backup>(int fd, const char *pathname, int flag,
                                                                  int mode) static -> int {
                if (pathname && !targetApkPath.empty() && strcmp(pathname, targetApkPath.c_str()) == 0) {
                    return backup(fd, redirectApkPath.c_str(), flag, mode);
                }
                return backup(fd, pathname, flag, mode);
            };

    static bool HookOpenat(const lsplant::HookHandler &handler) { return handler(__openat_); }

    LSP_DEF_NATIVE_METHOD(void, SigBypass, enableOpenatHook,
                          jstring jOrigApkPath,
                          jstring jCacheApkPath,
                          jstring jPkgName) {

        if (jOrigApkPath == nullptr || jCacheApkPath == nullptr) {
            LOGE("Invalid arguments: paths cannot be null.");
            return;
        }

        lsplant::JUTFString strOrig(env, jOrigApkPath);
        lsplant::JUTFString strRedirect(env, jCacheApkPath);

        targetApkPath = strOrig.get();
        redirectApkPath = strRedirect.get();

        if (jPkgName != nullptr) {
            lsplant::JUTFString strPkg(env, jPkgName);
            currentPackageName = strPkg.get();
        }

        LOGI("Enable OpenAt Hook: %s -> %s (Pkg: %s)",
             targetApkPath.c_str(), redirectApkPath.c_str(), currentPackageName.c_str());

        auto r = HookOpenat(lsplant::InitInfo{
                .inline_hooker =
                [](auto t, auto r) {
                    void *bk = nullptr;
                    int ret = HookInline(t, r, &bk);
                    if (ret == 0) openat_backup = bk;
                    return ret == 0 ? bk : nullptr;
                },
                .art_symbol_resolver = [](auto symbol) {
                    return GetC()->getSymbAddress(symbol);
                },
        });
        if (!r) {
            LOGE("Hook __openat (libc) fail");
        }
        // 无论 Hook 成功与否，都确保清除 libc.so 的 ElfImg
        GetC(true);
    }

    LSP_DEF_NATIVE_METHOD(void, SigBypass, disableOpenatHook) {
        LOGI("Disable OpenAt Hook requested");
        targetApkPath.clear();
        redirectApkPath.clear();
    }

    // 註冊 JNI 方法
    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(SigBypass, enableOpenatHook, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
            LSP_NATIVE_METHOD(SigBypass, disableOpenatHook, "()V")
    };

    void RegisterBypass(JNIEnv *env) { REGISTER_LSP_NATIVE_METHODS(SigBypass); }

}  // namespace lspd
