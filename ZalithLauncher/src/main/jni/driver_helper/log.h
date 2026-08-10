//
// Created by maks on 30.06.2026.
//

#ifndef POJAVLAUNCHER_LOG_H
#define POJAVLAUNCHER_LOG_H

#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#endif //POJAVLAUNCHER_LOG_H
