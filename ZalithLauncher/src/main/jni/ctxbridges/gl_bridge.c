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
#include "gl_bridge.h"
#include "egl_loader.h"

//
// Created by maks on 17.09.2022.
//

static constexpr char g_LogTag[] = "GLBridge";
static constexpr int MAX_RETRIES = 20;
static constexpr struct timespec WAIT_INTERVAL = {0, 10000000L}; // 10ms
static_assert(sizeof(gl_render_window_t) <= 256,
              "gl_render_window_t size unexpectedly large");

static __thread gl_render_window_t* currentBundle = nullptr;
static EGLDisplay g_EglDisplay = EGL_NO_DISPLAY;
static int g_userSwapInterval = 0;
// 在 static int g_userSwapInterval = 0; 之后添加
static void (*g_ANativeWindow_setSwapInterval)(ANativeWindow* window, int interval) = nullptr;

static bool wait_for_surface_ready(EGLDisplay display, EGLSurface surface) {
    int width = 0, height = 0;
    for (int i = 0; i < MAX_RETRIES; ++i) {
        eglQuerySurface_p(display, surface, EGL_WIDTH, &width);
        eglQuerySurface_p(display, surface, EGL_HEIGHT, &height);
        if (width > 0 && height > 0) {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                "New surface ready: %dx%d", width, height);
            return true;
        }
        nanosleep(&WAIT_INTERVAL, nullptr);
    }
    __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                        "Surface not ready after %d retries", MAX_RETRIES);
    return false;
}

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

gl_render_window_t* gl_init_context(gl_render_window_t* share) {
    gl_render_window_t* bundle = (gl_render_window_t*)malloc(sizeof(gl_render_window_t));
    if (bundle == nullptr) return nullptr;
    memset(bundle, 0, sizeof(gl_render_window_t));

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

    // 设置 API
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
    const EGLint egl_context_attributes[] = {
        EGL_CONTEXT_CLIENT_VERSION, libgl_es,
        EGL_NONE
    };
    bundle->context = eglCreateContext_p(g_EglDisplay, bundle->config,
                                         (share == nullptr) ? EGL_NO_CONTEXT : share->context,
                                         egl_context_attributes);

    if (bundle->context == EGL_NO_CONTEXT) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "eglCreateContext_p() finished with error: %04x", eglGetError_p());
        free(bundle);
        return nullptr;
    }
    return bundle;
}

void gl_swap_surface(gl_render_window_t* bundle) {
    if (bundle->newNativeSurface != nullptr) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to new native surface");

        // 保存当前上下文并解梆
        EGLContext current_ctx = eglGetCurrentContext_p();
        EGLSurface current_draw = eglGetCurrentSurface_p(EGL_DRAW);
        if (current_draw != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }

        if (bundle->surface != EGL_NO_SURFACE) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = EGL_NO_SURFACE;
        }
        if (bundle->nativeSurface != nullptr) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = nullptr;
        }

        bundle->nativeSurface = bundle->newNativeSurface;
        bundle->newNativeSurface = nullptr;
        ANativeWindow_acquire(bundle->nativeSurface);
        ANativeWindow_setBuffersGeometry(bundle->nativeSurface, 0, 0, bundle->format);

        bundle->surface = eglCreateWindowSurface_p(g_EglDisplay, bundle->config,
                                                   bundle->nativeSurface, nullptr);
        if (bundle->surface == EGL_NO_SURFACE) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                "eglCreateWindowSurface failed: %04x", eglGetError_p());
            goto fallback_pbuffer;
        }

        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        if (g_ANativeWindow_setSwapInterval) {
          g_ANativeWindow_setSwapInterval(bundle->nativeSurface, g_userSwapInterval);
          __android_log_print(ANDROID_LOG_DEBUG, g_LogTag,
                              "Applied swap interval %d to new surface", g_userSwapInterval);
        }

        if (current_ctx == bundle->context) {
            eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context);
        }

        wait_for_surface_ready(g_EglDisplay, bundle->surface);
        return;
    }

fallback_pbuffer:
    {
        EGLContext ctx_fb = eglGetCurrentContext_p();
        if (eglGetCurrentSurface_p(EGL_DRAW) != EGL_NO_SURFACE) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }
        if (bundle->nativeSurface != nullptr) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = nullptr;
        }
        if (bundle->surface != EGL_NO_SURFACE) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = EGL_NO_SURFACE;
        }

        __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                            "No new surface, switching to 1x1 pbuffer");
        static constexpr EGLint pbuffer_attrs[] = {
            EGL_WIDTH, 1280,
            EGL_HEIGHT, 720,
            EGL_NONE
        };
        bundle->surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config, pbuffer_attrs);
        if (ctx_fb == bundle->context) {
            eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context);
            eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        }
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

static uint64_t get_time_ms() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
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
        if (currentBundle->nativeSurface != nullptr) {
            currentBundle->state = STATE_RENDERER_ALIVE;
        }
    }

    if (currentBundle->surface == EGL_NO_SURFACE) {
        eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        gl_swap_surface(currentBundle);
        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface, currentBundle->context);
        if (currentBundle->nativeSurface != nullptr) {
            currentBundle->state = STATE_RENDERER_ALIVE;
        }
        eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
        if (currentBundle->surface == EGL_NO_SURFACE) {
            return;
        }
    }

    int w, h;
    eglQuerySurface_p(g_EglDisplay, currentBundle->surface, EGL_WIDTH, &w);
    eglQuerySurface_p(g_EglDisplay, currentBundle->surface, EGL_HEIGHT, &h);
    if (w <= 0 || h <= 0) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                            "Surface invalid size %dx%d, skip swap", w, h);
        return;
    }

    if (!eglSwapBuffers_p(g_EglDisplay, currentBundle->surface)) {
        EGLint err = eglGetError_p();
        if (err == EGL_BAD_SURFACE || err == EGL_BAD_CURRENT_SURFACE || err == EGL_CONTEXT_LOST) {
            static uint64_t last_rebuild_time = 0;
            uint64_t now = get_time_ms();
            if (now - last_rebuild_time > 200) {
                __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                    "Rebuilding surface due to error 0x%04x", err);
                eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                gl_swap_surface(currentBundle);
                eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                                 currentBundle->context);
                if (currentBundle->nativeSurface != nullptr) {
                    currentBundle->state = STATE_RENDERER_ALIVE;
                }
                eglSwapInterval_p(g_EglDisplay, g_userSwapInterval);
                last_rebuild_time = now;
            }
        } else {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "eglSwapBuffers failed: 0x%04x (ignored)", err);
        }
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
