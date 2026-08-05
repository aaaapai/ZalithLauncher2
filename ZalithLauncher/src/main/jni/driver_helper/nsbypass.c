//
// Created by maks on 05.06.2023.
//
#include "nsbypass.h"
#include "android_namespace_func.h"
#include <dlfcn.h>
#include <android/dlext.h>
#include <android/log.h>
#include <sys/mman.h>
#include <sys/user.h>
#include <string.h>
#include <stdio.h>
#include <linux/limits.h>
#include <errno.h>
#include <unistd.h>
#include <asm/unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <elf.h>
#include "elf_defs.h"
#include <inttypes.h>

#define TAG __FILE_NAME__
#include "log.h"

/* Library search path */
#ifdef PLATFORM_64
#define SEARCH_PATH "/system/lib64"
#else
#define SEARCH_PATH "/system/lib"
#endif

static struct android_namespace_t* driver_namespace = nullptr;

// 原始 android_dlopen_ext 函数指针（绕过钩子）
static void* (*orig_android_dlopen_ext)(const char*, int, const android_dlextinfo*) = nullptr;

static void init_orig_android_dlopen_ext() {
    if (orig_android_dlopen_ext) return;
    // 尝试从 libdl_android.so 获取（如果已加载）
    void* dl_android = dlopen("libdl_android.so", RTLD_NOLOAD);
    if (dl_android) {
        orig_android_dlopen_ext = (void* (*)(const char*, int, const android_dlextinfo*))
            dlsym(dl_android, "android_dlopen_ext");
    }
    // 若失败，回退到 RTLD_NEXT
    if (!orig_android_dlopen_ext) {
        orig_android_dlopen_ext = (void* (*)(const char*, int, const android_dlextinfo*))
            dlsym(RTLD_NEXT, "android_dlopen_ext");
    }
    if (!orig_android_dlopen_ext) {
        LOGE("Failed to locate original android_dlopen_ext");
    }
}

bool linker_ns_load(const char* lib_search_path) {
    if(driver_namespace != nullptr) return true;
    android_ldfuncs_t ldfuncs;
    if(!locate_namespace_funcs(&ldfuncs)) {
        return false;
    }

    char full_path[strlen(SEARCH_PATH) + strlen(lib_search_path) + 2 + 1];
    sprintf(full_path, "%s:%s", SEARCH_PATH, lib_search_path);
    driver_namespace = ldfuncs.create_namespace("pojav-driver",
                                                      full_path,
                                                      full_path,
                                                      3 /* TYPE_SHAFED | TYPE_ISOLATED */,
                                                      "/system/:/system_ext/:/data/:/vendor/:/apex/:/dev", nullptr);
    if (driver_namespace == nullptr) {
        LOGI("Failed to create namespace");
        ldfuncs.close(ldfuncs.dl_handle);
        return false;
    }

    ldfuncs.link_namespaces(driver_namespace, nullptr, "ld-android.so");
    ldfuncs.link_namespaces(driver_namespace, nullptr, "libnativeloader.so");
    ldfuncs.link_namespaces(driver_namespace, nullptr, "libnativeloader_lazy.so");
    ldfuncs.close(ldfuncs.dl_handle);
    return true;
}

void* linker_ns_dlopen(const char* name, int flag) {
    if (driver_namespace == nullptr) {
        LOGI("Namespace not initialized, using dlopen fallback for %s", name);
        return dlopen(name, flag);
    }
    init_orig_android_dlopen_ext();
    if (!orig_android_dlopen_ext) {
        LOGE("No original android_dlopen_ext available");
        return nullptr;
    }
    android_dlextinfo dlextinfo;
    dlextinfo.flags = ANDROID_DLEXT_USE_NAMESPACE;
    dlextinfo.library_namespace = driver_namespace;
    void* handle = orig_android_dlopen_ext(name, flag, &dlextinfo);
    if (!handle) {
        LOGE("android_dlopen_ext failed for %s: %s", name, dlerror());
    }
    return handle;
}

