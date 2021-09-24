package com.record.tool.record.video.gl

data class TextureVideoFrame(
    var textureId: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var captureTimeStamp: Long = 0L,
    var eglContext14: android.opengl.EGLContext? = null
)
