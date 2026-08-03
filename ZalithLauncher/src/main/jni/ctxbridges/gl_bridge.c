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
#include "gl_bridge.h"
#include "egl_loader.h"

//
// Created by maks on 17.09.2022.
//

static const char* g_LogTag = "GLBridge";
static __thread gl_render_window_t* currentBundle;
static EGLDisplay g_EglDisplay;

bool gl_init() {
    dlsym_EGL();
    g_EglDisplay = eglGetDisplay_p(EGL_DEFAULT_DISPLAY);

    if (g_EglDisplay == EGL_NO_DISPLAY)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "%s",
                            "eglGetDisplay_p(EGL_DEFAULT_DISPLAY) returned EGL_NO_DISPLAY");
        return false;
    }
    if (eglInitialize_p(g_EglDisplay, 0, 0) != EGL_TRUE)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglInitialize_p() failed: %04x",
                            eglGetError_p());
        return false;
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

gl_render_window_t* gl_init_context(gl_render_window_t *share) {
    gl_render_window_t* bundle = malloc(sizeof(gl_render_window_t));
    memset(bundle, 0, sizeof(gl_render_window_t));
    EGLint egl_attributes[] = { EGL_BLUE_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_RED_SIZE, 8,
                    EGL_ALPHA_SIZE, 8,
                    EGL_DEPTH_SIZE, 24,
                    EGL_SURFACE_TYPE,
                    EGL_WINDOW_BIT|EGL_PBUFFER_BIT,
                    EGL_RENDERABLE_TYPE,
                    EGL_OPENGL_ES2_BIT,
                    EGL_NONE
                    };
    EGLint num_configs = 0;

    if (eglChooseConfig_p(g_EglDisplay, egl_attributes, NULL, 0, &num_configs) != EGL_TRUE)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglChooseConfig_p() failed: %04x",
                            eglGetError_p());
        free(bundle);
        return NULL;
    }

    if (num_configs == 0)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "%s",
                            "eglChooseConfig_p() found no matching config");
        free(bundle);
        return NULL;
    }

    eglChooseConfig_p(g_EglDisplay, egl_attributes, &bundle->config, 1, &num_configs);
    eglGetConfigAttrib_p(g_EglDisplay, bundle->config, EGL_NATIVE_VISUAL_ID, &bundle->format);

    {
        EGLBoolean bindResult;

        if (!strncmp(getenv("POJAV_RENDERER"), "opengles3_desktopgl", 19))
        {
            printf("EGLBridge: Binding to OpenGL\n");
            bindResult = eglBindAPI_p(EGL_OPENGL_API);
        } else {
            printf("EGLBridge: Binding to OpenGL ES\n");
            bindResult = eglBindAPI_p(EGL_OPENGL_ES_API);
        }
        if (!bindResult) printf("EGLBridge: bind failed: %p\n", eglGetError_p());
    }

    int libgl_es = strtol(getenv("LIBGL_ES"), NULL, 0);
    if (libgl_es < 0 || libgl_es > INT16_MAX) libgl_es = 2;
    const EGLint egl_context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, libgl_es, EGL_NONE };
    bundle->context = eglCreateContext_p(g_EglDisplay, bundle->config, share == NULL ? EGL_NO_CONTEXT : share->context, egl_context_attributes);

    if (bundle->context == EGL_NO_CONTEXT)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglCreateContext_p() finished with error: %04x",
                            eglGetError_p());
        free(bundle);
        return NULL;
    }
    return bundle;
}

