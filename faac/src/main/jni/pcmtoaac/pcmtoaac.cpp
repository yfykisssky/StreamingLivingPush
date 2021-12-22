#include "pcmtoaac.h"
#include "tools.h"
#include <stdio.h>
#include <memory.h>
#include <android/log.h>

faacEncHandle encoder;
faacEncConfigurationPtr encodeConfig;
bool decoderIsInit = false;
unsigned long sampleRate;
unsigned char channels;
unsigned long maxOutputSamples;
unsigned long inputSamples;
unsigned long inputMaxBufferSize;

//pcm位深，用于计算一帧pcm大小
const int PCM_BITS_SIZE = 16;

int initEncoder(unsigned long sampleRate, int channels,int useBitRate) {

    encoder = faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputSamples);

    inputMaxBufferSize = inputSamples * maxOutputSamples / 8;

    LOGE("inputSamples:%d", inputSamples);
    LOGE("maxOutputSamples:%d", maxOutputSamples);
    LOGE("inputMaxBufferSize:%d", inputMaxBufferSize);

    encodeConfig = faacEncGetCurrentConfiguration(encoder);
    //设置AAC类型
    encodeConfig->aacObjectType = LOW;
    //是否允许一个声道为低频通道
    encodeConfig->useLfe = 0;
    //是否使用瞬时噪声定形滤波器
    encodeConfig->useTns = 0;
    //是否允许midSide coding
    encodeConfig->allowMidside = 0;
    //RAW_STREAM = 0, ADTS_STREAM=1
    encodeConfig->outputFormat = 0;
    //设置比特率
    encodeConfig->bitRate = useBitRate;
    //设置输入PCM格式
    encodeConfig->inputFormat = FAAC_INPUT_16BIT;

    faacEncSetConfiguration(encoder, encodeConfig);

    return inputMaxBufferSize;

}

int unInitEncoder() {
    faacEncClose(encoder);
    return 0;
}

int convertToAac(unsigned char *bufferPCM,
                 unsigned char *bufferAAC,
                 int bufferAacSize) {

    // 输入样本数，用实际读入字节数计算，一般只有读到文件尾时才不是nPCMBufferSize/(nPCMBitSize/8);
    //inputSamples = buf_sizePCM / (PCM_BITS_SIZE / 8);
    int ret = 0;
    //ret为0时不代表编码失败，而是编码速度较慢，导致缓存还未完全flush
    while (ret == 0) {
        ret = faacEncEncode(encoder, (int *) bufferPCM, inputSamples, bufferAAC, maxOutputSamples);
        LOGE("encode aac:%d", ret);
    }

    //ret>0 时表示编码成功，且返回值为编码后数据长度
    if (ret > 0) {
        LOGE("encode aac size:%d", ret);
    } else {
        LOGE("encode failed:%d", ret);
    }

    return ret;
}