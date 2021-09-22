package com.rtmppush.tool

import android.util.Log
import com.record.tool.utils.PushLogUtils
import java.util.concurrent.LinkedBlockingQueue

open class RtmpPushTool {

    interface Callback {
        fun onStatus(status: Int)
    }

    companion object {
        init {
            System.loadLibrary("librtmplib")
        }
    }

    private val callback: Callback? = null
    private var url: String? = null
    private var queueVideoFrame: LinkedBlockingQueue<VideoFrame>? = null
    private var queueAudioFrame: LinkedBlockingQueue<AudioFrame>? = null
    private var isPushing = false

    private external fun connect(url: String?): Boolean
    private external fun isConnect(): Boolean
    private external fun disConnect()

    private external fun sendAudioData(
        data: ByteArray?,
        len: Int,
        tms: Long,
        isHeader: Boolean
    ): Boolean

    private external fun sendVideoData(data: ByteArray?, len: Int, tms: Long): Boolean

    inner class PushSendDataRunnable : Runnable {
        override fun run() {
            if (connect(url)) {
                while (isPushing) {

                    if (queueAudioFrame?.size ?: 0 > 0) {

                        queueAudioFrame?.take()?.let { frame ->
                            if (frame.byteArray?.isNotEmpty() == true) {
                                val frameSize = frame.byteArray?.size ?: 0
                                val isSend = sendAudioData(
                                    frame.byteArray,
                                    frameSize,
                                    frame.timestamp,
                                    frame.isHeader
                                )
                                PushLogUtils.logAudioPushTimeStamp(isSend, frameSize)
                            }
                        }
                    }

                    if (queueVideoFrame?.size ?: 0 > 0) {

                        queueVideoFrame?.take()?.let { frame ->
                            if (frame.byteArray?.isNotEmpty() == true) {
                                val frameSize = frame.byteArray?.size ?: 0
                                val isSend = sendVideoData(
                                    frame.byteArray,
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
                disConnect()
            }
        }
    }

    fun addVideoFrame(frame: VideoFrame) {
        if (!isConnect() || !isPushing) return
        queueVideoFrame?.add(frame)
    }

    fun addAudioFrame(frame: AudioFrame) {
        if (!isConnect() || !isPushing) return
        queueAudioFrame?.add(frame)
    }

    fun startPushing(url: String) {

        this.url = url

        isPushing = true

        queueVideoFrame = LinkedBlockingQueue<VideoFrame>(Integer.MAX_VALUE)
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>(Integer.MAX_VALUE)

        Thread(PushSendDataRunnable()).start()
    }

    fun stopPushing() {
        isPushing = false
    }
}
