package com.record.tool.record.video.screen

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Surface
import android.view.Window
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.StreamPushInstance
import com.record.tool.record.video.screen.service.ScreenRecordService

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecordManager {

    private val TAG = this::class.java.simpleName

    private var appContext = AppApplication.appContext

    fun setDataRecordCallBack(dataRecordCallBack: DataRecordCallBack?) {
        this.dataRecordCallBack = dataRecordCallBack
    }

    interface DataRecordCallBack {
        fun onAudioDataRecord(data: ByteArray?, byteSize:Int)
        fun onErrorCode(code: StreamPushInstance.StateCode)

        fun onLogTest(log: String)
    }

    private var recordService: ScreenRecordService? = null
    private var serviceIsBind = false

    private var dataRecordCallBack: DataRecordCallBack? = null

    private var inputSurface: Surface? = null
    private var screenWith: Int? = null
    private var screenHeight: Int? = null
    private var inputFps: Int? = null

    private val recordServiceConn = object : ServiceConnection {

        private var mediaProjection: MediaProjection? = null

        fun setProjection(mediaProjection: MediaProjection?) {
            this.mediaProjection = mediaProjection
        }

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            recordService = (service as? ScreenRecordService.LocalBinder)?.getService()

            mediaProjection?.let { projection ->
                inputSurface?.let { sur ->
                    recordService?.setEcodeInputSurface(
                        sur,
                        screenWith ?: 0,
                        screenHeight ?: 0,
                        inputFps ?: 0
                    )
                    recordService?.setDataRecordCallBack(dataRecordCallBack)
                    recordService?.startCapture(projection)
                }
            }

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            recordService?.stopCapture()
            recordService = null
            dataRecordCallBack?.onErrorCode(StreamPushInstance.StateCode.SERVICE_DISCON_ERROR)
            dataRecordCallBack?.onLogTest("service disconnect")
        }

    }

    fun startRecording(mediaProjection: MediaProjection?) {
        val recordServiceIntent =
            Intent(appContext?.applicationContext, ScreenRecordService::class.java)
        recordServiceConn.setProjection(mediaProjection)
        serviceIsBind = appContext?.bindService(
            recordServiceIntent,
            recordServiceConn,
            Context.BIND_AUTO_CREATE
        ) ?: false
    }

    fun resumeRecording() {
        recordService?.resumeCapture()
    }

    fun pauseRecording() {
        recordService?.pauseCapture()
    }

    fun setEcodeInputSurface(
        inputSurface: Surface?,
        screenWith: Int?,
        screenHeight: Int?,
        inputFps: Int?
    ) {
        this.inputSurface = inputSurface
        this.screenWith = screenWith
        this.screenHeight = screenHeight
        this.inputFps = inputFps
    }

    fun reqRecordPerAndStart() {
        val intent = Intent(appContext, ScreenCaptureRequestPerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appContext?.startActivity(intent)
    }

    fun stopCapture() {
        recordService?.stopCapture()
        if (serviceIsBind) {
            appContext?.unbindService(recordServiceConn)
            serviceIsBind = false
        }
    }

}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCaptureRequestPerActivity : Activity() {

    companion object {
        private const val REQUEST_CODE = 1001
    }

    private var mMediaProjectionManager: MediaProjectionManager? = null

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        (this.applicationContext.getSystemService(Service.MEDIA_PROJECTION_SERVICE)
                as? MediaProjectionManager)?.let {
            mMediaProjectionManager = it
            val intent = mMediaProjectionManager?.createScreenCaptureIntent()
            try {
                this.startActivityForResult(intent, REQUEST_CODE)
            } catch (e: Exception) {
                StreamPushInstance.instance.hasPerAndStartRecord(null)
                finish()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (intent != null) {
            val projection = mMediaProjectionManager?.getMediaProjection(resultCode, intent)
            StreamPushInstance.instance.hasPerAndStartRecord(projection)
        } else {
            StreamPushInstance.instance.hasPerAndStartRecord(null)
        }

        finish()
    }

}
