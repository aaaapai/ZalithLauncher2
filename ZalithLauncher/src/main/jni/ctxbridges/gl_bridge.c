#include <EGL/egl.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>
#include <malloc.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <stdbool.h>
#include <environ/environ.h>
#include <unistd.h>
#include <time.h>
#include "../utils.h"
#include "gl_bridge.h"
#include "egl_loader.h"

static constexpr char g_LogTag[] = "GLBridge";
static_assert(sizeof(gl_render_window_t) <= 256,
              "gl_render_window_t size unexpectedly large");

static __thread gl_render_window_t* currentBundle = nullptr;
static EGLDisplay g_EglDisplay = EGL_NO_DISPLAY;
static int g_userSwapInterval = 0;
static void (*g_ANativeWindow_setSwapInterval)(ANativeWindow* window, int interval) = nullptr;

// ---------- 环境变量缓存 ----------
static bool g_env_checked = false;
static bool g_use_opengl = false;
static bool g_skip_vsync_in_zink = false;

static void check_env_once(void) {
    if (g_env_checked) return;
    const char* renderer = getenv("POJAV_RENDERER");
    g_use_opengl = (renderer && !strncmp(renderer, "opengles3_desktopgl", 19));
    const char* vsync = getenv("POJAV_VSYNC_IN_ZINK");
    g_skip_vsync_in_zink = (vsync && !strcmp(vsync, "1"));
    g_env_checked = true;
}

// ---------- 尺寸缓存 ----------
static int cached_width = 0, cached_height = 0;
static bool cache_valid = false;

// ---------- 错误恢复冷却计时器 ----------
static uint64_t last_surface_rebuild_time = 0;
static uint64_t last_context_rebuild_time = 0;
static const uint64_t SURFACE_REBUILD_COOLDOWN_MS = 1000;  // 1 秒
static const uint64_t CONTEXT_REBUILD_COOLDOWN_MS = 5000; // 5 秒

#if defined(__GNUC__) || defined(__clang__)
#define LIKELY(x)   __builtin_expect(!!(x), 1)
#define UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
#define LIKELY(x)   (x)
#define UNLIKELY(x) (x)
#endif

bool gl_init() {
    static bool initialized = false;
    if (initialized) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "EGL already initialized, skipping");
        return true;
    }

    dlsym_EGL();

    if (UNLIKELY(eglGetDisplay_p == nullptr || eglInitialize_p == nullptr ||
                 eglTerminate_p == nullptr)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "Critical EGL function pointers missing");
        return false;
    }

    for (int retry = 0; retry < 3; ++retry) {
        g_EglDisplay = eglGetDisplay_p(EGL_DEFAULT_DISPLAY);
        if (g_EglDisplay == EGL_NO_DISPLAY) {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "eglGetDisplay failed (attempt %d)", retry + 1);
            usleep(100000);
            continue;
        }

        if (eglInitialize_p(g_EglDisplay, nullptr, nullptr) == EGL_TRUE) {
            break;
        }

        EGLint err = eglGetError_p();
        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                            "eglInitialize failed: 0x%04x (attempt %d)", err, retry + 1);
        eglTerminate_p(g_EglDisplay);
        g_EglDisplay = EGL_NO_DISPLAY;
        usleep(100000);
    }

    if (UNLIKELY(g_EglDisplay == EGL_NO_DISPLAY)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "EGL initialization failed after 3 attempts");
        return false;
    }

    g_ANativeWindow_setSwapInterval = (void (*)(ANativeWindow*, int))dlsym(RTLD_DEFAULT, "ANativeWindow_setSwapInterval");
    if (g_ANativeWindow_setSwapInterval == nullptr) {
        void* nativewindow = dlopen("libnativewindow.so", RTLD_NOLOAD);
        if (!nativewindow) nativewindow = dlopen("libnativewindow.so", RTLD_LAZY);
        if (nativewindow) {
            g_ANativeWindow_setSwapInterval = (void (*)(ANativeWindow*, int))dlsym(nativewindow, "ANativeWindow_setSwapInterval");
        }
    }
    if (g_ANativeWindow_setSwapInterval == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                            "ANativeWindow_setSwapInterval not found, EGL only fallback");
    } else {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "ANativeWindow_setSwapInterval loaded");
    }

    check_env_once();

    initialized = true;
    return true;
}

gl_render_window_t* gl_get_current() {
    return currentBundle;
}

