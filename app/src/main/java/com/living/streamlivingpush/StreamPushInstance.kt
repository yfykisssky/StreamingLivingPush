package com.living.streamlivingpush

import android.media.projection.MediaProjection
import android.os.Build
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.common.base.EventBusMsg
import com.record.tool.record.video.camera.CameraRecordManager
import com.record.tool.tools.AudioEncoder
import com.record.tool.tools.VideoEncoder
import com.record.tool.utils.StateMonitorTool
import com.rtmppush.tool.AudioFrame
import com.rtmppush.tool.RtmpPushTool
import com.rtmppush.tool.VideoFrame
import org.greenrobot.eventbus.EventBus

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

    private var encoderMonitorTool = StateMonitorTool()

    private var rtmpPushTool: RtmpPushTool? = null

    private var recordStateCallBack: RecordStateCallBack? = null
    // private var recordScreenTool: ScreenRecordManager? = null

    private var recordCameraTool: CameraRecordManager? = null


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

        //recordScreenTool = ScreenRecordManager()
        recordCameraTool = CameraRecordManager()

        encodeVideoTool = VideoEncoder()
        encodeAudioTool = AudioEncoder()
        rtmpPushTool = RtmpPushTool()
    }

    fun getView(): TextureView? {
        return recordCameraTool?.getPreviewView()
    }

    fun s() {
        recordCameraTool?.startPushImage()
    }

    fun r() {
        recordCameraTool?.stopPushImage()
    }

    fun toogle() {
        recordCameraTool?.toogleMirror()
    }

    fun switchCamera() {
        recordCameraTool?.switchCamera()
    }

    fun reset() {
        val sur = encodeVideoTool?.resetEncoder(8000000, 30, 1280, 720, 2)
        recordCameraTool?.resetEncodeSettings(sur, 1280, 720, 30)
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
        val surface = encodeVideoTool?.initEncoder(useBit, fps, screenHeight, screenWith)

        if (surface == null) {
            recordStateCallBack?.onState(StateCode.ENCODE_INIT_ERROR)
            return
        }

        /* recordScreenTool?.setEcodeInputSurface(
             surface,
             screenWith,
             screenHeight,
             fps
         )*/

        recordCameraTool?.setEcodeInputSurface(
            surface,
            screenWith,
            screenHeight,
            fps
        )

        encodeAudioTool?.initEncoder(audioBitRate)

    }

    fun startRecordAndSendData(pushUrl: String) {

        /* if (TextUtils.isEmpty(pushUrl)) {
             return
         }*/

        isRecordAndEncoding = true

        /*recordScreenTool?.setDataRecordCallBack(object : ScreenRecordManager.DataRecordCallBack {

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

        })*/

        encodeVideoTool?.setDataCallBackListener(object : VideoEncoder.DataCallBackListener {

            override fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long) {
                val vFrame = VideoFrame()
                vFrame.byteArray = byteArray
                vFrame.timestamp = timeStamp
                rtmpPushTool?.addVideoFrame(vFrame)

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
                rtmpPushTool?.addAudioFrame(aFrame)
            }

            override fun onEncodeError() {
                recordStateCallBack?.onState(StateCode.ENCODE_ERROR)
            }

            override fun onLogTest(log: String) {
                recordStateCallBack?.onLog(log)
            }
        })

        encoderMonitorTool.startMonitor()

        encodeAudioTool?.startEncode()

        recordCameraTool?.startCapture(0)

        //recordScreenTool?.reqRecordPerAndStart()

        rtmpPushTool?.startPushing(pushUrl)

    }

    fun hasPerAndStartRecord(projection: MediaProjection?) {
        if (projection == null) {
            recordStateCallBack?.onState(StateCode.SCREEN_REFUSED)
        } else {
            // recordScreenTool?.startRecording(projection)
        }
    }

    fun resumeRecording() {
        //recordScreenTool?.resumeRecording()
    }

    fun pauseRecording() {
        // recordScreenTool?.pauseRecording()
    }

    fun stopRecordAndDestory() {

        encoderMonitorTool.stopMonitor()

        rtmpPushTool?.stopPushing()
        rtmpPushTool = null

        //recordScreenTool?.stopCapture()
        //recordScreenTool = null

        encodeVideoTool?.stopEncode()
        encodeVideoTool = null

        encodeAudioTool?.stopEncode()
        encodeAudioTool = null

        isRecordAndEncoding = false
    }

}