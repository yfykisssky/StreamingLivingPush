package com.living.streamlivingpush

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.push.*
import com.record.tool.record.video.screen.service.ProjectionForegroundService
import kotlinx.android.synthetic.main.activity_main.*
import com.huawei.hms.ml.scan.HmsScan

import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions

import com.huawei.hms.hmsscankit.ScanUtil
import android.content.Intent
import com.encoder.VideoSoftEncoder
import com.living.faac.AccFaacEncodeTool
import com.living.x264.X264EncodeTool
import com.push.tool.socket.HostTransTool
import com.record.tool.record.video.screen.floatwindow.StmFloatWindowHelper
import com.record.tool.utils.PingUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    private var videoBitRate = 8000
    private var videoFps = 30
    private var videoWith = 1280
    private var videoHeight = 720

    private var audioBitRate = 128

    private var pushInstance = StreamSocketScreenPushInstance()

    private var checkDis: Disposable? = null
    private var stmFloatWindowHelper = StmFloatWindowHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        VideoSoftEncoder.initBitmaps(this)
        X264EncodeTool().test()

        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            10
        )

        stmFloatWindowHelper.setCallBackListener(object: StmFloatWindowHelper.CallBackListener{
            override fun onChangeBitrate(newBit: Int) {
                pushInstance.resetVideoBit(newBit)
            }
        })

        if (stmFloatWindowHelper.hasNoPer()) {
            stmFloatWindowHelper.startOverlaySettingActivity(this)
        }

        start?.setOnClickListener {
            ProjectionForegroundService.startService()
            stmFloatWindowHelper.showWindow()

            checkDis?.dispose()
            checkDis = Observable.create<Boolean> {
                val result = PingUtils.ping(socketIp)
                it.onNext(result)
                it.onComplete()
            }.subscribeOn(Schedulers.io()).timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    if (it) {
                        startPush()
                    }
                }, {
                }, {})

        }

        stop?.setOnClickListener {

            ProjectionForegroundService.stopService()
            stmFloatWindowHelper.closeWindow()

            stopPush()
        }

        sanCode?.setOnClickListener {
            toScan()
        }

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

        resetBnt?.setOnClickListener {
            val fps = editFps?.text.toString().toInt()
            val bit = editBit?.text.toString().toInt()

            videoBitRate = bit
            videoFps = fps

            /* StreamPushInstance.instance.reset(
                     videoBitRate,
                     videoFps,
                     audioBitRate
             )*/

            updateFpsBitShow()
        }

        updateFpsBitShow()

    }

    @SuppressLint("SetTextI18n")
    private fun updateFpsBitShow() {
        bitrate?.text = videoBitRate.toString() + "kbps"
        fps?.text = videoFps.toString() + "f/s"
    }

    private fun startPush() {
        pushInstance.initTool()
        pushInstance.initEncodeSettings(
            videoBitRate,
            videoFps,
            videoWith,
            videoHeight,
            audioBitRate
        )

        //cameraPreviewView?.addView(pushInstance.getPreviewView())

        pushInstance.startPushing(socketIp, socketPort)
    }

    private fun stopPush() {
        pushInstance.stopPushing()
    }

    companion object {
        const val REQUEST_CODE_SCAN_ONE = 1011
    }

    private fun toScan() {
        ScanUtil.startScan(
            this,
            REQUEST_CODE_SCAN_ONE,
            HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE).create()
        )
    }

    private var socketIp = ""
    private var socketPort = -1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            val obj = data.getParcelableExtra(ScanUtil.RESULT) as? HmsScan
            if (obj != null) {
                HostTransTool.str2Obj(obj.originalValue)?.let { scanResult ->
                    socketIp = scanResult.ipAddress ?: ""
                    socketPort = scanResult.port
                    scanCodeResult?.text = "$socketIp:$socketPort"
                }
            }
        }
    }

}