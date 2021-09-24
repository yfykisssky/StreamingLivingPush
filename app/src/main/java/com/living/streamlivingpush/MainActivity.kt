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

    }

    private fun startPush() {

        val pushUrl = pushUrlEdit.text.toString()

        StreamPushInstance.instance.initRecoderAndEncoder()
        StreamPushInstance.instance.prepareRecord(8000000, 30, 1280, 720, 64000)

        val cameraView = StreamPushInstance.instance.getView()
        cameraPreviewView.addView(cameraView)

        StreamPushInstance.instance.startRecordAndSendData("rtmp://tx-test-publish.xxqapp.cn/xxq-live/SLT30T1804027493040682240T192531130384600009T3563750367567716864?txSecret=5816f85737a7fb4206cf1fd5068188cd&txTime=614F15CA")

    }

    private fun stopPush() {
        StreamPushInstance.instance.stopRecordAndDestory()
    }

}