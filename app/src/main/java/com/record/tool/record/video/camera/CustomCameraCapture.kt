package com.record.tool.record.video.camera

import android.hardware.Camera
import android.os.Build
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.common.base.BaseCapture
import com.record.tool.record.video.gl.ExtraHandle
import com.record.tool.record.video.gl.PauseImageTool
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.ToSurfaceViewFrameRender
import com.record.tool.record.video.gl.render.opengl.Rotation
import java.io.IOException

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
class CustomCameraCapture : BaseCapture() {

    companion object {
        //误差系数,修正帧率
        private const val TIME_COEFFICIENT = 0.8
    }

    private var recordWith = 0
    private var recordHeight = 0

    private var outFrameWith = 0
    private var outFrameHeight = 0

    private var screenWith = 0
    private var screenHeight = 0

    private var recordFps = 0
    private var renderInterval = 0L
    private var lastRenderTime = 0L

    private var mCamera: Camera? = null
    private var cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
    private val mCameraInfo = Camera.CameraInfo()
    private var mCameraChanging = false

    private data class CameraSize(var width: Int = -1, var height: Int = -1)

    private var isMirrorPush: Boolean = false

    private var pauseImageTool: PauseImageTool? = null

    override fun initRenderEnd() {
        pauseImageTool = PauseImageTool()
        openCamera()
    }

    private var mCustomSurfaceViewRender: ToSurfaceViewFrameRender? = null
    private var mCustomPreviewView: TextureView? = null

    private var callBack: TextureHandleCallBack? = null

    interface TextureHandleCallBack {
        fun onTextureUpdate(frame: TextureVideoFrame): TextureVideoFrame
    }

    fun setTextureHandleCallBack(callBack: TextureHandleCallBack?) {
        this.callBack = callBack
    }

    private var dataCallBack: RecordDataCallBack? = null

    interface RecordDataCallBack {
        fun onDataCallBack(frame: TextureVideoFrame)
    }

    fun setRecordDataCallBack(dataCallBack: RecordDataCallBack?) {
        this.dataCallBack = dataCallBack
    }

    fun updatePreviewRenderView(textureView: TextureView?) {
        mCustomPreviewView = textureView
        if (mCustomSurfaceViewRender == null) {
            mCustomSurfaceViewRender = ToSurfaceViewFrameRender()
            mCustomSurfaceViewRender?.setSurfaceSizeChangedListener { width, height ->
                screenWith = width
                screenHeight = height
            }
            mCustomSurfaceViewRender?.updateRenderTextureView(mCustomPreviewView)
        }
    }

    fun updateSettings(
        outWidth: Int,
        outHeight: Int,
        fps: Int
    ) {

        outFrameWith = outWidth
        outFrameHeight = outHeight

        getRecordSize(outWidth, outHeight).let {
            this.recordWith = it.first
            this.recordHeight = it.second
        }

        this.recordFps = fps
        renderInterval = ((1000L / recordFps) * TIME_COEFFICIENT).toLong()

    }

    //摄像头一般横向分辨率输出，重新设置采集宽高
    private fun getRecordSize(outWidth: Int, outHeight: Int): Pair<Int, Int> {
        var with = outWidth
        var height = outHeight
        //纵向
        if (outWidth < outHeight) {
            with = outHeight
            height = outWidth
        }
        return Pair(with, height)
    }

    fun startCapture(cameraId: Int) {
        this.cameraId = cameraId
        startInternal()
    }

    fun stopCapture() {
        releaseRender()
        releaseCamera()
        resetData()

        pauseImageTool?.destory()
        pauseImageTool = null
    }

    private fun resetData() {
        isMirrorPush = false
    }

    fun toogleMirror() {
        isMirrorPush = !isMirrorPush
    }

    fun isMirror(): Boolean {
        return isMirrorPush
    }


    //降低帧率
    override fun needCutFrame(): Boolean {
        val currentTimeStamp = System.currentTimeMillis()
        val intervalTime = currentTimeStamp - lastRenderTime
        return if (intervalTime > renderInterval) {
            lastRenderTime = currentTimeStamp
            false
        } else {
            true
        }
    }

