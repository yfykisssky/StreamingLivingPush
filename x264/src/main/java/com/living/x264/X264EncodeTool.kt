package com.living.x264

class X264EncodeTool {

    fun updateSettings(bitrate: Int, fps: Int, width: Int, height: Int) {
        X264NativeJni.updateSettings(bitrate, fps, width, height)
    }

    fun initEncoder() {
        X264NativeJni.initEncoder()
    }

    fun destoryEncoder() {
        X264NativeJni.destoryEncoder()
    }

    fun getHeaders(): ByteArray? {
        return X264NativeJni.getHeaders()
    }

    fun nv21EncodeToH264(nv21Bytes: ByteArray): ByteArray? {
        return X264NativeJni.nv21EncodeToH264(nv21Bytes)
    }

}