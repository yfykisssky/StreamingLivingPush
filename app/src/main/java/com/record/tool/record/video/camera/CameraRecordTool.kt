package com.record.tool.record.video.camera

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.StreamPushInstance
import com.record.tool.record.video.camera.service.CameraRecordService

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

    private var recordService: CameraRecordService? = null
    private var serviceIsBind = false

    private var dataRecordCallBack: DataRecordCallBack? = null

    private var inputSurface: Surface? = null
    private var screenWith: Int? = null
    private var screenHeight: Int? = null
    private var inputFps: Int? = null

    private var cameraPreviewView: TextureView? = null

    private val recordServiceConn = object : ServiceConnection {

        private var useCameraId: Int = 0

        fun setCameraId(useCameraId: Int) {
            this.useCameraId = useCameraId
        }

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            recordService = (service as? CameraRecordService.LocalBinder)?.getService()

            inputSurface?.let { sur ->
                recordService?.setEcodeInputSurface(
                    sur,
                    screenWith ?: 0,
                    screenHeight ?: 0,
                    inputFps ?: 0
                )
                recordService?.setDataRecordCallBack(dataRecordCallBack)
                recordService?.startCapture(useCameraId, getPreviewView())
            }

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            recordService?.stopCapture()
            recordService = null
            dataRecordCallBack?.onErrorCode(StreamPushInstance.StateCode.SERVICE_DISCON_ERROR)
            dataRecordCallBack?.onLogTest("service disconnect")
        }

    }

    fun startCapture(useCameraId: Int) {
        val recordServiceIntent =
            Intent(appContext?.applicationContext, CameraRecordService::class.java)
        recordServiceConn.setCameraId(useCameraId)
        serviceIsBind = appContext?.bindService(
            recordServiceIntent,
            recordServiceConn,
            Context.BIND_AUTO_CREATE
        ) ?: false
    }

    fun getPreviewView(): TextureView? {
        appContext?.let {
            if (cameraPreviewView == null) {
                cameraPreviewView = TextureView(it)
            }
        }
        return cameraPreviewView
    }

    fun switchCamera(){
        recordService?.switchCamera()
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

    fun stopCapture() {
        recordService?.stopCapture()
        if (serviceIsBind) {
            appContext?.unbindService(recordServiceConn)
            serviceIsBind = false
        }
    }

}
