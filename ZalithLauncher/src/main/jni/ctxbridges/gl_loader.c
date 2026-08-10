#include "gl_loader.h"
#include <dlfcn.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "GLLoader"

glFlush_func glFlush_ptr = NULL;
glGetError_func glGetError_ptr = NULL;
glGetString_func glGetString_ptr = NULL;

bool dlsym_GL() {
    void* libmg = dlopen("libmobileglues.so", RTLD_LAZY);
    if (!libmg) {
        // fallback to absolute path if needed (but usually in LD_LIBRARY_PATH)
        libmg = dlopen("libmobileglues.so", RTLD_LAZY);
    }
    if (libmg) {
        // Try direct dlsym
        glFlush_ptr = (glFlush_func)dlsym(libmg, "glFlush");
        glGetError_ptr = (glGetError_func)dlsym(libmg, "glGetError");
        glGetString_ptr = (glGetString_func)dlsym(libmg, "glGetString");
        if (glFlush_ptr && glGetError_ptr && glGetString_ptr) {
            __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Loaded GL functions from libmobileglues.so");
            return true;
        }
        // Try via glXGetProcAddress
        typedef void* (*glXGetProcAddress_t)(const char*);
        glXGetProcAddress_t glXGetProcAddress = (glXGetProcAddress_t)dlsym(libmg, "glXGetProcAddress");
        if (glXGetProcAddress) {
            glFlush_ptr = (glFlush_func)glXGetProcAddress("glFlush");
            glGetError_ptr = (glGetError_func)glXGetProcAddress("glGetError");
            glGetString_ptr = (glGetString_func)glXGetProcAddress("glGetString");
            if (glFlush_ptr && glGetError_ptr && glGetString_ptr) {
                __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Loaded GL functions via glXGetProcAddress");
                return true;
            }
        }
    }

    // Fallback to LIBGL_GLES
    const char* libPath = getenv("LIBGL_GLES");
    if (libPath) {
        void* libgl = dlopen(libPath, RTLD_LAZY);
        if (libgl) {
            glFlush_ptr = (glFlush_func)dlsym(libgl, "glFlush");
            glGetError_ptr = (glGetError_func)dlsym(libgl, "glGetError");
            glGetString_ptr = (glGetString_func)dlsym(libgl, "glGetString");
            if (glFlush_ptr && glGetError_ptr && glGetString_ptr) {
                __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Loaded GL functions from LIBGL_GLES: %s", libPath);
                return true;
            }
        }
    }
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to load GL functions");
    return false;
}
