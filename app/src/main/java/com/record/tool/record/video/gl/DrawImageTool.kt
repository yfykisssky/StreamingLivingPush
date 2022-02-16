package com.record.tool.record.video.gl

import android.opengl.GLES20
import com.record.tool.record.video.gl.basic.FrameBuffer
import com.record.tool.record.video.gl.render.opengl.GPUImageFilter
import com.record.tool.record.video.gl.render.opengl.ImgDrawFilter
import com.record.tool.record.video.gl.render.opengl.OpenGlUtils
import java.nio.FloatBuffer

class DrawImageTool {

    private var inputWidth = 0
    private var inputHeight = 0
    private var mFrameBuffer: FrameBuffer? = null
    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null
    private var nomalDrawFilter: GPUImageFilter? = null

    private var imgDrawFilter: ImgDrawFilter? = null

    private var pointX = 0.0f
    private var pointY = 0.0f

    fun updatePoints(x: Float, y: Float) {
        pointX = x
        pointY = y
    }

    fun onDrawTexture(inputTextureId: Int): Int {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer?.frameBufferId ?: 0)

        GLES20.glViewport(0, 0, inputWidth, inputHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        nomalDrawFilter?.onDraw(inputTextureId, mGLCubeBuffer, mGLTextureBuffer)

        imgDrawFilter?.updateShowOnLocation(pointX, pointY)
        imgDrawFilter?.onDraw()

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        return mFrameBuffer?.textureId ?: 0
    }

    fun init(width: Int, height: Int) {

        inputWidth = width
        inputHeight = height

        val cubeAndTextureBuffer = OpenGlUtils.nomalCubeAndTextureBuffer(width, height)

        mGLCubeBuffer = OpenGlUtils.getCubeBuffer(cubeAndTextureBuffer.first)
        mGLTextureBuffer = OpenGlUtils.getTextBuffer(cubeAndTextureBuffer.second)

        mFrameBuffer = FrameBuffer(width, height)
        mFrameBuffer?.initialize()

        nomalDrawFilter = GPUImageFilter()
        nomalDrawFilter?.init()

        imgDrawFilter = ImgDrawFilter()
        imgDrawFilter?.init()
        imgDrawFilter?.updateDrawPanelSize(width, height)
        imgDrawFilter?.updateShowScaleWithWidth(0.2f)
        imgDrawFilter?.copyToTemp()

    }

    fun destory() {
        mFrameBuffer?.uninitialize()
        mFrameBuffer = null

        nomalDrawFilter?.destroy()
        imgDrawFilter?.destroy()
    }


}