package com.living.streamlivingpush

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.push.StreamRtmpScreenPushInstance
import com.living.streamlivingpush.push.StreamSocketScreenPushInstance
import com.record.tool.record.video.screen.service.PerReqForegroundService
import kotlinx.android.synthetic.main.activity_main.*


@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

    private var videoBitRate = 8000
    private var videoFps = 30
    private var videoWith = 1920
    private var videoHeight = 1080

    private var audioBitRate = 64

    private var pushInstance= StreamRtmpScreenPushInstance.instance

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
        pushInstance.initEncodeSettings(  videoBitRate,
            videoFps,
            videoWith,
            videoHeight,
            audioBitRate)
        pushInstance.startPushing("")
    }

    private fun stopPush() {
        pushInstance.stopPushing()
    }

}