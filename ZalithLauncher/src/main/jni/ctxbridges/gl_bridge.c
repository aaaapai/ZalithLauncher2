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
static constexpr int MAX_RETRIES = 20;
static constexpr struct timespec WAIT_INTERVAL = {0, 10000000L}; // 10ms
static_assert(sizeof(gl_render_window_t) <= 256,
              "gl_render_window_t size unexpectedly large");

static __thread gl_render_window_t* currentBundle = nullptr;
static EGLDisplay g_EglDisplay = EGL_NO_DISPLAY;
static int g_userSwapInterval = 0;
static void (*g_ANativeWindow_setSwapInterval)(ANativeWindow* window, int interval) = nullptr;


bool gl_init() {
    dlsym_EGL();
    g_EglDisplay = eglGetDisplay_p(EGL_DEFAULT_DISPLAY);

    if (g_EglDisplay == EGL_NO_DISPLAY) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglGetDisplay_p(EGL_DEFAULT_DISPLAY) returned EGL_NO_DISPLAY");
        return false;
    }
    if (eglInitialize_p(g_EglDisplay, nullptr, nullptr) != EGL_TRUE) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglInitialize_p() failed: %04x", eglGetError_p());
        return false;
    }

    g_ANativeWindow_setSwapInterval = (void (*)(ANativeWindow*, int))dlsym(RTLD_DEFAULT, "ANativeWindow_setSwapInterval");
    if (g_ANativeWindow_setSwapInterval == NULL) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                            "ANativeWindow_setSwapInterval not found, EGL only fallback");
    } else {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "ANativeWindow_setSwapInterval loaded successfully");
    }
    return true;
}

gl_render_window_t* gl_get_current() {
    return currentBundle;
}

static void gl4esi_get_display_dimensions(int* width, int* height) {
    if (currentBundle == NULL) goto zero;
    EGLSurface surface = currentBundle->surface;
    EGLBoolean result_width = eglQuerySurface_p(g_EglDisplay, surface, EGL_WIDTH, width);
    EGLBoolean result_height = eglQuerySurface_p(g_EglDisplay, surface, EGL_HEIGHT, height);
    if (!result_width || !result_height) goto zero;
    return;
zero:
    *width = 0;
    *height = 0;
}

static bool gl_rebuild_context(gl_render_window_t* bundle) {
    if (bundle == nullptr) return false;
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
    if (new_ctx == EGL_NO_CONTEXT) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "Failed to rebuild context: 0x%04x", eglGetError_p());
        return false;
    }
    bundle->context = new_ctx;
    bundle->context_lost = false;
    __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                        "Context rebuilt successfully (new=%p)", new_ctx);
    return true;
}

gl_render_window_t* gl_init_context(gl_render_window_t* share) {
    gl_render_window_t* bundle = (gl_render_window_t*)calloc(1, sizeof(gl_render_window_t));
    if (bundle == nullptr) return nullptr;

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

    if (eglChooseConfig_p(g_EglDisplay, egl_attributes, nullptr, 0, &num_configs) != EGL_TRUE) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglChooseConfig_p() failed: %04x", eglGetError_p());
        free(bundle);
        return nullptr;
    }

    if (num_configs == 0) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglChooseConfig_p() found no matching config");
        free(bundle);
        return nullptr;
    }

    eglChooseConfig_p(g_EglDisplay, egl_attributes, &bundle->config, 1, &num_configs);
    eglGetConfigAttrib_p(g_EglDisplay, bundle->config, EGL_NATIVE_VISUAL_ID, &bundle->format);

    const char* renderer = getenv("POJAV_RENDERER");
    EGLBoolean bindResult;
    if (renderer && !strncmp(renderer, "opengles3_desktopgl", 19)) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL");
        bindResult = eglBindAPI_p(EGL_OPENGL_API);
    } else {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL ES");
        bindResult = eglBindAPI_p(EGL_OPENGL_ES_API);
    }
    if (!bindResult) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglBindAPI failed: %04x", eglGetError_p());
    }

    int libgl_es = (int)strtol(getenv("LIBGL_ES"), nullptr, 0);
    if (libgl_es < 0 || libgl_es > INT16_MAX) libgl_es = 2;
    bundle->client_version = libgl_es;
    bundle->share_context = (share == nullptr) ? EGL_NO_CONTEXT : share->context;

    const EGLint egl_context_attributes[] = {
        EGL_CONTEXT_CLIENT_VERSION, libgl_es,
        EGL_NONE
    };
    bundle->context = eglCreateContext_p(g_EglDisplay, bundle->config,
                                         bundle->share_context,
                                         egl_context_attributes);

    if (bundle->context == EGL_NO_CONTEXT) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglCreateContext_p() finished with error: %04x", eglGetError_p());
        free(bundle);
        return nullptr;
    }
    bundle->context_lost = false;
    return bundle;
}

