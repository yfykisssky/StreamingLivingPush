#include "x264encoder.h"

X264Encoder::X264Encoder() {

    qp_max = 0;
    qp_min = 0;
    i_fps = 0;
    width = 0;
    height = 0;

    pParameter = nullptr;
    x264EncoderHandle = nullptr;

    pPicture = nullptr;
    pOutput = nullptr;
}

X264Encoder::~X264Encoder() {
    this->closeX264Encoder();
}

void X264Encoder::setResolution(int w, int h) {
    width = w;
    height = h;
}

void X264Encoder::setBitrate(int i_bitrate) {
    this->bitrateVideo = i_bitrate / 1024;
}

void X264Encoder::setFps(int fps) {
    i_fps = fps;
}

void X264Encoder::setQp_Max(unsigned int qp_max) {
    this->qp_max = qp_max;
}

void X264Encoder::setQp_Min(unsigned int qp_min) {
    this->qp_min = qp_min;
}

bool X264Encoder::openX264Encoder() {

    this->closeX264Encoder();

    if (!updateSettings()) {
        return false;
    }

    if (!x264EncoderHandle) {

        x264EncoderHandle = x264_encoder_open(pParameter);

        if (!x264EncoderHandle) {
            closeWithError("open_encoder");
            return false;
        }
    }

    if (!createInPic()) {
        return false;
    }
    if (!createOutPic()) {
        return false;
    }

    return true;
}

void X264Encoder::configParams() {

    //指明作用的level值，可能与编码复杂度有关
    pParameter->i_level_idc = 30;
    //宽度高度
    pParameter->i_width = width;
    pParameter->i_height = height;
    //是否允许非确定性时线程优化
    pParameter->b_deterministic = 1;
    //i_threads = N并行编码的时候如果b_sliced_threads=1那么是并行slice编码，
    //如果b_sliced_threads=0，那么是并行frame编码。并行slice无延时，并行frame有延时
    pParameter->i_threads = 1;
    //编码比特流的CSP，仅支持i420，色彩空间设置
    pParameter->i_csp = X264_CSP_I420;
    //帧率的分子
    pParameter->i_fps_num = i_fps;
    //帧率的分母
    pParameter->i_fps_den = 1;
    //两个参考帧之间的B帧数目
    pParameter->i_bframe = 0;
    //将I帧拆成一个一个I块，然后放到P帧中
    pParameter->b_intra_refresh = 0;
    //多少帧一个I帧
    pParameter->i_keyint_max = i_fps * 2;
    pParameter->i_keyint_min = i_fps * 2;
    //重复SPS/PPS 放到关键帧前面
    pParameter->b_repeat_headers = 0;
    //码率控制方式
    //CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    pParameter->rc.i_rc_method = X264_RC_CRF;
    //1pass VBR, nominal QP. 实际质量，值越大图像越花,越小越清晰
    pParameter->rc.f_rf_constant = 25;
    //最大码率因子，该选项仅在使用CRF并开启VBV时有效，
    //图像质量的最大值，可能会导致VBV下溢
    //pParameter->rc.f_rf_constant_max = 45;
    //码率,单位kbps
    pParameter->rc.i_bitrate = bitrateVideo;

    //是否开启基于macroblock的qp控制方法
    //这个不为0,将导致编码延时帧
    pParameter->rc.b_mb_tree = 0;

    //允许的误差
    pParameter->rc.f_rate_tolerance = 0.1;
    //平均码率模式下，最大瞬时码率，默认0
    pParameter->rc.i_vbv_max_bitrate = (int) (bitrateVideo * 1.2);


    //baseline模式无效
    /*   pParameter->analyse.b_transform_8x8 = 0;
       pParameter->rc.f_aq_strength = 1.5;

       pParameter->rc.i_aq_mode = 0;
       pParameter->rc.f_qcompress = 0.0;
       pParameter->rc.f_ip_factor = 0.5;
       pParameter->rc.f_rate_tolerance = 0.1;

       pParameter->i_slice_max_size = 1200;

       pParameter->b_deblocking_filter = 1;
       pParameter->i_deblocking_filter_alphac0 = 4;
       pParameter->i_deblocking_filter_beta = 4;

       pParameter->analyse.i_direct_mv_pred = X264_DIRECT_PRED_AUTO;
       pParameter->analyse.i_me_method = X264_ME_DIA;
       pParameter->analyse.i_me_range = 16;
       pParameter->analyse.i_subpel_refine = 2;
       pParameter->analyse.i_noise_reduction = 1;*/

    pParameter->i_log_level = X264_LOG_NONE;

}

bool X264Encoder::updateSettings() {

    if (!pParameter) {
        pParameter = (x264_param_t *) malloc(sizeof(x264_param_t));

        if (!pParameter) {
            closeWithError("malloc_param");
            return false;
        }

        memset(pParameter, 0, sizeof(x264_param_t));
    }

    /*
    * 获取 x264 编码器参数
    * int x264_param_default_preset( x264_param_t *, const char *preset, const char *tune )
    * 参数一 : x264_param_t * : x264 编码参数指针
    *
    * 参数二 : const char *preset : 设置编码速度, 这里开发直播, 需要尽快编码推流,
    * 这里设置最快的速度 ultrafast, 字符串常量, 值从 下面的参数中选择 ;
    * static const char * const x264_preset_names[] = { "ultrafast", "superfast", "veryfast",
    * "faster", "fast", "medium", "slow", "slower", "veryslow", "placebo", 0 };
    *
    * 参数三 : const char *tune : 视频编码场景设置, 这里选择 zerolatency 无延迟编码
    * static const char * const x264_tune_names[] = { "film", "animation", "grain",
    * "stillimage", "psnr", "ssim", "fastdecode", "zerolatency", 0 };
    *
    * 编码速度快, 意味着牺牲了画面的质量
    */
    int ret = x264_param_default_preset(pParameter, "ultrafast", "zerolatency");
    if (ret != 0) {
        closeWithError("default_preset");
        return false;
    }

    configParams();

    if (x264_param_apply_profile(pParameter, "baseline")) {
        closeWithError("apply_profile");
        return false;
    }

    return true;
}

