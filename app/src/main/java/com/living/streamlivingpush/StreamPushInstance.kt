package com.living.streamlivingpush

import android.media.projection.MediaProjection
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.ScreenRecordManager
import com.record.tool.tools.AudioEncoder
import com.record.tool.tools.VideoEncoder
import com.rtmppush.tool.AudioFrame
import com.rtmppush.tool.RtmpPushTool
import com.rtmppush.tool.VideoFrame

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamPushInstance {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
        val instance: StreamPushInstance by lazy {
            StreamPushInstance()
        }
    }

    var isRecordAndEncoding = false
        private set

    private var encodeVideoTool: VideoEncoder? = null
    private var encodeAudioTool: AudioEncoder? = null

    private var rtmpPushTool: RtmpPushTool? = null

    private var recordStateCallBack: RecordStateCallBack? = null
    private var recordTool: ScreenRecordManager? = null

    enum class StateCode {

        SCREEN_REFUSED,
        SCREEN_STOP,
        AUDIO_ERROR,

        ENCODE_INIT_ERROR,
        ENCODE_ERROR,

        SERVICE_DISCON_ERROR
    }

    interface RecordStateCallBack {
        fun onState(code: StateCode)
        fun onLog(log: String)
    }

    fun setRecordStateCallBackListener(recordStateCallBack: RecordStateCallBack?) {
        this.recordStateCallBack = recordStateCallBack
    }

    fun initRecoderAndEncoder() {
        recordTool = ScreenRecordManager()
        encodeVideoTool = VideoEncoder()
        encodeAudioTool = AudioEncoder()
        rtmpPushTool = RtmpPushTool()
    }

    fun prepareRecord(
        bitRateVideo: Int,
        fps: Int,
        screenWith: Int,
        screenHeight: Int,
        audioBitRate: Int
    ) {
        //kbps to bits/sec
        val useBit = (bitRateVideo * 1024 / 8)
        val surface = encodeVideoTool?.initEncoder(useBit, fps, screenWith, screenHeight)

        if (surface == null) {
            recordStateCallBack?.onState(StateCode.ENCODE_INIT_ERROR)
            return
        }

        recordTool?.setEcodeInputSurface(
            surface,
            screenWith,
            screenHeight,
            fps
        )

        encodeAudioTool?.initEncoder(audioBitRate)

    }

    fun startRecordAndSendData(pushUrl: String) {

        if(TextUtils.isEmpty(pushUrl)){
            return
        }

        isRecordAndEncoding = true

        recordTool?.setDataRecordCallBack(object : ScreenRecordManager.DataRecordCallBack {

            override fun onAudioDataRecord(data: ByteArray?, byteSize: Int) {
                data?.let {
                    val frame = RecordAudioFrame()
                    frame.byteArray = data
                    frame.byteSize = byteSize
                    encodeAudioTool?.addFrameData(frame)
                }
            }

            override fun onErrorCode(code: StateCode) {
                if (code == StateCode.SERVICE_DISCON_ERROR) {
                    if (isRecordAndEncoding) {
                        recordStateCallBack?.onState(StateCode.SERVICE_DISCON_ERROR)
                    }
                } else {
                    recordStateCallBack?.onState(code)
                }
            }

            override fun onLogTest(log: String) {
                recordStateCallBack?.onLog(log)
            }

        })

        encodeVideoTool?.setDataCallBackListener(object : VideoEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long) {
                val vFrame = VideoFrame()
                vFrame.byteArray = byteArray
                vFrame.timestamp = timeStamp
                rtmpPushTool?.addVideoFrame(vFrame)
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
                rtmpPushTool?.addAudioFrame(aFrame)
            }

            override fun onEncodeError() {
                recordStateCallBack?.onState(StateCode.ENCODE_ERROR)
            }

            override fun onLogTest(log: String) {
                recordStateCallBack?.onLog(log)
            }
        })

        encodeAudioTool?.startEncode()

        recordTool?.reqRecordPerAndStart()

        rtmpPushTool?.startPushing(pushUrl)

    }

    fun hasPerAndStartRecord(projection: MediaProjection?) {
        if (projection == null) {
            recordStateCallBack?.onState(StateCode.SCREEN_REFUSED)
        } else {
            recordTool?.startRecording(projection)
        }
    }

    fun resumeRecording() {
        recordTool?.resumeRecording()
    }

    fun pauseRecording() {
        recordTool?.pauseRecording()
    }

    fun stopRecordAndDestory() {

        rtmpPushTool?.stopPushing()
        rtmpPushTool = null

        recordTool?.stopCapture()
        recordTool = null

        encodeVideoTool?.stopEncode()
        encodeVideoTool = null

        encodeAudioTool?.stopEncode()
        encodeAudioTool = null

        isRecordAndEncoding = false
    }

}