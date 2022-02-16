package com.record.tool.record.video.camera

import android.os.Build
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.opencv.OpenCvFaceCheckTool
import com.record.tool.record.video.gl.DrawImageTool
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.utils.PushLogUtils

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraRecordManager {

    private val TAG = this::class.java.simpleName

    private var appContext = AppApplication.appContext

    interface DataCallBack {
        fun onTextureVideoFrame(frame: TextureVideoFrame)
    }

    private var dataCallBack: DataCallBack? = null
    fun setDataCallBack(dataCallBack: DataCallBack?) {
        this.dataCallBack = dataCallBack
    }

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

    var drawImageTool: DrawImageTool? = null

    var openCvHeadCheckTool: OpenCvFaceCheckTool? = null

    fun startCapture(useCameraId: Int) {
        if (mCameraCapture == null) {
            mCameraCapture = CustomCameraCapture()
            mCameraCapture?.setTextureHandleCallBack(object :
                CustomCameraCapture.TextureHandleCallBack {
                override fun onTextureUpdate(frame: TextureVideoFrame): TextureVideoFrame {

                    if (openCvHeadCheckTool == null) {
                        openCvHeadCheckTool = OpenCvFaceCheckTool()
                        openCvHeadCheckTool?.init(useWith, useHeight)
                    }

                    openCvHeadCheckTool?.onFrameUpdate(frame.textureId)?.let { rects ->
                        PushLogUtils.logVideoFaceCheckRects(rects)
                    }

                    if (drawImageTool == null) {
                        drawImageTool = DrawImageTool()
                        drawImageTool?.init(useWith, useHeight)
                    }

                    frame.textureId = drawImageTool?.onDrawTexture(frame.textureId) ?: 0

                    return frame
                }
            })

            mCameraCapture?.setRecordDataCallBack(object : CustomCameraCapture.RecordDataCallBack {
                override fun onDataCallBack(frame: TextureVideoFrame) {
                    dataCallBack?.onTextureVideoFrame(frame)
                }
            })
        }
        mCameraCapture?.updateSettings(useWith, useHeight, useFps)
        mCameraCapture?.updatePreviewRenderView(getPreviewView())
        mCameraCapture?.startCapture(useCameraId)
    }

    fun stopCapture() {
        mCameraCapture?.stopCapture()
        openCvHeadCheckTool?.destory()
        drawImageTool?.destory()
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
