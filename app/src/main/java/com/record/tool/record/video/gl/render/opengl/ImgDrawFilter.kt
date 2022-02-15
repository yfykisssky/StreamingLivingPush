package com.record.tool.record.video.gl.render.opengl

import android.graphics.BitmapFactory
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.R
import java.nio.FloatBuffer

class ImgDrawFilter : GPUImageFilter() {

    companion object {
        val IMAGE_CUBE: FloatArray = FloatArray(OpenGlUtils.CUBE.size)
    }

    private val appContext = AppApplication.appContext

    private var imgTextureId = OpenGlUtils.NO_TEXTURE

    private var sizeWidth = 0
    private var sizeHeight = 0

    private var imageWidth = 0
    private var imageHeight = 0

    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null

    private fun resetCube() {
        val copySize = OpenGlUtils.CUBE.size
        System.arraycopy(OpenGlUtils.CUBE, 0, IMAGE_CUBE, 0, copySize)
    }

    private fun loadBitmapId(resId: Int) {
        if (imgTextureId == OpenGlUtils.NO_TEXTURE) {
            val pauseBitmap =
                BitmapFactory.decodeResource(appContext?.resources, resId)
            imgTextureId = OpenGlUtils.loadBitmapTexture(pauseBitmap, OpenGlUtils.NO_TEXTURE)
            imageWidth = pauseBitmap.width
            imageHeight = pauseBitmap.height
            pauseBitmap.recycle()
        }
    }

    fun updateDrawPanelSize(sizeWidth: Int, sizeHeight: Int) {
        this.sizeWidth = sizeWidth
        this.sizeHeight = sizeHeight

        //归一化比例参数，也可以构造mat在shader中转换
        val scale = this.sizeWidth.toFloat() / this.sizeHeight.toFloat()

        for (index in IMAGE_CUBE.indices) {
            if (index % 2 == 1) {
                IMAGE_CUBE[index] = IMAGE_CUBE[index] * scale
            }
        }

    }

    fun updateShowScaleWithWidth(scale: Float) {
        //归一化比例参数，也可以构造mat在shader中转换
        for (index in IMAGE_CUBE.indices) {
            IMAGE_CUBE[index] = IMAGE_CUBE[index] * scale
        }
    }

    fun updateShowOnLocation(fromCenterX: Float, fromCenterY: Float) {
        //归一化比例参数，也可以构造mat在shader中转换
        for (index in IMAGE_CUBE.indices) {
            if (index % 2 == 1) {
                IMAGE_CUBE[index] += fromCenterY
            } else {
                IMAGE_CUBE[index] += fromCenterX
            }
        }
    }

    override fun init() {
        super.init()

        resetCube()

        loadBitmapId(R.drawable.arrow_test)

    }

    override fun onUninit() {
        super.onUninit()

        OpenGlUtils.deleteTexture(imgTextureId)

        resetCube()

        mGLCubeBuffer?.clear()
        mGLTextureBuffer?.clear()
    }

    fun onDraw() {

        val cubeAndTextureBuffer =
            OpenGlUtils.nomalCubeAndTextureBuffer(imageWidth, imageHeight, IMAGE_CUBE)

        mGLCubeBuffer = OpenGlUtils.getCubeBuffer(cubeAndTextureBuffer.first)
        mGLTextureBuffer = OpenGlUtils.getTextBuffer(cubeAndTextureBuffer.second)

        super.onDraw(imgTextureId, mGLCubeBuffer, mGLTextureBuffer)
    }

}