package com.common.base

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.EGLContext
import android.os.*
import android.widget.ImageView
import com.record.tool.record.video.gl.*
import com.record.tool.record.video.gl.basic.FrameBuffer
import com.record.tool.record.video.gl.render.EglCore
import com.record.tool.record.video.gl.render.opengl.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

abstract class BaseCapture : OnFrameAvailableListener {

    companion object {
        private const val MSG_KIND_INIT_RENDER = 0
        private const val MSG_KIND_RELEASE_RENDER = 1
        private const val MSG_KIND_UPDATE_RENDING = 2
    }

    protected val TAG = this::class.java.simpleName

    protected var mSurfaceTexture: SurfaceTexture? = null
    private var mEglCore: EglCore? = null
    private var mFrameBuffer: FrameBuffer? = null
    private var mOesInputFilter: OesInputFilter? = null
    private var mGpuImageFilterGroup: GPUImageFilterGroup? = null
    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null
    private val mTextureTransform = FloatArray(16)
    private var mSurfaceTextureId = OpenGlUtils.NO_TEXTURE
    private var mRenderHandlerThread: HandlerThread? = null

    interface BaseRecordDataCallBack {
        fun onDataCallBack(frame: TextureVideoFrame)
    }

    @Volatile
    private var mRenderHandler: RenderHandler? = null

    protected fun initRender() {
        mRenderHandlerThread = HandlerThread("RenderHandlerThread")
        mRenderHandlerThread?.start()
        mRenderHandler = mRenderHandlerThread?.looper?.let { RenderHandler(it, this) }
        mRenderHandler?.sendEmptyMessage(MSG_KIND_INIT_RENDER)
    }

    abstract fun initRenderEnd()

    protected fun releaseRender() {
        mRenderHandler?.sendEmptyMessage(MSG_KIND_RELEASE_RENDER)
    }

    abstract fun needCutFrame(): Boolean
    abstract fun frameAvailable()
    protected fun updateTextureImmediately() {
        mRenderHandler?.sendEmptyMessage(MSG_KIND_UPDATE_RENDING)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mRenderHandler?.sendEmptyMessage(MSG_KIND_UPDATE_RENDING)
        frameAvailable()
    }

    abstract fun onRender2DTextureFrame(textureFrame: TextureVideoFrame)

    private fun updateTexture() {
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

                val textureFrame = TextureVideoFrame()
                textureFrame.eglContext14 = mEglCore?.eglContext as? EGLContext
                textureFrame.textureId = mFrameBuffer?.textureId ?: -1
                textureFrame.width = getTransOutWith()
                textureFrame.height = getTransOutHeight()
                textureFrame.captureTimeStamp = System.nanoTime()

                onRender2DTextureFrame(textureFrame)

            }
        } catch (e: Exception) {
            //Log.e(TAG, "onFrameAvailable: " + e.message, e)
        }
    }

    private class RenderHandler(looper: Looper, capture: BaseCapture) : Handler(looper) {

        private val readerWeakReference = WeakReference(capture)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            readerWeakReference.get()?.let {
                when (msg.what) {
                    MSG_KIND_INIT_RENDER -> {
                        it.initGLRender()
                    }
                    MSG_KIND_RELEASE_RENDER -> {
                        it.releaseGLRender()
                    }
                    MSG_KIND_UPDATE_RENDING -> {
                        it.updateTexture()
                    }
                }
            }

        }

    }

    abstract fun getCaptureWith(): Int
    abstract fun getCaptureHeight(): Int

    abstract fun getTransOutWith(): Int
    abstract fun getTransOutHeight(): Int

    abstract fun getOutRotation(): Rotation
    abstract fun needFlipHorizontal(): Boolean
    abstract fun needFlipVertical(): Boolean

    @SuppressLint("Recycle")
    private fun initGLRender() {

        val cubeAndTextureBuffer = OpenGlUtils.calcCubeAndTextureBuffer(
            ImageView.ScaleType.CENTER_CROP,
            getOutRotation(),
            needFlipHorizontal(),
            needFlipVertical(),
            getCaptureWith(),
            getCaptureHeight(),
            getTransOutWith(),
            getTransOutHeight()
        )

        mGLCubeBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.CUBE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLCubeBuffer?.put(cubeAndTextureBuffer.first)
        mGLTextureBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.TEXTURE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLTextureBuffer?.put(cubeAndTextureBuffer.second)

        mEglCore = EglCore(getCaptureWith(), getCaptureHeight())
        mEglCore?.makeCurrent()

        mSurfaceTextureId = OpenGlUtils.generateTextureOES()
        mSurfaceTexture = SurfaceTexture(mSurfaceTextureId)
        mSurfaceTexture?.setDefaultBufferSize(getCaptureWith(), getCaptureHeight())
        mSurfaceTexture?.setOnFrameAvailableListener(this@BaseCapture)

        mFrameBuffer = FrameBuffer(getCaptureWith(), getCaptureHeight())
        mFrameBuffer?.initialize()

        mGpuImageFilterGroup = GPUImageFilterGroup()
        mOesInputFilter = OesInputFilter()

        mGpuImageFilterGroup?.addFilter(mOesInputFilter)
        mGpuImageFilterGroup?.addFilter(GPUImageFilter(false))
        mGpuImageFilterGroup?.init()
        mGpuImageFilterGroup?.onOutputSizeChanged(getCaptureWith(), getCaptureHeight())

        initRenderEnd()
    }

    fun releaseGLRender() {

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

    }

}