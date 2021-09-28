package com.record.tool.record.video.camera

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.Camera
import android.opengl.EGLContext
import android.opengl.GLES20
import android.os.*
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.record.tool.record.video.gl.PauseImageTool
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.ToSurfaceFrameRender
import com.record.tool.record.video.gl.ToSurfaceViewFrameRender
import com.record.tool.record.video.gl.basic.FrameBuffer
import com.record.tool.record.video.gl.render.EglCore
import com.record.tool.record.video.gl.render.opengl.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
class CustomCameraCapture : OnFrameAvailableListener {

    companion object {
        //误差系数,修正帧率
        private const val TIME_COEFFICIENT = 0.8

        private const val MSG_KIND_START = 0
        private const val MSG_KIND_UPDATE = 1
    }

    private var appContext = AppApplication.appContext

/*    private var cameraWith = 0
    private var cameraHeight = 0*/

    private var recordWith = 0
    private var recordHeight = 0

    private var screenWith = 0
    private var screenHeight = 0

    private var recordFps = 0
    private var renderInterval = 0L
    private var lastRenderTime = 0L

    private var mCamera: Camera? = null
    private var cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
    private val mCameraInfo = Camera.CameraInfo()
    private var mCameraChanging = false

    private var mSurfaceTexture: SurfaceTexture? = null
    private var mEglCore: EglCore? = null
    private var mFrameBuffer: FrameBuffer? = null
    private var mOesInputFilter: OesInputFilter? = null
    private var mGpuImageFilterGroup: GPUImageFilterGroup? = null
    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null
    private val mTextureTransform = FloatArray(16) // OES纹理转换为2D纹理
    private var mSurfaceTextureId = OpenGlUtils.NO_TEXTURE
    private var mFrameUpdated = false
    private var mRenderHandlerThread: HandlerThread? = null

    private var mCustomSurfaceRender: ToSurfaceFrameRender? = null
    private var mCustomSurfaceViewRender: ToSurfaceViewFrameRender? = null
    private var mCustomPreviewView: TextureView? = null

    private data class CameraSize(var width: Int = -1, var height: Int = -1)

    private var callBack: TextureHandleCallBack? = null

    private var pauseImageTool: PauseImageTool? = null

    interface TextureHandleCallBack {
        fun onTextureUpdate(frame: TextureVideoFrame): TextureVideoFrame
    }

    fun setTextureHandleCallBack(callBack: TextureHandleCallBack?) {
        this.callBack = callBack
    }

    @Volatile
    private var mRenderHandler: RenderHandler? = null

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mFrameUpdated = true
        mRenderHandler?.sendEmptyMessage(MSG_KIND_UPDATE)
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

    //更新编码surface
    fun updateInputRender(
        outSurface: Surface?,
        outWidth: Int,
        outHeight: Int,
        fps: Int
    ) {

        this.recordWith = outWidth
        this.recordHeight = outHeight
        this.recordFps = fps
        renderInterval = ((1000L / recordFps) * TIME_COEFFICIENT).toLong()

        if (mCustomSurfaceRender == null) {
            mCustomSurfaceRender = ToSurfaceFrameRender()
            mCustomSurfaceRender?.setOutPutSurface(outSurface, recordHeight, recordWith)
        } else {
            mCustomSurfaceRender?.resetOutPutSurface(outSurface, recordHeight, recordWith)
        }

    }

    fun startCapture(cameraId: Int) {
        this.cameraId = cameraId
        mRenderHandlerThread = HandlerThread("RenderHandlerThread")
        mRenderHandlerThread?.start()
        mRenderHandler = mRenderHandlerThread?.looper?.let { RenderHandler(it, this) }
        mRenderHandler?.sendEmptyMessage(MSG_KIND_START)
    }

    fun stopCapture() {
        releaseRender()
        releaseCamera()
    }

    fun toogleMirror() {
        val isMirrorPush = !(mCustomSurfaceRender?.isFlipHorizontal ?: false)
        mCustomSurfaceRender?.setIsFlipHorizontal(isMirrorPush)
    }

    fun isMirror(): Boolean {
        return mCustomSurfaceRender?.isFlipHorizontal ?: false
    }


