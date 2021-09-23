package com.living.streamlivingpush

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.record.tool.record.video.screen.service.PerReqForegroundService
import kotlinx.android.synthetic.main.activity_main.*

@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : Activity() {

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

            StreamPushInstance.instance.initRecoderAndEncoder()
            StreamPushInstance.instance.prepareRecord(8000, 30, 1280, 720, 64000)

            val cameraView = StreamPushInstance.instance.getView()
            cameraPreviewView.addView(cameraView)

            StreamPushInstance.instance.startRecordAndSendData("")

        }

        resume?.setOnClickListener {
           // cameraCapture?.switchCamera()
            StreamPushInstance.instance.switchCamera()
        }


    }

    private fun startPush() {

        val pushUrl = pushUrlEdit.text.toString()

        StreamPushInstance.instance.initRecoderAndEncoder()
        StreamPushInstance.instance.prepareRecord(8000, 15, 1280, 720, 64000)
        StreamPushInstance.instance.startRecordAndSendData(pushUrl)

    }

    private fun stopPush() {
        StreamPushInstance.instance.stopRecordAndDestory()
    }

}