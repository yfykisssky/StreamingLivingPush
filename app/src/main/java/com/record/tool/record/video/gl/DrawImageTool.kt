package com.record.tool.record.video.gl

import android.opengl.GLES20
import com.play.ArrowDrawFilter
import com.play.PostDrawFilter
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

    private var drawFiltersTop = ArrayList<ImgDrawFilter>()
    private var drawFiltersBottom = ArrayList<ImgDrawFilter>()

    private var imgArrowDrawFilter: ImgDrawFilter? = null

    private var pointX = 0.0f
    private var pointY = 0.0f

    private var postStartX = -1.0f

    private var yPoints =
        floatArrayOf(
            -0.7f, -0.75f, -0.8f, -0.85f, -0.9f,
            -0.8f, -0.75f, -0.7f, -0.8f, -0.7f,
            -0.9f, -0.7f, -0.8f, -0.85f, -0.7f,
            -0.8f, -0.7f, -0.9f, -0.85f, -0.7f
        )

    fun updateHeadCheckPoints(x: Float, y: Float) {
        pointX = x
        pointY = y
    }

    fun onDrawTexture(inputTextureId: Int): Int {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer?.frameBufferId ?: 0)

        GLES20.glViewport(0, 0, inputWidth, inputHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        nomalDrawFilter?.onDraw(inputTextureId, mGLCubeBuffer, mGLTextureBuffer)

        imgArrowDrawFilter?.updateShowOnLocation(pointX, pointY)
        imgArrowDrawFilter?.onDraw()

        drawFiltersTop.forEachIndexed { index, filter ->
            val pointX = postStartX + index * 0.3f
            filter.updateShowOnLocation(pointX, -yPoints[index])
            filter.onDraw()
        }

        drawFiltersBottom.forEachIndexed { index, filter ->
            val pointX = postStartX + index * 0.3f
            filter.updateShowOnLocation(pointX, yPoints[index])
            filter.onDraw()
        }

        postStartX -= 0.01f

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

        imgArrowDrawFilter = ArrowDrawFilter()
        imgArrowDrawFilter?.init()
        imgArrowDrawFilter?.updateDrawPanelSize(width, height)
        imgArrowDrawFilter?.updateShowScaleWithWidth(0.2f)

        for (i in 1..20) {
            drawFiltersTop.add(createPostDrawFilter(width, height, 0.05f, false))
        }

        for (i in 1..20) {
            drawFiltersBottom.add(createPostDrawFilter(width, height, 0.05f, true))
        }

    }

    private fun createPostDrawFilter(
        width: Int,
        height: Int,
        scaleWithWith: Float,
        filpVertical: Boolean
    ): PostDrawFilter {
        val filter = PostDrawFilter(filpVertical)
        filter.init()
        filter.updateDrawPanelSize(width, height)
        filter.updateShowScaleWithWidth(scaleWithWith)
        return filter
    }

    fun destory() {
        mFrameBuffer?.uninitialize()
        mFrameBuffer = null

        nomalDrawFilter?.destroy()
        imgArrowDrawFilter?.destroy()
    }


}