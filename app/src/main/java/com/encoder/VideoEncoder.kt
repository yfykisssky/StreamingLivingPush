package com.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.record.tool.utils.PushLogUtils
import android.os.Bundle
import android.util.Range
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.ToSurfaceFrameRender


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VideoEncoder {

    companion object {}

    private var codec: MediaCodec? = null
    private var isEncoding = false
    private var encodeStartTimeStamp = 0L

    private var encodeControlStartTimeStamp = 0L

    private var encoderThread: Thread? = null

    private var dataCallBackListener: DataCallBackListener? = null
    private val vBufferInfo = MediaCodec.BufferInfo()

    private var iFrameInterval = 2
    private var bitRate = 0
    private var maxFps = 0
    private var frameWith = 0
    private var frameHeight = 0

    private var mCustomSurfaceRender: ToSurfaceFrameRender? = null

    //部分机型会jni crash,需要加黑名单
    private var canTrySetCrashFromats = false

    //crash
    private var tryCrashFromats = HashMap<String, Int>()

    init {
        //BITRATE_MODE_CBR输出码率恒定
        //BITRATE_MODE_CQ保证图像质量
        //BITRATE_MODE_VBR图像复杂则码率高，图像简单则码率低
        tryCrashFromats[MediaFormat.KEY_BITRATE_MODE] =
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
        tryCrashFromats[MediaFormat.KEY_COMPLEXITY] =
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR

        tryCrashFromats[MediaFormat.KEY_PROFILE] =
            MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
    }

    private fun createInputRender(outSurface: Surface) {
        if (mCustomSurfaceRender == null) {
            mCustomSurfaceRender = ToSurfaceFrameRender()
            mCustomSurfaceRender?.setOutPutSurface(outSurface, frameWith, frameHeight)
        }
    }

    private fun releaseInputRender() {
        mCustomSurfaceRender?.stop()
        mCustomSurfaceRender = null
    }

    private fun resetInputRender(outSurface: Surface) {
        mCustomSurfaceRender?.resetOutPutSurface(outSurface, frameWith, frameHeight)
    }

    fun addRenderFrame(textureFrame: TextureVideoFrame) {

        mCustomSurfaceRender?.setIsFlipHorizontal(textureFrame.extraHandle?.fillHorizontal ?: false)
        mCustomSurfaceRender?.onRenderVideoFrame(textureFrame)

    }

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long)
        fun onEncodeError()

        fun onLogTest(log: String)
    }

    //重置编码器参数
    fun resetEncoder(): Boolean {

        isEncoding = false
        encoderThread?.join()

        codec?.stop()
        codec?.reset()
        configEncoder()
        getEncoderSurface()?.let {
            resetInputRender(it)
            beginEncode()
            return true
        }

        return false
    }

    fun getSetBitRate(): Int {
        return bitRate
    }

    fun getSetFps(): Int {
        return maxFps
    }

    //修改编码宽高会导致花屏
    fun updateResetEncodeSettings(
        bitRate: Int,
        maxFps: Int
    ) {
        if (bitRate > 0) {
            this.bitRate = bitRate
        }
        if (maxFps > 0) {
            this.maxFps = maxFps
        }
    }

    fun updateEncodeSettings(
        bitRate: Int,
        maxFps: Int,
        frameWith: Int,
        frameHeight: Int,
        gopTime: Int = 2
    ) {
        this.bitRate = bitRate
        this.maxFps = maxFps
        this.frameWith = frameWith
        this.frameHeight = frameHeight
        iFrameInterval = gopTime
    }

    private fun configEncoder() {
        try {
            tryConfigCrashFormat()
            codec?.configure(getEncodeFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }
    }

    private fun getEncoderSurface(): Surface? {
        return codec?.createInputSurface()
    }

    fun initEncoder(): Boolean {

        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            configEncoder()
            getEncoderSurface()?.let {
                createInputRender(it)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }

        return false

    }

    private fun getSetBitRateRange(): Range<Int>? {
        return codec?.codecInfo?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities?.bitrateRange
    }

    //检查是否最大码率
    fun checkCanSetBitRate(setBit: Int): Int {
        var newBitRate = 0
        getSetBitRateRange()?.let {
            newBitRate = setBit
            if (newBitRate > it.upper) {
                newBitRate = it.upper
            }
            if (newBitRate < it.lower) {
                newBitRate = it.lower
            }
        }
        return newBitRate
    }

    private fun getEncodeFormat(): MediaFormat {
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

        tryCrashFromats.forEach { map ->
            format.setInteger(
                map.key,
                map.value
            )
        }

        //部分机型最大帧率无效
        //MediaFormat.KEY_MAX_FPS_TO_ENCODER
        return format
    }

    private fun tryConfigCrashFormat() {

        if (canTrySetCrashFromats) {
            val entries = tryCrashFromats.entries.iterator()
            while (entries.hasNext()) {
                val entry = entries.next()
                try {
                    val format = MediaFormat()
                    format.setInteger(
                        entry.key,
                        entry.value
                    )
                    codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    entries.remove()
                }
            }
        } else {
            tryCrashFromats.clear()
        }

    }

    fun startEncode() {
        encodeControlStartTimeStamp = System.currentTimeMillis()
        beginEncode()
    }

    private fun beginEncode() {
        isEncoding = true
        encoderThread = Thread(EncodeRunnable())
        encoderThread?.start()
    }

    fun stopEncode() {
        encodeControlStartTimeStamp = 0L
        encodeStartTimeStamp = 0L
        releaseInputRender()
        isEncoding = false
        encoderThread?.join()
        releaseEncoder()
    }

    private fun releaseEncoder() {
        codec?.stop()
        codec?.release()
    }

    private var iFrameReqSetListener: IFrameReqSetListener? = null

    interface IFrameReqSetListener {
        fun onIFrameReqSet(gopTime: Int): Boolean
    }

    fun setIFrameReqSetListener(iFrameReqSetListener: IFrameReqSetListener) {
        this.iFrameReqSetListener = iFrameReqSetListener
    }

    private inner class EncodeRunnable : Runnable {

        fun handlePts(): Long {
            if (encodeStartTimeStamp == 0L) {
                encodeStartTimeStamp = vBufferInfo.presentationTimeUs
            }
            val ptsNew = (vBufferInfo.presentationTimeUs - encodeStartTimeStamp) / 1000
            PushLogUtils.logVideoTimeStamp(ptsNew)
            return ptsNew
        }

        //设置I帧,主动设置
        private fun needSetIFrame(): Boolean {
            val currentTimeStamp = System.currentTimeMillis()
            return if ((currentTimeStamp - encodeControlStartTimeStamp) >= (iFrameInterval * 1000)) {
                val params = Bundle()
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                codec?.setParameters(params)
                encodeControlStartTimeStamp = currentTimeStamp
                true
            } else {
                false
            }
        }

        override fun run() {

            try {
                codec?.start()
                while (isEncoding) {
                    //synchronized(encoderRuningLock) {
                    /*timeoutUs：用于等待返回可用buffer的时间
                  timeoutUs == 0立马返回
                  timeoutUs < 0无限期等待可用buffer
                  timeoutUs > 0等待timeoutUs时间*/
                    val isIFrame = needSetIFrame()

                    if (isIFrame) {
                        //需要重置
                        if (iFrameReqSetListener?.onIFrameReqSet(iFrameInterval) == true) {
                            isEncoding = false
                        }
                    }

                    if (isEncoding) {
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

                }
            } catch (e: Exception) {
                e.printStackTrace()
                releaseEncoder()
                dataCallBackListener?.onEncodeError()
                dataCallBackListener?.onLogTest(e.message ?: "")
            } finally {
            }

        }
    }

}