void gl_swap_surface(gl_render_window_t* bundle) {
    // ===== 情况1：有新 Surface 待切换（立即执行，无延迟）=====
    if (bundle->newNativeSurface != NULL) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to new native surface");

        // ① 释放旧的 EGL Surface
        if (bundle->surface != NULL) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = NULL;
        }

        // ② 释放旧的 ANativeWindow（减少引用计数）
        if (bundle->nativeSurface != NULL) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = NULL;
        }

        // ③ 切换到新窗口
        bundle->nativeSurface = bundle->newNativeSurface;
        bundle->newNativeSurface = NULL;                // 清空，避免重复处理
        ANativeWindow_acquire(bundle->nativeSurface);   // 增加引用计数
        ANativeWindow_setBuffersGeometry(bundle->nativeSurface, 0, 0, bundle->format);

        // ④ 创建新的 EGL Surface
        bundle->surface = eglCreateWindowSurface_p(g_EglDisplay, bundle->config,
                                                   bundle->nativeSurface, NULL);
        if (bundle->surface == EGL_NO_SURFACE) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                "eglCreateWindowSurface failed: %04x", eglGetError_p());
        }
        return;  // 已完成切换，直接返回
    }

    // ===== 情况2：无新 Surface，回退到 1×1 Pbuffer =====
    // 先尝试释放旧的资源（如果仍有效）
    if (bundle->nativeSurface != NULL) {
        // 检查窗口是否依然有效（Android 可能在后台已销毁，此时 getWidth 返回负值）
        int width = ANativeWindow_getWidth(bundle->nativeSurface);
        if (width > 0) {
            ANativeWindow_release(bundle->nativeSurface);
        } else {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                                "Native surface invalid (width=%d), skipping release", width);
        }
        bundle->nativeSurface = NULL;
    }

    if (bundle->surface != NULL) {
        eglDestroySurface_p(g_EglDisplay, bundle->surface);
        bundle->surface = NULL;
    }

    // 创建 Pbuffer 作为后备，保证渲染不中断
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "No new surface, switching to 1x1 pbuffer");
    const EGLint pbuffer_attrs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    bundle->surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config, pbuffer_attrs);
}

void gl_make_current(gl_render_window_t* bundle) {

    if (bundle == NULL)
    {
        if (eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT))
        {
            currentBundle = NULL;
        }
        return;
    }

    bool hasSetMainWindow = false;
    if (pojav_environ->mainWindowBundle == NULL)
    {
        pojav_environ->mainWindowBundle = (basic_render_window_t*)bundle;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Main window bundle is now %p", pojav_environ->mainWindowBundle);
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        hasSetMainWindow = true;
    }

    if (bundle->surface == NULL)
        gl_swap_surface(bundle);

    if (eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context))
    {
        currentBundle = bundle;
    } else {
        if (hasSetMainWindow)
        {
            pojav_environ->mainWindowBundle->newNativeSurface = NULL;
            gl_swap_surface((gl_render_window_t*)pojav_environ->mainWindowBundle);
            pojav_environ->mainWindowBundle = NULL;
        }
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglMakeCurrent returned with error: %04x", eglGetError_p());
    }

}


void gl_swap_buffers() {
    // 如果处于“新窗口等待”状态，立即完成切换（这里会调用修正后的 gl_swap_surface）
    if (currentBundle->state == STATE_RENDERER_NEW_WINDOW) {
        // 先解绑当前上下文，避免干扰
        eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        gl_swap_surface(currentBundle);                // 此时会立即切换并释放旧资源
        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                         currentBundle->context);
        // 标记状态为存活，避免反复进入
        if (currentBundle->nativeSurface != NULL) {
            currentBundle->state = STATE_RENDERER_ALIVE;
        }
    }

    // 正常交换缓冲
    if (currentBundle->surface != NULL) {
        if (!eglSwapBuffers_p(g_EglDisplay, currentBundle->surface) &&
            eglGetError_p() == EGL_BAD_SURFACE) {
            // Surface 已失效，触发重新切换（进入上面的分支）
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            // 清空 newNativeSurface 以避免误切换（实际由 gl_setup_window 设置）
            currentBundle->newNativeSurface = NULL;
            gl_swap_surface(currentBundle);
            eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface,
                             currentBundle->context);
            if (currentBundle->nativeSurface != NULL) {
                currentBundle->state = STATE_RENDERER_ALIVE;
            }
            __android_log_print(ANDROID_LOG_INFO, g_LogTag,
                                "Surface died, recreated");
        }
    }
}

void gl_setup_window() {
    if (pojav_environ->mainWindowBundle != NULL)
    {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Main window bundle is not NULL, changing state");
        pojav_environ->mainWindowBundle->state = STATE_RENDERER_NEW_WINDOW;
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
    }
}

void gl_swap_interval(int swapInterval) {
    const char *renderer = getenv("POJAV_RENDERER");
    if (renderer && !strcmp(renderer, "opengles3_desktopgl_zink_kopper") && !getenv("POJAV_VSYNC_IN_ZINK")) {
        return;
    }
    eglSwapInterval_p(g_EglDisplay, swapInterval);
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
