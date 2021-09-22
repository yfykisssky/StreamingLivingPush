package com.record.tool.record

import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.IBinder
import android.view.Surface
import com.living.streamlivingpush.StreamPushInstance
import com.record.tool.record.audio.AudioCapture
import com.record.tool.record.video.screen.ScreenRenderCapture
import com.record.tool.utils.PushLogUtils
import com.record.tool.utils.SysUtils

class ScreenRecordService : Service() {

    private var mScreenCapture: ScreenRenderCapture? = null
    private var mAudioCapture: AudioCapture? = null

    private var dataRecordCallBack: ScreenRecordManager.DataRecordCallBack? = null
    private val mBinder: IBinder = LocalBinder()

    private var inputSurface: Surface? = null
    private var screenWith: Int = -1
    private var screenHeight: Int = -1
    private var inputFps: Int = -1

    inner class LocalBinder : Binder() {
        fun getService(): ScreenRecordService {
            return this@ScreenRecordService
        }
    }

    fun setDataRecordCallBack(dataRecordCallBack: ScreenRecordManager.DataRecordCallBack?) {
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

    fun startCapture(projection: MediaProjection) {

        if (mScreenCapture == null) {
            mScreenCapture = ScreenRenderCapture()
            mScreenCapture?.setOnScreenCaptureListener(object :
                ScreenRenderCapture.OnScreenCaptureListener {
                override fun onRecordStoped() {
                    dataRecordCallBack?.onErrorCode(StreamPushInstance.StateCode.SCREEN_STOP)
                }

                override fun onLogTest(log: String) {
                    dataRecordCallBack?.onLogTest(log)
                }
            })
        }

        mScreenCapture?.initProjection(projection)
        mScreenCapture?.initCapture(
            inputSurface,
            screenWith,
            screenHeight,
            SysUtils.getDpi(),
            inputFps
        )

        mScreenCapture?.startScreenCapture()

        if (mAudioCapture == null) {
            mAudioCapture = AudioCapture()
        }

        mAudioCapture?.setRecordListener(object : AudioCapture.RecordListener {
            override fun onData(data: ByteArray?, byteSize: Int) {
                dataRecordCallBack?.onAudioDataRecord(data, byteSize)
                PushLogUtils.logAudioTimeStamp(System.currentTimeMillis())
            }

            override fun onError() {
                dataRecordCallBack?.onErrorCode(StreamPushInstance.StateCode.AUDIO_ERROR)
            }

            override fun onLogTest(log: String) {
                dataRecordCallBack?.onLogTest(log)
            }

        })

        mAudioCapture?.start()

    }

    fun stopCapture() {
        mScreenCapture?.stopScreenCapture()
        mScreenCapture = null
        mAudioCapture?.stop()
        mAudioCapture = null
    }

    fun resumeCapture() {
        mAudioCapture?.resumeCapture()
        mScreenCapture?.resumeCapture()
    }

    fun pauseCapture() {
        mAudioCapture?.pauseCapture()
        mScreenCapture?.pauseCapture()
    }

    override fun onCreate() {}

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}