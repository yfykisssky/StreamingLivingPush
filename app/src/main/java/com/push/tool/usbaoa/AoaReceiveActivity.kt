package com.push.tool.usbaoa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import com.living.streamlivingpush.R
import kotlinx.android.synthetic.main.activity_usb_aoa.*

class AoaReceiveActivity : Activity() {

    companion object {
        private val TAG = this::class.java.simpleName
        private const val CHECK_TIME_OUT_READDATA = 200L
    }

    private val channelName = AoaReceiveActivity::class.java.canonicalName

    private var usbConnectTool: UsbConnectTool? = null

    //aoa连接状态
    var isAoaConnected = false
        private set

    private fun initUsbTool(context: Context, activityName: String?) {
        usbConnectTool = UsbConnectTool()
        usbConnectTool?.init(activityName, context)
    }

    fun stopConnect() {
        setUsbCloseState()
        usbConnectTool?.stopConnect()
        usbConnectTool = null
    }

    private fun setUsbConnectState() {
        isAoaConnected = true
    }

    private fun setUsbCloseState() {
        isAoaConnected = false
    }

    private fun setUsbStateListener() {
        usbConnectTool?.setUsbConnectCallBackListener(object : UsbConnectTool.UsbConnectCallBack {
            override fun onConnect() {
                setUsbConnectState()
                runOnUiThread{
                    state?.text="conn"
                }

            }

            override fun onDisConnect() {
                setUsbCloseState()
                runOnUiThread {
                    state?.text = "disconn"
                }
            }

            override fun onRefused() {
                setUsbCloseState()
            }

            override fun onWiteDataError() {
                runOnUiThread {
                    state?.text = "w err"
                }
            }

            override fun onReadDataError() {
                runOnUiThread {
                    state?.text = "r err"
                }
            }

            override fun onReadData(byteArray: ByteArray) {
                runOnUiThread {
                    state?.text = "wwwww"
                }
            }

        })
    }

    private fun startUsbConnect(intent: Intent): Boolean {
        return if (usbConnectTool?.startConnect(intent) == true) {
            setUsbStateListener()
            true
        } else {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUsbTool(this, channelName)

        checkUsbAndConnect()
    }

    private fun checkUsbAndConnect() {
        startUsbConnect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) {
            checkUsbAndConnect()
        }

    }

}