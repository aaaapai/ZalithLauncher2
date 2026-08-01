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
#include <inttypes.h>
#include "gl_bridge.h"
#include "egl_loader.h"
#include "gl_loader.h"

//
// Created by maks on 17.09.2022.
//

static const char* g_LogTag = "GLBridge";
static __thread gl_render_window_t* currentBundle;
static EGLDisplay g_EglDisplay;
static EGLContext g_DummyContext = EGL_NO_CONTEXT;

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

    dlsym_GL();
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
    EGLint egl_attributes_desktopgl[] = { EGL_BLUE_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_RED_SIZE, 8,
                    EGL_ALPHA_SIZE, 8,
                    EGL_DEPTH_SIZE, 24,
                    EGL_SURFACE_TYPE,
                    EGL_WINDOW_BIT|EGL_PBUFFER_BIT,
                    EGL_RENDERABLE_TYPE,
                    EGL_OPENGL_ES3_BIT|EGL_OPENGL_ES2_BIT|EGL_OPENGL_ES_BIT,
                    EGL_NONE
                    };
    EGLint egl_attributes[] = { EGL_BLUE_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_RED_SIZE, 8,
                    EGL_ALPHA_SIZE, 8,
                    EGL_BUFFER_SIZE, 32,
                    EGL_DEPTH_SIZE, 24,
                    EGL_STENCIL_SIZE, 8,
                    EGL_SURFACE_TYPE,
                    EGL_WINDOW_BIT|EGL_PBUFFER_BIT,
                    EGL_RENDERABLE_TYPE,
                    EGL_OPENGL_ES3_BIT|EGL_OPENGL_ES2_BIT|EGL_OPENGL_ES_BIT,
                    EGL_NONE
                    };
    EGLint num_configs = 0;
    const char* renderer = getenv("POJAV_RENDERER");
    bool isDesktopGL = renderer && !strncmp(renderer, "opengles3_desktopgl", 19);

    if (isDesktopGL) {
        if (eglChooseConfig_p(g_EglDisplay, egl_attributes_desktopgl, NULL, 0, &num_configs) != EGL_TRUE)
        {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglChooseConfig_p() failed: %04x",
                                eglGetError_p());
            free(bundle);
            return NULL;
        }
    } else {
        if (eglChooseConfig_p(g_EglDisplay, egl_attributes, NULL, 0, &num_configs) != EGL_TRUE)
        {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglChooseConfig_p() failed: %04x",
                                eglGetError_p());
            free(bundle);
            return NULL;
        }
    }

    if (num_configs == 0)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "%s",
                            "eglChooseConfig_p() found no matching config");
        free(bundle);
        return NULL;
    }

    if (isDesktopGL) {
        eglChooseConfig_p(g_EglDisplay, egl_attributes_desktopgl, &bundle->config, 1, &num_configs);
    } else {
        eglChooseConfig_p(g_EglDisplay, egl_attributes, &bundle->config, 1, &num_configs);
    }
    eglGetConfigAttrib_p(g_EglDisplay, bundle->config, EGL_NATIVE_VISUAL_ID, &bundle->format);
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "EGL config chosen: %p, format: %d", bundle->config, bundle->format);

    {
        EGLBoolean bindResult;

        if (isDesktopGL)
        {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL");
            bindResult = eglBindAPI_p(EGL_OPENGL_API);
        } else {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Binding to OpenGL ES");
            bindResult = eglBindAPI_p(EGL_OPENGL_ES_API);
        }
        if (!bindResult) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglBindAPI failed: %04x", eglGetError_p());
        }
    }

    // 如果还没有 dummy 上下文，创建一个占用 0x1
    if (g_DummyContext == EGL_NO_CONTEXT) {
        EGLint dummy_pbuffer[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
        EGLSurface dummySurface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config, dummy_pbuffer);
        if (dummySurface != EGL_NO_SURFACE) {
            const EGLint dummy_ctx_attrs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
            g_DummyContext = eglCreateContext_p(g_EglDisplay, bundle->config, EGL_NO_CONTEXT, dummy_ctx_attrs);
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Dummy context created: %p", g_DummyContext);
            eglDestroySurface_p(g_EglDisplay, dummySurface);
        }
    }

    int libgl_es = strtol(getenv("LIBGL_ES"), NULL, 0);
    if (libgl_es < 0 || libgl_es > INT16_MAX) libgl_es = 2;
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Requesting ES version: %d", libgl_es);
    const EGLint egl_context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, libgl_es, EGL_NONE };
    bundle->context = eglCreateContext_p(g_EglDisplay, bundle->config, share == NULL ? EGL_NO_CONTEXT : share->context, egl_context_attributes);

    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "eglCreateContext returned: %p", bundle->context);

    if (bundle->context == EGL_NO_CONTEXT)
    {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglCreateContext_p() finished with error: %04x",
                            eglGetError_p());
        free(bundle);
        return NULL;
    }

    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "gl_init_context returning bundle=%p, context=%p", bundle, bundle->context);
    return bundle;
}

