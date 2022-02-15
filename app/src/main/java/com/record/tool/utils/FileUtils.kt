package com.record.tool.utils

import android.content.Context
import com.living.streamlivingpush.AppApplication
import java.io.File
import java.io.FileOutputStream

class FileUtils {

    companion object {

        private val appContext = AppApplication.appContext

        fun copyFileFromRaw(rawResId: Int, savedFileName: String): String? {

            var filePath: String? = null

            appContext?.resources?.openRawResource(rawResId)?.let { inputStream ->
                val cascadeDir = appContext.getDir("raws", Context.MODE_PRIVATE)
                val cascadeFile = File(cascadeDir, savedFileName)
                val outStream = FileOutputStream(cascadeFile)
                try {
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                    filePath = cascadeFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    inputStream.close()
                    outStream.close()
                }
            }

            return filePath

        }
    }


}