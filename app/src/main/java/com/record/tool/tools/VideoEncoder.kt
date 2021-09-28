package com.record.tool.tools

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.record.tool.utils.PushLogUtils
import android.os.Bundle


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VideoEncoder {

    companion object {}

    private var codec: MediaCodec? = null
    private var isEncoding = false
    private var encodeStartTimeStamp = 0L

    private var encoderThread: Thread? = null

    private var dataCallBackListener: DataCallBackListener? = null
    private val vBufferInfo = MediaCodec.BufferInfo()

    private var iFrameInterval = 2

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long)
        fun onEncodeError()

        fun onLogTest(log: String)
    }

    //重置编码器参数
    fun resetEncoder(
        bitRate: Int,
        maxFps: Int,
        frameWith: Int,
        frameHeight: Int,
        gopTime: Int = 2
    ): Surface? {

        isEncoding = false
        encoderThread?.join()
        codec?.reset()

        val inputSurface = initEncoder(bitRate, maxFps, frameWith, frameHeight, gopTime)
        startEncode()

        return inputSurface
    }

    fun initEncoder(
        bitRate: Int,
        maxFps: Int,
        frameWith: Int,
        frameHeight: Int,
        gopTime: Int = 2
    ): Surface? {

        iFrameInterval = gopTime

        try {
            val format = MediaFormat()
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            format.setInteger(MediaFormat.KEY_WIDTH, frameWith)
            format.setInteger(MediaFormat.KEY_HEIGHT, frameHeight)

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
        encoderThread = Thread(EncodeRunnable())
        encoderThread?.start()
    }

    fun stopEncode() {
        isEncoding = false
        encoderThread?.join()
        codec?.release()
    }

    private inner class EncodeRunnable : Runnable {

        fun handlePts(): Long {
            if (vBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                return Long.MIN_VALUE
            }
            //微秒to毫秒
            val ptsNew = (vBufferInfo.presentationTimeUs / 1000)
            PushLogUtils.logVideoTimeStamp(ptsNew)
            return ptsNew
        }

        //设置I帧,主动设置
        private fun needSetIFrame() {
            val currentTimeStamp = System.currentTimeMillis()
            if ((encodeStartTimeStamp - currentTimeStamp) >= (iFrameInterval * 1000)) {
                val params = Bundle()
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                codec?.setParameters(params)
                encodeStartTimeStamp = currentTimeStamp
            }
        }

        override fun run() {

            try {
                codec?.start()
                encodeStartTimeStamp = System.currentTimeMillis()
                while (isEncoding) {
                    //synchronized(encoderRuningLock) {
                    /*timeoutUs：用于等待返回可用buffer的时间
                  timeoutUs == 0立马返回
                  timeoutUs < 0无限期等待可用buffer
                  timeoutUs > 0等待timeoutUs时间*/
                    needSetIFrame()

                    val outputBufferId = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                    if (outputBufferId >= 0) {

                        codec?.getOutputBuffer(outputBufferId)?.let { encodedData ->

                            val dataToWrite = ByteArray(vBufferInfo.size)
                            encodedData[dataToWrite, 0, vBufferInfo.size]
                            val pts = handlePts()
                            PushLogUtils.encode(dataToWrite.size)
                            dataCallBackListener?.onDataCallBack(dataToWrite, pts)
                            codec?.releaseOutputBuffer(outputBufferId, false)

                        }
                    }
                }
                codec?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
                codec?.stop()
                codec?.release()
                dataCallBackListener?.onEncodeError()
                dataCallBackListener?.onLogTest(e.message ?: "")
            } finally {
            }

        }
    }

}