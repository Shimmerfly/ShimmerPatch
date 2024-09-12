/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#ifndef LSPATCH_OAT_FILE_MANAGER_H
#define LSPATCH_OAT_FILE_MANAGER_H

#include "context.h"
#include "utils/hook_helper.hpp"

using namespace lsplant;

namespace art {
class FileManager {
public:
    inline static MemberHooker<
        "_ZN3art14OatFileManager25RunBackgroundVerificationERKNSt3__"
        "16vectorIPKNS_7DexFileENS1_9allocatorIS5_EEEEP8_jobjectPKc",
        FileManager, void(const std::vector<const void *> &, jobject, const char *)>
        RunBackgroundVerificationWithContext_ =
            +[](FileManager *thiz, const std::vector<const void *> &dex_files, jobject class_loader,
                const char *class_loader_context) {
                if (lspd::Context::GetInstance()->GetCurrentClassLoader() == nullptr) {
                    LOGD("Disabled background verification");
                    return;
                }
                RunBackgroundVerificationWithContext_(thiz, dex_files, class_loader,
                                                      class_loader_context);
            };

    inline static MemberHooker<
        "_ZN3art14OatFileManager25RunBackgroundVerificationERKNSt3__"
        "16vectorIPKNS_7DexFileENS1_9allocatorIS5_EEEEP8_jobject",
        FileManager, void(const std::vector<const void *> &, jobject)>
        RunBackgroundVerification_ =
            +[](FileManager *thiz, const std::vector<const void *> &dex_files,
                jobject class_loader) {
                if (lspd::Context::GetInstance()->GetCurrentClassLoader() == nullptr) {
                    LOGD("Disabled background verification");
                    return;
                }
                RunBackgroundVerification_(thiz, dex_files, class_loader);
            };

public:
    static void DisableBackgroundVerification(const lsplant::HookHandler &handler) {
        const int api_level = lspd::GetAndroidApiLevel();
        if (api_level >= __ANDROID_API_Q__) {
            handler.hook(RunBackgroundVerificationWithContext_);
            handler.hook(RunBackgroundVerification_);
        }
    }
};
}  // namespace art

#endif  // LSPATCH_OAT_FILE_MANAGER_H
