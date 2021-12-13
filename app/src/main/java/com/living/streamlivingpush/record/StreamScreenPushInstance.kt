package com.living.streamlivingpush.record

import com.living.streamlivingpush.base.BaseStreamPushInstance
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.audio.AudioCapture
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.screen.ScreenRecordManager

abstract class StreamScreenPushInstance : BaseStreamPushInstance() {

    private var recordScreenTool: ScreenRecordManager? = null

    private var audioRecordTool: AudioCapture? = null

    protected fun initRecoder() {
        recordScreenTool = ScreenRecordManager()
        audioRecordTool = AudioCapture()

        audioRecordTool?.updateInsideVolume(1F)
    }

    override fun resetRecordFpsSettings(fps: Int) {
        recordScreenTool?.resetSettings(fps)
    }

    override fun initEncodeSettings(
        bitRateVideo: Int,
        fps: Int,
        screenWith: Int,
        screenHeight: Int,
        audioBitRate: Int
    ) {
        super.initEncodeSettings(
            bitRateVideo,
            fps,
            screenWith,
            screenHeight,
            audioBitRate
        )
        recordScreenTool?.setSettings(
            screenWith,
            screenHeight,
            fps
        )
    }

    fun usePriImgPush(usePri: Boolean) {
        if (usePri) {
            recordScreenTool?.startPushImage()
        } else {
            recordScreenTool?.stopPushImage()
        }
    }

    protected fun startRecode() {

        super.startPush()

        recordScreenTool?.setDataCallBack(object : ScreenRecordManager.DataCallBack {
            override fun onTextureVideoFrame(frame: TextureVideoFrame) {
                addVideoRenderFrame(frame)
            }

            override fun onStoped() {
            }

            override fun onRefused() {

            }

        })

        recordScreenTool?.reqRecordPerAndStart { projection ->
            audioRecordTool?.setMediaProjection(projection)
            audioRecordTool?.start()
        }

        audioRecordTool?.setRecordListener(object : AudioCapture.RecordListener {
            override fun onData(data: ByteArray?) {
                data?.let {
                    val frame = RecordAudioFrame()
                    frame.byteArray = it
                    frame.byteSize = it.size
                    addAudioRenderFrame(frame)
                }
            }

            override fun onError() {

            }

        })


    }

    protected fun stopRecode() {

        super.stopPush()

        recordScreenTool?.stopCapture()
        recordScreenTool = null

        audioRecordTool?.stop()
        audioRecordTool = null

    }

}