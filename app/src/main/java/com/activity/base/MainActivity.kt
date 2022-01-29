package com.activity.base

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.encoder.VideoSoftEncoder
import com.living.streamlivingpush.push.StreamAoaScreenPushInstance
import com.record.tool.record.video.screen.floatwindow.StmFloatWindowHelper
import com.record.tool.record.video.screen.service.ProjectionForegroundService


@RequiresApi(Build.VERSION_CODES.M)
open class BasePushActivity : Activity() {

    private var videoBitRate = 8000
    private var videoFps = 30
    private var videoWith = 1280
    private var videoHeight = 720

    private var audioBitRate = 128

    protected var pushInstance = StreamAoaScreenPushInstance()
    private var stmFloatWindowHelper = StmFloatWindowHelper()

    protected fun initTool(){
        pushInstance.initTool()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            10
        )

        stmFloatWindowHelper.setCallBackListener(object : StmFloatWindowHelper.CallBackListener {
            override fun onChangeBitrate(newBit: Int) {
                pushInstance.resetVideoBit(newBit)
            }
        })

        if (stmFloatWindowHelper.hasNoPer()) {
            stmFloatWindowHelper.startOverlaySettingActivity(this)
        }

        /*      start?.setOnClickListener {


                  startPush()

                  checkDis?.dispose()
                  checkDis = Observable.create<Boolean> {
                      val result = PingUtils.ping(socketIp)
                      it.onNext(result)
                      it.onComplete()
                  }.subscribeOn(Schedulers.io()).timeout(5, TimeUnit.SECONDS)
                      .observeOn(AndroidSchedulers.mainThread()).subscribe({
                          if (it) {

                          }
                      }, {
                      }, {})

              }*/

/*        stop?.setOnClickListener {



            stopPush()
        }

        sanCode?.setOnClickListener {
            toScan()
        }*/

        /*     pause?.setOnClickListener {
                 StreamPushInstance.instance.s()
             }

             resume?.setOnClickListener {
                 StreamPushInstance.instance.r()
             }

             switchCamera?.setOnClickListener {
                 StreamPushInstance.instance.switchCamera()
             }

             flip?.setOnClickListener {
                 StreamPushInstance.instance.toogle()
             }*/

/*        resetBnt?.setOnClickListener {
            val fps = editFps?.text.toString().toInt()
            val bit = editBit?.text.toString().toInt()

            videoBitRate = bit
            videoFps = fps

            *//* *//*

            updateFpsBitShow()
        }*/

    }

    protected fun reset(){
      /*  pushInstance.reset(
            videoBitRate,
            videoFps,
            audioBitRate
        )*/
    }

    private fun initFloatWindow(){
        ProjectionForegroundService.startService()
        stmFloatWindowHelper.showWindow()
    }

    private fun initPush(){
        initFloatWindow()

        pushInstance.initEncodeSettings(
            videoBitRate,
            videoFps,
            videoWith,
            videoHeight,
            audioBitRate
        )
    }

    protected fun startPush() {
        initPush()
        pushInstance.startPushing()
        //cameraPreviewView?.addView(pushInstance.getPreviewView())
       // pushInstance.startPushing()
        //pushInstance.startPushing(true)
    }

    protected fun startPush(socketIp: String, socketPort: Int) {
        initPush()
        //cameraPreviewView?.addView(pushInstance.getPreviewView())

        //pushInstance.startPushing(socketIp, socketPort)
    }

    protected fun stopPush() {

        ProjectionForegroundService.stopService()
        stmFloatWindowHelper.closeWindow()

        pushInstance.stopPushing()
    }

    companion object {
        const val REQUEST_CODE_SCAN_ONE = 1011
    }

}