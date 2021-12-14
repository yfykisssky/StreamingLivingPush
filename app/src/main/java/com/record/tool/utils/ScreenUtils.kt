package com.record.tool.utils

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import kotlin.math.roundToInt

class ScreenUtils {

    companion object {
        fun dp2px(context: Context?, dp: Float): Int {
            context?.let { con ->
                val px = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    con.resources.displayMetrics
                )
                return px.roundToInt()
            }
            return (dp * 3).toInt()
        }

        fun getScreenWidth(context: Context?): Int {
            return getDisplayMetrics(context)?.widthPixels ?: 1080
        }

        fun getScreenHeight(context: Context?): Int {
            return getDisplayMetrics(context)?.heightPixels ?: 2160
        }

        private fun getDisplayMetrics(context: Context?): DisplayMetrics? {
            return context?.resources?.displayMetrics
        }

    }

}