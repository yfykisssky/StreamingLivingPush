package com.play

import com.living.streamlivingpush.R
import com.record.tool.record.video.gl.render.opengl.ImgDrawFilter

class ArrowDrawFilter: ImgDrawFilter() {
    override fun getImgResId(): Int {
        return R.drawable.arrow_test
    }

    override fun flipHorizontal(): Boolean {
        return false
    }

    override fun flipVertical(): Boolean {
        return false
    }
}