static void gl4esi_get_display_dimensions(int* width, int* height) {
    if (cache_valid) {
        *width = cached_width;
        *height = cached_height;
        return;
    }
    if (UNLIKELY(currentBundle == NULL)) goto zero;
    EGLSurface surface = currentBundle->surface;
    EGLBoolean result_width = eglQuerySurface_p(g_EglDisplay, surface, EGL_WIDTH, &cached_width);
    EGLBoolean result_height = eglQuerySurface_p(g_EglDisplay, surface, EGL_HEIGHT, &cached_height);
    if (LIKELY(result_width && result_height)) {
        cache_valid = true;
        *width = cached_width;
        *height = cached_height;
        return;
    }
zero:
    *width = 0;
    *height = 0;
    cache_valid = false;
}

static bool gl_rebuild_context(gl_render_window_t* bundle) {
    if (UNLIKELY(bundle == nullptr)) return false;
    if (bundle->context != EGL_NO_CONTEXT) {
        eglDestroyContext_p(g_EglDisplay, bundle->context);
        bundle->context = EGL_NO_CONTEXT;
    }
    const EGLint egl_context_attributes[] = {
        EGL_CONTEXT_CLIENT_VERSION, bundle->client_version,
        EGL_NONE
    };
    EGLContext new_ctx = eglCreateContext_p(g_EglDisplay, bundle->config,
                                            bundle->share_context,
                                            egl_context_attributes);
    if (UNLIKELY(new_ctx == EGL_NO_CONTEXT)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "Failed to rebuild context: 0x%04x", eglGetError_p());
        return false;
    }
    bundle->context = new_ctx;
    bundle->context_lost = false;
    __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                        "Context rebuilt successfully");
    return true;
}

gl_render_window_t* gl_init_context(gl_render_window_t* share) {
    gl_render_window_t* bundle = (gl_render_window_t*)calloc(1, sizeof(gl_render_window_t));
    if (UNLIKELY(bundle == nullptr)) return nullptr;

    EGLint egl_attributes[] = {
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_NONE
    };
    EGLint num_configs = 0;

    if (UNLIKELY(eglChooseConfig_p(g_EglDisplay, egl_attributes, nullptr, 0, &num_configs) != EGL_TRUE)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglChooseConfig_p() failed: %04x", eglGetError_p());
        free(bundle);
        return nullptr;
    }

    if (UNLIKELY(num_configs == 0)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglChooseConfig_p() found no matching config");
        free(bundle);
        return nullptr;
    }

    eglChooseConfig_p(g_EglDisplay, egl_attributes, &bundle->config, 1, &num_configs);
    eglGetConfigAttrib_p(g_EglDisplay, bundle->config, EGL_NATIVE_VISUAL_ID, &bundle->format);

    check_env_once();
    EGLBoolean bindResult;
    if (g_use_opengl) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL");
        bindResult = eglBindAPI_p(EGL_OPENGL_API);
    } else {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL ES");
        bindResult = eglBindAPI_p(EGL_OPENGL_ES_API);
    }
    if (UNLIKELY(!bindResult)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglBindAPI failed: %04x", eglGetError_p());
    }

    int libgl_es = (int)strtol(getenv("LIBGL_ES"), nullptr, 0);
    if (UNLIKELY(libgl_es < 0 || libgl_es > INT16_MAX)) libgl_es = 2;
    bundle->client_version = libgl_es;
    bundle->share_context = (share == nullptr) ? EGL_NO_CONTEXT : share->context;

    const EGLint egl_context_attributes[] = {
        EGL_CONTEXT_CLIENT_VERSION, libgl_es,
        EGL_NONE
    };
    bundle->context = eglCreateContext_p(g_EglDisplay, bundle->config,
                                         bundle->share_context,
                                         egl_context_attributes);

    if (UNLIKELY(bundle->context == EGL_NO_CONTEXT)) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglCreateContext_p() finished with error: %04x", eglGetError_p());
        free(bundle);
        return nullptr;
    }
    bundle->context_lost = false;
    return bundle;
}

// 创建窗口表面（重试3次，间隔10ms）
static EGLSurface try_create_window_surface(EGLDisplay display, EGLConfig config,
                                            ANativeWindow* window, EGLint format,
                                            int width, int height) {
    ANativeWindow_setBuffersGeometry(window, width, height, format);
    EGLint surface_attribs[] = {
        EGL_WIDTH, width,
        EGL_HEIGHT, height,
        EGL_NONE
    };
    EGLSurface surface = eglCreateWindowSurface_p(display, config, window, surface_attribs);
    if (LIKELY(surface != EGL_NO_SURFACE)) {
        __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                            "Created window surface with attributes (w=%d,h=%d)", width, height);
        return surface;
    }
    EGLint err = eglGetError_p();
    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                        "Strategy1 failed (0x%04x), trying strategy2", err);
    while (eglGetError_p() != EGL_SUCCESS) {}

    ANativeWindow_setBuffersGeometry(window, 0, 0, format);
    surface = eglCreateWindowSurface_p(display, config, window, nullptr);
    if (LIKELY(surface != EGL_NO_SURFACE)) {
        __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                            "Created window surface without attributes");
        return surface;
    }
    err = eglGetError_p();
    __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                        "Both strategies failed, last error: 0x%04x", err);
    return EGL_NO_SURFACE;
}

