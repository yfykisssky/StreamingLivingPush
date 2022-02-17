package com.play

import com.living.streamlivingpush.R
import com.record.tool.record.video.gl.render.opengl.ImgDrawFilter

class PostDrawFilter constructor(private val filpVertical: Boolean = false) : ImgDrawFilter() {
    override fun getImgResId(): Int {
        return R.drawable.post_test
    }

    override fun flipHorizontal(): Boolean {
        return false
    }

    override fun flipVertical(): Boolean {
        return filpVertical
    }
}