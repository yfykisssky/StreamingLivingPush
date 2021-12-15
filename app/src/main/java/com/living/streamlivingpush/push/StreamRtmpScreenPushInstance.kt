package com.living.streamlivingpush.push

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.record.StreamScreenPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.rtmp.RtmpPushTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamRtmpScreenPushInstance : StreamScreenPushInstance() {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
    }

    var isRecordAndEncoding = false
        private set

    private var rtmpPushTool: RtmpPushTool? = null

    fun initTool() {
        rtmpPushTool = RtmpPushTool()
        super.initRecoder()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        rtmpPushTool?.addVideoFrame(frame)
    }

    override fun onAudioFrameAva(frame: AudioFrame) {
        rtmpPushTool?.addAudioFrame(frame)
    }

    fun startPushing(pushUrl: String) {
        rtmpPushTool?.startPushing(pushUrl)
        super.startRecode()
        isRecordAndEncoding = true
    }

    fun stopPushing() {

        super.stopRecode()

        rtmpPushTool?.stopPushing()
        rtmpPushTool = null

        isRecordAndEncoding = false
    }

}