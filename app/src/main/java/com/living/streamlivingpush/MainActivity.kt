package com.living.streamlivingpush

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.living.streamlivingpush.base.BaseStreamPushInstance
import com.living.streamlivingpush.instances.interfaces.IInstance
import com.living.streamlivingpush.instances.local.StreamCamLocaInstance
import com.living.streamlivingpush.instances.local.interfaces.ILocal
import com.living.streamlivingpush.instances.push.interfaces.IRtmpPush
import com.living.streamlivingpush.instances.push.interfaces.ISocketPush
import com.living.streamlivingpush.record.interfaces.ICamRecord
import com.living.streamlivingpush.record.interfaces.IRecord
import com.opencv.OpenCvFaceCheckTool
import com.push.tool.socket.HostTransTool
import com.record.tool.record.video.screen.floatwindow.StmFloatWindowHelper
import com.record.tool.record.video.screen.service.ProjectionForegroundService
import kotlinx.android.synthetic.main.activity_main.*


@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    private var videoBitRate = 8000
    private var videoFps = 30
    private var videoWith = 720
    private var videoHeight = 1280

    private var audioBitRate = 128

    private var pushInstance: BaseStreamPushInstance = StreamCamLocaInstance()

    private var rtmpPushUrl = ""

    private var stmFloatWindowHelper = StmFloatWindowHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testImageView = findViewById(R.id.testImageViewId)

        OpenCvFaceCheckTool.initClassifier()
        //startActivity(Intent(this,OpenCvTestActivity::class.java))

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

        start?.setOnClickListener {
            ProjectionForegroundService.startService()
            stmFloatWindowHelper.showWindow()

            startPush()

        }

        stop?.setOnClickListener {

            ProjectionForegroundService.stopService()
            stmFloatWindowHelper.closeWindow()

            stopPush()
        }

        sanCode?.setOnClickListener {
            toScan()
        }

        switchCamera?.setOnClickListener {
            (pushInstance as? ICamRecord)?.switchCamera()
        }

        toggleMirror?.setOnClickListener {
            (pushInstance as? ICamRecord)?.toggleMirror()
        }

        privateImg?.setOnClickListener {
            (pushInstance as? IRecord)?.usePriImgPush(true)
        }

        privateImgNo?.setOnClickListener {
            (pushInstance as? IRecord)?.usePriImgPush(false)
        }

        resetBnt?.setOnClickListener {
            val fps = editFps?.text.toString().toInt()
            val bit = editBit?.text.toString().toInt()

            videoBitRate = bit
            videoFps = fps

            pushInstance.resetVideoBit(videoBitRate)

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
        (pushInstance as? IInstance)?.initInstance()
        pushInstance.initEncodeSettings(
            videoBitRate,
            videoFps,
            videoWith,
            videoHeight,
            audioBitRate
        )

        (pushInstance as? ICamRecord)?.let { push ->
            push.setStartUseCamId(1)
            cameraPreviewView?.addView(push.getPreviewView())
        }

        when (pushInstance) {
            is ILocal -> {
                (pushInstance as? ILocal)?.startLocal(false)
            }
            is ISocketPush -> {
                (pushInstance as? ISocketPush)?.startPushing(socketIp, socketPort)
            }
            is IRtmpPush -> {
                (pushInstance as? IRtmpPush)?.startPushing(rtmpPushUrl)
            }
        }

    }

    private fun stopPush() {
        when (pushInstance) {
            is ILocal -> {
                (pushInstance as? ILocal)?.stopLocal()
            }
            is IRtmpPush,
            is ISocketPush -> {
                (pushInstance as? ISocketPush)?.stopPushing()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_SCAN_ONE = 1011

        @SuppressLint("StaticFieldLeak")
        var testImageView: ImageView? = null
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