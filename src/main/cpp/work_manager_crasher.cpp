//
// Created by Viktor Shpyrka on 12/20/23.
//

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "SAMPLE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_example_workmanager_WorkManagerNativeCrasher_executeNativeCrash(JNIEnv *env,
jobject thiz) {
    LOGI("AAA Crashing the process");
    raise(SIGSEGV);
    LOGI("AAA SIGSEGV Sent");
}
