LOCAL_PATH := $(call my-dir)
HERE_PATH := $(LOCAL_PATH)

# include $(HERE_PATH)/crash_dump/libbase/Android.mk
# include $(HERE_PATH)/crash_dump/libbacktrace/Android.mk
# include $(HERE_PATH)/crash_dump/debuggerd/Android.mk

include $(CLEAR_VARS)
LOCAL_MODULE := log_stub
LOCAL_SRC_FILES := log_stub.cpp
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -U_GNU_SOURCE -U__USE_GNU -std=c++17
include $(BUILD_STATIC_LIBRARY)

LOCAL_PATH := $(HERE_PATH)

$(call import-module,prefab/bytehook)
LOCAL_PATH := $(HERE_PATH)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -ldl -landroid                         # 移除了 -llog
LOCAL_MODULE := pojavexec
LOCAL_SHARED_LIBRARIES := driver_helper
LOCAL_STATIC_LIBRARIES := log_stub                     # 新增
LOCAL_CFLAGS += -rdynamic
LOCAL_SRC_FILES := \
    bigcoreaffinity.c \
    egl_bridge.c \
    ctxbridges/br_loader.c \
    ctxbridges/gl_bridge.c \
    ctxbridges/osm_bridge.c \
    ctxbridges/gl_loader.c \
    ctxbridges/egl_loader.c \
    ctxbridges/osmesa_loader.c \
    ctxbridges/swap_interval_no_egl.c \
    ctxbridges/virgl_bridge.c \
    environ/environ.c \
    logger/logger.c \
    input_bridge_v3.c \
    jre_launcher.c \
    utils.c \
    stdio_is.c \
    java_exec_hooks.c \
    lwjgl_dlopen_hook.c

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_CFLAGS += -DADRENO_POSSIBLE -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -D_GNU_SOURCE -U__USE_GNU -std=c23
LOCAL_LDLIBS += -lEGL -lGLESv2
endif
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -ldl                                        # 移除了 -llog
LOCAL_MODULE := vulkan_check
LOCAL_SHARED_LIBRARIES := driver_helper
LOCAL_STATIC_LIBRARIES := log_stub                          # 新增
LOCAL_SRC_FILES := vulkan_checker.c
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -D_GNU_SOURCE -U__USE_GNU -std=c23
include $(BUILD_SHARED_LIBRARY)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_CFLAGS += -DADRENO_POSSIBLE
endif

include $(CLEAR_VARS)
LOCAL_MODULE := exithook
LOCAL_LDLIBS := -ldl                                        # 移除了 -llog
LOCAL_SHARED_LIBRARIES := bytehook pojavexec
LOCAL_STATIC_LIBRARIES := log_stub                          # 新增
LOCAL_SRC_FILES := exit_hook.c
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -D_GNU_SOURCE -U__USE_GNU -std=c23
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -ldl -landroid                              # 移除了 -llog
LOCAL_MODULE := driver_helper
LOCAL_STATIC_LIBRARIES := log_stub                          # 新增
LOCAL_SRC_FILES := \
    driver_helper/driver_helper.c \
    driver_helper/nsbypass.c \
    driver_helper/arm64_func_locator.c \
    driver_helper/fake_dlfcn.c \
    driver_helper/func_locator.c

LOCAL_CFLAGS += -g -rdynamic
LOCAL_CFLAGS += -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -U_GNU_SOURCE -D__USE_GNU -std=c23

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_CFLAGS += -DADRENO_POSSIBLE
LOCAL_LDLIBS += -lEGL -lGLESv3
endif
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := linkerhook
LOCAL_SRC_FILES := \
    linkerhook/linkerhook.cpp \
    linkerhook/linkerns.c
LOCAL_LDFLAGS := -z global
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -D_GNU_SOURCE -U__USE_GNU
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := pojavexec_awt
LOCAL_SRC_FILES := \
    awt_bridge.c
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -U_GNU_SOURCE -U__USE_GNU -std=c23
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := awt_headless
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(HERE_PATH)/awt_xawt
include $(CLEAR_VARS)
LOCAL_MODULE := awt_xawt
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SHARED_LIBRARIES := awt_headless
LOCAL_SRC_FILES := xawt_fake.c
LOCAL_CFLAGS += -O2 -D_FILE_OFFSET_BITS=64 -D_ALLBSD_SOURCE -DLINUX -D__USE_BSD -DANDROID -U_GNU_SOURCE -U__USE_GNU -std=c23
include $(BUILD_SHARED_LIBRARY)
