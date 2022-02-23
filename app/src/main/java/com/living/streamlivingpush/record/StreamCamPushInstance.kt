package com.living.streamlivingpush.record

import android.view.TextureView
import com.living.streamlivingpush.base.BaseStreamPushInstance
import com.living.streamlivingpush.record.interfaces.ICamRecord
import com.living.streamlivingpush.record.interfaces.IRecord
import com.record.tool.record.video.camera.CameraRecordManager
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.screen.ScreenRecordManager

abstract class StreamCamPushInstance : BaseStreamPushInstance(), ICamRecord {

    private var useCamId = 0
    private var recordCameraTool: CameraRecordManager? = null

    override fun initRecoder() {
        recordCameraTool = CameraRecordManager()
    }

    override fun setStartUseCamId(camId: Int) {
        useCamId = camId
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

    override fun getPreviewView(): TextureView? {
        return recordCameraTool?.getPreviewView()
    }

    override fun usePriImgPush(usePri: Boolean) {
        if (usePri) {
            recordCameraTool?.startPushImage()
        } else {
            recordCameraTool?.stopPushImage()
        }
    }

    override fun toggleMirror() {
        recordCameraTool?.toogleMirror()
    }

    override fun switchCamera() {
        recordCameraTool?.switchCamera()
    }

    override fun startRecode() {

        super.startPush()

        recordCameraTool?.setDataCallBack(object : CameraRecordManager.DataCallBack {
            override fun onTextureVideoFrame(frame: TextureVideoFrame) {
                addVideoRenderFrame(frame)
            }
        })

        recordCameraTool?.startCapture(useCamId)
    }

    override fun stopRecode() {

        super.stopPush()

        recordCameraTool?.stopCapture()
        recordCameraTool = null
    }

}