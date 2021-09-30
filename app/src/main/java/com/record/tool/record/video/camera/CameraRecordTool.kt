package com.record.tool.record.video.camera

import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.StreamPushInstance
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.utils.StateMonitorTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraRecordManager {

    private val TAG = this::class.java.simpleName

    private var appContext = AppApplication.appContext

    fun setDataRecordCallBack(dataRecordCallBack: DataRecordCallBack?) {
        this.dataRecordCallBack = dataRecordCallBack
    }

    interface DataRecordCallBack : CustomCameraCapture.RecordDataCallBack {
        fun onErrorCode(code: StreamPushInstance.StateCode)
        fun onLogTest(log: String)
    }

    private var dataRecordCallBack: DataRecordCallBack? = null

    private var useWith: Int = 0
    private var useHeight: Int = 0
    private var useFps: Int = 0

    private var mCameraCapture: CustomCameraCapture? = null
    private var cameraPreviewView: TextureView? = null

    fun toogleMirror() {
        mCameraCapture?.toogleMirror()
    }

    fun isMirror(): Boolean? {
        return mCameraCapture?.isMirror()
    }

    fun startCapture(useCameraId: Int) {
        if (mCameraCapture == null) {
            mCameraCapture = CustomCameraCapture()
            mCameraCapture?.setTextureHandleCallBack(object :
                    CustomCameraCapture.TextureHandleCallBack {
                override fun onTextureUpdate(frame: TextureVideoFrame): TextureVideoFrame {
                    return frame
                }
            })

            mCameraCapture?.setRecordDataCallBack(object : CustomCameraCapture.RecordDataCallBack {
                override fun onDataCallBack(frame: TextureVideoFrame) {
                    dataRecordCallBack?.onDataCallBack(frame)
                }
            })
        }
        mCameraCapture?.updateSettings(useWith, useHeight, useFps)
        mCameraCapture?.updatePreviewRenderView(cameraPreviewView)
        mCameraCapture?.startCapture(useCameraId)
    }

    fun stopCapture() {
        mCameraCapture?.stopCapture()
    }

    fun getPreviewView(): TextureView? {
        appContext?.let {
            if (cameraPreviewView == null) {
                cameraPreviewView = TextureView(it)
            }
        }
        return cameraPreviewView
    }

    fun switchCamera() {
        mCameraCapture?.switchCamera()
    }

    fun startPushImage() {
        mCameraCapture?.startPushImage()
    }

    fun stopPushImage() {
        mCameraCapture?.stopPushImage()
    }

    fun setSettings(
            screenWith: Int?,
            screenHeight: Int?,
            inputFps: Int?
    ) {
        this.useWith = screenWith ?: 0
        this.useHeight = screenHeight ?: 0
        this.useFps = inputFps ?: 0
    }

    fun resetSettings(
            inputFps: Int?
    ) {
        this.useFps = inputFps ?: 0
        mCameraCapture?.updateSettings(useWith, useHeight, useFps)
    }

}
