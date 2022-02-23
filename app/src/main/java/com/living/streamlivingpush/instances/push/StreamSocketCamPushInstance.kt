package com.living.streamlivingpush.instances.push

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.instances.push.interfaces.ISocketPush
import com.living.streamlivingpush.record.StreamCamPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.rtmp.RtmpPushTool
import com.push.tool.socket.SocketClient

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamSocketCamPushInstance : StreamCamPushInstance(), ISocketPush {

    var isRecordAndEncoding = false
        private set

    private var socketPushTool: SocketClient? = null

    override fun initInstance() {
        super.initRecoder()
        socketPushTool = SocketClient()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        socketPushTool?.addVideoFrame(frame)
    }

    override fun onAudioFrameAva(frame: AudioFrame) {
        socketPushTool?.addAudioFrame(frame)
    }

    override fun startPushing(connectIp: String, connectPort: Int) {

        super.startRecode()

        socketPushTool?.openSocket(connectIp, connectPort)

        isRecordAndEncoding = true
    }

    override fun stopPushing() {

        super.stopRecode()

        socketPushTool?.closeSocket()
        socketPushTool = null

        isRecordAndEncoding = false
    }

}