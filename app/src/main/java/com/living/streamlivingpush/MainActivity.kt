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

    private var url = "rtmp://tx-test-publish.xxqapp.cn/xxq-live/SLT30T1804027493040682240T192531130384600009T3554994123251659008?txSecret=cba0b776a35628b2f3b67b76b274477d&txTime=61471F11"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)

        start?.setOnClickListener{
            PerReqForegroundService.startService()

            startPush()
        }

        stop?.setOnClickListener{
            PerReqForegroundService.stopService()

            stopPush()
        }

        pause?.setOnClickListener{

        }

        resume?.setOnClickListener{

        }

    }

    fun startPush() {
        StreamPushInstance.instance.initRecoderAndEncoder()
        StreamPushInstance.instance.prepareRecord(8000, 30, 1280, 720, 64000)
        StreamPushInstance.instance.startRecordAndSendData(url)
    }

    fun stopPush() {
        StreamPushInstance.instance.stopRecordAndDestory()
    }
}