// 仅重建 Surface（不切换窗口）
static bool try_recreate_surface(gl_render_window_t* bundle) {
    if (UNLIKELY(bundle == nullptr || bundle->nativeSurface == nullptr))
        return false;

    ANativeWindow* win = bundle->nativeSurface;
    int w = ANativeWindow_getWidth(win);
    int h = ANativeWindow_getHeight(win);
    if (UNLIKELY(w <= 0 || h <= 0)) {
        return false;
    }

    EGLSurface new_surface = EGL_NO_SURFACE;
    for (int retry = 0; retry < 3; retry++) {
        new_surface = try_create_window_surface(g_EglDisplay, bundle->config,
                                                win, bundle->format, w, h);
        if (new_surface != EGL_NO_SURFACE) break;
        usleep(10000); // 10ms
    }

    if (UNLIKELY(new_surface == EGL_NO_SURFACE)) {
        return false;
    }

    if (bundle->surface != EGL_NO_SURFACE && bundle->surface != bundle->pbuffer_surface) {
        eglDestroySurface_p(g_EglDisplay, bundle->surface);
    }
    bundle->surface = new_surface;
    cache_valid = false;
    return true;
}

void gl_swap_surface(gl_render_window_t* bundle) {
    if (LIKELY(bundle->newNativeSurface != nullptr)) {
        if (bundle->newNativeSurface == bundle->nativeSurface) {
            __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                                "New surface is same as current, skip");
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            return;
        }

        int w = ANativeWindow_getWidth(bundle->newNativeSurface);
        int h = ANativeWindow_getHeight(bundle->newNativeSurface);
        if (UNLIKELY(w <= 0 || h <= 0)) {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "New surface size invalid (%dx%d), discarding", w, h);
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            bundle->last_fail_time = get_time_ms() + 500;
            goto fallback_pbuffer;
        }

        uint64_t now = get_time_ms();
        if (UNLIKELY(bundle->last_fail_time > 0 && now < bundle->last_fail_time)) {
            __android_log_print(ANDROID_LOG_DEBUG, g_LogTag, "In cooldown, skip new surface");
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            return;
        }

        EGLSurface new_surface = EGL_NO_SURFACE;
        for (int retry = 0; retry < 3; retry++) {
            new_surface = try_create_window_surface(g_EglDisplay, bundle->config,
                                                    bundle->newNativeSurface,
                                                    bundle->format, w, h);
            if (new_surface != EGL_NO_SURFACE) break;
            usleep(10000);
        }

        if (UNLIKELY(new_surface == EGL_NO_SURFACE)) {
            bundle->last_fail_time = now + 500;
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            goto fallback_pbuffer;
        }

        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to new surface (w=%d,h=%d)", w, h);

        EGLContext current_ctx = eglGetCurrentContext_p();
        EGLSurface current_draw = eglGetCurrentSurface_p(EGL_DRAW);
        if (current_draw != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }

        if (bundle->surface != EGL_NO_SURFACE && bundle->surface != bundle->pbuffer_surface) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
        }
        if (bundle->nativeSurface != nullptr && bundle->nativeSurface != bundle->newNativeSurface) {
            ANativeWindow_release(bundle->nativeSurface);
        }

        bundle->surface = new_surface;
        bundle->nativeSurface = bundle->newNativeSurface;
        bundle->newNativeSurface = nullptr;
        ANativeWindow_acquire(bundle->nativeSurface);

        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        if (g_ANativeWindow_setSwapInterval) {
            g_ANativeWindow_setSwapInterval(bundle->nativeSurface, g_userSwapInterval);
        }

        if (current_ctx == bundle->context) {
            eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context);
        }

        cache_valid = false;
        bundle->last_fail_time = 0;
        return;
    }

    if (LIKELY(bundle->surface != EGL_NO_SURFACE)) {
        return;
    }

