package com.living.streamlivingpush.instances.push

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.instances.push.interfaces.IRtmpPush
import com.living.streamlivingpush.record.StreamCamPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.rtmp.RtmpPushTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamRtmpCamPushInstance : StreamCamPushInstance(), IRtmpPush {

    var isRecordAndEncoding = false
        private set

    private var rtmpPushTool: RtmpPushTool? = null

    override fun initInstance() {
        super.initRecoder()
        rtmpPushTool = RtmpPushTool()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        rtmpPushTool?.addVideoFrame(frame)
    }

    override fun onAudioFrameAva(frame: AudioFrame) {
        rtmpPushTool?.addAudioFrame(frame)
    }

    override fun startPushing(pushUrl: String) {

        super.startRecode()

        rtmpPushTool?.startPushing(pushUrl)

        isRecordAndEncoding = true
    }

    override fun stopPushing() {

        super.stopRecode()

        rtmpPushTool?.stopPushing()
        rtmpPushTool = null

        isRecordAndEncoding = false
    }

}