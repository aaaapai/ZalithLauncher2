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

// EGL function pointers (will be resolved from Mesa driver)
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

static void* g_mesa_handle = nullptr;
static void* g_egl_handle = nullptr;

void* pojavGetEGLptr(void) {
    return g_egl_handle;
}

void dlsym_EGL() {
    void* dl_handle = nullptr;
    char* eglName = nullptr;
    char* gles = getenv("LIBGL_GLES");

    if (gles) {
        if (strstr(gles, "mesa") != nullptr) {
            eglName = "libEGL_mesa.so";
        } else if (strstr(gles, "angle") != nullptr) {
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
    
    if (use_namespace != 1) goto fallback_dlopen;

    const char* ns_path = getenv("DRIVER_PATH");
    if (!ns_path || strlen(ns_path) == 0) {
        const char* ld_path = getenv("LD_LIBRARY_PATH");
        if (ld_path) {
            static char path_buf[512];
            strncpy(path_buf, ld_path, sizeof(path_buf) - 1);
            path_buf[sizeof(path_buf) - 1] = '\0';
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

        if (strstr(eglName, "mesa") != nullptr) {
            void* existing = dlopen("libgallium_dri.so", RTLD_NOLOAD | RTLD_GLOBAL);
            if (existing) {
                fprintf(stderr, "[EGL Loader] libgallium_dri.so already loaded, closing to reload from namespace\n");
                dlclose(existing);
            }
            void* gallium = linker_ns_dlopen("libgallium_dri.so", RTLD_LOCAL | RTLD_NOW);
            if (gallium) {
                fprintf(stderr, "[EGL Loader] Preloaded libgallium_dri.so from namespace\n");
            } else {
                fprintf(stderr, "[EGL Loader] WARNING: libgallium_dri.so not found in namespace, Mesa may fail\n");
            }
        }

        void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
        if (!linkerhook) {
            fprintf(stderr, "[EGL Loader] liblinkerhook.so not found, fallback to normal dlopen\n");
            goto fallback_dlopen;
        }

        void (*set_handles)(void*, void*, void*) = (void (*)(void*, void*, void*))
                dlsym(linkerhook, "set_handles");
        void* (*hook_android_dlopen_ext)(const char*, int, const android_dlextinfo*) =
                (void* (*)(const char*, int, const android_dlextinfo*))
                dlsym(linkerhook, "android_dlopen_ext");

        if (!set_handles || !hook_android_dlopen_ext) {
            fprintf(stderr, "[EGL Loader] linkerhook missing set_handles or android_dlopen_ext\n");
            dlclose(linkerhook);
            goto fallback_dlopen;
        }

        // 4. Get system android_dlopen_ext (real implementation)
        void* sys_android_dlopen_ext = dlsym(RTLD_DEFAULT, "android_dlopen_ext");
        if (!sys_android_dlopen_ext) {
            void* dl_android_tmp = dlopen("libdl_android.so", RTLD_NOW);
            if (dl_android_tmp) {
                sys_android_dlopen_ext = dlsym(dl_android_tmp, "android_dlopen_ext");
                // Do not dlclose, keep symbol available
            }
        }
        if (!sys_android_dlopen_ext) {
            fprintf(stderr, "[EGL Loader] Cannot find system android_dlopen_ext\n");
            dlclose(linkerhook);
            goto fallback_dlopen;
        }

        // 5. Get android_get_exported_namespace (optional, but needed by hook)
        void* (*android_get_exported_namespace)(const char*) =
                (void* (*)(const char*))dlsym(RTLD_DEFAULT, "android_get_exported_namespace");
        if (!android_get_exported_namespace) {
            void* dl_android = dlopen("libdl_android.so", RTLD_LAZY);
            if (dl_android) {
                android_get_exported_namespace = (void* (*)(const char*))
                        dlsym(dl_android, "android_get_exported_namespace");
            }
        }

        // 6. Load Mesa driver (actual backend)
        void* mesa_handle = linker_ns_dlopen(eglName, RTLD_LOCAL | RTLD_NOW);
        if (!mesa_handle) {
            fprintf(stderr, "[EGL Loader] Failed to load Mesa EGL: %s\n", dlerror());
            dlclose(linkerhook);
            goto fallback_dlopen;
        }
        g_mesa_handle = mesa_handle;
        fprintf(stderr, "[EGL Loader] Mesa driver loaded at %p\n", mesa_handle);

        set_handles(mesa_handle, sys_android_dlopen_ext, android_get_exported_namespace);
        fprintf(stderr, "[EGL Loader] set_handles called with Mesa driver handle\n");

        linker_ns_set_android_dlopen_ext(hook_android_dlopen_ext);
        fprintf(stderr, "[EGL Loader] Hook installed, dlopen(\"libEGL.so\") will return Mesa handle\n");

        const char* cache_dir = getenv("CACHE_DIR");
        const char* patch_name = getenv("SYSTEM_EGL_PATCH_NAME") ? getenv("SYSTEM_EGL_PATCH_NAME") : "libHGL.so";
        if (cache_dir && strlen(cache_dir) > 0) {
            void* sys_egl = linker_ns_dlopen_unique(cache_dir, "libEGL.so", patch_name, RTLD_LOCAL | RTLD_NOW);
            if (sys_egl) {
                g_egl_handle = sys_egl;
                fprintf(stderr, "[EGL Loader] Patched system libEGL loaded as %s (handle %p)\n", patch_name, sys_egl);
            } else {
                fprintf(stderr, "[EGL Loader] Failed to load patched system libEGL: %s\n", dlerror());
                g_egl_handle = mesa_handle;
            }
        } else {
            fprintf(stderr, "[EGL Loader] CACHE_DIR not set, using Mesa handle as placeholder\n");
            g_egl_handle = mesa_handle;
        }

        dl_handle = g_egl_handle;
        goto success;
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
    g_mesa_handle = dl_handle;
    g_egl_handle = dl_handle;

success:
    if (g_egl_handle) {
        char ptr_str[32];
        snprintf(ptr_str, sizeof(ptr_str), "%p", g_egl_handle);
        setenv("EGL_PTR", ptr_str, 1);
        fprintf(stderr, "[EGL Loader] EGL_PTR set to %s\n", ptr_str);
    } else {
        fprintf(stderr, "[EGL Loader] FATAL: g_egl_handle is null\n");
        abort();
    }

    #define RESOLVE(name) name##_p = (decltype(name##_p))dlsym(g_mesa_handle, #name)
    RESOLVE(eglBindAPI);
    RESOLVE(eglChooseConfig);
    RESOLVE(eglCreateContext);
    RESOLVE(eglCreatePbufferSurface);
    RESOLVE(eglCreateWindowSurface);
    RESOLVE(eglDestroyContext);
    RESOLVE(eglDestroySurface);
    RESOLVE(eglGetConfigAttrib);
    RESOLVE(eglGetCurrentContext);
    RESOLVE(eglGetDisplay);
    RESOLVE(eglGetError);
    RESOLVE(eglInitialize);
    RESOLVE(eglMakeCurrent);
    RESOLVE(eglSwapBuffers);
    RESOLVE(eglReleaseThread);
    RESOLVE(eglSwapInterval);
    RESOLVE(eglTerminate);
    RESOLVE(eglGetCurrentSurface);
    RESOLVE(eglQuerySurface);
    RESOLVE(eglQueryContext);
    #undef RESOLVE

    if (!eglMakeCurrent_p || !eglSwapBuffers_p || !eglGetDisplay_p) {
        fprintf(stderr, "[EGL Loader] WARNING: Some EGL functions not resolved\n");
    } else {
        fprintf(stderr, "[EGL Loader] All EGL functions resolved\n");
    }
}