bool X264Encoder::createInPic() {
    if (!pPicture) {

        pPicture = (x264_picture_t *) malloc(sizeof(x264_picture_t));

        if (!pPicture) {
            closeWithError("create_in_pic");
            return false;
        }

        memset(pPicture, 0, sizeof(x264_picture_t));
    }

    if (x264_picture_alloc(pPicture, X264_CSP_I420, width, height)) {
        closeWithError("alloc_in_pic");
        return false;
    }
    return true;
}

bool X264Encoder::createOutPic() {
    if (!pOutput) {
        pOutput = (x264_picture_t *) malloc(sizeof(x264_picture_t));

        if (!pOutput) {
            closeWithError("create_out_pic");
            return false;
        }

        memset(pOutput, 0, sizeof(x264_picture_t));
    }
    return true;
}

void X264Encoder::closeWithError(char *str) {
    this->closeX264Encoder();
    LOGE("close with error:%s", str);
}

bool X264Encoder::closeX264Encoder() {
    if (pPicture) {
        x264_picture_clean(pPicture);
        delete pPicture;
        pPicture = nullptr;
    }
    if (pOutput) {
        x264_picture_clean(pOutput);
        delete pOutput;
        pOutput = nullptr;
    }
    return true;
}

int X264Encoder::getX264Headers(uint8_t **outBuf) {
    x264_nal_t *nals;
    int nalsCount = 0;
    int ret = x264_encoder_headers(x264EncoderHandle, &nals, &nalsCount);
    if (ret < 0) {
        LOGE("encode header error:%d", ret);
        return -1;
    } else {
        LOGE("encode header pi_nal:%d", nalsCount);
        return getBytesFromNal(outBuf, nals, nalsCount);
    }
}

/*
    int x264_encoder_encode( x264_t *, x264_nal_t **pp_nal, int *pi_nal,
                             x264_picture_t *pic_in, x264_picture_t *pic_out );
    函数原型 :
        x264_t * 参数 : x264 视频编码器
        x264_nal_t **pp_nal 参数 : 编码后的帧数据, 可能是 1 帧, 也可能是 3 帧
        int *pi_nal 参数 : 编码后的帧数, 1 或 3
        x264_picture_t *pic_in 参数 : 输入的 NV21 格式的图片数据
        x264_picture_t *pic_out 参数 : 输出的图片数据

    普通帧 : 一般情况下, 一张图像编码出一帧数据, pp_nal 是一帧数据, pi_nal 表示帧数为 1
    关键帧 : 如果这个帧是关键帧, 那么 pp_nal 将会编码出 3 帧数据, pi_nal 表示帧数为 3
    关键帧数据 : SPS 帧, PPS 帧, 画面帧
 */
int X264Encoder::x264EncoderProcess(uint8_t *pSrcData, uint8_t **outBuf) {
    // 参数中的 data 是 NV21 格式的
    // 前面 YByteCount 字节个 Y 灰度数据
    // 之后是 UVByteCount 字节个 VU 数据交替存储
    // UVByteCount 字节 V 数据, UVByteCount 字节 U 数据

    // 从 Camera 采集的 NV21 格式的 data 数据中
    // 将 YUV 中的 Y 灰度值数据, U 色彩值数据, V 色彩饱和度数据提取出来
    memcpy(pPicture->img.plane[0], pSrcData, YByteCount);

    // 取出 NV21 数据中交替存储的 VU 数据
    // V 在前 ( 偶数位置 ), U 在后 ( 奇数位置 ), 交替存储
    for (int i = 0; i < UVByteCount; i++) {
        // U 色相 / 色彩值数据, 存储在 YByteCount 后的奇数索引位置
        *(pPicture->img.plane[1] + i) = *(pSrcData + YByteCount + i * 2 + 1);

        // V 色彩饱和度数据, 存储在 YByteCount 后的偶数索引位置
        *(pPicture->img.plane[2] + i) = *(pSrcData + YByteCount + i * 2);
    }
    x264_nal_t *nals;
    int nalsCount = 0;
    int ret = x264_encoder_encode(x264EncoderHandle, &nals, &nalsCount, pPicture, pOutput);
    if (ret < 0) {
        LOGE("encode error:%d", ret);
        return -1;
    } else {
        LOGE("encode pi_nal:%d", nalsCount);
        return getBytesFromNal(outBuf, nals, nalsCount);
    }

}

int X264Encoder::getBytesFromNal(uint8_t **outBuf, x264_nal_t *nals, int nalsCount) {
    int outBufSize = -1;
    for (int i = 0; i < nalsCount; i++) {
        outBufSize += nals[i].i_payload;
    }
    *outBuf = new uint8_t[outBufSize];
    memset(*outBuf, 0, outBufSize);
    uint8_t *tempData = *outBuf;
    for (int i = 0; i < nalsCount; i++) {
        if (nals[i].p_payload != nullptr) {
            memcpy(tempData, nals[i].p_payload, nals[i].i_payload);
            tempData += nals[i].i_payload;
        }
    }
    return outBufSize;
}
