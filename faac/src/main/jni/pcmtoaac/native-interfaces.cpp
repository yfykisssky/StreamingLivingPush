#include "pcmtoaac.h"
#include "pcmtoaac.cpp"
#include <jni.h>

unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength(array);
    auto *buf = new unsigned char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}

jbyteArray as_byte_array(JNIEnv *env, unsigned char *buf, int len) {
    jbyteArray array = env->NewByteArray(len);
    env->SetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_living_faac_AccFaacNativeJni_startFaacEngine(JNIEnv *env, jclass clazz) {

}