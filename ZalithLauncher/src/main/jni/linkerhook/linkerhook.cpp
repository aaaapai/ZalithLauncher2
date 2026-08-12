//
// Created by Vera-Firefly on 17.01.2025.
//

#include <android/dlext.h>
#include <string.h>
#include <stdio.h>
#include <atomic>
#include <dlfcn.h>
#include "linkerhook.h"

static void* (*dlopen_ext_impl)(const char* filename, int flags, const android_dlextinfo* extinfo, const void* caller_addr);
static struct android_namespace_t* (*get_exported_namespace_impl)(const char* name);

static void* ready_handle;
static std::atomic<void*> global_ready_handle{nullptr};

static const char* supported_namespaces[] = {"sphal", "vendor", "default"};

typedef struct android_namespace_t* (*get_namespace_func_t)(const char*);

static bool g_intercept_enabled = false;
static bool g_intercept_checked = false;

static bool should_intercept_libEGL() {
    if (!g_intercept_checked) {
        const char* override = getenv("MESA_LOADER_DRIVER_OVERRIDE");
        g_intercept_enabled = (override && strcmp(override, "kgsl") == 0);
        g_intercept_checked = true;
        fprintf(stderr, "[LinkerHook] Intercept libEGL: %s\n", g_intercept_enabled ? "ENABLED" : "DISABLED");
    }
    return g_intercept_enabled;
}

static struct android_namespace_t* multi_get_namespace(const char* name) {
    get_namespace_func_t func = nullptr;
    struct android_namespace_t* ns = nullptr;

    func = (get_namespace_func_t)dlsym(RTLD_DEFAULT, "android_get_exported_namespace");
    if (func) {
        ns = func(name);
        if (ns) return ns;
    }

    void* dl_android = dlopen("libdl_android.so", RTLD_LAZY);
    if (dl_android) {
        func = (get_namespace_func_t)dlsym(dl_android, "android_get_exported_namespace");
        if (func) {
            ns = func(name);
            if (ns) return ns;
        }
    }

    void* libc = dlopen("libc.so", RTLD_LAZY);
    if (libc) {
        func = (get_namespace_func_t)dlsym(libc, "android_get_exported_namespace");
        if (func) {
            ns = func(name);
            if (ns) return ns;
        }
    }

    void* linker_handle = dlopen("/apex/com.android.runtime/bin/linker64", RTLD_LAZY);
    if (!linker_handle) {
        linker_handle = dlopen("/system/bin/linker64", RTLD_LAZY);
    }
    if (linker_handle) {
        func = (get_namespace_func_t)dlsym(linker_handle, "__loader_android_get_exported_namespace");
        if (func) {
            ns = func(name);
            if (ns) return ns;
        }
    }

    fprintf(stderr, "[LinkerHook] All namespace get methods failed for '%s'\n", name);
    return nullptr;
}

void set_handles(void* handle, void* dlopen_ext, void* get_namespace) {
    ready_handle = handle;
    global_ready_handle.store(handle);
    dlopen_ext_impl = (decltype(dlopen_ext_impl))dlopen_ext;
    if (get_namespace) {
        get_exported_namespace_impl = (decltype(get_exported_namespace_impl))get_namespace;
        fprintf(stderr, "[LinkerHook] Using provided android_get_exported_namespace\n");
    } else {
        get_exported_namespace_impl = multi_get_namespace;
        fprintf(stderr, "[LinkerHook] Using fallback multi_get_namespace\n");
    }
}

static void* checkIfGlobalReadyHandle() {
    void* handle = global_ready_handle.load();
    if (handle == nullptr) {
        fprintf(stderr, "Global ready handle is null, falling back to ready_handle.\n");
        return ready_handle;
    }
    return handle;
}

void* dlopen_ext(const char* filename, int flags, const android_dlextinfo* extinfo) {
    if (strstr(filename, "vulkan.") || strstr(filename, "vulkanmemoryallocator")) {
        return checkIfGlobalReadyHandle();
    }
    if (should_intercept_libEGL() && strstr(filename, "EGL.")) {
        return checkIfGlobalReadyHandle();
    }
    return dlopen_ext_impl(filename, flags, extinfo, reinterpret_cast<const void*>(&dlopen_ext));
}

void* load_sphal_library(const char* filename, int flags) {
    if (strstr(filename, "vulkan.") || strstr(filename, "vulkanmemoryallocator.")) {
        return checkIfGlobalReadyHandle();
    }

    if (should_intercept_libEGL() && strstr(filename, "EGL.")) {
        return checkIfGlobalReadyHandle();
    }

    if (!dlopen_ext_impl) {
        fprintf(stderr, "[LinkerHook] dlopen_ext_impl is null, using dlopen\n");
        return dlopen(filename, flags);
    }

    struct android_namespace_t* androidNamespace = nullptr;
    if (get_exported_namespace_impl) {
        for (const char* ns_name : supported_namespaces) {
            androidNamespace = get_exported_namespace_impl(ns_name);
            if (androidNamespace != NULL) break;
        }
    }

    if (androidNamespace == nullptr) {
        flags &= ~ANDROID_DLEXT_USE_NAMESPACE;
        fprintf(stderr, "[LinkerHook] No namespace found, using default (NULL extinfo) for %s\n", filename);
        return dlopen_ext_impl(filename, flags, nullptr, reinterpret_cast<const void*>(&dlopen_ext));
    }

    android_dlextinfo extinfo = {
        .flags = ANDROID_DLEXT_USE_NAMESPACE,
        .library_namespace = androidNamespace
    };
    return dlopen_ext_impl(filename, flags, &extinfo, reinterpret_cast<const void*>(&dlopen_ext));
}

uint64_t hook_atrace_get_enabled_tags() {
    return 0;
}

extern "C" __attribute__((visibility("default"), used))
void app__pojav_linkerhook_pass_handles(void* handle, void* dlopen_ext, void* get_namespace) {
    set_handles(handle, dlopen_ext, get_namespace);
}
