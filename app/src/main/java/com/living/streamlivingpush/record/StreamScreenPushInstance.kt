package com.living.streamlivingpush.record

import com.living.streamlivingpush.base.BaseStreamPushInstance
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.screen.ScreenRecordManager

abstract class StreamScreenPushInstance : BaseStreamPushInstance() {

    private var recordScreenTool: ScreenRecordManager? = null

    protected fun initRecoder() {
        recordScreenTool = ScreenRecordManager()
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
        recordScreenTool?.reqRecordPerAndStart()
    }

    protected fun stopRecode() {

        super.stopPush()

        recordScreenTool?.stopCapture()
        recordScreenTool = null
    }

}