void gl_swap_surface(gl_render_window_t* bundle) {
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "gl_swap_surface: bundle=%p, newNativeSurface=%p", 
                       bundle, bundle->newNativeSurface);
    
    // 如果有新 Surface 待切换，先释放旧资源（如果有）
    if (bundle->newNativeSurface != NULL) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to new native surface, releasing old if any");
        
        // 先解绑当前上下文（如果有）
        if (currentBundle == bundle) {
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        }
        
        // 释放旧的 nativeSurface 和 eglSurface
        if (bundle->nativeSurface != NULL) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = NULL;
        }
        if (bundle->surface != NULL) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = NULL;
        }
        
        // 使用新窗口创建
        bundle->nativeSurface = bundle->newNativeSurface;
        bundle->newNativeSurface = NULL;
        ANativeWindow_acquire(bundle->nativeSurface);
        ANativeWindow_setBuffersGeometry(bundle->nativeSurface, 0, 0, bundle->format);
        bundle->surface = eglCreateWindowSurface_p(g_EglDisplay, bundle->config, bundle->nativeSurface, NULL);
        
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "eglCreateWindowSurface returned: %p", bundle->surface);
        
        if (bundle->surface == EGL_NO_SURFACE) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglCreateWindowSurface failed: %04x, falling back to pbuffer", eglGetError_p());
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = NULL;
            const EGLint pbuffer_attrs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
            bundle->surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config, pbuffer_attrs);
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "pbuffer surface created: %p", bundle->surface);
        }
        return;
    }

    // 无新窗口：按原有逻辑释放并回退到 pbuffer
    int32_t nativeWindowWidth = ANativeWindow_getWidth(pojav_environ->pojavWindow);
    int32_t nativeWindowHeight = ANativeWindow_getHeight(pojav_environ->pojavWindow);
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "No new surface, current window dimensions: %d x %d", 
                       nativeWindowWidth, nativeWindowHeight);
    
    if ((nativeWindowWidth > 0) || (nativeWindowHeight > 0)) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Native surface dimensions valid, releasing");
        if (bundle->nativeSurface != NULL) {
            ANativeWindow_release(bundle->nativeSurface);
            bundle->nativeSurface = NULL;
        }
        if (bundle->surface != NULL) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = NULL;
        }
    } else {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag,
                            "Native surface dimensions (%d x %d) invalid! Assuming Android already released window.",
                            nativeWindowWidth, nativeWindowHeight);
        bundle->nativeSurface = NULL;
        if (bundle->surface != NULL) {
            eglDestroySurface_p(g_EglDisplay, bundle->surface);
            bundle->surface = NULL;
        }
    }

    // 回退到 1x1 pbuffer
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Switching to 1x1 pbuffer");
    const EGLint pbuffer_attrs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    bundle->surface = eglCreatePbufferSurface_p(g_EglDisplay, bundle->config, pbuffer_attrs);
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "pbuffer surface created: %p", bundle->surface);
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

    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "gl_make_current: bundle=%p, surface=%p, context=%p", 
                       bundle, bundle->surface, bundle->context);

    bool hasSetMainWindow = false;
    if (pojav_environ->mainWindowBundle == NULL)
    {
        pojav_environ->mainWindowBundle = (basic_render_window_t*)bundle;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Main window bundle is now %p", pojav_environ->mainWindowBundle);
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        hasSetMainWindow = true;
    }

    // 只在 surface 为 EGL_NO_SURFACE 时才创建，不检查地址大小
    if (bundle->surface == EGL_NO_SURFACE) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag, "surface is EGL_NO_SURFACE, calling gl_swap_surface");
        gl_swap_surface(bundle);
    }

    EGLBoolean makeCurrentResult = eglMakeCurrent_p(g_EglDisplay, bundle->surface, bundle->surface, bundle->context);
    __android_log_print(ANDROID_LOG_INFO, g_LogTag, "eglMakeCurrent result: %d", makeCurrentResult);

    if (makeCurrentResult)
    {
        currentBundle = bundle;
        EGLContext cur = eglGetCurrentContext_p();
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "eglGetCurrentContext = %p (expected %p)", cur, bundle->context);
        
        EGLint ver;
        if (eglQueryContext_p(g_EglDisplay, bundle->context, EGL_CONTEXT_CLIENT_VERSION, &ver)) {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Context client version = %d", ver);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglQueryContext failed: 0x%04x", eglGetError_p());
        }
    } else {
        if (hasSetMainWindow)
        {
            pojav_environ->mainWindowBundle->newNativeSurface = NULL;
            gl_swap_surface((gl_render_window_t*)pojav_environ->mainWindowBundle);
            pojav_environ->mainWindowBundle = NULL;
        }
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "eglMakeCurrent returned with error: %04x", eglGetError_p());
    }

    // 调用 glFlush 初始化 GL TLS
    if (glFlush_ptr) {
        glFlush_ptr();
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "glFlush() called successfully");
    } else {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag, "glFlush_ptr is NULL");
    }
    
    // 调用 glGetError 消费错误
    if (glGetError_ptr) {
        GLenum err = glGetError_ptr();
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "glGetError after flush: 0x%04x", err);
        while ((err = glGetError_ptr()) != GL_NO_ERROR) {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "glGetError cleared: 0x%04x", err);
        }
    }

}

