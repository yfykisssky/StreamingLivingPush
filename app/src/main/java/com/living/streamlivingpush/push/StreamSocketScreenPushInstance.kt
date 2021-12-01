package com.living.streamlivingpush.push

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.record.StreamScreenPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.socket.SocketClient

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamSocketScreenPushInstance : StreamScreenPushInstance() {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
        val instance: StreamSocketScreenPushInstance by lazy {
            StreamSocketScreenPushInstance()
        }
    }

    var isRecordAndEncoding = false
        private set

    private var socketPushTool: SocketClient? = null

    fun initTool() {
        super.initRecoder()
        socketPushTool = SocketClient()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        socketPushTool?.addVideoFrame(frame)
    }

    override fun onAudioFrameAva(frame: AudioFrame) {
        socketPushTool?.addAudioFrame(frame)
    }

    fun startPushing(connectIp: String, connectPort: Int) {

        super.startRecode()

        socketPushTool?.openSocket(connectIp, connectPort)

        isRecordAndEncoding = true
    }

    fun stopPushing() {

        super.stopRecode()

        socketPushTool?.closeSocket()
        socketPushTool = null

        isRecordAndEncoding = false
    }

}