#ifndef STREAMLIVINGPUSH_X264ENCODER_H
#define STREAMLIVINGPUSH_X264ENCODER_H

#include "tools.h"
extern "C"{
#include <stdlib.h>
#include <string.h>
#include "x264.h"
#include "stdint.h"
}

class X264Encoder {
public:

    X264Encoder();

    ~X264Encoder();

    bool openX264Encoder();

    long x264EncoderProcess(uint8_t *pSrcData, int srcDataSize, x264_nal_t **nals, int& nalsCount);

    bool closeX264Encoder();

    void setResolution(int w, int h);

    void setBitrate(int i_bitrate);

    void setFps(int fps);

    void setQp_Max(unsigned int qp_max);

    void setQp_Min(unsigned int qp_min);

    long getX264Headers(x264_nal_t **nals, int &nalsCount);

private:

    x264_param_t *pParameter;
    x264_t *x264EncoderHandle;
    x264_picture_t *pPicture;
    x264_picture_t *pOutput;

    int YByteCount = 1280 * 720;
    int UVByteCount = YByteCount / 4;

    int bitrateVideo;
    int i_fps;
    int width;
    int height;
    unsigned int qp_max;
    unsigned int qp_min;

    void closeWithError(char *str);

    bool createInPic();

    bool createOutPic();

    bool updateSettings();

    void configParams();

};


#endif //STREAMLIVINGPUSH_X264ENCODER_H
