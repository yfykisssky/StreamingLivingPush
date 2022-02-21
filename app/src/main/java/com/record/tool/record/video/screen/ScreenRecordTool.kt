package com.record.tool.record.video.screen

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.utils.SysUtils

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecordManager {


    private var appContext = AppApplication.appContext

    interface DataCallBack {
        fun onTextureVideoFrame(frame: TextureVideoFrame)
        fun onStoped()
        fun onRefused()
    }

    private var dataCallBack: DataCallBack? = null
    fun setDataCallBack(dataCallBack: DataCallBack?) {
        this.dataCallBack = dataCallBack
    }

    private var mScreenCapture: CustomScreenCapture? = null

    private var useWith: Int = 0
    private var useHeight: Int = 0
    private var useFps: Int = 0

    companion object {
        private val CLASS_TAG_NAME = this::class.java.simpleName
        var perReqHashMap = HashMap<String, PerReqResultCallBack?>()
        fun getCallBackListener(): PerReqResultCallBack? {
            return perReqHashMap[CLASS_TAG_NAME]
        }
    }

    interface PerReqResultCallBack {
        fun onResult(projection: MediaProjection?)
    }

    fun startCapture(projection: MediaProjection?) {
        if (mScreenCapture == null) {
            mScreenCapture = CustomScreenCapture()
            mScreenCapture?.setRecordDataCallBack(object :
                CustomScreenCapture.RecordDataCallBack {
                override fun onRecordStoped() {

                }

                override fun onLogTest(log: String) {

                }

                override fun onDataCallBack(frame: TextureVideoFrame) {
                    dataCallBack?.onTextureVideoFrame(frame)
                }
            })
        }

        mScreenCapture?.updateSettings(
            useWith,
            useHeight,
            SysUtils.getDpi(),
            useFps
        )

        mScreenCapture?.startCapture(projection)

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
        mScreenCapture?.updateSettings(useWith, useHeight, SysUtils.getDpi(), useFps)
    }

    fun reqRecordPerAndStart(callBack: ((projection: MediaProjection?) -> Unit?)?) {

        val perReqResultCallBack = object : PerReqResultCallBack {
            override fun onResult(projection: MediaProjection?) {
                perReqHashMap.clear()
                if (projection == null) {
                    dataCallBack?.onRefused()
                } else {
                    startCapture(projection)
                }
                callBack?.invoke(projection)
            }
        }

        perReqHashMap[CLASS_TAG_NAME] = perReqResultCallBack

        val intent = Intent(appContext, ScreenCaptureRequestPerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appContext?.startActivity(intent)
    }

    fun stopCapture() {
        perReqHashMap.clear()
        mScreenCapture?.stopCapture()
    }

    fun startPushImage() {
        mScreenCapture?.startPushImage()
    }

    fun stopPushImage() {
        mScreenCapture?.stopPushImage()
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
                ScreenRecordManager.getCallBackListener()?.onResult(null)
                finish()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (intent != null) {
            val projection = mMediaProjectionManager?.getMediaProjection(resultCode, intent)
            ScreenRecordManager.getCallBackListener()?.onResult(projection)
        } else {
            ScreenRecordManager.getCallBackListener()?.onResult(null)
        }

        finish()
    }

}
