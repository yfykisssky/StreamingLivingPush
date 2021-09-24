package com.record.tool.record.video.screen

import android.annotation.TargetApi
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.ToSurfaceFrameRender
import com.record.tool.record.video.gl.basic.FrameBuffer
import com.record.tool.record.video.gl.basic.ImgTexFormat
import com.record.tool.record.video.gl.basic.ImgTexFrame
import com.record.tool.record.video.gl.gles.GLRender
import com.record.tool.record.video.gl.gles.SinkConnector
import com.record.tool.record.video.gl.gles.SrcConnector
import com.record.tool.record.video.gl.render.opengl.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRenderCapture {

    companion object {
        private val TAG = ScreenRenderCapture::class.java.simpleName

        //误差系数,修正帧率
        private const val TIME_COEFFICIENT = 0.8
    }

    private var mOnScreenCaptureListener: OnScreenCaptureListener? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mediaProjectionCallback = MediaProjectionCallback()

    private var mFrameBuffer: FrameBuffer? = null
    private var mOesInputFilter: OesInputFilter? = null
    private var mGpuImageFilterGroup: GPUImageFilterGroup? = null
    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null
    private var mGLRender: GLRender? = null

    private var mWidth = -1
    private var mHeight = -1
    private var fpsDelayMillis = 16L

    private var mTextureId = 0
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var frameListener = SurfaceTexture.OnFrameAvailableListener { st ->

        mGLRender?.requestRender()

        if (needFillFrame) {
            mFillFrameRunnable?.let { runnable ->
                mMainHandler?.removeCallbacks(runnable)
                mMainHandler?.postDelayed(runnable, fpsDelayMillis)
            }
        }
    }

    //降低帧率
    private fun needCutFrame(): Boolean {
        return if (needCutFrame) {
            val currentTimeStamp = System.currentTimeMillis()
            val intervalTime = currentTimeStamp - lastRenderTime
            if (intervalTime > renderInterval) {
                lastRenderTime = currentTimeStamp
                false
            } else {
                true
            }
        } else {
            false
        }
    }

    private var renderInterval = 0L
    private var lastRenderTime = 0L

    private var mImgTexFormat: ImgTexFormat? = null
    private var mMainHandler: Handler? = null
    private var mScreenDensity: Int = 1

    private var mFillFrameRunnable: Runnable? = null

    private var mCustomFrameRender: ToSurfaceFrameRender? = null
    private var canRender = false

    private var needFillFrame = true
    private var needCutFrame = true

    private var mImgTexSrcConnector: SrcConnector<ImgTexFrame>? = null

    interface OnScreenCaptureListener {
        fun onRecordStoped()

        fun onLogTest(log: String)
    }

    fun initCapture(
        outSurface: Surface?,
        outWidth: Int,
        outHeight: Int,
        screenDensity: Int,
        fps: Int
    ) {

        mWidth = outWidth
        mHeight = outHeight
        mScreenDensity = screenDensity
        fpsDelayMillis = ((1000L / fps) * TIME_COEFFICIENT).toLong()
        renderInterval = ((1000L / fps) * TIME_COEFFICIENT).toLong()

        if (mGLRender == null) {
            mGLRender = GLRender()
        }
        mGLRender?.addListener(mGLRenderListener)

        if (mCustomFrameRender == null) {
            mCustomFrameRender = ToSurfaceFrameRender()
        }
        mCustomFrameRender?.setOutPutSurface(outSurface, mWidth, mHeight)

        mImgTexSrcConnector = SrcConnector()
        mImgTexFormat =
            ImgTexFormat(
                ImgTexFormat.COLOR_FORMAT_EXTERNAL_OES,
                mWidth,
                mHeight
            )
        mImgTexSrcConnector?.onFormatChanged(mImgTexFormat)

        mMainHandler = Looper.myLooper()?.let { Handler(it) }

        //编码
        mImgTexSrcConnector?.connect(object : SinkConnector<ImgTexFrame?>() {
            override fun onFormatChanged(obj: Any) {}
            override fun setSurface(surface: Surface) {}
            override fun onFrameAvailable(frameImg: ImgTexFrame?, byteBuffer: ByteBuffer?) {
                val frame = TextureVideoFrame()
                frame.textureId = frameImg?.mTextureId ?: -1
                frame.width = frameImg?.mFormat?.mWidth ?: 0
                frame.height = frameImg?.mFormat?.mHeight ?: 0
                frame.captureTimeStamp = frameImg?.pts ?: 0
                frame.eglContext14 = mGLRender?.eglContext
                mCustomFrameRender?.onRenderVideoFrame(frame)
            }
        })

        if (needFillFrame) {
            mFillFrameRunnable = Runnable {
                delayRender()
            }
        }

    }

    //补空帧
    private fun delayRender() {
        mGLRender?.requestRender()
        mFillFrameRunnable?.let { runnable ->
            mMainHandler?.postDelayed(runnable, fpsDelayMillis)
        }
    }

    fun setOnScreenCaptureListener(listener: OnScreenCaptureListener?) {
        mOnScreenCaptureListener = listener
    }

    fun initProjection(projection: MediaProjection?) {
        mMediaProjection = projection
    }

    private fun resetRender(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        mVirtualDisplay?.release()
        mVirtualDisplay = null
        mTextureId = OpenGlUtils.generateTextureOES()
        mSurfaceTexture?.release()
        mSurface?.release()
        mSurfaceTexture = SurfaceTexture(mTextureId)
        mSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
        mSurface = Surface(mSurfaceTexture)
        mSurfaceTexture?.setOnFrameAvailableListener(frameListener)

        mGLCubeBuffer = OpenGlUtils.createNormalCubeVerticesBuffer()
        mGLTextureBuffer = OpenGlUtils.createTextureCoordsBuffer(Rotation.NORMAL, false, false)

        // 创建一个FrameBuffer，作为输出给到外面（外面不能异步使用）
        mFrameBuffer = FrameBuffer(mWidth, mHeight)
        mFrameBuffer?.initialize()
        mGpuImageFilterGroup = GPUImageFilterGroup()
        mOesInputFilter = OesInputFilter()
        mGpuImageFilterGroup?.addFilter(mOesInputFilter)
        mGpuImageFilterGroup?.addFilter(GPUImageFilter(true))
        mGpuImageFilterGroup?.init()
        mGpuImageFilterGroup?.onOutputSizeChanged(mWidth, mHeight)
    }

    private fun doDrawFrame() {

        try {
            mSurfaceTexture?.updateTexImage()
        } catch (e: Exception) {
            e.printStackTrace()
            mOnScreenCaptureListener?.onLogTest(e.message ?: "")
            return
        }

        if (!canRender) {
            return
        }

        if (needCutFrame()) {
            return
        }

        val texMatrix = FloatArray(16)
        mSurfaceTexture?.getTransformMatrix(texMatrix)
        //oes输入转换
        mOesInputFilter?.setTexutreTransform(texMatrix)
        GLES20.glViewport(0, 0, mWidth, mHeight)
        //2D纹理渲染
        mGpuImageFilterGroup?.draw(
            mTextureId,
            mFrameBuffer?.frameBufferId ?: 0,
            mGLCubeBuffer,
            mGLTextureBuffer
        )
        val frame =
            ImgTexFrame(
                mImgTexFormat,
                mFrameBuffer?.textureId ?: 0,
                texMatrix,
                mSurfaceTexture?.timestamp ?: 0
            )
        try {
            mImgTexSrcConnector?.onFrameAvailable(frame, null)
        } catch (e: Exception) {
            e.printStackTrace()
            mOnScreenCaptureListener?.onLogTest(e.message ?: "")
        }

    }

    fun resumeCapture() {
        canRender = true
    }

    fun pauseCapture() {
        canRender = false
    }

    fun startScreenCapture() {
        resumeCapture()
        mGLRender?.init(mWidth, mHeight)
    }

    private val mGLRenderListener: GLRender.GLRenderListener = object : GLRender.GLRenderListener {
        override fun onReady() {
        }

        override fun onSizeChanged(width: Int, height: Int) {
            resetRender(width, height)
            toStartInput()
        }

        override fun onDrawFrame() {
            doDrawFrame()
        }

        override fun onReleased() {}
    }

    private fun toStartInput() {
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            mWidth, mHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
            null, null
        )
        mMediaProjection?.registerCallback(mediaProjectionCallback, null)
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {

        override fun onStop() {
            super.onStop()
            mOnScreenCaptureListener?.onRecordStoped()
        }

    }

    fun stopScreenCapture() {
        pauseCapture()
        mMainHandler?.removeCallbacksAndMessages(null)

        mMediaProjection?.unregisterCallback(mediaProjectionCallback)

        mVirtualDisplay?.release()
        mMediaProjection?.stop()
        mVirtualDisplay = null
        mMediaProjection = null

        mCustomFrameRender?.stop()
        mCustomFrameRender = null
    }

}