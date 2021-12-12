package com.living.streamlivingpush

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.push.StreamRtmpCamPushInstance
import com.living.streamlivingpush.push.StreamRtmpScreenPushInstance
import com.living.streamlivingpush.push.StreamSocketScreenPushInstance
import com.record.tool.record.video.screen.service.PerReqForegroundService
import kotlinx.android.synthetic.main.activity_main.*
import com.huawei.hms.ml.scan.HmsScan

import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions

import com.huawei.hms.hmsscankit.ScanUtil
import android.content.Intent
import com.push.tool.socket.HostTransTool
import com.push.tool.socket.ScanResult


@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    private var videoBitRate = 6000
    private var videoFps = 35
    private var videoWith = 1280
    private var videoHeight = 720

    private var audioBitRate = 1280

    private var pushInstance = StreamRtmpScreenPushInstance.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            10
        )

        start?.setOnClickListener {
            PerReqForegroundService.startService()

            startPush()
        }

        stop?.setOnClickListener {
            PerReqForegroundService.stopService()

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

        pushInstance.startPushing("rtmp://tx-test-publish.xxqapp.cn/xxq-live/SLT30S1804027493040682240S192531130384600009S0S1639044498568?txSecret=f3b53625c5c0fec36b0771396a919584&txTime=61B32712")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            val obj = data.getParcelableExtra(ScanUtil.RESULT) as? HmsScan
            if (obj != null) {
                HostTransTool.str2Obj(obj.originalValue)?.let { scanResult ->
                    val ip = scanResult.ipAddress ?: ""
                    val port = scanResult.port
                    scanCodeResult?.text = "$ip:$port"
                    handleSocket(ip, port)
                }
            }
        }
    }

    private fun handleSocket(ipAddress: String, port: Int) {

    }

}