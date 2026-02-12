//
// Created by maks on 24.09.2022.
//

#include <stdlib.h>
#include <android/log.h>
#include <assert.h>
#include <string.h>
#include "environ.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/rect.h>
#include "../jemalloc/jemalloc.h"

struct pojav_environ_s *pojav_environ;
__attribute__((constructor)) void env_init() {
    char* strptr_env = getenv("POJAV_ENVIRON");
    if(strptr_env == NULL) {
        int deviceApiLevel = android_get_device_api_level();
        __android_log_print(ANDROID_LOG_INFO, "Environ", "No environ found, creating...");
        if (deviceApiLevel <= 29 && !getenv("POJAV_FORCE_MALLOC_SYSTEM")) {
          pojav_environ = je_malloc(sizeof(struct pojav_environ_s));
        } else {
          pojav_environ = malloc(sizeof(struct pojav_environ_s));
        }
        assert(pojav_environ);
        memset(pojav_environ, 0 , sizeof(struct pojav_environ_s));
        if(asprintf(&strptr_env, "%p", pojav_environ) == -1) abort();
        setenv("POJAV_ENVIRON", strptr_env, 1);
        if (deviceApiLevel <= 29 && !getenv("POJAV_FORCE_MALLOC_SYSTEM")) {
           je_free(strptr_env);
        }else{
           free(strptr_env);
        }
    }else{
        __android_log_print(ANDROID_LOG_INFO, "Environ", "Found existing environ: %s", strptr_env);
        pojav_environ = (void*) strtoul(strptr_env, NULL, 0x10);
    }
    __android_log_print(ANDROID_LOG_INFO, "Environ", "%p", pojav_environ);
}
