package com.record.tool.record.video.gl.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable
import android.view.View


class BitmapTool {

    companion object {
        fun zoomBitmap(sourceBitmap: Bitmap, radio: Float): Bitmap? {
            val bitmapWidth = sourceBitmap.width
            val bitmapHeight = sourceBitmap.height
            val matrix = Matrix()
            matrix.postScale(radio, radio)
            return Bitmap.createBitmap(sourceBitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true)
        }

        fun flipBitmap(
            sourceBitmap: Bitmap,
            flipHorizontal: Boolean,
            flipVertical: Boolean
        ): Bitmap {
            val m = Matrix()
            if (flipHorizontal) {
                m.setScale(-1F, 1F)
            }
            if (flipVertical) {
                m.setScale(1F, -1F)
            }
            val bitmapWidth = sourceBitmap.width
            val bitmapHeight = sourceBitmap.height
            val flipBitmap =
                Bitmap.createBitmap(sourceBitmap, 0, 0, bitmapWidth, bitmapHeight, m, true)
            sourceBitmap.recycle()
            return flipBitmap
        }

        fun getBitmapFromRes(context: Context, resId: Int): Bitmap? {
            return BitmapFactory.decodeResource(context.resources, resId)
        }

        //android加载bitmap会上下镜像，需要翻转
        fun getGlBitmapFromRes(context: Context, resId: Int): Bitmap? {
            val resBitmap = getBitmapFromRes(context, resId)
            return resBitmap?.let { flipBitmap(it, flipHorizontal = false, flipVertical = true) }
        }

        fun getBitmapFromView(
            view: View,
            viewWidth: Int, viewHeight: Int,
            drawWidth: Int, drawHeight: Int
        ): Bitmap? {
            //draw之前需要measure,layout操作
            view.measure(0, 0)
            view.layout(0, 0, viewWidth, viewHeight)
            Bitmap.createBitmap(drawWidth, drawHeight, Bitmap.Config.ARGB_8888)?.let { bitmap ->
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                canvas.setBitmap(null)
                return bitmap
            }
            return null
        }

        //android加载bitmap会上下镜像，需要翻转
        fun getGlBitmapFromView(
            view: View,
            viewWidth: Int, viewHeight: Int,
            drawWidth: Int, drawHeight: Int
        ): Bitmap? {
            val resBitmap = getBitmapFromView(view, viewWidth, viewHeight, drawWidth, drawHeight)
            return resBitmap?.let { flipBitmap(it, flipHorizontal = false, flipVertical = true) }
        }

    }

}