// 尝试创建窗口表面：先带属性，失败则清错并重试不带属性
static EGLSurface try_create_window_surface(EGLDisplay display, EGLConfig config,
                                            ANativeWindow* window, EGLint format,
                                            int width, int height) {
    // 1. 带尺寸属性尝试（MobileGL 需要）
    EGLint surface_attribs[] = {
        EGL_WIDTH, width,
        EGL_HEIGHT, height,
        EGL_NONE
    };
    EGLSurface surface = eglCreateWindowSurface_p(display, config, window, surface_attribs);
    if (surface != EGL_NO_SURFACE) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Created window surface with attributes (w=%d,h=%d)", width, height);
        return surface;
    }
    // 带属性失败，清除错误并尝试不带属性（兼容 MobileGLUES 等标准实现）
    EGLint err = eglGetError_p();
    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                        "eglCreateWindowSurface with attributes failed (0x%04x), retry without attributes", err);
    while (eglGetError_p() != EGL_SUCCESS) {} // 清空错误队列

    surface = eglCreateWindowSurface_p(display, config, window, nullptr);
    if (surface != EGL_NO_SURFACE) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Created window surface without attributes");
        return surface;
    }
    err = eglGetError_p();
    __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                        "eglCreateWindowSurface without attributes also failed: 0x%04x", err);
    return EGL_NO_SURFACE;
}

void gl_swap_surface(gl_render_window_t* bundle) {
    if (bundle->newNativeSurface != nullptr) {
        int w = ANativeWindow_getWidth(bundle->newNativeSurface);
        int h = ANativeWindow_getHeight(bundle->newNativeSurface);
        if (w <= 0 || h <= 0) {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "New surface invalid size %dx%d, discarding", w, h);
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            return;
        }

        uint64_t now = get_time_ms();
        if (bundle->last_fail_time > 0 && (now - bundle->last_fail_time) < 200) {
            __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                                "Too soon after previous fail, skip new surface");
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            return;
        }

        // 重要：使用 0,0 让窗口保持原生尺寸，兼容两种后端
        ANativeWindow_setBuffersGeometry(bundle->newNativeSurface, 0, 0, bundle->format);

        EGLSurface new_surface = try_create_window_surface(
            g_EglDisplay, bundle->config, bundle->newNativeSurface,
            bundle->format, w, h);

        if (new_surface == EGL_NO_SURFACE) {
            bundle->last_fail_time = now;
            ANativeWindow_release(bundle->newNativeSurface);
            bundle->newNativeSurface = nullptr;
            return;
        }

        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to new surface (w=%d, h=%d)", w, h);

        EGLContext current_ctx = eglGetCurrentContext_p();
        EGLSurface current_draw = eglGetCurrentSurface_p(EGL_DRAW);
        if (current_draw != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }

        if (bundle->surface != EGL_NO_SURFACE && bundle->surface != bundle->pbuffer_surface) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
        }
        if (bundle->nativeSurface != nullptr) {
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

        bundle->last_fail_time = 0;
        return;
    }

    // 无新窗口，若表面为空则 fallback 到 PBuffer
    if (bundle->surface == EGL_NO_SURFACE) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag, "Surface is NULL, fallback to pbuffer");
        goto fallback_pbuffer;
    }
    return;

fallback_pbuffer:
    {
        if (eglGetCurrentSurface_p(EGL_DRAW) != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }

        if (!bundle->pbuffer_created) {
            static const EGLint pbuffer_attrs[] = {
                EGL_WIDTH, 1280,
                EGL_HEIGHT, 720,
                EGL_NONE
            };
            bundle->pbuffer_surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config,
                                                                pbuffer_attrs);
            bundle->pbuffer_created = true;
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Created pbuffer surface once");
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
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switched to pbuffer surface");
    }
}

