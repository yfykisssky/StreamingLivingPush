package com.record.tool.record.video.gl.render.opengl

import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.GLES20
import android.view.View
import android.view.ViewGroup
import com.living.streamlivingpush.AppApplication
import com.record.tool.record.video.gl.utils.BitmapTool
import com.record.tool.record.video.gl.utils.MatTransTool
import java.nio.FloatBuffer

abstract class ImgDrawFilter : GPUImageFilter {

    private val appContext = AppApplication.appContext

    private var imgTextureId = OpenGlUtils.NO_TEXTURE

    private var sizeWidth = 0
    private var sizeHeight = 0

    private var imageWidth = 0
    private var imageHeight = 0

    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null

    private var matTransTool = MatTransTool()
    private var mTextureTransform = 0
    private var mLocPoint = 0

    abstract fun getImgResId(): Int

    abstract fun flipHorizontal(): Boolean
    abstract fun flipVertical(): Boolean

    constructor() : super(
        ShaderUtils.VERTEX_SHADER_WITH_TRANS,
        ShaderUtils.FRAGMENT_SHADER
    )

    private fun resetCube() {
        matTransTool.resetTransMat()
    }

    private fun resetLoc() {
        matTransTool.resetLocVec()
    }

    private fun loadBitmapId(resId: Int) {
        if (imgTextureId == OpenGlUtils.NO_TEXTURE) {

            appContext?.let { con ->
                BitmapTool.getGlBitmapFromRes(con, resId)?.let { bitmap ->
                    imgTextureId = OpenGlUtils.loadBitmapTexture(bitmap, OpenGlUtils.NO_TEXTURE)
                    imageWidth = bitmap.width
                    imageHeight = bitmap.height
                    bitmap.recycle()
                }
            }

            /*   val view = View(appContext)
               view.setBackgroundColor(Color.RED)

               appContext?.let { con ->
                   BitmapTool.getGlBitmapFromView(view, 400,400,720, 1280)?.let { bitmap ->
                       imgTextureId = OpenGlUtils.loadBitmapTexture(bitmap, OpenGlUtils.NO_TEXTURE)
                       imageWidth = bitmap.width
                       imageHeight = bitmap.height
                       bitmap.recycle()
                   }
               }*/

            OpenGlUtils.nomalCubeAndTextureBuffer(
                imageWidth,
                imageHeight,
                flipHorizontal(),
                flipVertical()
            ).let { buffers ->
                mGLCubeBuffer = OpenGlUtils.getCubeBuffer(buffers.first)
                mGLTextureBuffer = OpenGlUtils.getTextBuffer(buffers.second)
            }
        }
    }

    fun updateDrawPanelSize(sizeWidth: Int, sizeHeight: Int) {
        this.sizeWidth = sizeWidth
        this.sizeHeight = sizeHeight
        val scale = this.sizeWidth.toFloat() / this.sizeHeight.toFloat()
        val scaleImg = this.imageWidth.toFloat() / this.imageHeight.toFloat()
        matTransTool.scale(1f, (scale / scaleImg), 1f)
    }

    fun updateShowScaleWithWidth(scale: Float) {
        matTransTool.scale(scale, scale, 1f)
    }

    fun updateShowOnLocation(fromCenterX: Float, fromCenterY: Float) {
        matTransTool.updateLoc(fromCenterX, fromCenterY, 0.0f)
    }

    override fun init() {
        super.init()

        matTransTool.init()

        loadBitmapId(getImgResId())

    }

    override fun onInit() {
        super.onInit()
        mTextureTransform = GLES20.glGetUniformLocation(mProgram.programId, "textureTransform")
        mLocPoint = GLES20.glGetUniformLocation(mProgram.programId, "locPoint")
    }

    override fun onUninit() {
        super.onUninit()

        OpenGlUtils.deleteTexture(imgTextureId)

        resetCube()
        resetLoc()

        mGLCubeBuffer?.clear()
        mGLTextureBuffer?.clear()
    }

    override fun beforeDrawArrays(textureId: Int) {
        super.beforeDrawArrays(textureId)
        GLES20.glUniformMatrix4fv(mTextureTransform, 1, false, matTransTool.getTransMat(), 0)
        GLES20.glUniform4fv(mLocPoint, 1, matTransTool.getLocVec(), 0)
    }

    fun onDraw() {
        super.onDraw(imgTextureId, mGLCubeBuffer, mGLTextureBuffer)
    }

}