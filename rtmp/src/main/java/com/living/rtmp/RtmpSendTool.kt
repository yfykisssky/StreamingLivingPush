package com.living.rtmp

class RtmpSendTool {

    fun connect(url: String?): Boolean {
        return RtmpNativeJni.connect(url)
    }

    fun isConnect(): Boolean {
        return RtmpNativeJni.isConnect()
    }

    fun disConnect() {
        return RtmpNativeJni.disConnect()
    }

    fun sendAudioData(data: ByteArray, len: Int, tms: Long): Boolean {
        return RtmpNativeJni.sendAudioData(data, len, tms)
    }

    fun sendVideoData(data: ByteArray?, len: Int, tms: Long): Boolean {
        return RtmpNativeJni.sendVideoData(data, len, tms)
    }

}