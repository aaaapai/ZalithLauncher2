// SDL3 launcher integration hook（移植自 AAMC Amethyst-Android）
//
// 通过 bytehook 拦截 libSDL3.so 的 SDL_InitSubSystem：
// 游戏（如 MC 26.3）初始化 SDL 时，通知 launcher 的 Java 侧完成 SDL 集成
// （动态加载 libSDL3.so、初始化 SDL JNI、把 native surface 尺寸同步给 SDLSurface）。
// 此后 CallbackBridge 会把输入事件同时转发给 GLFW 与 SDL。

#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "environ/environ.h"
#include "utils.h"
#include "logger/logger.h"

#include <bytehook.h>
#include <dlfcn.h>
#include <jni.h>
#include <stdlib.h>

// --- 最小 SDL3 声明（仅 hook 所需；完整 headers 由 lwjgl-sdl 绑定侧提供） ---
typedef uint32_t SDL_InitFlags;
typedef struct SDL_Window SDL_Window;
typedef struct SDL_Rect { int x, y, w, h; } SDL_Rect;

bool SDL_InitSubSystem(SDL_InitFlags flags);
bool SDL_SetHint(const char *name, const char *value);
bool SDL_SetTextInputArea(SDL_Window *window, const SDL_Rect *rect, int cursor);
void SDL_SetError(const char *fmt, ...);
const char *SDL_GetError(void);

DECL_DLSYM(SDL_InitSubSystem)
DECL_DLSYM(SDL_SetHint);
DECL_DLSYM(SDL_SetTextInputArea);
DECL_DLSYM(SDL_SetError);
DECL_DLSYM(SDL_GetError);


static bool custom_SDL_InitSubSystem_Func(SDL_InitFlags flags) {
    // Call notifyLauncher on SDL_InitSubSystem, this sets up all the JNI stuff needed by SDL.
    TRY_ATTACH_ENV(dvm_env, pojav_environ->dalvikJavaVMPtr, "SDL_InitSubSystem failed!",
            SET_DLSYM_PTR(dlopen("libSDL3.so", RTLD_NOLOAD), SDL_SetError);
            if (SDL_SetError_p) SDL_SetError_p("Failed to load SDL launcher integration android-side. This is not an SDL bug, please contact the launcher developer.");
            return false;
            );

    // Just in case of bozo
    jint safeFlags;
    if (flags > INT32_MAX) {
        safeFlags = -1;
    } else safeFlags = (jint)flags;

    notifyLauncher(dvm_env, NOTIF_TYPE_SDL, (int[]){ACTION_INIT_LAUNCHER_INTEGRATION, safeFlags}, 2);

    // This is the normal for the launcher, the default in SDL is false.
    SET_DLSYM_PTR(dlopen("libSDL3.so", RTLD_NOLOAD), SDL_SetHint);
    if (SDL_SetHint_p) SDL_SetHint_p("SDL_RETURN_KEY_HIDES_IME", "true");
    // FIXME: MobileGlues has issues with passing in the proper EGL params to make this work
    const char *egl = getenv("POJAVEXEC_EGL");
    if (egl && strcmp(egl, "libmobileglues.so") == 0) {
        SDL_SetHint_p("SDL_OPENGL_FORCE_SRGB_FRAMEBUFFER", "0");
    }

    // Call original func after doing all the needed setup
    bool r = BYTEHOOK_CALL_PREV(custom_SDL_InitSubSystem_Func, SDL_InitSubSystem_t, flags);
    if (!r){
        SET_DLSYM_PTR(dlopen("libSDL3.so", RTLD_NOLOAD), SDL_GetError);
        LOG_TO_E("SDL_Hook", "SDL_InitSubsystem Error: %s", SDL_GetError_p());
    }
    BYTEHOOK_POP_STACK();
    return r;
}

void create_sdl_hooks(bytehook_stub_t (*bytehook_hook_all_p)(const char *callee_path_name, const char *sym_name, void *new_func,
                                                             bytehook_hooked_t hooked, void *hooked_arg)) {
    // Don't set callee_path_name to anything besides NULL or else it won't be able to find the symbol
    bytehook_stub_t stub_SDL_InitSubSystem = bytehook_hook_all_p(NULL, "SDL_InitSubSystem", &custom_SDL_InitSubSystem_Func, NULL, NULL);
    LOG_TO_I("SDL_Hook", "Successfully initialized SDL hook, stub: %p", stub_SDL_InitSubSystem);
}
