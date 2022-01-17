package com.push.tool.usbaoa

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.activity.base.BasePushActivity
import com.living.streamlivingpush.R
import kotlinx.android.synthetic.main.activity_usb_aoa.*

@RequiresApi(Build.VERSION_CODES.M)
class AoaReceiveActivity : BasePushActivity() {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val channelName = AoaReceiveActivity::class.java.canonicalName
    private var logState = ""

    private fun setUsbStateListener() {
        pushInstance.setUsbConnectCallBackListener(object : UsbConnectTool.UsbConnectCallBack {
            override fun onConnect() {
                logState("conn")
            }

            override fun onDisConnect() {
                logState("dis conn")
            }

            override fun onRefused() {

            }

            override fun onWiteDataError() {
                logState("write err")
            }

            override fun onReadDataError() {
                logState("read err")
            }

            override fun onReadData(byteArray: ByteArray) {
                //logState("data read")
            }

            override fun onLogOut(log: String) {
                logState(log)
            }

        })
    }

    private fun logState(stateStr: String) {
        runOnUiThread {
            logState += "\n"
            logState += stateStr
            state?.text = logState
        }
    }

    private fun clearState() {
        runOnUiThread {
            state?.text = ""
        }
    }

    private fun initView(){
        startBnt?.setOnClickListener {
            startPush()
        }
        stopBnt?.setOnClickListener {
            stopPush()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_aoa)

        initView()

        pushInstance.updateContext(this)
        pushInstance.updateActivityName(channelName)

        initTool()

        setUsbStateListener()
        checkUsbAndConnect()
    }

    private fun checkUsbAndConnect() {
        clearState()
        pushInstance.startUsbConnect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            checkUsbAndConnect()
        }

    }

}