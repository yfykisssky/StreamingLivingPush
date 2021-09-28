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
    private var videoFps = 15
    private var videoWith = 1280
    private var videoHeight = 720

    private var audioBitRate = 6400


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
            //StreamPushInstance.instance.r()

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
                videoWith,
                videoHeight,
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
            "rtmp://wangsu-test-publish.xxqapp.cn/xxq-live-backup/SLW30W1804027493040682240W192531130384600009W3566562346863207168?txSecret=3a16f7d30f4ec57adb602679f0279e8b&txTime=6151A481&wsSecret=a916739a92797034d5428e70c0339ae6&keeptime=86400&wsTime=1632654081"
        StreamPushInstance.instance.startRecordAndSendData(p)

    }

    private fun stopPush() {
        StreamPushInstance.instance.stopRecordAndDestory()
    }

}