    override fun frameAvailable() {

    }

    //setDisplayOrientation 90,宽高取反
    override fun getCaptureWith(): Int {
        return recordHeight
    }

    override fun getCaptureHeight(): Int {
        return recordWith
    }

    override fun getTransOutWith(): Int {
        return outFrameWith
    }

    override fun getTransOutHeight(): Int {
        return outFrameHeight
    }

    override fun getOutRotation(): Rotation {
        return cameraUseOrientation()
    }

    override fun needFlipHorizontal(): Boolean {
        return needCameraFlipHorizontal()
    }

    override fun needFlipVertical(): Boolean {
        return needCameraFlipVertical()
    }

    override fun onRender2DTextureFrame(textureFrame: TextureVideoFrame) {

        var textureFrameOut = textureFrame
        //预览前处理
        textureFrameOut = callBack?.onTextureUpdate(textureFrameOut) ?: textureFrameOut

        mCustomSurfaceViewRender?.onRenderVideoFrame(textureFrameOut)

        textureFrameOut.extraHandle = ExtraHandle(fillHorizontal = isMirrorPush)

        //编码前处理
        textureFrameOut = pauseImageTool?.transTexture(textureFrameOut) ?: textureFrameOut
        textureFrameOut.captureTimeStamp = System.nanoTime()

        dataCallBack?.onDataCallBack(textureFrameOut)

    }

    private fun hasOnlyOneCamera(): Boolean {
        return Camera.getNumberOfCameras() == 1
    }

    fun switchCamera() {
        if (hasOnlyOneCamera() || mCameraChanging) {
            return
        }

        cameraId = 1 - cameraId

        mCameraChanging = true
        releaseCamera()
        updateCamSettings(cameraId)
        openCamera()
        mCameraChanging = false
    }

    private fun setDefaultParameters() {

        mCamera?.parameters?.let { parameters ->
            if (parameters.supportedFocusModes.contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                )
            ) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
            val flashModes = parameters.supportedFlashModes
            if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
            }

            getUsedPreviewSize(CameraSize(recordWith, recordHeight))?.let {
                recordWith = it.width
                recordHeight = it.height
                parameters.setPreviewSize(it.width, it.height)
            }

            mCamera?.parameters = parameters
            /* maxExposureCompensation = parameters.maxExposureCompensation / 2
             minExposureCompensation = parameters.minExposureCompensation / 2*/
        }

    }

    private fun startInternal() {

        try {
            releaseCamera()
            updateCamSettings(cameraId)
            initRender()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    //需要水平翻转
    private fun needCameraFlipHorizontal(): Boolean {
        return false
    }

    //旋转角度
    private fun cameraUseOrientation(): Rotation {
        return Rotation.ROTATION_0
    }

    //需要竖直翻转
    private fun needCameraFlipVertical(): Boolean {
        return false
    }

    private fun updateCamSettings(cameraId: Int) {
        mCamera = Camera.open(cameraId)
        mCamera?.setDisplayOrientation(90)
        this.cameraId = cameraId
        Camera.getCameraInfo(cameraId, mCameraInfo)
        setDefaultParameters()
    }


    private fun openCamera(): Boolean {
        try {
            mCamera?.setPreviewTexture(mSurfaceTexture)
            mCamera?.startPreview()
        } catch (e: java.lang.Exception) {
            mCamera = null
            return false
        }
        return true
    }

    private fun releaseCamera() {
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
        mCameraChanging = false
    }

    private fun getUsedPreviewSize(previewSize: CameraSize): CameraSize? {

        val sizes = mCamera?.parameters?.supportedPreviewSizes

        return sizes?.find { size ->
            size.width == previewSize.width && size.height == previewSize.height
        }?.let {
            CameraSize(it.width, it.height)
        }

    }

    fun startPushImage() {
        pauseImageTool?.startReplace()
    }

    fun stopPushImage() {
        pauseImageTool?.stopReplace()
    }

}