    //降低帧率
    private fun needCutFrame(): Boolean {
        val currentTimeStamp = System.currentTimeMillis()
        val intervalTime = currentTimeStamp - lastRenderTime
        return if (intervalTime > renderInterval) {
            lastRenderTime = currentTimeStamp
            false
        } else {
            true
        }
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
        openCamera(cameraId)
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

        initRender()

        try {
            mSurfaceTextureId = OpenGlUtils.generateTextureOES()
            mSurfaceTexture = SurfaceTexture(mSurfaceTextureId)
            mSurfaceTexture?.setOnFrameAvailableListener(this@CustomCameraCapture)
            openCamera(cameraId)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    //需要水平翻转
    private fun needCameraFlipHorizontal(): Boolean {
        return mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
    }

    //旋转角度
    private fun cameraUseOrientation(): Rotation {
        return when (mCameraInfo.orientation) {
            Rotation.ROTATION_90.asInt() -> {
                Rotation.ROTATION_90
            }
            Rotation.ROTATION_180.asInt() -> {
                Rotation.ROTATION_90
            }
            Rotation.ROTATION_270.asInt() -> {
                Rotation.ROTATION_270
            }
            Rotation.ROTATION_0.asInt() -> {
                Rotation.ROTATION_90
            }
            else -> {
                Rotation.NORMAL
            }
        }
    }

    //需要竖直翻转
    private fun needCameraFlipVertical(): Boolean {
        return !(mCameraInfo.orientation == 90 || mCameraInfo.orientation == 270)
    }

    private fun openCamera(cameraId: Int): Boolean {
        try {
            releaseCamera()
            mCamera = Camera.open(cameraId)
            this.cameraId = cameraId
            Camera.getCameraInfo(cameraId, mCameraInfo)
            setDefaultParameters()
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

    private fun updateTexture() {
        synchronized(this) {
            if (mFrameUpdated) {
                mFrameUpdated = false
            }
            try {
                mSurfaceTexture?.let { sur ->
                    sur.updateTexImage()

                    if (needCutFrame()) {
                        return
                    }

                    sur.getTransformMatrix(mTextureTransform)
                    mOesInputFilter?.setTexutreTransform(mTextureTransform)
                    mGpuImageFilterGroup?.draw(
                        mSurfaceTextureId,
                        mFrameBuffer?.frameBufferId ?: 0,
                        mGLCubeBuffer,
                        mGLTextureBuffer
                    )
                    GLES20.glFinish()

                    var textureFrame = TextureVideoFrame()
                    textureFrame.eglContext14 = mEglCore?.eglContext as? EGLContext
                    textureFrame.textureId = mFrameBuffer?.textureId ?: -1
                    textureFrame.width = recordHeight
                    textureFrame.height = recordWith
                    textureFrame.captureTimeStamp = sur.timestamp

                    //预览前处理
                    textureFrame = callBack?.onTextureUpdate(textureFrame) ?: textureFrame

                    mCustomSurfaceViewRender?.onRenderVideoFrame(textureFrame)

                    //编码前处理
                    textureFrame = pauseImageTool?.transTexture(textureFrame) ?: textureFrame

                    mCustomSurfaceRender?.onRenderVideoFrame(textureFrame)

                }
            } catch (e: Exception) {
                //Log.e(TAG, "onFrameAvailable: " + e.message, e)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private class RenderHandler(looper: Looper, capture: CustomCameraCapture) : Handler(looper) {

        private val readerWeakReference = WeakReference(capture)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            readerWeakReference.get()?.let {
                if (MSG_KIND_START == msg.what) {
                    it.startInternal()
                } else if (MSG_KIND_UPDATE == msg.what) {
                    it.updateTexture()
                }
            }

        }

    }

    private fun getUsedPreviewSize(previewSize: CameraSize): CameraSize? {

        val sizes = mCamera?.parameters?.supportedPreviewSizes

        return sizes?.find { size ->
            size.width == previewSize.width && size.height == previewSize.height
        }?.let {
            CameraSize(it.width, it.height)
        }

    }

    private fun initRender() {
        val cubeAndTextureBuffer = OpenGlUtils.calcCubeAndTextureBuffer(
            ImageView.ScaleType.CENTER_CROP,
            cameraUseOrientation(),
            needCameraFlipHorizontal(),
            needCameraFlipVertical(),
            recordWith,
            recordHeight,
            recordHeight,
            recordWith
        )
        mGLCubeBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.CUBE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLCubeBuffer?.put(cubeAndTextureBuffer.first)
        mGLTextureBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.TEXTURE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLTextureBuffer?.put(cubeAndTextureBuffer.second)

        mEglCore = EglCore(recordWith, recordHeight)
        mEglCore?.makeCurrent()
        mSurfaceTextureId = OpenGlUtils.generateTextureOES()
        mSurfaceTexture = SurfaceTexture(mSurfaceTextureId)
        mFrameBuffer = FrameBuffer(recordWith, recordHeight)
        mFrameBuffer?.initialize()
        mGpuImageFilterGroup = GPUImageFilterGroup()
        mOesInputFilter = OesInputFilter()
        mGpuImageFilterGroup?.addFilter(mOesInputFilter)
        mGpuImageFilterGroup?.addFilter(GPUImageFilter(true))
        mGpuImageFilterGroup?.init()
        mGpuImageFilterGroup?.onOutputSizeChanged(recordWith, recordHeight)

        pauseImageTool = PauseImageTool()
    }

    private fun releaseRender() {

        mRenderHandlerThread?.quit()

        mGpuImageFilterGroup?.destroy()
        mGpuImageFilterGroup = null

        mFrameBuffer?.uninitialize()
        mFrameBuffer = null

        if (mSurfaceTextureId != OpenGlUtils.NO_TEXTURE) {
            OpenGlUtils.deleteTexture(mSurfaceTextureId)
            mSurfaceTextureId = OpenGlUtils.NO_TEXTURE
        }

        mSurfaceTexture?.release()
        mSurfaceTexture = null

        mEglCore?.unmakeCurrent()
        mEglCore?.destroy()
        mEglCore = null

        mCustomSurfaceRender?.stop()
        mCustomSurfaceRender = null

        mCustomSurfaceViewRender?.stop()
        mCustomSurfaceViewRender = null

        mCustomPreviewView = null

        pauseImageTool?.destory()
        pauseImageTool = null

    }

    fun startPushImage() {
        pauseImageTool?.startReplace()
    }

    fun stopPushImage() {
        pauseImageTool?.stopReplace()
    }

}