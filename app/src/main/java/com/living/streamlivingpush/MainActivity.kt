package com.living.streamlivingpush

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.record.tool.record.video.screen.service.PerReqForegroundService
import kotlinx.android.synthetic.main.activity_main.*


@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    companion object {
    }

    private var videoBitRate = 8000
    private var videoFps = 30
    private var videoWith = 1280
    private var videoHeight = 720

    private var audioBitRate = 64


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
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

        pause?.setOnClickListener {
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
        }

        resetBnt?.setOnClickListener {
            val fps = editFps?.text.toString().toInt()
            val bit = editBit?.text.toString().toInt()

            videoBitRate = bit
            videoFps = fps

            StreamPushInstance.instance.reset(
                    videoBitRate,
                    videoFps,
                    audioBitRate
            )

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

        val pushUrl = pushUrlEdit.text.toString()

        StreamPushInstance.instance.initRecoderAndEncoder()
        StreamPushInstance.instance.prepareRecord(
                videoBitRate,
                videoFps,
                videoWith,
                videoHeight,
                audioBitRate
        )

        val cameraView = StreamPushInstance.instance.getView()
        cameraPreviewView.addView(cameraView)

        //StreamPushInstance.instance.startRecordAndSendData("rtmp://192.168.26.242:1935/test/room")

        val p =
                "rtmp://tx-test-publish.xxqapp.cn/xxq-live/SLT30T1804027500439434496T192890947374980029T3588307572623180800?txSecret=8171d198c36d476cd6ee0684f653b436&txTime=61656B76"
        StreamPushInstance.instance.startRecordAndSendData(p)

    }

    private fun stopPush() {
        StreamPushInstance.instance.stopRecordAndDestory()
    }

}