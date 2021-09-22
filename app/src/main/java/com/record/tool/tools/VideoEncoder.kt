package com.record.tool.tools

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.record.tool.utils.PushLogUtils

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VideoEncoder {

    companion object {
        private const val DEFAULT_I_FRAME_INTERVAL = 2 // seconds
        private const val REPEAT_FRAME_DELAY_US = 100000 // repeat after 100ms
    }

    private var codec: MediaCodec? = null
    private var isEncoding = false

    private var dataCallBackListener: DataCallBackListener? = null
    private val vBufferInfo = MediaCodec.BufferInfo()

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?,timeStamp:Long)
        fun onEncodeError()

        fun onLogTest(log: String)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun initEncoder(bitRate: Int, maxFps: Int, frameWith: Int, frameHeight: Int): Surface? {

        try {
            val format = MediaFormat()
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL)
            format.setInteger(MediaFormat.KEY_WIDTH, frameWith)
            format.setInteger(MediaFormat.KEY_HEIGHT, frameHeight)
            // display the very first frame, and recover from bad quality when no new frames
            format.setLong(
                MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
                REPEAT_FRAME_DELAY_US.toLong()
            ) // µs

            //部分机型最大帧率无效
            //MediaFormat.KEY_MAX_FPS_TO_ENCODER
            //crash
            //MediaFormat.KEY_PROFILE

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            return codec?.createInputSurface()
        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }

        return null

    }

    fun startEncode() {
        isEncoding = true
        EncodeThread().start()
    }

    fun stopEncode() {
        isEncoding = false
    }

    private inner class EncodeThread : Thread() {

        fun handlePts(): Long {
            if (vBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                return Long.MIN_VALUE
            }
            //微秒to毫秒
            val ptsNew = (vBufferInfo.presentationTimeUs / 1000)
            PushLogUtils.logVideoTimeStamp(ptsNew)
            return ptsNew
        }

        override fun run() {
            try {
                codec?.start()
                while (isEncoding) {
                    /*timeoutUs：用于等待返回可用buffer的时间
                      timeoutUs == 0立马返回
                      timeoutUs < 0无限期等待可用buffer
                      timeoutUs > 0等待timeoutUs时间*/
                    val outputBufferId = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                    if (outputBufferId >= 0) {

                        codec?.getOutputBuffer(outputBufferId)?.let { encodedData ->

                            val dataToWrite = ByteArray(vBufferInfo.size)
                            encodedData[dataToWrite, 0, vBufferInfo.size]
                            val pts = handlePts()
                            PushLogUtils.encode()
                            dataCallBackListener?.onDataCallBack(dataToWrite,pts)
                            codec?.releaseOutputBuffer(outputBufferId, false)

                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dataCallBackListener?.onEncodeError()
                dataCallBackListener?.onLogTest(e.message ?: "")
            } finally {
                codec?.stop()
                codec?.release()
            }
        }
    }

}