void gl_make_current(gl_render_window_t* bundle) {
    if (bundle == nullptr) {
        if (eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
            currentBundle = nullptr;
        }
        return;
    }

    bool hasSetMainWindow = false;
    if (pojav_environ->mainWindowBundle == nullptr) {
        pojav_environ->mainWindowBundle = (basic_render_window_t*)bundle;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Main window bundle is now %p", pojav_environ->mainWindowBundle);
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        hasSetMainWindow = true;
    }

    if (bundle->surface == EGL_NO_SURFACE) {
        gl_swap_surface(bundle);
    }

    if (eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context)) {
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
    if (currentBundle->state == STATE_RENDERER_NEW_WINDOW) {
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

    if (currentBundle->surface == EGL_NO_SURFACE) {
        eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        gl_swap_surface(currentBundle);
        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface, currentBundle->context);
        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        if (currentBundle->surface == EGL_NO_SURFACE) {
            return;
        }
    }

    // 直接交换，不依赖尺寸查询
    if (currentBundle->surface != EGL_NO_SURFACE) {
        if (!eglSwapBuffers_p(g_EglDisplay, currentBundle->surface)) {
            EGLint err = eglGetError_p();
            if (err == EGL_BAD_SURFACE || err == EGL_BAD_CURRENT_SURFACE) {
                static uint64_t last_rebuild_time = 0;
                uint64_t now = get_time_ms();
                if (now - last_rebuild_time > 200) {
                    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                        "SwapBuffers failed (0x%04x), rebuilding surface", err);
                    eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                    if (currentBundle->surface != EGL_NO_SURFACE && currentBundle->surface != currentBundle->pbuffer_surface) {
                        eglDestroySurface_p(g_EglDisplay, currentBundle->surface);
                        currentBundle->surface = EGL_NO_SURFACE;
                    }
                    if (currentBundle->nativeSurface != nullptr) {
                        currentBundle->newNativeSurface = currentBundle->nativeSurface;
                        ANativeWindow_acquire(currentBundle->newNativeSurface);
                        gl_swap_surface(currentBundle);
                    } else {
                        gl_swap_surface(currentBundle);
                    }
                    if (currentBundle->surface != EGL_NO_SURFACE) {
                        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                                         currentBundle->context);
                        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
                    }
                    last_rebuild_time = now;
                }
            } else if (err == EGL_CONTEXT_LOST) {
                static uint64_t last_context_rebuild = 0;
                uint64_t now = get_time_ms();
                if (now - last_context_rebuild > 1000) {
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
                    last_context_rebuild = now;
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
    if (pojav_environ->mainWindowBundle != nullptr) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "Main window bundle is not NULL, changing state");
        pojav_environ->mainWindowBundle->state = STATE_RENDERER_NEW_WINDOW;
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
    }
}

void gl_swap_interval(int swapInterval) {
    g_userSwapInterval = swapInterval;
    const char* renderer = getenv("POJAV_RENDERER");
    if (renderer && !strcmp(renderer, "opengles3_desktopgl_zink_kopper") &&
        !getenv("POJAV_VSYNC_IN_ZINK")) {
        return;
    }

    eglSwapInterval_p(g_EglDisplay, swapInterval);

    gl_render_window_t* bundle = gl_get_current();
    if (bundle && bundle->nativeSurface && g_ANativeWindow_setSwapInterval) {
        g_ANativeWindow_setSwapInterval(bundle->nativeSurface, swapInterval);
        __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                            "ANativeWindow_setSwapInterval(%d) called", swapInterval);
    }
}

JNIEXPORT void JNICALL
Java_org_lwjgl_opengl_PojavRendererInit_nativeInitGl4esInternals(JNIEnv *env, jclass clazz,
                                                            jobject function_provider) {
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "GL4ES internals initializing...");
    jclass funcProviderClass = (*env)->GetObjectClass(env, function_provider);
    jmethodID method_getFunctionAddress = (*env)->GetMethodID(env, funcProviderClass, "getFunctionAddress", "(Ljava/lang/CharSequence;)J");
#define GETSYM(N) ((*env)->CallLongMethod(env, function_provider, method_getFunctionAddress, (*env)->NewStringUTF(env, N)));

    void (*set_getmainfbsize)(void (*new_getMainFBSize)(int* width, int* height)) = (void*)GETSYM("set_getmainfbsize");
    if(set_getmainfbsize != NULL) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "GL4ES internals initialized dimension callback");
        set_getmainfbsize(gl4esi_get_display_dimensions);
    }
#undef GETSYM
}
