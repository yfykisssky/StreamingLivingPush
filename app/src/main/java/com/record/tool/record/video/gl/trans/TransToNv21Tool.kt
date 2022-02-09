package com.record.tool.record.video.gl.trans

import android.opengl.GLES20.*
import com.record.tool.record.video.gl.basic.FrameBuffer
import com.record.tool.record.video.gl.render.opengl.OpenGlUtils
import com.record.tool.record.video.gl.render.opengl.Program
import com.record.tool.record.video.gl.render.opengl.Rotation
import com.record.tool.utils.PushLogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

//纹理转nv21 bytes,需要在opengl线程
class TransToNv21Tool {

    companion object {
        private const val VERTEX_SHADER =
            "#version 300 es                            \n" +
                    "layout(location = 0) in vec4 a_position;   \n" +
                    "layout(location = 1) in vec2 a_texCoord;   \n" +
                    "out vec2 v_texCoord;                       \n" +
                    "void main()                                \n" +
                    "{                                          \n" +
                    "   gl_Position = a_position;               \n" +
                    "   v_texCoord = a_texCoord;                \n" +
                    "}                                          \n"

        private const val FRAGMENT_SHADER =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "in vec2 v_texCoord;\n" +
                    "layout(location = 0) out vec4 outColor;\n" +
                    "uniform sampler2D s_TextureMap;\n" +
                    "uniform float u_Offset;  //偏移量 1.0/width\n" +
                    "//Y =  0.299R + 0.587G + 0.114B\n" +
                    "//U = -0.147R - 0.289G + 0.436B\n" +
                    "//V =  0.615R - 0.515G - 0.100B\n" +
                    "const vec3 COEF_Y = vec3( 0.299,  0.587,  0.114);\n" +
                    "const vec3 COEF_U = vec3(-0.147, -0.289,  0.436);\n" +
                    "const vec3 COEF_V = vec3( 0.615, -0.515, -0.100);\n" +
                    "const float UV_DIVIDE_LINE = 2.0 / 3.0;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    vec2 texelOffset = vec2(u_Offset, 0.0);\n" +
                    "    if(v_texCoord.y <= UV_DIVIDE_LINE) {\n" +
                    "        //在纹理坐标 y < (2/3) 范围，需要完成一次对整个纹理的采样，\n" +
                    "        //一次采样（加三次偏移采样）4 个 RGBA 像素（R,G,B,A）生成 1 个（Y0,Y1,Y2,Y3），整个范围采样结束时填充好 width*height 大小的缓冲区；\n" +
                    "\n" +
                    "        vec2 texCoord = vec2(v_texCoord.x, v_texCoord.y * 3.0 / 2.0);\n" +
                    "        vec4 color0 = texture(s_TextureMap, texCoord);\n" +
                    "        vec4 color1 = texture(s_TextureMap, texCoord + texelOffset);\n" +
                    "        vec4 color2 = texture(s_TextureMap, texCoord + texelOffset * 2.0);\n" +
                    "        vec4 color3 = texture(s_TextureMap, texCoord + texelOffset * 3.0);\n" +
                    "\n" +
                    "        float y0 = dot(color0.rgb, COEF_Y);\n" +
                    "        float y1 = dot(color1.rgb, COEF_Y);\n" +
                    "        float y2 = dot(color2.rgb, COEF_Y);\n" +
                    "        float y3 = dot(color3.rgb, COEF_Y);\n" +
                    "        outColor = vec4(y0, y1, y2, y3);\n" +
                    "    }\n" +
                    "    else {\n" +
                    "        //当纹理坐标 y > (2/3) 范围，一次采样（加三次偏移采样）4 个 RGBA 像素（R,G,B,A）生成 1 个（V0,U0,V0,U1），\n" +
                    "        //又因为 VU plane 缓冲区的高度为 height/2 ，VU plane 在垂直方向的采样是隔行进行，整个范围采样结束时填充好 width*height/2 大小的缓冲区。\n" +
                    "        vec2 texCoord = vec2(v_texCoord.x, (v_texCoord.y - UV_DIVIDE_LINE) * 3.0);\n" +
                    "        vec4 color0 = texture(s_TextureMap, texCoord);\n" +
                    "        vec4 color1 = texture(s_TextureMap, texCoord + texelOffset);\n" +
                    "        vec4 color2 = texture(s_TextureMap, texCoord + texelOffset * 2.0);\n" +
                    "        vec4 color3 = texture(s_TextureMap, texCoord + texelOffset * 3.0);\n" +
                    "\n" +
                    "        float v0 = dot(color0.rgb, COEF_V) + 0.5;\n" +
                    "        float u0 = dot(color1.rgb, COEF_U) + 0.5;\n" +
                    "        float v1 = dot(color2.rgb, COEF_V) + 0.5;\n" +
                    "        float u1 = dot(color3.rgb, COEF_U) + 0.5;\n" +
                    "        outColor = vec4(v0, u0, v1, u1);\n" +
                    "    }\n" +
                    "}"

        private const val VERTEX_POS_INDX = 0
        private const val TEXTURE_POS_INDX = 1
    }


