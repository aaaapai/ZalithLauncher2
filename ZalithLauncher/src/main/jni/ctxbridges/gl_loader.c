#include "gl_loader.h"
#include <dlfcn.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "GLLoader"

glFlush_func glFlush_p = NULL;
glGetError_func glGetError_p = NULL;
glGetString_func glGetString_p = NULL;

// 检查 renderer 是否包含 _desktopgl 后缀
static bool is_desktopgl_renderer() {
    const char* renderer = getenv("POJAV_RENDERER");
    if (!renderer) return false;
    // 检查是否以 _desktopgl 结尾
    size_t len = strlen(renderer);
    if (len < 10) return false; // "_desktopgl" 长度为 10
    return strcmp(renderer + len - 10, "_desktopgl") == 0;
}

bool dlsym_GL() {
    // 检查是否使用 _desktopgl 后缀
    bool desktopgl = is_desktopgl_renderer();
    
    const char* libPath = getenv("LIBGL_GLES");
    
    if (!libPath) {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "LIBGL_GLES not set");
        
        if (desktopgl) {
            __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "_desktopgl detected, using eglGetProcAddress");
            
            // 通过 eglGetProcAddress 获取 GL 函数
            typedef void* (*eglGetProcAddress_func)(const char*);
            eglGetProcAddress_func eglGetProcAddress = (eglGetProcAddress_func)dlsym(RTLD_DEFAULT, "eglGetProcAddress");
            
            if (eglGetProcAddress) {
                glFlush_p = (glFlush_func)eglGetProcAddress("glFlush");
                glGetError_p = (glGetError_func)eglGetProcAddress("glGetError");
                glGetString_p = (glGetString_func)eglGetProcAddress("glGetString");
            }
        } else {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Not _desktopgl and LIBGL_GLES not set, skipping GL loader");
            return false;
        }
    } else {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Loading GL library from LIBGL_GLES: %s", libPath);
        
        void* libgl = dlopen(libPath, RTLD_LAZY);
        if (!libgl) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to dlopen %s: %s", libPath, dlerror());
            return false;
        }
        
        glFlush_p = (glFlush_func)dlsym(libgl, "glFlush");
        glGetError_p = (glGetError_func)dlsym(libgl, "glGetError");
        glGetString_p = (glGetString_func)dlsym(libgl, "glGetString");
    }
    
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, 
                       "GL functions: flush=%p, getError=%p, getString=%p", 
                       glFlush_p, glGetError_p, glGetString_p);
    
    // 至少需要 glFlush 和 glGetError
    return (glFlush_p != NULL && glGetError_p != NULL);
}