fallback_pbuffer:
    {
        if (eglGetCurrentSurface_p(EGL_DRAW) != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }

        if (!bundle->pbuffer_created) {
            static const EGLint pbuffer_attrs[] = {
                EGL_WIDTH, 1,
                EGL_HEIGHT, 1,
                EGL_NONE
            };
            bundle->pbuffer_surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config,
                                                                pbuffer_attrs);
            bundle->pbuffer_created = true;
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Created pbuffer surface (1280x720)");
        }

        if (bundle->surface != EGL_NO_SURFACE && bundle->surface != bundle->pbuffer_surface) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
        }
        if (bundle->nativeSurface != nullptr) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = nullptr;
        }

        bundle->surface = bundle->pbuffer_surface;
        if (eglGetCurrentContext_p() == bundle->context) {
            eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context);
            eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        }
        cache_valid = false;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switched to pbuffer surface");
    }
}

void gl_make_current(gl_render_window_t* bundle) {
    if (UNLIKELY(bundle == nullptr)) {
        if (eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
            currentBundle = nullptr;
        }
        return;
    }

    bool hasSetMainWindow = false;
    if (UNLIKELY(pojav_environ->mainWindowBundle == nullptr)) {
        pojav_environ->mainWindowBundle = (basic_render_window_t*)bundle;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Main window bundle is now %p", pojav_environ->mainWindowBundle);
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        hasSetMainWindow = true;
    }

    if (UNLIKELY(bundle->surface == EGL_NO_SURFACE)) {
        gl_swap_surface(bundle);
    }

    if (LIKELY(eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context))) {
        currentBundle = bundle;
    } else {
        if (hasSetMainWindow) {
            pojav_environ->mainWindowBundle->newNativeSurface = nullptr;
            gl_swap_surface((gl_render_window_t*)pojav_environ->mainWindowBundle);
            pojav_environ->mainWindowBundle = nullptr;
        }
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglMakeCurrent returned with error: %04x", eglGetError_p());
    }
}

void gl_swap_buffers() {
    if (UNLIKELY(currentBundle->state == STATE_RENDERER_NEW_WINDOW)) {
        EGLContext old_ctx = eglGetCurrentContext_p();
        EGLSurface old_draw = eglGetCurrentSurface_p(EGL_DRAW);
        if (old_draw != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }
        gl_swap_surface(currentBundle);
        if (old_ctx == currentBundle->context) {
            eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                             currentBundle->context);
        }
        currentBundle->state = STATE_RENDERER_ALIVE;
    }

    if (UNLIKELY(currentBundle->surface == EGL_NO_SURFACE)) {
        eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        gl_swap_surface(currentBundle);
        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface, currentBundle->context);
        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        if (currentBundle->surface == EGL_NO_SURFACE) {
            return;
        }
    }

    if (LIKELY(currentBundle->surface != EGL_NO_SURFACE)) {
        if (UNLIKELY(!eglSwapBuffers_p(g_EglDisplay, currentBundle->surface))) {
            EGLint err = eglGetError_p();
            uint64_t now = get_time_ms();

            if (err == EGL_BAD_SURFACE || err == EGL_BAD_CURRENT_SURFACE) {
                if (now - last_surface_rebuild_time > SURFACE_REBUILD_COOLDOWN_MS) {
                    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                        "SwapBuffers error 0x%04x, trying surface recreate", err);
                    if (eglMakeCurrent_p(g_EglDisplay, currentBundle->surface,
                                         currentBundle->surface, currentBundle->context)) {
                        if (eglSwapBuffers_p(g_EglDisplay, currentBundle->surface)) {
                            return;
                        }
                    }
                    eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                    if (try_recreate_surface(currentBundle)) {
                        if (eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                                             currentBundle->context)) {
                            eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
                            __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                                "Surface recreated successfully");
                            last_surface_rebuild_time = now;
                            return;
                        } else {
                            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                                "Failed to make new surface current");
                        }
                    } else {
                        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                            "Surface recreate failed, falling back to pbuffer");
                        gl_swap_surface(currentBundle);
                        if (currentBundle->surface != EGL_NO_SURFACE) {
                            eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                                             currentBundle->context);
                            eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
                        }
                    }
                    last_surface_rebuild_time = now;
                } else {
                    __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                                        "Surface rebuild cooldown active, skipping rebuild");
                }
            } else if (UNLIKELY(err == EGL_CONTEXT_LOST)) {
                if (now - last_context_rebuild_time > CONTEXT_REBUILD_COOLDOWN_MS) {
                    __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                        "Context lost (0x%04x), rebuilding entire context", err);
                    eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

                    if (currentBundle->surface != EGL_NO_SURFACE) {
                        eglDestroySurface_p(g_EglDisplay, currentBundle->surface);
                        currentBundle->surface = EGL_NO_SURFACE;
                    }
                    if (currentBundle->pbuffer_surface != EGL_NO_SURFACE) {
                        eglDestroySurface_p(g_EglDisplay, currentBundle->pbuffer_surface);
                        currentBundle->pbuffer_surface = EGL_NO_SURFACE;
                        currentBundle->pbuffer_created = false;
                    }
                    if (currentBundle->nativeSurface != nullptr) {
                        ANativeWindow_release(currentBundle->nativeSurface);
                        currentBundle->nativeSurface = nullptr;
                    }

                    if (gl_rebuild_context(currentBundle)) {
                        currentBundle->newNativeSurface = pojav_environ->pojavWindow;
                        if (currentBundle->newNativeSurface != nullptr) {
                            ANativeWindow_acquire(currentBundle->newNativeSurface);
                        }
                        gl_swap_surface(currentBundle);
                        if (eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                                             currentBundle->context)) {
                            eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
                            __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                                "Context and surface rebuilt successfully");
                        } else {
                            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                                "Failed to make new context current");
                        }
                    } else {
                        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                            "Failed to rebuild context, giving up");
                    }
                    last_context_rebuild_time = now;
                } else {
                    __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                                        "Context rebuild cooldown active, skipping");
                }
            } else {
                __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                    "eglSwapBuffers failed: 0x%04x (ignored)", err);
            }
        }
    } else {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag, "Surface is NULL, skipping swap");
    }
}

