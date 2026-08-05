//
// Created by Vera-Firefly on 17.01.2025.
//
#include <EGL/egl.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include <stdio.h>          // 添加 printf
#include "nsbypass.h"
#include "GL/gl.h"

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
    // 使用 hook.c 中导出的正确符号名
    void (*linkerhookPassHandles)(void*, void*, void*) = dlsym(linkerhook, "app__pojav_linkerhook_pass_handles");

    if (!linkerhookPassHandles || !android_get_exported_namespace) {
        printf("Missing symbols: linkerhookPassHandles=%p, android_get_exported_namespace=%p\n",
               linkerhookPassHandles, android_get_exported_namespace);
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(turnip_driver_handle);
        return NULL;
    }

    linkerhookPassHandles(turnip_driver_handle, android_dlopen_ext, android_get_exported_namespace);

    // 使用唯一的 patch_name
    const char* patch_name = "libhhlvlk.so";  // 长度必须与 libvulkan.so 相同 (12)
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
