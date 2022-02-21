package com.record.tool.record.video.screen

import android.annotation.TargetApi
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.common.base.BaseCapture
import com.record.tool.record.video.gl.PauseImageTool
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.render.opengl.*
import com.record.tool.utils.PushLogUtils

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CustomScreenCapture : BaseCapture() {

    companion object {
        //误差系数,修正帧率
        private const val TIME_COEFFICIENT = 0.8
    }

    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mediaProjectionCallback = MediaProjectionCallback()

    private var mWidth = -1
    private var mHeight = -1
    private var fpsDelayMillis = 0L
    private var mSurface: Surface? = null

    private var renderInterval = 0L
    private var lastRenderTime = 0L

    private var mMainHandler: Handler? = null
    private var mScreenDensity: Int = 1

    private var mFillFrameRunnable: Runnable? = null

    private var needFillFrame = true
    private var needCutFrame = true

    private var dataCallBack: RecordDataCallBack? = null

    private var pauseImageTool: PauseImageTool? = null

    interface RecordDataCallBack : BaseRecordDataCallBack {
        fun onRecordStoped()
        fun onLogTest(log: String)
    }

    fun setRecordDataCallBack(dataCallBack: RecordDataCallBack?) {
        this.dataCallBack = dataCallBack
    }

    override fun frameAvailable() {
        if (needFillFrame) {
            mFillFrameRunnable?.let { runnable ->
                mMainHandler?.removeCallbacks(runnable)
                mMainHandler?.postDelayed(runnable, fpsDelayMillis)
            }
        }
    }

    //降低帧率
    override fun needCutFrame(): Boolean {
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

    override fun getCaptureWith(): Int {
        return mWidth
    }

    override fun getCaptureHeight(): Int {
        return mHeight
    }

    override fun getTransOutWith(): Int {
        return mWidth
    }

    override fun getTransOutHeight(): Int {
        return mHeight
    }

    override fun getOutRotation(): Rotation {
        return Rotation.ROTATION_0
    }

    override fun needFlipHorizontal(): Boolean {
        return false
    }

    override fun needFlipVertical(): Boolean {
        return false
    }

    override fun onRender2DTextureFrame(textureFrame: TextureVideoFrame) {
        //编码前处理
        dataCallBack?.onDataCallBack(pauseImageTool?.transTexture(textureFrame) ?: textureFrame)
    }

    fun updateSettings(
        outWidth: Int,
        outHeight: Int,
        screenDensity: Int,
        fps: Int
    ) {

        mWidth = outWidth
        mHeight = outHeight
        mScreenDensity = screenDensity
        fpsDelayMillis = (1000L / fps)
        renderInterval = ((1000L / fps) * TIME_COEFFICIENT).toLong()

    }

    //补空帧
    private fun delayRender() {
        updateTextureImmediately()
        mFillFrameRunnable?.let { runnable ->
            mMainHandler?.postDelayed(runnable, fpsDelayMillis)
        }
    }

    fun startCapture(projection: MediaProjection?) {
        initMediaProjection(projection)
        initRender()
        if (needFillFrame) {
            mFillFrameRunnable = Runnable {
                delayRender()
            }
        }
    }

    override fun initRenderEnd() {
        mSurface = Surface(mSurfaceTexture)
        toStartInput()

        pauseImageTool = PauseImageTool()
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
            dataCallBack?.onRecordStoped()
        }

    }

    fun stopCapture() {
        releaseMediaProjection()
        releaseRender()

        pauseImageTool?.destory()
        pauseImageTool = null
    }

    private fun initMediaProjection(projection: MediaProjection?) {
        mMediaProjection = projection
        mMainHandler = Looper.myLooper()?.let { Handler(it) }
    }

    private fun releaseMediaProjection() {

        mMainHandler?.removeCallbacksAndMessages(null)
        mMediaProjection?.unregisterCallback(mediaProjectionCallback)

        mVirtualDisplay?.release()
        mMediaProjection?.stop()
        mVirtualDisplay = null
        mMediaProjection = null

    }

    fun startPushImage() {
        pauseImageTool?.startReplace()
    }

    fun stopPushImage() {
        pauseImageTool?.stopReplace()
    }

}