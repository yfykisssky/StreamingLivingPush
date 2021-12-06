package com.living.streamlivingpush.base

import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.tools.AudioEncoder
import com.record.tool.tools.VideoEncoder
import com.record.tool.utils.EncodeControlUtils
import com.record.tool.utils.StateMonitorTool
import com.record.tool.utils.TransUtils

abstract class BaseStreamPushInstance {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
    }

    private var encodeVideoTool: VideoEncoder? = null
    private var encodeAudioTool: AudioEncoder? = null

    private var encoderMonitorTool = StateMonitorTool()

    protected var recordStateCallBack: RecordStateCallBack? = null

    enum class StateCode {
        SCREEN_REFUSED,
        SCREEN_STOP,
        AUDIO_ERROR,

        ENCODE_INIT_ERROR,
        ENCODE_ERROR,
    }

    interface RecordStateCallBack {
        fun onState(code: StateCode)
        fun onLog(log: String)
    }

    fun setRecordStateCallBackListener(recordStateCallBack: RecordStateCallBack?) {
        this.recordStateCallBack = recordStateCallBack
    }

    private fun initEncoder() {
        encodeVideoTool = VideoEncoder()
        encodeAudioTool = AudioEncoder()
    }

    private fun resetVideoEncodeSettings(
        bitRateVideo: Int,
        fps: Int
    ) {
        var setBit = bitRateVideo
        var setFps = fps
        encodeVideoTool?.checkCanSetBitRate(bitRateVideo)?.let { bitRate ->
            if (bitRate != 0) {
                setBit = bitRate
            }
        }
        encodeVideoTool?.updateResetEncodeSettings(
            TransUtils.kbps2bs(setBit),
            setFps
        )
        encodeVideoTool?.resetEncoder()
        encoderMonitorTool.updateTargetData(TransUtils.kbps2bs(setBit), setFps)
    }

    protected fun addVideoRenderFrame(frame: TextureVideoFrame) {
        encodeVideoTool?.addRenderFrame(frame)
    }

    protected fun addAudioRenderFrame(frame: RecordAudioFrame) {
        encodeAudioTool?.addFrameData(frame)
    }

    open fun initEncodeSettings(
        bitRateVideo: Int,
        fps: Int,
        screenWith: Int,
        screenHeight: Int,
        audioBitRate: Int
    ) {

        initEncoder()

        encodeVideoTool?.updateEncodeSettings(
            TransUtils.kbps2bs(bitRateVideo),
            fps,
            screenWith,
            screenHeight
        )
        encodeVideoTool?.initEncoder()

        encodeAudioTool?.initEncoder(audioBitRate)

        encoderMonitorTool.updateTargetData(TransUtils.kbps2bs(bitRateVideo), fps)
    }

    abstract fun onVideoFrameAva(frame: VideoFrame)
    abstract fun onAudioFrameAva(frame: AudioFrame)

    private fun startEncode() {
        encodeVideoTool?.setDataCallBackListener(object : VideoEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long) {
                val vFrame = VideoFrame()
                vFrame.byteArray = byteArray
                vFrame.timestamp = timeStamp

                onVideoFrameAva(vFrame)

                encoderMonitorTool.updateBitrate(byteArray?.size ?: 0)
                encoderMonitorTool.updateFpsCount()
            }

            override fun onEncodeError() {
                recordStateCallBack?.onState(StateCode.ENCODE_ERROR)
            }

            override fun onLogTest(log: String) {
                recordStateCallBack?.onLog(log)
            }
        })

        encodeVideoTool?.startEncode()


        encodeAudioTool?.setDataCallBackListener(object : AudioEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long, isHeader: Boolean) {
                val aFrame = AudioFrame()
                aFrame.isHeader = isHeader
                aFrame.byteArray = byteArray
                aFrame.timestamp = timeStamp

                onAudioFrameAva(aFrame)
            }

            override fun onEncodeError() {
                recordStateCallBack?.onState(StateCode.ENCODE_ERROR)
            }

            override fun onLogTest(log: String) {
                recordStateCallBack?.onLog(log)
            }
        })

        encodeAudioTool?.startEncode()

    }

    private fun startMonitor() {
        encoderMonitorTool.setCountCallBack(object : StateMonitorTool.CountCallBack {
            override fun onCount(bitRate: Int, fps: Int) {
                EncodeControlUtils.checkNeedReset(
                    bitRate,
                    encoderMonitorTool.tagBitRate,
                    encodeVideoTool?.getSetBitRate() ?: 0
                ).let {
                    if (it.first) {
                        //resetVideoEncodeSettings(it.second, 30)
                    }
                }
            }
        })

        encoderMonitorTool.startMonitor()
    }

    protected fun startPush() {
        startEncode()
        startMonitor()
    }


    protected fun stopPush() {

        encoderMonitorTool.stopMonitor()

        encodeVideoTool?.stopEncode()
        encodeVideoTool = null

        encodeAudioTool?.stopEncode()
        encodeAudioTool = null

    }

}