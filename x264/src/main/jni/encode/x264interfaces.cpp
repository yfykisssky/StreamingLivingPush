#include <jni.h>
#include "tools.h"
#include "tools.c"
#include <malloc.h>
#include <leak/include/MemoryTrace.hpp>
#include "x264encoder.h"

extern "C" {

X264Encoder x264Encoder;

JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_initEncoder(JNIEnv *env, jclass clazz) {

    leaktracer::MemoryTrace::GetInstance().startMonitoringAllThreads();
    bool initX264Encoder = x264Encoder.openX264Encoder();
    LOGE("init %d", initX264Encoder);

}

JNIEXPORT void JNICALL
Java_com_living_x264_X264NativeJni_destoryEncoder(JNIEnv
                                                  *env,
                                                  jclass clazz
) {
    //x264Encoder.closeX264Encoder();
    leaktracer::MemoryTrace::GetInstance().stopAllMonitoring();
    LOGE("To writeLeaksToFile %s.", "/leaks.out");
    leaktracer::MemoryTrace::GetInstance().writeLeaksToFile("/data/user/0/com.living.streamlivingpush/files/leaks.out");
    LOGE("To writeLeaksToFilesssss %s.", "/leaks.out");
}

JNIEXPORT jbyteArray JNICALL
Java_com_living_x264_X264NativeJni_nv21EncodeToH264(JNIEnv *env, jclass clazz,
                                                    jbyteArray nv21_bytes) {
    //todo:这里好像有内存泄漏导致OOM
    unsigned char *array = as_unsigned_char_array(env, nv21_bytes);
    uint8_t *bufData = nullptr;
    int bufLen = x264Encoder.x264EncoderProcess(array, &bufData);
    jbyteArray retArray = as_byte_array(env, bufData, bufLen);

    delete []array;
    delete []bufData;

    return retArray;

}

JNIEXPORT jbyteArray JNICALL
Java_com_living_x264_X264NativeJni_getHeaders(JNIEnv *env, jclass clazz) {

    uint8_t *bufData = nullptr;
    int bufLen = x264Encoder.getX264Headers(&bufData);
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
