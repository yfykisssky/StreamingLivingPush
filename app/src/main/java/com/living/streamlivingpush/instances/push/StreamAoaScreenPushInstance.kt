package com.living.streamlivingpush.instances.push

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.record.StreamScreenPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.usbaoa.UsbConnectTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamAoaScreenPushInstance : StreamScreenPushInstance() {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
    }

    var isRecordAndEncoding = false
        private set

    private var usbConnectTool: UsbConnectTool? = null

    //真正接受到数据标记开始
    private var isRealConnected = false

    private var usbConnectCallBack: UsbConnectTool.UsbConnectCallBack? = null
    fun setUsbConnectCallBackListener(usbConnectCallBack: UsbConnectTool.UsbConnectCallBack?) {
        this.usbConnectCallBack = usbConnectCallBack
    }

    private var con: Context? = null
    private var activityName: String? = null

    //aoa连接状态
    var isAoaConnected = false
        private set

    fun updateContext(con: Context) {
        this.con = con
    }

    fun updateActivityName(activityName: String?) {
        this.activityName = activityName
    }

    fun initTool() {
        super.initRecoder()
        usbConnectTool = UsbConnectTool()
        usbConnectTool?.init(activityName, con)
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        usbConnectTool?.addVideoFrame(frame)
    }

    override fun onAudioFrameAva(frame: AudioFrame) {
        usbConnectTool?.addAudioFrame(frame)
    }

    private fun setUsbConnectState() {
        isAoaConnected = true
    }

    private fun setUsbCloseState() {
        isAoaConnected = false
        isRealConnected = false
    }

    private fun setUsbStateListener() {
        usbConnectTool?.setUsbConnectCallBackListener(object : UsbConnectTool.UsbConnectCallBack {
            override fun onConnect() {
                setUsbConnectState()
                usbConnectCallBack?.onConnect()
            }

            override fun onDisConnect() {
                setUsbCloseState()
                usbConnectCallBack?.onDisConnect()
            }

            override fun onRefused() {
                setUsbCloseState()
                usbConnectCallBack?.onRefused()
            }

            override fun onWiteDataError() {
                usbConnectCallBack?.onWiteDataError()
            }

            override fun onReadDataError() {
                usbConnectCallBack?.onReadDataError()
            }

            override fun onReadData(byteArray: ByteArray) {
                if (!isRealConnected) {
                    isRealConnected = true
                    //收到心跳字段，真正建立连接
                    usbConnectTool?.isRealConnect()
                }
                usbConnectCallBack?.onReadData(byteArray)
            }

            override fun onLogOut(log: String) {
                usbConnectCallBack?.onLogOut(log)
            }

        })
    }

    fun startUsbConnect(intent: Intent): Boolean {
        return if (usbConnectTool?.startConnect(intent) == true) {
            setUsbStateListener()
            true
        } else {
            false
        }
    }

    fun startPushing() {
        super.startRecode()

        isRecordAndEncoding = true
    }

    fun stopPushing() {
        super.stopRecode()

        setUsbCloseState()
        usbConnectTool?.stopConnect()
        usbConnectTool = null

        isRecordAndEncoding = false

    }

}