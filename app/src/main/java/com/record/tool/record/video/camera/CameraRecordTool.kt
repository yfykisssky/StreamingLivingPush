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

    interface DataRecordCallBack {
        fun onErrorCode(code: StreamPushInstance.StateCode)
        fun onLogTest(log: String)
    }

    private var dataRecordCallBack: DataRecordCallBack? = null

    private var inputSurface: Surface? = null
    private var screenWith: Int = 0
    private var screenHeight: Int = 0
    private var inputFps: Int = 0

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
        }
        mCameraCapture?.updateInputRender(inputSurface, screenWith, screenHeight, inputFps)
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

    fun setEcodeInputSurface(
        inputSurface: Surface?,
        screenWith: Int?,
        screenHeight: Int?,
        inputFps: Int?
    ) {
        updateEncodeSettings(
            inputSurface,
            screenWith,
            screenHeight,
            inputFps
        )
    }

    private fun updateEncodeSettings(
        inputSurface: Surface?,
        screenWith: Int?,
        screenHeight: Int?,
        inputFps: Int?
    ) {
        this.inputSurface = inputSurface
        this.screenWith = screenWith ?: 0
        this.screenHeight = screenHeight ?: 0
        this.inputFps = inputFps ?: 0
    }

    fun resetEncodeSettings(
        inputSurface: Surface?,
        inputFps: Int?
    ) {
        updateEncodeSettings(
            inputSurface,
            screenWith,
            screenHeight,
            inputFps
        )
        mCameraCapture?.updateInputRender(
            this.inputSurface,
            this.screenWith,
            this.screenHeight,
            this.inputFps
        )
    }

}
