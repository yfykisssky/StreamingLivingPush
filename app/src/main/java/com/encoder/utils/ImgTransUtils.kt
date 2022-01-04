package com.encoder.utils

import android.content.Context
import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable

class ImgTransUtils {
    companion object {
        fun getBitmapsFromGif(con: Context, rawResId: Int): ArrayList<Bitmap> {
            val list = ArrayList<Bitmap>()
            val gifResources = GifDrawable(con.resources, rawResId)
            val totalCount = gifResources.numberOfFrames

            for (index in 0 until totalCount) {
                list.add(gifResources.seekToFrameAndGet(index))
            }
            return list
        }
    }
}