void gl_setup_window() {
    if (UNLIKELY(pojav_environ->mainWindowBundle != nullptr)) {
        ANativeWindow* win = pojav_environ->pojavWindow;
        if (win != nullptr) {
            int w = ANativeWindow_getWidth(win);
            int h = ANativeWindow_getHeight(win);
            if (UNLIKELY(w <= 0 || h <= 0)) {
                __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                    "gl_setup_window: pojavWindow invalid (%dx%d), skip", w, h);
                return;
            }
        } else {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "gl_setup_window: pojavWindow is NULL, skip");
            return;
        }

        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Main window bundle is not NULL, changing state");
        pojav_environ->mainWindowBundle->state = STATE_RENDERER_NEW_WINDOW;
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        cache_valid = false;
    }
}

void gl_swap_interval(int swapInterval) {
    g_userSwapInterval = swapInterval;

    check_env_once();
    if (g_use_opengl && !strcmp(getenv("POJAV_RENDERER"), "opengles3_desktopgl_zink_kopper") &&
        g_skip_vsync_in_zink) {
        return;
    }

    EGLContext current_ctx = eglGetCurrentContext_p();
    if (UNLIKELY(current_ctx == EGL_NO_CONTEXT)) {
        __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                            "gl_swap_interval: no current context, skip");
        return;
    }

    EGLSurface current_draw = eglGetCurrentSurface_p(EGL_DRAW);
    if (UNLIKELY(current_draw == EGL_NO_SURFACE)) {
        __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                            "gl_swap_interval: no current surface, skip");
        return;
    }

    if (LIKELY(eglSwapInterval_p(g_EglDisplay, swapInterval))) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "eglSwapInterval(%d) succeeded", swapInterval);
        gl_render_window_t* bundle = gl_get_current();
        if (bundle && bundle->nativeSurface && g_ANativeWindow_setSwapInterval) {
            g_ANativeWindow_setSwapInterval(bundle->nativeSurface, swapInterval);
            __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                "ANativeWindow_setSwapInterval(%d) called", swapInterval);
        }
        return;
    }

    EGLint err = eglGetError_p();
    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                        "eglSwapInterval(%d) failed: 0x%04x (ignored)", swapInterval, err);
}

JNIEXPORT void JNICALL
Java_org_lwjgl_opengl_PojavRendererInit_nativeInitGl4esInternals(JNIEnv *env, jclass clazz,
                                                            jobject function_provider) {
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "GL4ES internals initializing...");
    jclass funcProviderClass = (*env)->GetObjectClass(env, function_provider);
    jmethodID method_getFunctionAddress = (*env)->GetMethodID(env, funcProviderClass, "getFunctionAddress", "(Ljava/lang/CharSequence;)J");
#define GETSYM(N) ((*env)->CallLongMethod(env, function_provider, method_getFunctionAddress, (*env)->NewStringUTF(env, N)));

    void (*set_getmainfbsize)(void (*new_getMainFBSize)(int* width, int* height)) = (void*)GETSYM("set_getmainfbsize");
    if (LIKELY(set_getmainfbsize != NULL)) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "GL4ES internals initialized dimension callback");
        set_getmainfbsize(gl4esi_get_display_dimensions);
    }
#undef GETSYM
}
