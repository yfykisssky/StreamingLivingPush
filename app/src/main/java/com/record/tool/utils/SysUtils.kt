package com.record.tool.utils

import android.app.Service
import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.AppApplication

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class SysUtils {

    companion object {

        //4096*2160 4K
        private const val MAX_SCREEN_WITH = 4096
        private const val MAX_SCREEN_HEIGHT = 2160

        private val appContext = AppApplication.appContext

        private fun getDisplay(): Display? {
            try {
                (appContext?.getSystemService(Service.WINDOW_SERVICE) as? WindowManager)?.let { manager ->
                    return manager.defaultDisplay
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        //生成实例的像素密度
        fun getDpi(): Int {
            return 1
        }

        private fun getScreenWithHeight(): Pair<Int, Int> {
            getDisplay()?.let { display ->
                val size = Point()
                display.getRealSize(size)
                return Pair(size.x, size.y)
            }
            return Pair(1, 1)
        }

        fun getLandEncodeWH(): Pair<Int, Int> {
            getScreenWithHeight().let {

                //宽高相反取值
                var width = it.second
                var height = it.first

                if (width % 16 != 0) {
                    width = (width - width % 16) + 16
                }

                if (height % 16 != 0) {
                    height = (height - height % 16) + 16
                }

                if (width > MAX_SCREEN_WITH) {
                    width = MAX_SCREEN_WITH
                }

                if (height > MAX_SCREEN_HEIGHT) {
                    height = MAX_SCREEN_HEIGHT
                }

                return Pair(width, height)
            }
        }

    }

}