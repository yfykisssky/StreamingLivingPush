package com.push.tool.rtmp

import com.living.rtmp.RtmpSendTool
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.base.BasePushTool
import com.record.tool.utils.PushLogUtils
import java.util.concurrent.LinkedBlockingQueue

open class RtmpPushTool : BasePushTool() {

    interface Callback {
        fun onStatus(status: Int)
    }

    companion object {

    }

    private val callback: Callback? = null
    private var url: String? = null
    private var isPushing = false

    private var rtmpSendTool: RtmpSendTool? = null

    inner class PushSendDataRunnable : Runnable {
        override fun run() {
            if (rtmpSendTool?.connect(url) == true) {
                while (isPushing) {

                    if (queueAudioFrame?.size ?: 0 > 0) {

                        queueAudioFrame?.take()?.let { frame ->
                            frame.byteArray?.let { bytes ->
                                val frameSize = bytes.size
                                val isSend = rtmpSendTool?.sendAudioData(
                                    bytes,
                                    frameSize,
                                    frame.timestamp
                                )
                                PushLogUtils.logAudioPushTimeStamp(isSend, frameSize)
                            }
                        }
                    }

                    if (queueVideoFrame?.size ?: 0 > 0) {

                        queueVideoFrame?.take()?.let { frame ->
                            frame.byteArray?.let { bytes ->
                                val frameSize = bytes.size
                                val isSend = rtmpSendTool?.sendVideoData(
                                    bytes,
                                    frameSize,
                                    frame.timestamp
                                )
                                PushLogUtils.logVideoPushTimeStamp(isSend, frameSize)
                            }
                        }

                    }

                }
                queueVideoFrame?.clear()
                queueVideoFrame = null
                queueAudioFrame?.clear()
                queueAudioFrame = null
                rtmpSendTool?.disConnect()
            }
        }
    }

    override fun addVideoFrame(frame: VideoFrame) {
        super.addVideoFrame(frame)
        queueVideoFrame?.add(frame)
    }

    override fun addAudioFrame(frame: AudioFrame) {
        super.addAudioFrame(frame)
        queueAudioFrame?.add(frame)
    }

    fun startPushing(url: String) {

        this.url = url

        rtmpSendTool = RtmpSendTool()

        isPushing = true

        queueVideoFrame = LinkedBlockingQueue<VideoFrame>(Integer.MAX_VALUE)
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>(Integer.MAX_VALUE)

        Thread(PushSendDataRunnable()).start()
    }

    fun stopPushing() {

        rtmpSendTool?.disConnect()
        rtmpSendTool = null

        isPushing = false
    }

}
