package com.record.tool.utils

import android.graphics.Rect
import android.graphics.YuvImage
import com.living.streamlivingpush.AppApplication
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class FileWriteTool {
    companion object {}

    private var fos: FileOutputStream? = null
    private var appContext = AppApplication.appContext
    private var SAVE_PATH: String = ""

    init {
        SAVE_PATH = (appContext?.filesDir?.absolutePath ?: "") + "/"
    }

    fun createFile(lastFileTypeName: String) {

        val fileName = "record_" + System.currentTimeMillis() + "." + lastFileTypeName
        val path = SAVE_PATH + fileName

        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        }

        try {
            fos = FileOutputStream(file, false)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    fun closeFile() {
        try {
            fos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeData(bytes: ByteArray) {
        fos?.write(bytes)
        fos?.flush()
    }

    //format:ImageFormat.NV21
    fun writeDataToJpg(bytes: ByteArray, imgWidth: Int, imgHeight: Int, format: Int) {
        val image = YuvImage(bytes, format, imgWidth, imgHeight, null)
        image.compressToJpeg(Rect(0, 0, imgWidth, imgHeight), 100, fos)
    }

}