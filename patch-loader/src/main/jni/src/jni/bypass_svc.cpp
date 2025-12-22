#include "bypass_svc.h"
#include "logging.h"
#include "native_util.h"
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/prctl.h>
#include <signal.h>
#include <ucontext.h>
#include <pthread.h>
#include <cstddef>
#include <cstring>
#include <cstdlib>

#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <queue>
#include <mutex>
#include <condition_variable>

namespace lspd {

    // --- 共用結構與變數 ---
    struct SyscallRequest {
        long sys_no;
        long args[6];
        long result;
        bool completed;
        std::mutex mtx;
        std::condition_variable cv;
    };

    static pthread_t g_trusted_thread;
    static bool g_is_hook_active = false;
    static std::queue<SyscallRequest*> g_request_queue;
    static std::mutex g_queue_mtx;
    static std::condition_variable g_queue_cv;

    // --- 核心功能實作 (ARM64 Only) ---

    // 1. 影子線程循環
    static void* trusted_thread_loop(void* arg) {
        LOGD("SvcBypass: Trusted thread started (TID: %d)", gettid());
        while (true) {
            SyscallRequest* req = nullptr;
            {
                std::unique_lock<std::mutex> lock(g_queue_mtx);
                g_queue_cv.wait(lock, [] { return !g_request_queue.empty(); });
                req = g_request_queue.front();
                g_request_queue.pop();
            }

            if (req) {
                // 執行真正的 syscall
                req->result = syscall(req->sys_no,
                                      req->args[0], req->args[1], req->args[2],
                                      req->args[3], req->args[4], req->args[5]);

                {
                    std::lock_guard<std::mutex> lock(req->mtx);
                    req->completed = true;
                }
                req->cv.notify_one();
            }
        }
        return nullptr;
    }

    // 2. SIGSYS 信號處理器
    static void sigsys_handler(int signo, siginfo_t* info, void* context) {
        if (signo != SIGSYS) return;

        ucontext_t* ctx = (ucontext_t*)context;
        SyscallRequest req;

        // ARM64: 從 regs 讀取 (x8=sys_no, x0-x5=args)
        // 直接存取 ARM64 特有的 regs 結構
        req.sys_no = ctx->uc_mcontext.regs[8];
        for (int i = 0; i < 6; ++i) {
            req.args[i] = ctx->uc_mcontext.regs[i];
        }
        req.completed = false;

        LOGD("SvcBypass: Trapped syscall %ld (PID: %d)", req.sys_no, getpid());

        {
            std::lock_guard<std::mutex> lock(g_queue_mtx);
            g_request_queue.push(&req);
        }
        g_queue_cv.notify_one();

        {
            std::unique_lock<std::mutex> lock(req.mtx);
            req.cv.wait(lock, [&req] { return req.completed; });
        }

        // 將結果寫回 x0
        ctx->uc_mcontext.regs[0] = req.result;
    }

    // --- JNI 接口層 ---

    LSP_DEF_NATIVE_METHOD(jboolean, SvcBypass, initSvcHook) {
        // 如果已經激活，直接返回成功
        if (g_is_hook_active) return JNI_TRUE;

        int ret = pthread_create(&g_trusted_thread, nullptr, trusted_thread_loop, nullptr);
        if (ret != 0) {
            LOGE("SvcBypass: Failed to create trusted thread");
            return JNI_FALSE;
        }

        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sa.sa_sigaction = sigsys_handler;
        sa.sa_flags = SA_SIGINFO | SA_NODEFER;
        if (sigaction(SIGSYS, &sa, nullptr) < 0) {
            LOGE("SvcBypass: Failed to register SIGSYS handler");
            return JNI_FALSE;
        }

        g_is_hook_active = true;
        LOGI("SvcBypass: Initialized successfully (ARM64)");
        return JNI_TRUE;
    }

    LSP_DEF_NATIVE_METHOD(void, SvcBypass, enableSvcRedirect,
                          jstring path, jstring orig, jstring pkg) {
        if (!g_is_hook_active) {
            LOGW("SvcBypass: Hook not initialized.");
            return;
        }

        // ARM64 BPF 規則
        struct sock_filter filter[] = {
                BPF_STMT(BPF_LD + BPF_W + BPF_ABS, (offsetof(struct seccomp_data, nr))),
                BPF_JUMP(BPF_JMP + BPF_JEQ + BPF_K, __NR_openat, 0, 1),
                BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_TRAP),
                BPF_STMT(BPF_RET + BPF_K, SECCOMP_RET_ALLOW),
        };

        struct sock_fprog prog = {
                .len = (unsigned short)(sizeof(filter) / sizeof(filter[0])),
                .filter = filter,
        };

