#include "pcmtoaac.h"
#include "pcmtoaac.cpp"
#include <jni.h>
#include "tools.h"
#include "tools.c"

extern "C"
JNIEXPORT void JNICALL
Java_com_living_faac_AccFaacNativeJni_startFaacEngine(JNIEnv *env, jclass clazz) {
    initEncoder(48000, 2,128000);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_living_faac_AccFaacNativeJni_stopFaacEngine(JNIEnv *env, jclass clazz) {
    unInitEncoder();
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_living_faac_AccFaacNativeJni_convertToAac(JNIEnv *env, jclass clazz,
                                                   jbyteArray pcm_bytes) {

    unsigned char aac_bytes[8192];
    unsigned char *pcm_array = as_unsigned_char_array(env, pcm_bytes);
    int aac_size=convertToAac(pcm_array, aac_bytes,0);
    unsigned char aac_bytes_result[aac_size];
    memcpy(aac_bytes_result, aac_bytes, aac_size);
    LOGE("AACC:%d",aac_size);
    return as_byte_array(env, aac_bytes_result, aac_size);

}

