package com.living.streamlivingpush.record

import android.view.TextureView
import com.living.streamlivingpush.base.BaseStreamPushInstance
import com.record.tool.record.video.camera.CameraRecordManager
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.screen.ScreenRecordManager

abstract class StreamCamPushInstance : BaseStreamPushInstance() {

    private var recordCameraTool: CameraRecordManager? = null

    protected fun initRecoder() {
        recordCameraTool = CameraRecordManager()
    }

    override fun resetRecordFpsSettings(fps: Int) {
        recordCameraTool?.resetSettings(fps)
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
        recordCameraTool?.setSettings(
            screenWith,
            screenHeight,
            fps
        )
    }

    fun getPreviewView(): TextureView? {
        return recordCameraTool?.getPreviewView()
    }

    fun usePriImgPush(usePri: Boolean) {
        if (usePri) {
            recordCameraTool?.startPushImage()
        } else {
            recordCameraTool?.stopPushImage()
        }
    }

    fun toogleMirror() {
        recordCameraTool?.toogleMirror()
    }

    fun switchCamera() {
        recordCameraTool?.switchCamera()
    }

    protected fun startRecode(useCamId: Int) {

        super.startPush()

        recordCameraTool?.setDataCallBack(object : CameraRecordManager.DataCallBack {
            override fun onTextureVideoFrame(frame: TextureVideoFrame) {
                addVideoRenderFrame(frame)
            }
        })

        recordCameraTool?.startCapture(useCamId)
    }

    protected fun stopRecode() {

        super.stopPush()

        recordCameraTool?.stopCapture()
        recordCameraTool = null
    }

}