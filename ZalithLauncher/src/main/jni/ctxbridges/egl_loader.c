//
// Created by maks on 21.09.2022.
// Modified to support Freedreno/Turnip driver namespace bypass,
// and export EGL handle via environment variable and function.
//
#include <stddef.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include <limits.h>
#include <stdio.h>
#include "br_loader.h"
#include "egl_loader.h"
#include "../driver_helper/nsbypass.h"

// EGL function pointers (unchanged)
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

// Global handle for the loaded EGL library
static void* g_egl_handle = nullptr;

// Exported function: returns the EGL library handle
void* pojavGetEGLptr(void) {
    return g_egl_handle;
}

void dlsym_EGL() {
    void* dl_handle = nullptr;
    char* eglName = nullptr;
    char* gles = getenv("LIBGL_GLES");
    const char* renderer = getenv("POJAV_RENDERER");

    // Determine EGL library name
    // If Freedreno/Turnip renderer is requested, prioritize POJAVEXEC_EGL or default Mesa EGL
    int use_freedreno = 0;
    if (renderer && strcmp(renderer, "opengles3_desktopgl_freedreno_kgsl") == 0) {
        use_freedreno = 1;
        char* execEgl = getenv("POJAVEXEC_EGL");
        if (execEgl && strlen(execEgl) > 0) {
            eglName = execEgl;
        } else {
            eglName = "libEGL_mesa.so";   // default Mesa EGL name
        }
    } else {
        // Original logic for other renderers
        if (gles && !strncmp(gles, "libGLESv2_angle.so", 18)) {
            eglName = "libEGL_angle.so";
        } else {
            char* execEgl = getenv("POJAVEXEC_EGL");
            eglName = gles ? gles : (execEgl ? execEgl : "libEGL.so");
        }
    }

    // Check if namespace bypass should be used (only for Freedreno and plain library name)
    int use_namespace = 0;
    if (use_freedreno && eglName && strchr(eglName, '/') == NULL) {
        use_namespace = 1;
    }

    // Initialize namespace if needed
    if (use_namespace) {
        static int ns_initialized = 0;
        static int ns_load_success = 0;   // 0=not tried, 1=success, -1=fail

        // Determine search path: environment variable POJAV_DRIVER_PATH, else fallback to LD_LIBRARY_PATH or default
        const char* driver_path = getenv("POJAV_DRIVER_PATH");
        char search_path[PATH_MAX];
        if (driver_path && strlen(driver_path) > 0) {
            strncpy(search_path, driver_path, sizeof(search_path) - 1);
            search_path[sizeof(search_path) - 1] = '\0';
        } else {
            const char* ld_path = getenv("LD_LIBRARY_PATH");
            if (ld_path && strlen(ld_path) > 0) {
                strncpy(search_path, ld_path, sizeof(search_path) - 1);
                search_path[sizeof(search_path) - 1] = '\0';
            } else {
                strcpy(search_path, "/vendor/lib64:/system/lib64");
            }
        }

        if (!ns_initialized) {
            if (linker_ns_load(search_path)) {
                ns_load_success = 1;
                fprintf(stderr, "[EGL] Namespace loaded successfully with path: %s\n", search_path);
            } else {
                ns_load_success = -1;
                fprintf(stderr, "[EGL] Failed to load namespace with path: %s, falling back to normal dlopen\n", search_path);
            }
            ns_initialized = 1;
        }

        if (ns_load_success == 1) {
            dl_handle = linker_ns_dlopen(eglName, RTLD_GLOBAL | RTLD_NOW);
            if (!dl_handle) {
                fprintf(stderr, "[EGL] linker_ns_dlopen(%s) failed: %s\n", eglName, dlerror());
            } else {
                fprintf(stderr, "[EGL] Loaded %s via namespace\n", eglName);
            }
        }
    }

    // Fallback to normal dlopen if namespace not used or failed
    if (!use_namespace || dl_handle == nullptr) {
        if (eglName)
            dl_handle = dlopen(eglName, RTLD_GLOBAL | RTLD_LAZY);
        if (dl_handle == nullptr)
            dl_handle = dlopen("libEGL.so", RTLD_GLOBAL | RTLD_LAZY);
        if (dl_handle) {
            fprintf(stderr, "[EGL] Loaded %s via normal dlopen\n", eglName ? eglName : "libEGL.so");
        }
    }

    if (dl_handle == nullptr) {
        fprintf(stderr, "[EGL] Failed to load EGL library, aborting\n");
        abort();
    }

    // Store the handle globally and export via environment variable
    g_egl_handle = dl_handle;
    char ptr_str[32];
    snprintf(ptr_str, sizeof(ptr_str), "%p", dl_handle);
    setenv("EGL_PTR", ptr_str, 1);
    fprintf(stderr, "[EGL] EGL_PTR set to %s\n", ptr_str);

    // Resolve all EGL function pointers
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
