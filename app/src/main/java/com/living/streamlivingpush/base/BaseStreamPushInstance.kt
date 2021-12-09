package com.living.streamlivingpush.base

import android.os.*
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.tools.AudioEncoder
import com.record.tool.tools.VideoEncoder
import com.record.tool.utils.*
import java.lang.ref.WeakReference

abstract class BaseStreamPushInstance {

    companion object {
        private val TAG_NAME = this::class.java.simpleName

        private const val DEFAULT_VIDEO_GOP = 2


        private const val MSG_RESET_ENCODER = 1

        private const val KEY_RESET_ENCODER_BIT = "KEY_RESET_ENCODER_BIT"
        private const val KEY_RESET_ENCODER_FPS = "KEY_RESET_ENCODER_FPS"
    }

    private var encodeVideoTool: VideoEncoder? = null
    private var encodeAudioTool: AudioEncoder? = null

    private var encoderMonitorTool = StateMonitorTool()
    private var encodeControlTool = EncodeControlTool()

    protected var recordStateCallBack: RecordStateCallBack? = null

    private var streamPushHandlerThread: HandlerThread? = null

    @Volatile
    private var mHandleHandler: HandleHandler? = null

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

        streamPushHandlerThread = HandlerThread("StreamPushThread")
        streamPushHandlerThread?.start()
        mHandleHandler = streamPushHandlerThread?.looper?.let { HandleHandler(it, this) }
    }

    private class HandleHandler(looper: Looper, reference: BaseStreamPushInstance) :
        Handler(looper) {

        private val readerWeakReference = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            readerWeakReference.get()?.let { reference ->
                when (msg.what) {
                    MSG_RESET_ENCODER -> {
                        msg.data?.let { bundle ->
                            val bit = bundle.getInt(KEY_RESET_ENCODER_BIT)
                            val fps = bundle.getInt(KEY_RESET_ENCODER_FPS)
                            reference.resetVideoEncodeSettings(bit, fps)
                        }
                    }
                    else -> {
                    }
                }
            }

        }

    }

    fun resetVideoEncodeSettings(
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
            setBit,
            setFps
        )

        encodeVideoTool?.resetEncoder()

        PushLogUtils.logVideoResetTime(setBit)
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
            screenHeight,
            DEFAULT_VIDEO_GOP
        )
        encodeVideoTool?.initEncoder()

        encodeAudioTool?.initEncoder(TransUtils.kbps2bs(audioBitRate))

        encoderMonitorTool.updateTargetData(TransUtils.kbps2bs(bitRateVideo), fps)

        encodeControlTool.resetData()

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

                encodeControlTool.updateData(byteArray?.size ?: 0, 0)

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

        encodeVideoTool?.setIFrameReqSetListener(object : VideoEncoder.IFrameReqSetListener {

            override fun onIFrameReqSet(gopTime: Int): Boolean {
                val oldBit = encodeVideoTool?.getSetBitRate() ?: 1
                encodeControlTool.countGopTime()

                val gopCountTimes = encodeControlTool.getGopCountTimes()
                val newBit = EncodeControlUtils.checkNeedReset(
                    encodeControlTool.getCountBitRate() / (gopTime * gopCountTimes),
                    encoderMonitorTool.tagBitRate,
                    oldBit
                )
                return if (newBit > 0) {
                    encodeControlTool.resetData()

                    return if (EncodeControlUtils.checkCanChangeWithRange(
                            oldBit,
                            newBit
                        )
                    ) {
                        toResetVideoEncode(newBit, 30)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
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

    private fun toResetVideoEncode(bit: Int, fps: Int) {
        val msg = Message()
        val bundle = Bundle()
        bundle.putInt(KEY_RESET_ENCODER_BIT, bit)
        bundle.putInt(KEY_RESET_ENCODER_FPS, fps)
        msg.data = bundle
        msg.what = MSG_RESET_ENCODER
        mHandleHandler?.sendMessage(msg)
    }

    private var time = 0

    private fun startMonitor() {
        encoderMonitorTool.setCountCallBack(object : StateMonitorTool.CountCallBack {
            override fun onCount(bitRate: Int, fps: Int) {

            }
        })

        encoderMonitorTool.startMonitor()
    }

    protected fun startPush() {
        startEncode()
        startMonitor()
    }


    protected fun stopPush() {

        streamPushHandlerThread?.quit()

        encoderMonitorTool.stopMonitor()

        encodeVideoTool?.stopEncode()
        encodeVideoTool = null

        encodeAudioTool?.stopEncode()
        encodeAudioTool = null

    }

}