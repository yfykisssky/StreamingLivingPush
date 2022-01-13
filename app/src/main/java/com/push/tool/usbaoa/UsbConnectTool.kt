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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class UsbConnectTool {

    companion object {
        private var ACTION_USB_PERMISSION = ""
        private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

        private const val AOA_MODEL_NAME = "PushingAOA"
        private const val READ_DATA_BYTES_SIZE = 1

        private const val RW_DATA_TIME_OUT = 500

        private const val CHECK_TIME_OUT_READDATA = 200L
    }

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var fd: FileDescriptor? = null
    private var context: Context? = null

    private var timerDis: Disposable? = null

    private var outputStream: FileOutputStream? = null
    private var inputStream: FileInputStream? = null

    private var usbConnectCallBack: UsbConnectCallBack? = null

    @Volatile
    private var isRegisListener = false

    private var isReading = false
    private var lastReadDataTimeStamp = Long.MAX_VALUE

    fun setUsbConnectCallBackListener(usbConnectCallBack: UsbConnectCallBack?) {
        this.usbConnectCallBack = usbConnectCallBack
    }

    interface UsbConnectCallBack {
        fun onConnect()
        fun onDisConnect()
        fun onRefused()

        fun onWiteDataError()
        fun onReadDataError()

        fun onReadData(byteArray: ByteArray)
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
        isReading = false
        outputStream = null
        inputStream = null
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
            Toast.makeText(context,e.message,Toast.LENGTH_LONG).show()
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
        //startTimer()
        usbConnectCallBack?.onConnect()
        startToReadData()
    }

    inner class ReadDataThread : Runnable {
        override fun run() {
            lastReadDataTimeStamp = Long.MAX_VALUE
            try {
                while (isReading) {
                    val readBytes = ByteArray(READ_DATA_BYTES_SIZE)
                    lastReadDataTimeStamp = System.currentTimeMillis()
                    inputStream?.read(readBytes)
                    lastReadDataTimeStamp = Long.MAX_VALUE
                    usbConnectCallBack?.onReadData(readBytes)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                inputStream = null
                isReading = false
                usbConnectCallBack?.onReadDataError()
            }
        }
    }

    private fun startToReadData() {
        isReading = true
        Thread(ReadDataThread()).start()
    }

    fun checkReadDataTimeOut(): Boolean {
        val currentTimeStamp = System.currentTimeMillis()
        return (currentTimeStamp - lastReadDataTimeStamp) >= RW_DATA_TIME_OUT
    }


    private fun startTimer() {
        timerDis?.dispose()
        io.reactivex.Observable.interval(CHECK_TIME_OUT_READDATA, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<Long?> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    timerDis = d
                }

                override fun onNext(time: Long) {
                    if (checkReadDataTimeOut()) {

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