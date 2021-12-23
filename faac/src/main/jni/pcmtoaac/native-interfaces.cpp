#include "pcmtoaac.h"
#include "pcmtoaac.cpp"
#include <jni.h>
#include "tools.h"
#include "tools.c"
#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_living_faac_AccFaacNativeJni_initFaacEngine(JNIEnv *env, jclass clazz, jlong sample_rate,
                                                      jint channels, jint bit_rate) {
    return initEncoder(sample_rate, channels, bit_rate);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_living_faac_AccFaacNativeJni_destoryFaacEngine(JNIEnv *env, jclass clazz) {
    unInitEncoder();
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_living_faac_AccFaacNativeJni_convertToAac(JNIEnv *env, jclass clazz,
                                                   jbyteArray pcm_bytes) {

    unsigned char aac_bytes[getMaxAacBytesSize()];
    unsigned char *pcm_array = as_unsigned_char_array(env, pcm_bytes);
    int aac_size = convertToAac(pcm_array, aac_bytes);
    unsigned char aac_bytes_result[aac_size];
    memcpy(aac_bytes_result, aac_bytes, aac_size);
    return as_byte_array(env, aac_bytes_result, aac_size);

}