void gl_swap_buffers() {
    if (currentBundle == NULL) {
        __android_log_print(ANDROID_LOG_WARN, g_LogTag, "gl_swap_buffers: currentBundle is NULL");
        return;
    }
    
    if (currentBundle->state == STATE_RENDERER_NEW_WINDOW)
    {
        eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        gl_swap_surface(currentBundle);
        eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface, currentBundle->context);
        currentBundle->state = STATE_RENDERER_ALIVE;
    }

    if (currentBundle->surface != NULL) {
        if (!eglSwapBuffers_p(g_EglDisplay, currentBundle->surface) && eglGetError_p() == EGL_BAD_SURFACE)
        {
            __android_log_print(ANDROID_LOG_WARN, g_LogTag, "SwapBuffers failed with EGL_BAD_SURFACE, recovering");
            eglMakeCurrent_p(g_EglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            currentBundle->newNativeSurface = NULL;
            gl_swap_surface(currentBundle);
            eglMakeCurrent_p(g_EglDisplay, currentBundle->surface, currentBundle->surface, currentBundle->context);
            if (currentBundle->nativeSurface != NULL && currentBundle->state == STATE_RENDERER_NEW_WINDOW) {
                currentBundle->state = STATE_RENDERER_ALIVE;
            }
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "The window has died, awaiting window change");
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
