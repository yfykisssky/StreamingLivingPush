package com.push.tool.usbaoa

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.widget.Toast
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.base.BasePushTool
import com.push.tool.socket.DataEncodeTool
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class UsbConnectTool : BasePushTool() {

    companion object {
        private var ACTION_USB_PERMISSION = ""
        private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

        private const val AOA_MODEL_NAME = "PushingAOA"
        private const val READ_DATA_BYTES_SIZE = 1

        private const val RW_DATA_TIME_OUT = 500

        private const val CHECK_TIME_OUT_READ_DATA = 200L

        private const val CONNECT_TIME_ACK = 100
    }

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var fd: FileDescriptor? = null
    private var context: Context? = null

    private var timerDis: Disposable? = null

    private var outputStream: FileOutputStream? = null
    private var inputStream: FileInputStream? = null

    private var writeDataThread: Thread? = null
    private var readDataThread: Thread? = null

    @Volatile
    private var isRegisListener = false

    private var isConnecting = false
    private var lastReadDataTimeStamp = Long.MAX_VALUE

    private var lastWriteTimeStamp = 0L

    private var usbConnectCallBack: UsbConnectCallBack? = null
    fun setUsbConnectCallBackListener(usbConnectCallBack: UsbConnectCallBack?) {
        this.usbConnectCallBack = usbConnectCallBack
    }

    private fun refreshWriteTimeStamp() {
        lastWriteTimeStamp = System.currentTimeMillis()
    }

    interface UsbConnectCallBack {
        fun onConnect()
        fun onDisConnect()
        fun onRefused()

        fun onWiteDataError()
        fun onReadDataError()

        fun onReadData(byteArray: ByteArray)

        fun onLogOut(log: String)
    }

    override fun addVideoFrame(frame: VideoFrame) {
        super.addVideoFrame(frame)
        queueVideoFrame?.add(frame)
    }

    override fun addAudioFrame(frame: AudioFrame) {
        super.addAudioFrame(frame)
        queueAudioFrame?.add(frame)
    }

    fun init(canonicalName: String?, context: Context?) {
        ACTION_USB_PERMISSION = "$canonicalName.usb_permission"
        this.context = context
    }

    fun startConnect(intent: Intent): Boolean {

        getUsbManager()?.let { usbManager ->
            if (usbManager.accessoryList != null) {
                usbManager.accessoryList.forEach { accessory ->
                    if (accessory.model == AOA_MODEL_NAME) {
                        checkAndConnect(intent, accessory)
                        return true
                    }
                }
            }
        }

        return false
    }

    fun stopConnect() {

        endTimer()

        synchronized(isRegisListener) {
            if (isRegisListener) {
                try {
                    context?.unregisterReceiver(mAccessoryPermissionReceiver)
                } catch (e: IllegalArgumentException) {
                } finally {
                    isRegisListener = false
                }
            }
        }

        destoryConnect()
    }

    private fun destoryConnect() {
        try {
            fileDescriptor?.close()
            outputStream?.close()
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        isConnecting = false

        outputStream = null
        inputStream = null

        readDataThread?.join()
        writeDataThread?.join()

        readDataThread = null
        writeDataThread = null

    }

    private val mAccessoryPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intentRec: Intent?) {

            intentRec?.let { intent ->
                when (intent.action) {
                    //UsbManager.ACTION_USB_ACCESSORY_DETACHED回调过慢
                    //ACTION_USB_STATE部分机型也比较慢
                    ACTION_USB_STATE -> {
                        if (!intent.getBooleanExtra("connected", false)) {
                            stopConnect()
                            usbConnectCallBack?.onDisConnect()
                            return
                        } else {
                        }
                    }
                    ACTION_USB_PERMISSION -> {
                        if (judgeHasAccess(intent)) {
                            getAccessory(intent)?.let { accessory ->
                                toConnect(accessory)
                            }
                        } else {
                            usbConnectCallBack?.onRefused()
                        }
                    }
                    else -> {
                    }
                }

            }

        }
    }

    private fun getUsbManager(): UsbManager? {
        return context?.getSystemService(Context.USB_SERVICE) as? UsbManager?
    }

    private fun getAccessory(intent: Intent): UsbAccessory? {
        return (intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_ACCESSORY)
                as? UsbAccessory?)
    }

    private fun judgeHasAccess(intent: Intent): Boolean {
        return intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
    }

    private fun checkAndConnect(intent: Intent, accessory: UsbAccessory?) {
        val hasPer = checkPerAndRequest(intent, accessory)
        if (hasPer) {
            toConnect(accessory)
        }
    }

    private fun checkPerAndRequest(intent: Intent, accessory: UsbAccessory?): Boolean {
        getUsbManager()?.let { usbManager ->

            if (!judgeHasAccess(intent)) {

                val permissionIntent = PendingIntent.getBroadcast(
                    context, 0,
                    Intent(ACTION_USB_PERMISSION), 0
                )
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
                filter.addAction(ACTION_USB_STATE)
                synchronized(isRegisListener) {
                    if (!isRegisListener) {
                        try {
                            context?.registerReceiver(mAccessoryPermissionReceiver, filter)
                            isRegisListener = true
                        } catch (e: IllegalArgumentException) {
                        }
                    }
                }

                usbManager.requestPermission(accessory, permissionIntent)

                return false
            } else {
                return true
            }
        }
        return false
    }

    private fun toConnect(accessory: UsbAccessory?) {

        try {
            getUsbManager()?.let { usbManager ->
                //强制重置
                destoryConnect()
                fileDescriptor = usbManager.openAccessory(accessory)
                if (fileDescriptor == null) {
                    usbConnectCallBack?.onDisConnect()
                    return
                }
                fd = fileDescriptor?.fileDescriptor
                inputStream = FileInputStream(fd)
                outputStream = FileOutputStream(fd)
                isConnected()
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }

    }

    inner class WriteDataThread : Thread() {

        private fun getSendBytes(): ByteArray? {

            queueVideoFrame?.poll()?.let { frame ->
                val byteData = frame.byteArray
                if (byteData?.isNotEmpty() == true) {
                    return DataEncodeTool.addVideoExtraData(byteData, frame.timestamp)
                }
            }
            queueAudioFrame?.poll()?.let { frame ->
                val byteData = frame.byteArray
                if (byteData?.isNotEmpty() == true) {
                    return DataEncodeTool.addAudioExtraData(byteData, frame.timestamp)
                }
            }

            //心跳包
            if ((System.currentTimeMillis() - lastWriteTimeStamp) > CONNECT_TIME_ACK) {
                return DataEncodeTool.getSocketAckExtraData()
            }

            return null
        }


        override fun run() {
            super.run()
            refreshWriteTimeStamp()
            usbConnectCallBack?.onLogOut("begin write")
            while (isConnecting) {

                val sendBytes = getSendBytes()
                if (sendBytes != null) {
                    usbConnectCallBack?.onLogOut("start write")
                    writeDataToUsb(sendBytes)
                    usbConnectCallBack?.onLogOut("write end")
                    refreshWriteTimeStamp()
                }

            }
        }
    }

    @Synchronized
    fun writeDataToUsb(byteArray: ByteArray) {
        try {
            outputStream?.write(byteArray)
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            outputStream = null
            usbConnectCallBack?.onWiteDataError()
        }
    }

    private fun isConnected() {
        usbConnectCallBack?.onConnect()
        startToReadData()
    }

    inner class ReadDataThread : Runnable {
        override fun run() {
            lastReadDataTimeStamp = Long.MAX_VALUE
            try {
                while (isConnecting) {
                    val readBytes = ByteArray(READ_DATA_BYTES_SIZE)
                    lastReadDataTimeStamp = System.currentTimeMillis()
                    inputStream?.read(readBytes)
                    lastReadDataTimeStamp = Long.MAX_VALUE
                    usbConnectCallBack?.onReadData(readBytes)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                inputStream = null
                isConnecting = false
                usbConnectCallBack?.onReadDataError()
            }
        }
    }

    private fun startToReadData() {
        isConnecting = true
        readDataThread = Thread(ReadDataThread())
        readDataThread?.start()
    }

    private fun startToWriteData() {
        writeDataThread = Thread(WriteDataThread())
        writeDataThread?.start()
    }

    fun checkReadDataTimeOut(): Boolean {
        val currentTimeStamp = System.currentTimeMillis()
        return (currentTimeStamp - lastReadDataTimeStamp) >= RW_DATA_TIME_OUT
    }

    fun isRealConnect(){
        queueVideoFrame = LinkedBlockingQueue<VideoFrame>(Integer.MAX_VALUE)
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>(Integer.MAX_VALUE)

        //开始监测心跳是否超时
        startCheckTimer()

        startToWriteData()
    }

    private fun startCheckTimer() {
        timerDis?.dispose()
        io.reactivex.Observable.interval(
            CHECK_TIME_OUT_READ_DATA,
            TimeUnit.MILLISECONDS
        ).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<Long?> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    timerDis = d
                }

                override fun onNext(time: Long) {
                    if (checkReadDataTimeOut()) {
                        usbConnectCallBack?.onDisConnect()
                    }
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    private fun endTimer() {
        timerDis?.dispose()
    }

}