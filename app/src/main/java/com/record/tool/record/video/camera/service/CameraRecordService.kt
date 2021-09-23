package com.record.tool.record.video.camera.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.Surface
import android.view.TextureView
import com.record.tool.record.video.camera.CameraRecordManager
import com.record.tool.record.video.camera.CustomCameraCapture
import com.record.tool.record.video.gl.TextureVideoFrame

class CameraRecordService : Service() {

    private var mCameraCapture: CustomCameraCapture? = null

    private var dataRecordCallBack: CameraRecordManager.DataRecordCallBack? = null
    private val mBinder: IBinder = LocalBinder()

    private var inputSurface: Surface? = null
    private var screenWith: Int = -1
    private var screenHeight: Int = -1
    private var inputFps: Int = -1

    inner class LocalBinder : Binder() {
        fun getService(): CameraRecordService {
            return this@CameraRecordService
        }
    }

    fun setDataRecordCallBack(dataRecordCallBack: CameraRecordManager.DataRecordCallBack?) {
        this.dataRecordCallBack = dataRecordCallBack
    }

    fun setEcodeInputSurface(
        inputSurface: Surface,
        screenWith: Int,
        screenHeight: Int,
        inputFps: Int
    ) {
        this.inputSurface = inputSurface
        this.screenWith = screenWith
        this.screenHeight = screenHeight
        this.inputFps = inputFps
    }

    fun switchCamera() {
        mCameraCapture?.switchCamera()
    }

    fun startCapture(cameraId: Int, textureView: TextureView?) {

        if (mCameraCapture == null) {
            mCameraCapture = CustomCameraCapture()
            mCameraCapture?.setTextureHandleCallBack(object :
                CustomCameraCapture.TextureHandleCallBack {
                override fun onTextureUpdate(frame: TextureVideoFrame): TextureVideoFrame {
                    return frame
                }
            })
        }
        mCameraCapture?.initCapture(cameraId, inputSurface, screenWith, screenHeight, inputFps)
        mCameraCapture?.updatePreviewRenderView(textureView)
        mCameraCapture?.startCapture()

    }

    fun stopCapture() {
        mCameraCapture?.stopCapture()
        mCameraCapture = null
    }

    fun resumeCapture() {

    }

    fun pauseCapture() {

    }

    override fun onCreate() {}

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}