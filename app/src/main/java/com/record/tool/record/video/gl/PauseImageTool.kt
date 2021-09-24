package com.record.tool.record.video.gl

import android.graphics.BitmapFactory
import android.opengl.EGLContext
import android.os.Handler
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.R
import com.record.tool.record.video.gl.render.EglCore
import com.record.tool.record.video.gl.render.opengl.OpenGlUtils

//隐私模式工具,需要运行在opengl线程
//新建图片纹理并进行替换
class PauseImageTool {

    private var isReplacing = false
    private var pauseImgTextureId = OpenGlUtils.NO_TEXTURE

    private val appContext = AppApplication.appContext

    fun startReplace() {
        isReplacing = true
    }

    fun stopReplace() {
        isReplacing = false
    }

    fun transTexture(frame: TextureVideoFrame): TextureVideoFrame {
        if (!isReplacing) {
            return frame
        }

        loadBitmapId()

        val frameNew = TextureVideoFrame()
        frameNew.textureId = pauseImgTextureId
        frameNew.eglContext14 = frame.eglContext14
        frameNew.width = frame.width
        frameNew.height = frame.height
        frameNew.captureTimeStamp = frame.captureTimeStamp

        return frameNew
    }

    fun destory() {
        OpenGlUtils.deleteTexture(pauseImgTextureId)
    }

    private fun loadBitmapId() {
        if (pauseImgTextureId == OpenGlUtils.NO_TEXTURE) {
            val pauseBitmap =
                BitmapFactory.decodeResource(appContext?.resources, R.drawable.pause_vertical)
            pauseImgTextureId = OpenGlUtils.loadBitmapTexture(pauseBitmap, OpenGlUtils.NO_TEXTURE)
            pauseBitmap.recycle()
        }
    }

}