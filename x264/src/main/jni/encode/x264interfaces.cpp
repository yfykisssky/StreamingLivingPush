#include <jni.h>
#include "tools.h"
#include "tools.c"
#include <malloc.h>
#include "x264encoder.h"

extern "C" {

X264Encoder x264Encoder;

JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_initEncoder(JNIEnv *env, jclass clazz) {

    bool initX264Encoder = x264Encoder.openX264Encoder();
    LOGE("init %d", initX264Encoder);

}

JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_destoryEncoder(JNIEnv
                                                  *env,
                                                  jclass clazz
) {
    x264Encoder.closeX264Encoder();
}

JNIEXPORT jbyteArray JNICALL
Java_com_living_x264_X264NativeJni_nv21EncodeToH264(JNIEnv *env, jclass clazz,
                                                    jbyteArray nv21_bytes) {

    unsigned char *array = as_unsigned_char_array(env, nv21_bytes);
    uint8_t *bufData = nullptr;
    int bufLen = x264Encoder.x264EncoderProcess(array, &bufData);
    delete[]array;
    if (bufLen != -1) {
        jbyteArray retArray = as_byte_array(env, bufData, bufLen);
        delete[]bufData;
        return retArray;
    } else {
        return nullptr;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_living_x264_X264NativeJni_getHeaders(JNIEnv *env, jclass clazz) {

    uint8_t *bufData = nullptr;
    int bufLen = x264Encoder.getX264Headers(&bufData);
    delete[]bufData;
    return as_byte_array(env, bufData, bufLen);

}

JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_updateSettings(JNIEnv *env, jclass clazz, jint bitrate, jint fps,
                                                  jint width, jint height) {
    x264Encoder.setBitrate(bitrate);
    x264Encoder.setResolution(width, height);
    x264Encoder.setFps(fps);

}

}
