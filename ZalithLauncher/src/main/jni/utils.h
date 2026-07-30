#pragma once

#include <stdbool.h>

#define CLIPBOARD_COPY 2000
#define CLIPBOARD_PASTE 2001
#define CLIPBOARD_OPEN 2002

#define TRY_ATTACH_ENV(env_name, vm, error_message, then) JNIEnv* env_name;\
do {                                                                       \
    env_name = get_attached_env(vm);                                       \
    if(env_name == NULL) {                                                 \
        printf(error_message);                                             \
        then                                                               \
    }                                                                      \
} while(0)

char** convert_to_char_array(JNIEnv *env, jobjectArray jstringArray);
jobjectArray convert_from_char_array(JNIEnv *env, char **charArray, int num_rows);
void free_char_array(JNIEnv *env, jobjectArray jstringArray, const char **charArray);
jstring convertStringJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jstring srcStr);

void hookExec();
void installLwjglDlopenHook();
void installEMUIIteratorMititgation();
JNIEXPORT jstring JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass clazz, jint action, jbyteArray copySrc);

JNIEnv* get_attached_env(JavaVM* jvm);
static bool notifyLauncher(JNIEnv *dvm_env, int type, int actions[], int len){
    jintArray actionArray = (*dvm_env)->NewIntArray(dvm_env, len);
    (*dvm_env)->SetIntArrayRegion(dvm_env, actionArray, 0, len, actions);
    return (*dvm_env)->CallStaticBooleanMethod(dvm_env, pojav_environ->bridgeClazz,
            pojav_environ->method_notifyLauncher, type, actionArray);
}
