#include <jni.h>
#include "packt.h"

Pusher *pusher = 0;

JNIEXPORT jboolean JNICALL
Java_com_living_rtmp_RtmpNativeJni_connect(JNIEnv *env, jobject obj, jstring url_) {
    const char *url = (*env)->GetStringUTFChars(env, url_, 0);
    int ret;
    do {
        pusher = malloc(sizeof(Pusher));
        memset(pusher, 0, sizeof(Pusher));
        pusher->rtmp = RTMP_Alloc();
        RTMP_Init(pusher->rtmp);
        pusher->rtmp->Link.timeout = 10;
        LOGI("connect %s", url);
        if (!(ret = RTMP_SetupURL(pusher->rtmp, url))) break;
        RTMP_EnableWrite(pusher->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(pusher->rtmp, 0))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(pusher->rtmp, 0))) break;
        LOGI("connect success");
    } while (0);
    (*env)->ReleaseStringUTFChars(env, url_, url);
    return ret;
}

JNIEXPORT jboolean JNICALL
Java_com_living_rtmp_RtmpNativeJni_isConnect(JNIEnv *env, jobject obj) {
    return pusher && pusher->rtmp && RTMP_IsConnected(pusher->rtmp);
}

JNIEXPORT void JNICALL
Java_com_living_rtmp_RtmpNativeJni_disConnect(JNIEnv *env, jobject obj) {
    if (pusher) {
        if (pusher->sps) {
            free(pusher->sps);
        }
        if (pusher->pps) {
            free(pusher->pps);
        }
        if (pusher->rtmp) {
            RTMP_Close(pusher->rtmp);
            RTMP_Free(pusher->rtmp);
        }
        free(pusher);
        pusher = 0;
    }
}

int sendPacket(RTMPPacket *packet) {
    int r = RTMP_SendPacket(pusher->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    free(packet);
    return r;
}

JNIEXPORT  jboolean JNICALL
Java_com_living_rtmp_RtmpNativeJni_sendVideoData(JNIEnv *env, jobject obj, jbyteArray data_,
                                                 jint len, jlong tms) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);
    int ret = 0;
    do {
        if (data[4] == 0x67) {//sps pps
            if (pusher && (!pusher->pps || !pusher->sps)) {
                parseVideoConfiguration(data, len, pusher);
            }
        } else {
            if (data[4] == 0x65) {//关键帧
                RTMPPacket *packet = packetVideoDecode(pusher);
                if (!(ret = sendPacket(packet))) {
                    break;
                }
            }
            RTMPPacket *packet = packetVideoData(data, len, tms, pusher);
            ret = sendPacket(packet);
        }
    } while (0);
    (*env)->ReleaseByteArrayElements(env, data_, data, 0);
    return ret;
}

JNIEXPORT  jboolean JNICALL
Java_com_living_rtmp_RtmpNativeJni_sendAudioData(JNIEnv *env, jobject obj, jbyteArray data_,
                                                 jint len, jlong tms) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);
    RTMPPacket *packet = packetAudioData(data, len, tms, pusher);
    int ret = sendPacket(packet);
    (*env)->ReleaseByteArrayElements(env, data_, data, 0);
    return ret;
}


