//
// Created by loves on 6/19/2021.
//

#ifndef LSPATCH_PROFILE_SAVER_H
#define LSPATCH_PROFILE_SAVER_H

#include "utils/hook_helper.hpp"

using namespace lsplant;

namespace art {
class ProfileSaver {
private:
    inline static MemberHooker<"_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt", ProfileSaver,
                               bool(bool, uint16_t *)>
        ProcessProfilingInfo_ = +[](ProfileSaver *thiz, bool a, uint16_t *b) {
            LOGD("skipped profile saving");
            return true;
        };

    inline static MemberHooker<"_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt", ProfileSaver,
                               bool(bool, bool, uint16_t *)>
        ProcessProfilingInfoWithBool_ = +[](ProfileSaver *thiz, bool, bool, uint16_t *) {
            LOGD("skipped profile saving");
            return true;
        };

    inline static Hooker<"execve",
                         int(const char *pathname, const char *argv[], char *const envp[])>
        execve_ = +[](const char *pathname, const char *argv[], char *const envp[]) {
            if (strstr(pathname, "dex2oat")) {
                size_t count = 0;
                while (argv[count++] != nullptr);
                std::unique_ptr<const char *[]> new_args =
                    std::make_unique<const char *[]>(count + 1);
                for (size_t i = 0; i < count - 1; ++i) new_args[i] = argv[i];
                new_args[count - 1] = "--inline-max-code-units=0";
                new_args[count] = nullptr;

                LOGD("dex2oat by disable inline!");
                int ret = execve_(pathname, new_args.get(), envp);
                return ret;
            }
            int ret = execve_(pathname, argv, envp);
            return ret;
        };

public:
    static void DisableInline(const HookHandler &handler) {
        handler.hook(ProcessProfilingInfo_);
        handler.hook(ProcessProfilingInfoWithBool_);
        handler.hook(execve_);
    }
};
}  // namespace art

#endif  // LSPATCH_PROFILE_SAVER_H
