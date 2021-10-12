package com.living.streamlivingpush

import android.media.projection.MediaProjection
import android.os.Build
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.record.tool.record.video.camera.CameraRecordManager
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.screen.ScreenRecordManager
import com.record.tool.tools.AudioEncoder
import com.record.tool.tools.VideoEncoder
import com.record.tool.utils.EncodeControlUtils
import com.record.tool.utils.StateMonitorTool
import com.record.tool.utils.TransUtils
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

    private var encoderMonitorTool = StateMonitorTool()

    private var rtmpPushTool: RtmpPushTool? = null

    private var recordStateCallBack: RecordStateCallBack? = null

    private var recordScreenTool: ScreenRecordManager? = null
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

        recordScreenTool = ScreenRecordManager()
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

    fun reset(
        bitRateVideo: Int,
        fps: Int,
        audioBitRate: Int
    ) {

        encodeVideoTool?.updateResetEncodeSettings(
            TransUtils.kbps2bs(bitRateVideo),
            fps,
            2
        )
        encodeVideoTool?.resetEncoder()
        recordCameraTool?.resetSettings(fps)
        encoderMonitorTool.updateTargetData(TransUtils.kbps2bs(bitRateVideo), fps)
    }

    fun resetI(
        bitRateVideo: Int,
        fps: Int
    ) {
        encodeVideoTool?.checkCanSetBitRate(bitRateVideo)?.let { setBit ->
            if (setBit != 0) {
                encodeVideoTool?.updateResetEncodeSettings(
                    setBit,
                    fps,
                    2
                )
                encodeVideoTool?.resetEncoder()
                recordCameraTool?.resetSettings(fps)
            }
        }
    }

    fun prepareRecord(
        bitRateVideo: Int,
        fps: Int,
        screenWith: Int,
        screenHeight: Int,
        audioBitRate: Int
    ) {

        encodeVideoTool?.updateEncodeSettings(
            TransUtils.kbps2bs(bitRateVideo),
            fps,
            screenWith,
            screenHeight
        )
        encodeVideoTool?.initEncoder()

        /*  recordCameraTool?.setSettings(
                  screenWith,
                  screenHeight,
                  fps
          )*/

        recordScreenTool?.setSettings(
            screenWith,
            screenHeight,
            fps
        )

        //encodeAudioTool?.initEncoder(audioBitRate)

        encoderMonitorTool.updateTargetData(TransUtils.kbps2bs(bitRateVideo), fps)
    }

    fun startRecordAndSendData(pushUrl: String) {

        /* if (TextUtils.isEmpty(pushUrl)) {
             return
         }*/

        isRecordAndEncoding = true

        /*    recordCameraTool?.setDataRecordCallBack(object : CameraRecordManager.DataRecordCallBack {
                override fun onErrorCode(code: StateCode) {

                }

                override fun onLogTest(log: String) {

                }

                override fun onDataCallBack(frame: TextureVideoFrame) {
                    encodeVideoTool?.addRenderFrame(frame)
                }
            })*/

        recordScreenTool?.setDataCallBack(object : ScreenRecordManager.DataCallBack {
            override fun onTextureVideoFrame(frame: TextureVideoFrame) {
                encodeVideoTool?.addRenderFrame(frame)
            }

            override fun onStoped() {
            }

        })

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

        /*encodeVideoTool?.startEncode()

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
        })*/

        encoderMonitorTool.setCountCallBack(object : StateMonitorTool.CountCallBack {
            override fun onCount(bitRate: Int, fps: Int) {
                EncodeControlUtils.checkNeedReset(
                    bitRate,
                    encoderMonitorTool.tagBitRate,
                    encodeVideoTool?.getSetBitRate() ?: 0
                ).let {
                    if (it.first) {
                        resetI(it.second, encoderMonitorTool.tagFps)
                    }
                }
            }
        })

        encoderMonitorTool.startMonitor()

        //encodeAudioTool?.startEncode()

        //recordCameraTool?.startCapture(0)

        recordScreenTool?.reqRecordPerAndStart()

        rtmpPushTool?.startPushing(pushUrl)

    }

    fun hasPerAndStartRecord(projection: MediaProjection?) {
        if (projection == null) {
            recordStateCallBack?.onState(StateCode.SCREEN_REFUSED)
        } else {
            recordScreenTool?.startCapture(projection)
        }
    }

    fun stopRecordAndDestory() {

        encoderMonitorTool.stopMonitor()

        rtmpPushTool?.stopPushing()
        rtmpPushTool = null

        recordScreenTool?.stopCapture()
        recordScreenTool = null

        encodeVideoTool?.stopEncode()
        encodeVideoTool = null

        encodeAudioTool?.stopEncode()
        encodeAudioTool = null

        isRecordAndEncoding = false
    }

}