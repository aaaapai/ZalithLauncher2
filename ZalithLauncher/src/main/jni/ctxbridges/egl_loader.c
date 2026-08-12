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
#include <android/dlext.h>

extern void linker_ns_set_android_dlopen_ext(void* (*func)(const char*, int, const android_dlextinfo*));

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

    if (gles) {
        if (strstr(gles, "mesa") != NULL) {
            eglName = "libEGL_mesa.so";
        } else if (strstr(gles, "angle") != NULL) {
            eglName = "libEGL_angle.so";
        } else {
            eglName = gles;
        }
    } else {
        eglName = getenv("POJAVEXEC_EGL") ? getenv("POJAVEXEC_EGL") : "libEGL.so";
    }

    int use_namespace = 0;
    if (eglName) {
        if (strcmp(eglName, "libEGL_mesa.so") == 0 || strcmp(eglName, "libEGL_angle.so") == 0) {
            use_namespace = 1;
        }
    }

    const char* ns_path = getenv("DRIVER_PATH");
    if (!ns_path || strlen(ns_path) == 0) {
        const char* ld_path = getenv("LD_LIBRARY_PATH");
        if (ld_path) {
            static char path_buf[512];
            strncpy(path_buf, ld_path, sizeof(path_buf) - 1);
            char* colon = strchr(path_buf, ':');
            if (colon) *colon = '\0';
            ns_path = path_buf;
        }
    }

    if (use_namespace && ns_path && strlen(ns_path) > 0) {
        if (!linker_ns_load(ns_path)) {
            fprintf(stderr, "[EGL Loader] Namespace init failed for %s, fallback to dlopen\n", ns_path);
            goto fallback_dlopen;
        }

        if (strstr(eglName, "mesa") != NULL) {
            void* existing = dlopen("libgallium_dri.so", RTLD_NOLOAD | RTLD_GLOBAL);
            if (existing) {
                fprintf(stderr, "[EGL Loader] libgallium_dri.so already loaded, closing to reload from namespace\n");
                dlclose(existing);
            }
            void* gallium = linker_ns_dlopen("libgallium_dri.so", RTLD_GLOBAL | RTLD_NOW);
            if (gallium) {
                fprintf(stderr, "[EGL Loader] Preloaded libgallium_dri.so from namespace\n");
            } else {
                fprintf(stderr, "[EGL Loader] WARNING: libgallium_dri.so not found in namespace, Mesa may fail\n");
            }
        }

        void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
        void* (*hook_android_dlopen_ext)(const char*, int, const android_dlextinfo*) = nullptr;
        void (*set_handles)(void*, void*, void*) = nullptr;
        if (linkerhook) {
            set_handles = (void (*)(void*, void*, void*)) dlsym(linkerhook, "set_handles");
            hook_android_dlopen_ext = (void* (*)(const char*, int, const android_dlextinfo*))
                                       dlsym(linkerhook, "android_dlopen_ext");
            if (set_handles && hook_android_dlopen_ext) {
                linker_ns_set_android_dlopen_ext(hook_android_dlopen_ext);
                fprintf(stderr, "[EGL Loader] linkerhook installed\n");
            } else {
                fprintf(stderr, "[EGL Loader] linkerhook symbols missing\n");
                dlclose(linkerhook);
                linkerhook = nullptr;
            }
        } else {
            fprintf(stderr, "[EGL Loader] liblinkerhook not found, dependencies may leak\n");
        }

        dl_handle = linker_ns_dlopen(eglName, RTLD_GLOBAL | RTLD_NOW);
        if (dl_handle) {
            fprintf(stderr, "[EGL Loader] Loaded %s via namespace from %s\n", eglName, ns_path);
            if (linkerhook && set_handles && hook_android_dlopen_ext) {
                void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
                void* (*android_get_exported_namespace)(const char*) = nullptr;
                if (dl_android) {
                    android_get_exported_namespace = (void* (*)(const char*))
                                                     dlsym(dl_android, "android_get_exported_namespace");
                }
                if (android_get_exported_namespace) {
                    set_handles(dl_handle, hook_android_dlopen_ext, android_get_exported_namespace);
                } else {
                    set_handles(dl_handle, hook_android_dlopen_ext, nullptr);
                }
            }
            goto success;
        } else {
            fprintf(stderr, "[EGL Loader] Namespace dlopen failed: %s\n", dlerror());
            if (linkerhook) dlclose(linkerhook);
        }
    }

fallback_dlopen:
    if (eglName) {
        dl_handle = dlopen(eglName, RTLD_LOCAL | RTLD_LAZY);
    }
    if (!dl_handle) {
        dl_handle = dlopen("libEGL.so", RTLD_LOCAL | RTLD_LAZY);
    }
    if (!dl_handle) {
        fprintf(stderr, "[EGL Loader] All dlopen attempts failed, aborting\n");
        abort();
    }
    fprintf(stderr, "[EGL Loader] Loaded via normal dlopen\n");

success:
    g_egl_handle = dl_handle;
    char ptr_str[32];
    snprintf(ptr_str, sizeof(ptr_str), "%p", dl_handle);
    setenv("EGL_PTR", ptr_str, 1);
    fprintf(stderr, "[EGL Loader] EGL_PTR set to %s\n", ptr_str);

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

