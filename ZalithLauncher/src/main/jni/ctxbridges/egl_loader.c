//
// Created by maks on 21.09.2022.
// Modified to support namespace hijacking for Freedreno/Turnip drivers.
//
#include <stddef.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include <limits.h>          // for PATH_MAX
#include "br_loader.h"
#include "egl_loader.h"
#include "../driver_helper/nsbypass.h"        // for linker_ns_load / linker_ns_dlopen

// EGL function pointers (all as before)
EGLBoolean (*eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
EGLBoolean (*eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);
EGLBoolean (*eglDestroySurface_p) (EGLDisplay dpy, EGLSurface surface);
EGLBoolean (*eglTerminate_p) (EGLDisplay dpy);
EGLBoolean (*eglReleaseThread_p) (void);
EGLContext (*eglGetCurrentContext_p) (void);
EGLDisplay (*eglGetDisplay_p) (NativeDisplayType display);
EGLBoolean (*eglInitialize_p) (EGLDisplay dpy, EGLint *major, EGLint *minor);
EGLBoolean (*eglChooseConfig_p) (EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
EGLBoolean (*eglGetConfigAttrib_p) (EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value);
EGLBoolean (*eglBindAPI_p) (EGLenum api);
EGLSurface (*eglCreatePbufferSurface_p) (EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list);
EGLSurface (*eglCreateWindowSurface_p) (EGLDisplay dpy, EGLConfig config, NativeWindowType window, const EGLint *attrib_list);
EGLBoolean (*eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
EGLint (*eglGetError_p) (void);
EGLContext (*eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
EGLBoolean (*eglSwapInterval_p) (EGLDisplay dpy, EGLint interval);
EGLSurface (*eglGetCurrentSurface_p) (EGLint readdraw);
EGLBoolean (*eglQuerySurface_p)(EGLDisplay display, EGLSurface surface, EGLint attribute, EGLint * value);
EGLBoolean (*eglQueryContext_p)(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value);

void dlsym_EGL() {
    void* dl_handle = nullptr;
    char* eglName = nullptr;
    char* gles = getenv("LIBGL_GLES");

    // Determine EGL library name (original logic)
    if (gles && !strncmp(gles, "libGLESv2_angle.so", 18))
    {
        eglName = "libEGL_angle.so";
    } else {
        char* execEgl = getenv("POJAVEXEC_EGL");
        eglName = gles ? gles : (execEgl ? execEgl : "libEGL.so");
    }

    // Check if we should use namespace hijacking for Freedreno/Turnip
    const char* renderer = getenv("POJAV_RENDERER");
    int use_namespace = 0;
    if (renderer && strcmp(renderer, "opengles3_desktopgl_freedreno_kgsl") == 0) {
        // Only use namespace if the library name is a plain name (no path)
        if (eglName && strchr(eglName, '/') == nullptr) {
            use_namespace = 1;
        }
    }

    // If namespace mode is requested, try to load via ns bypass
    if (use_namespace) {
        static int ns_initialized = 0;
        const char* search_path = getenv("LD_LIBRARY_PATH");
        if (!search_path || strlen(search_path) == 0) {
            search_path = "/vendor/lib64:/system/lib64";   // typical location for Turnip drivers
        }

        // Initialize namespace only once
        if (!ns_initialized) {
            if (linker_ns_load(search_path)) {
                ns_initialized = 1;
            } else {
                // If namespace creation fails, fall back to normal loading
                use_namespace = 0;
            }
        }

        if (ns_initialized) {
            dl_handle = linker_ns_dlopen(eglName, RTLD_GLOBAL | RTLD_LAZY);
            // If namespace dlopen fails, fallback to normal dlopen (handled later)
        }
    }

    // Fallback to normal dlopen if namespace loading was not used or failed
    if (!use_namespace || dl_handle == nullptr) {
        if (eglName)
            dl_handle = dlopen(eglName, RTLD_GLOBAL | RTLD_LAZY);
        if (dl_handle == nullptr)
            dl_handle = dlopen("libEGL.so", RTLD_GLOBAL | RTLD_LAZY);
    }

    // Abort if we still don't have a handle
    if (dl_handle == nullptr) abort();

    // Resolve all EGL function pointers (same as original)
    eglBindAPI_p = GLGetProcAddress(dl_handle, "eglBindAPI");
    eglChooseConfig_p = GLGetProcAddress(dl_handle, "eglChooseConfig");
    eglCreateContext_p = GLGetProcAddress(dl_handle, "eglCreateContext");
    eglCreatePbufferSurface_p = GLGetProcAddress(dl_handle, "eglCreatePbufferSurface");
    eglCreateWindowSurface_p = GLGetProcAddress(dl_handle, "eglCreateWindowSurface");
    eglDestroyContext_p = GLGetProcAddress(dl_handle, "eglDestroyContext");
    eglDestroySurface_p = GLGetProcAddress(dl_handle, "eglDestroySurface");
    eglGetConfigAttrib_p = GLGetProcAddress(dl_handle, "eglGetConfigAttrib");
    eglGetCurrentContext_p = GLGetProcAddress(dl_handle, "eglGetCurrentContext");
    eglGetDisplay_p = GLGetProcAddress(dl_handle, "eglGetDisplay");
    eglGetError_p = GLGetProcAddress(dl_handle, "eglGetError");
    eglInitialize_p = GLGetProcAddress(dl_handle, "eglInitialize");
    eglMakeCurrent_p = GLGetProcAddress(dl_handle, "eglMakeCurrent");
    eglSwapBuffers_p = GLGetProcAddress(dl_handle, "eglSwapBuffers");
    eglReleaseThread_p = GLGetProcAddress(dl_handle, "eglReleaseThread");
    eglSwapInterval_p = GLGetProcAddress(dl_handle, "eglSwapInterval");
    eglTerminate_p = GLGetProcAddress(dl_handle, "eglTerminate");
    eglGetCurrentSurface_p = GLGetProcAddress(dl_handle, "eglGetCurrentSurface");
    eglQuerySurface_p = GLGetProcAddress(dl_handle, "eglQuerySurface");
    eglQueryContext_p = GLGetProcAddress(dl_handle, "eglQueryContext");
}

