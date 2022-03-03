package com.living.streamlivingpush.base

import android.os.*
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.video.gl.TextureVideoFrame
import com.encoder.AudioEncoder
import com.encoder.AudioSoftEncoder
import com.encoder.VideoEncoder
import com.encoder.VideoSoftEncoder
import com.record.tool.utils.*
import java.lang.ref.WeakReference

abstract class BaseStreamPushInstance {

    companion object {
        protected val TAG_NAME: String = this::class.java.simpleName

        private const val DEFAULT_VIDEO_GOP = 2


        private const val MSG_RESET_ENCODER = 1

        private const val KEY_RESET_ENCODER_BIT = "KEY_RESET_ENCODER_BIT"
        private const val KEY_RESET_ENCODER_FPS = "KEY_RESET_ENCODER_FPS"
    }

    private var encodeVideoTool: VideoSoftEncoder? = null
    private var encodeAudioTool: AudioSoftEncoder? = null

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
        encodeVideoTool = VideoSoftEncoder()
        encodeAudioTool = AudioSoftEncoder()

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
                            reference.resetRecordFpsSettings(fps)
                        }
                    }
                    else -> {
                    }
                }
            }

        }

    }

    abstract fun resetRecordFpsSettings(fps: Int)

    fun resetVideoEncodeSettings(
        bitRateVideo: Int,
        fps: Int
    ) {

        encodeVideoTool?.updateResetEncodeSettings(
            bitRateVideo,
            fps
        )

        encodeVideoTool?.resetEncoder()

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
            TransUtils.kbps2bps(bitRateVideo),
            fps,
            screenWith,
            screenHeight,
            DEFAULT_VIDEO_GOP
        )
        encodeVideoTool?.initEncoder()

        encodeAudioTool?.initEncoder(TransUtils.kbps2bps(audioBitRate))

        encoderMonitorTool.updateTargetData(TransUtils.kbps2bps(bitRateVideo), fps)
        encoderMonitorTool.updateTargetDataAudio(TransUtils.kbps2bps(audioBitRate))

        encodeControlTool.resetData()

    }

    abstract fun onVideoFrameAva(frame: VideoFrame)
    abstract fun onAudioFrameAva(frame: AudioFrame)

    private fun startEncode() {
        encodeVideoTool?.setDataCallBackListener(object : VideoSoftEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long) {
                val vFrame = VideoFrame()
                vFrame.byteArray = byteArray
                vFrame.timestamp = timeStamp

                onVideoFrameAva(vFrame)

                encodeControlTool.updateData(byteArray?.size ?: 0)

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

        encodeVideoTool?.setIFrameReqSetListener(object : VideoSoftEncoder.IFrameReqSetListener {

            override fun onIFrameReqSet(gopTime: Int): Boolean {
                return false
            }

        })

        encodeVideoTool?.startEncode()


        encodeAudioTool?.setDataCallBackListener(object : AudioSoftEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long) {
                val aFrame = AudioFrame()

                aFrame.byteArray = byteArray
                aFrame.timestamp = timeStamp

                onAudioFrameAva(aFrame)

                encoderMonitorTool.updateBitrateAudio(aFrame.byteArray?.size ?: 0)
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

    fun resetVideoBit(bitRate: Int) {
        val bitRateVideo = TransUtils.kbps2bps(bitRate)
        toResetVideoEncode(bitRateVideo,-1)
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