        if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)) {
            LOGE("SvcBypass: prctl(NO_NEW_PRIVS) failed");
            return;
        }

        if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog)) {
            LOGE("SvcBypass: prctl(SECCOMP) failed");
        } else {
            LOGI("SvcBypass: Seccomp filter applied (ARM64)");
        }
    }

    LSP_DEF_NATIVE_METHOD(void, SvcBypass, disableSvcRedirect) {
        LOGW("SvcBypass: Cannot disable Seccomp filters once applied.");
    }

    LSP_DEF_NATIVE_METHOD(jboolean, SvcBypass, isSvcHookActive) {
        return g_is_hook_active ? JNI_TRUE : JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jstring, SvcBypass, getDebugInfo) {
        return env->NewStringUTF("SvcBypass: Active (ARM64)");
    }

    LSP_DEF_NATIVE_METHOD(jint, SvcBypass, getCurrentPid) {
        return getpid();
    }

    LSP_DEF_NATIVE_METHOD(jint, SvcBypass, getInitialPid) {
        return getpid();
    }

    LSP_DEF_NATIVE_METHOD(void, SvcBypass, logSvcHookStats) {
    }

    LSP_DEF_NATIVE_METHOD(jboolean, SvcBypass, isChildProcess) {
        return JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jstring, SvcBypass, checkFd, jint fd) {
        char path[512];
        char link[64];
        snprintf(link, sizeof(link), "/proc/self/fd/%d", fd);
        ssize_t len = readlink(link, path, sizeof(path) - 1);
        if (len != -1) {
            path[len] = '\0';
            return env->NewStringUTF(path);
        }
        return nullptr;
    }

    LSP_DEF_NATIVE_METHOD(jint, SvcBypass, dupFd, jint fd) {
        return dup(fd);
    }

    LSP_DEF_NATIVE_METHOD(jlong, SvcBypass, getFdInode, jint fd) {
        struct stat st;
        if (fstat(fd, &st) == 0) return (jlong)st.st_ino;
        return -1;
    }

    LSP_DEF_NATIVE_METHOD(jboolean, SvcBypass, isSystemFile, jint fd) {
        return JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jint, SvcBypass, findSystemApkFd, jstring path) {
        return -1;
    }

    LSP_DEF_NATIVE_METHOD(jobjectArray, SvcBypass, getSystemApkFds) {
        return nullptr;
    }

    LSP_DEF_NATIVE_METHOD(void, SvcBypass, refreshSystemFds) {
    }

    LSP_DEF_NATIVE_METHOD(jbyteArray, SvcBypass, readCertificateFromFd, jint fd) {
        return nullptr;
    }

    LSP_DEF_NATIVE_METHOD(jbyteArray, SvcBypass, readCertificateFromPath, jstring path) {
        return nullptr;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(SvcBypass, initSvcHook, "()Z"),
            LSP_NATIVE_METHOD(SvcBypass, enableSvcRedirect, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
            LSP_NATIVE_METHOD(SvcBypass, disableSvcRedirect, "()V"),
            LSP_NATIVE_METHOD(SvcBypass, isSvcHookActive, "()Z"),
            LSP_NATIVE_METHOD(SvcBypass, logSvcHookStats, "()V"),
            LSP_NATIVE_METHOD(SvcBypass, getDebugInfo, "()Ljava/lang/String;"),
            LSP_NATIVE_METHOD(SvcBypass, getCurrentPid, "()I"),
            LSP_NATIVE_METHOD(SvcBypass, getInitialPid, "()I"),
            LSP_NATIVE_METHOD(SvcBypass, isChildProcess, "()Z"),
            LSP_NATIVE_METHOD(SvcBypass, checkFd, "(I)Ljava/lang/String;"),
            LSP_NATIVE_METHOD(SvcBypass, dupFd, "(I)I"),
            LSP_NATIVE_METHOD(SvcBypass, getFdInode, "(I)J"),
            LSP_NATIVE_METHOD(SvcBypass, isSystemFile, "(I)Z"),
            LSP_NATIVE_METHOD(SvcBypass, findSystemApkFd, "(Ljava/lang/String;)I"),
            LSP_NATIVE_METHOD(SvcBypass, getSystemApkFds, "()[[Ljava/lang/String;"),
            LSP_NATIVE_METHOD(SvcBypass, refreshSystemFds, "()V"),
            LSP_NATIVE_METHOD(SvcBypass, readCertificateFromFd, "(I)[B"),
            LSP_NATIVE_METHOD(SvcBypass, readCertificateFromPath, "(Ljava/lang/String;)[B"),
    };

    void RegisterSvcBypass(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(SvcBypass);
    }
}
