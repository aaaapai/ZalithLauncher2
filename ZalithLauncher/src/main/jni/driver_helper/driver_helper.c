//
// Created by Vera-Firefly on 17.01.2025.
//
#include <EGL/egl.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include <stdio.h>
#include "nsbypass.h"
#include "GL/gl.h"

// 声明 nsbypass 提供的设置函数
extern void linker_ns_set_android_dlopen_ext(void* (*func)(const char*, int, const android_dlextinfo*));

//#define ADRENO_POSSIBLE
#ifdef ADRENO_POSSIBLE

bool checkAdrenoGraphics() {
    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY || eglInitialize(eglDisplay, NULL, NULL) != EGL_TRUE) 
        return false;

    EGLint egl_attributes[] = {
        EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8, EGL_DEPTH_SIZE, 24, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE
    };

    EGLint num_configs = 0;
    if (eglChooseConfig(eglDisplay, egl_attributes, NULL, 0, &num_configs) != EGL_TRUE || num_configs == 0) {
        eglTerminate(eglDisplay);
        return false;
    }

    EGLConfig eglConfig;
    eglChooseConfig(eglDisplay, egl_attributes, &eglConfig, 1, &num_configs);

    const EGLint egl_context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    EGLContext context = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, egl_context_attributes);
    if (context == EGL_NO_CONTEXT) {
        eglTerminate(eglDisplay);
        return false;
    }

    if (eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, context) != EGL_TRUE) {
        eglDestroyContext(eglDisplay, context);
        eglTerminate(eglDisplay);
        return false;
    }

    const char* vendor = (const char*)glGetString(GL_VENDOR);
    const char* renderer = (const char*)glGetString(GL_RENDERER);

    bool is_adreno = (vendor && renderer && strcmp(vendor, "Qualcomm") == 0 && strstr(renderer, "Adreno") != NULL);

    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(eglDisplay, context);
    eglTerminate(eglDisplay);

    return is_adreno;
}

void* loadTurnipVulkan(const char* driver_path, const char* native_dir, const char* cache_dir) {
    if (!checkAdrenoGraphics()) {
        printf("Not Adreno graphics, skipping Turnip\n");
        return NULL;
    }

    if (!native_dir) {
        printf("native_dir is NULL\n");
        return NULL;
    }

    // 加载命名空间
    if (!linker_ns_load(native_dir)) {
        printf("linker_ns_load failed for %s\n", native_dir);
        return NULL;
    }

    // 加载 liblinkerhook.so
    void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
    if (!linkerhook) {
        printf("Failed to load liblinkerhook.so: %s\n", dlerror());
        return NULL;
    }

    const char* target_driver = (driver_path && strlen(driver_path) > 0) ? driver_path : "libvulkan_freedreno.so";
    void* turnip_driver_handle = linker_ns_dlopen(target_driver, RTLD_LOCAL | RTLD_NOW);
    if (!turnip_driver_handle) {
        printf("Failed to load Turnip driver: %s\n", dlerror());
        dlclose(linkerhook);
        return NULL;
    }

    void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
    if (!dl_android) {
        printf("Failed to load libdl_android.so\n");
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }

    void* android_get_exported_namespace = dlsym(dl_android, "android_get_exported_namespace");
    // 注意：liblinkerhook 导出的是 set_handles
    void (*set_handles)(void*, void*, void*) = (void (*)(void*, void*, void*))
        dlsym(linkerhook, "set_handles");

    if (!set_handles || !android_get_exported_namespace) {
        printf("Missing symbols: set_handles=%p, android_get_exported_namespace=%p\n",
               set_handles, android_get_exported_namespace);
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }

    set_handles(turnip_driver_handle, android_dlopen_ext, android_get_exported_namespace);

    // 获取 liblinkerhook 的 android_dlopen_ext 包装函数，并设置到 nsbypass 中
    void* (*hook_android_dlopen_ext)(const char*, int, const android_dlextinfo*) =
        (void* (*)(const char*, int, const android_dlextinfo*))dlsym(linkerhook, "android_dlopen_ext");
    if (hook_android_dlopen_ext) {
        linker_ns_set_android_dlopen_ext(hook_android_dlopen_ext);
        printf("Set android_dlopen_ext to hook version from liblinkerhook\n");
    } else {
        printf("Warning: cannot find android_dlopen_ext in liblinkerhook, using system version\n");
    }

    // 加载 patched libvulkan
    const char* patch_name = "libhhlvlk.so";
    void* libvulkan = linker_ns_dlopen_unique(cache_dir, "libvulkan.so", patch_name, RTLD_LOCAL | RTLD_NOW);
    if (!libvulkan) {
        printf("Failed to load patched libvulkan.so\n");
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }

    printf("Turnip Vulkan loaded successfully\n");
    return libvulkan;
}

#endif
