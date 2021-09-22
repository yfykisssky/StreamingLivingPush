package com.record.tool.utils

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class H264CheckTool {

    companion object {

        private const val H264_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC

        //COLOR_FormatSurface
        private val h264SupportColor = listOf(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

        fun checkSupportHwEncoder(): Boolean {

            try {
                for (index in 0 until MediaCodecList.getCodecCount()) {
                    var mediaCodecInfo: MediaCodecInfo?
                    try {
                        mediaCodecInfo = MediaCodecList.getCodecInfoAt(index)
                    } catch (e: IllegalArgumentException) {
                        continue
                    }
                    if (mediaCodecInfo?.isEncoder == true) {

                        mediaCodecInfo.supportedTypes?.forEach { type ->
                            if (type == H264_TYPE) {
                                //COLOR_FormatSurface是否支持
                                val colorFromat = mediaCodecInfo.getCapabilitiesForType(H264_TYPE)
                                val colorList =
                                    colorFromat?.colorFormats?.intersect(h264SupportColor)
                                if (colorList?.isNotEmpty() == true) {
                                    return true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
            return false
        }


    }

}