#include <jni.h>
#include "tools.h"
#include "tools.c"

#include <x264.h>
extern "C"
JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_test(JNIEnv *env, jclass clazz) {
x264_zone_t *p;
x264_picture_t *c;
LOGE("XXXXXXXXXX");
}
