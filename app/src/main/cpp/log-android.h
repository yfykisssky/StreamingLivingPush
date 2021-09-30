

#include <string.h>
#include <android/log.h>
#include <malloc.h>
#include <stdbool.h>

#ifndef _LOG_H_
#define _LOG_H_

bool isDebug = true;

void OUT_LOG(const char *content) {
    if (!isDebug) {
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, "PushLogUtils:Native", "%s", content);
}
#endif