bool patch_elf_soname(int patchfd, int realfd, size_t size, const char* patchname) {
    char* target = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED, patchfd, 0);
    if(!target) return false;
    if(read(realfd, target, size) != size) goto fail;

    ELF_EHDR *ehdr = (ELF_EHDR*)target;
    ELF_SHDR *shdr = (ELF_SHDR*)(target + ehdr->e_shoff);
    for(ELF_HALF i = 0; i < ehdr->e_shnum; i++) {
        ELF_SHDR *hdr = &shdr[i];
        if(hdr->sh_type == SHT_DYNAMIC) {
            char* strtab = target + shdr[hdr->sh_link].sh_offset;
            ELF_DYN *dynEntries = (ELF_DYN*)(target + hdr->sh_offset);
            for(ELF_XWORD k = 0; k < (hdr->sh_size / hdr->sh_entsize); k++) {
                ELF_DYN* dynEntry = &dynEntries[k];
                if(dynEntry->d_tag == DT_SONAME) {
                    char* soname = strtab + dynEntry->d_un.d_val;
                    size_t soname_len = strlen(soname);
                    size_t patchname_len = strlen(patchname);
                    if(patchname_len != soname_len) goto fail;
                    strcpy(soname, patchname);
                    munmap(target, size);
                    return true;
                }
            }
        }
    }

    fail:
    munmap(target, size);
    return false;
}

#define PAGE_ALIGN(addr)        (((addr)+pagesize-1)&(~(pagesize-1)))

void* linker_ns_dlopen_unique(const char* tmpdir, const char* name, const char* patch_name, int flags) {
    if (driver_namespace == nullptr) {
        LOGI("Namespace not initialized, using dlopen fallback for %s", name);
        return dlopen(name, flags);
    }
    init_orig_android_dlopen_ext();
    if (!orig_android_dlopen_ext) {
        LOGE("No original android_dlopen_ext available");
        return nullptr;
    }

    int pagesize = getpagesize();
    char pathbuf[PATH_MAX];
    static uint16_t patchid;
    int patch_fd, real_fd;
    size_t fsize, totalsize;

    snprintf(pathbuf, PATH_MAX, "%s/%s", SEARCH_PATH, name);
    real_fd = open(pathbuf, O_RDONLY);
    if(real_fd == -1) {
        LOGE("Failed to open %s from system: %s", pathbuf, strerror(errno));
        return nullptr;
    }

    {
        struct stat64 real_stat;
        if (fstat64(real_fd, &real_stat)) {
            LOGE("fstat64 failed for %s: %s", pathbuf, strerror(errno));
            goto fail_real;
        }
        fsize = real_stat.st_size;
        totalsize = PAGE_ALIGN(fsize);
    }

    patch_fd = (int) syscall(__NR_memfd_create, patch_name, MFD_CLOEXEC);
    if(patch_fd == -1) {
        snprintf(pathbuf, PATH_MAX, "%s/%"PRIu16"", tmpdir, patchid++);
        patch_fd = open(pathbuf, O_CREAT | O_RDWR, S_IRUSR | S_IWUSR);
    }
    if(patch_fd == -1) {
        LOGE("Failed to create patch fd: %s", strerror(errno));
        goto fail_real;
    }

    if(ftruncate64(patch_fd, totalsize) == -1) {
        LOGE("ftruncate64 failed: %s", strerror(errno));
        goto fail_both;
    }

    bool patch_result = patch_elf_soname(patch_fd, real_fd, fsize, patch_name);
    close(real_fd);
    if(!patch_result) {
        LOGE("Failed to patch SONAME of %s", name);
        close(patch_fd);
        return nullptr;
    }

    android_dlextinfo extinfo;
    extinfo.flags = ANDROID_DLEXT_USE_NAMESPACE | ANDROID_DLEXT_USE_LIBRARY_FD;
    extinfo.library_fd = patch_fd;
    extinfo.library_namespace = driver_namespace;
    return orig_android_dlopen_ext(patch_name, flags, &extinfo);

    fail_both:
    close(patch_fd);
    fail_real:
    close(real_fd);
    return nullptr;
}
