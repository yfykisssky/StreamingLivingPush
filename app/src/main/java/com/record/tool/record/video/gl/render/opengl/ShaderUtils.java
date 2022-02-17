package com.record.tool.record.video.gl.render.opengl;

public class ShaderUtils {

    public static final String VERTEX_SHADER = ""
        + "attribute vec4 position;\n"
        + "attribute vec4 inputTextureCoordinate;\n"
        + " \n"
        + "varying vec2 textureCoordinate;\n"
        + " \n"
        + "void main()\n"
        + "{\n"
        + "    gl_Position = position;\n"
        + "    textureCoordinate = inputTextureCoordinate.xy;\n"
        + "}";

    public static final String VERTEX_SHADER_WITH_TRANS = ""
        + "attribute vec4 position;\n"
        + "attribute vec4 inputTextureCoordinate;\n"
        + "uniform mat4 textureTransform;\n"
        + "uniform vec4 locPoint;\n"
        + "\n"
        + "varying highp vec2 textureCoordinate;\n"
        + "void main()\n"
        + "{\n"
        + "    gl_Position = textureTransform * position + locPoint;\n"
        + "    textureCoordinate = inputTextureCoordinate.xy;\n"
        + "}\n";

    public static final String FRAGMENT_SHADER = ""
        + "varying highp vec2 textureCoordinate;\n"
        + " \n"
        + "uniform sampler2D inputImageTexture;\n"
        + " \n"
        + "void main()\n"
        + "{\n"
        + "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n"
        + "}";

    public static final String FRAGMENT_SHADER_FLIP = ""
        + "varying highp vec2 textureCoordinate;\n"
        + " \n"
        + "uniform sampler2D inputImageTexture;\n"
        + " \n"
        + "void main()\n"
        + "{\n"
        + "     gl_FragColor = texture2D(inputImageTexture, vec2(textureCoordinate.x, 1.0 - textureCoordinate.y));\n"
        + "}";

    public static final String VERTEX_SHADER_OES_INPUT = ""
        + "attribute vec4 position;\n"
        + "attribute vec4 inputTextureCoordinate;\n"
        + "uniform mat4 textureTransform;\n"
        + "\n"
        + "varying highp vec2 textureCoordinate;\n"
        + "void main()\n"
        + "{\n"
        + "    gl_Position = position;\n"
        + "    textureCoordinate = (textureTransform * inputTextureCoordinate).xy;\n"
        + "}\n";

    public static final String FRAGMENT_SHADER_OES_INPUT = ""
        + "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "varying highp vec2 textureCoordinate;\n"
        + " \n"
        + "uniform samplerExternalOES inputImageTexture;\n"
        + " \n"
        + "void main()\n"
        + "{\n"
        + "   gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n"
        + "}";
}