    private var mFrameBuffer: FrameBuffer? = null
    private var imgWidth = 0
    private var imgHeight = 0
    private var nv21BufSize = 0
    private var nv21ByteBuf: ByteBuffer? = null

    private var texelOffset = 0F

    private var program: Program? = null
    private var mFboSamplerLoc = 0

    private val cubeBuffer = OpenGlUtils.createNormalCubeVerticesBuffer()
    private val textureBuffer = OpenGlUtils.createTextureCoordsBuffer(Rotation.NORMAL, false, false)

    fun init(inputWidth: Int, inputHeight: Int) {

        imgWidth = inputWidth / 4
        imgHeight = (inputHeight * 1.5).toInt()

        mFrameBuffer = FrameBuffer(imgWidth, imgHeight)
        mFrameBuffer?.initialize()

        program = Program(VERTEX_SHADER, FRAGMENT_SHADER)
        program?.build()

        mFboSamplerLoc = glGetUniformLocation(program?.programId ?: 0, "s_TextureMap")

        //NV21 buffer = width * height * 1.5;
        nv21BufSize = inputWidth * inputHeight * 3 / 2
        nv21ByteBuf = ByteBuffer.allocate(nv21BufSize).order(ByteOrder.LITTLE_ENDIAN)

        //归一化坐标换算
        texelOffset = 1F / inputWidth
    }

    fun trans(inputTextureId: Int): ByteArray {

        PushLogUtils.updateVideoSoftTransTime()

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffer?.frameBufferId ?: 0)

        glViewport(0, 0, imgWidth, imgHeight)
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        glUseProgram(program?.programId ?: 0)

        cubeBuffer.position(0)
        glEnableVertexAttribArray(VERTEX_POS_INDX)
        glVertexAttribPointer(VERTEX_POS_INDX, 2, GL_FLOAT, false, 0, cubeBuffer)

        textureBuffer.position(0)
        glEnableVertexAttribArray(TEXTURE_POS_INDX)
        glVertexAttribPointer(
            TEXTURE_POS_INDX, 2, GL_FLOAT, false, 0,
            textureBuffer
        )

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, inputTextureId)
        glUniform1i(mFboSamplerLoc, 0)

        glUniform1f(glGetUniformLocation(program?.programId ?: 0, "u_Offset"), texelOffset)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glReadPixels(0, 0, imgWidth, imgHeight, GL_RGBA, GL_UNSIGNED_BYTE, nv21ByteBuf)

        val nv21Buf = ByteArray(nv21BufSize)

        nv21ByteBuf?.get(nv21Buf)
        nv21ByteBuf?.clear()

        glDisableVertexAttribArray(VERTEX_POS_INDX)
        glDisableVertexAttribArray(TEXTURE_POS_INDX)

        glBindTexture(GL_TEXTURE_2D, 0)

        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        PushLogUtils.logVideoSoftTransTime()

        return nv21Buf
    }

    fun destory() {
        mFrameBuffer?.uninitialize()
        program?.destroy()
        program = null
        nv21ByteBuf?.clear()
        nv21ByteBuf = null
    }

}