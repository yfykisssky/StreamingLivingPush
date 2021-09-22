package com.record.tool.utils

import android.app.Activity
import com.living.streamlivingpush.AppApplication
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class FileTestUTils {
    companion object {
        fun reqPre(activity: Activity) {
           /* YppPermission.requestStorage(activity, YppPermissionScene.XXQ_NORMAL) { aBoolean ->

            }*/
        }
    }

    private var fos: FileOutputStream? = null
    private var appContext = AppApplication.appContext

    fun initFile() {
        val path =
            appContext?.filesDir?.absolutePath + "/record_" + System.currentTimeMillis() + ".h264"

        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        }

        try {
            //获得一个可写的输入流
            fos = FileOutputStream(file, false)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            if (fos != null) {

            }
        }
    }

    fun endFile() {
        try {
            fos?.flush()
            fos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeData(bytes: ByteArray) {
        fos?.write(bytes)
    }

}