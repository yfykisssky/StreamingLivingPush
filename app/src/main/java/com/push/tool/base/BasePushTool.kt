package com.push.tool.base

import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.utils.CheckUtils
import com.record.tool.utils.FrameType
import java.util.concurrent.LinkedBlockingQueue


open class BasePushTool {

    protected var queueVideoFrame: LinkedBlockingQueue<VideoFrame>? = null
    protected var queueAudioFrame: LinkedBlockingQueue<AudioFrame>? = null

    private var firstVideoRecTimeStamp = 0L
    private var firstAudioRecTimeStamp = 0L

    protected var delaySendDataTime = -1

    open fun addVideoFrame(frame: VideoFrame) {
        if (firstVideoRecTimeStamp == 0L) {
            firstVideoRecTimeStamp = System.currentTimeMillis()
        }
        if (delaySendDataTime != -1) {
            val newFrameTimeStamp = frame.timestamp
            if (checkNeedDisCrad(newFrameTimeStamp)) {
                val disCardTimeStamp = newFrameTimeStamp - delaySendDataTime
                val videoDIsCardTimeStamp = disCardVideoGop(disCardTimeStamp)
                disCardAudioFrames(videoDIsCardTimeStamp)
            }
        }
    }

    open fun addAudioFrame(frame: AudioFrame) {
        if (firstAudioRecTimeStamp == 0L) {
            firstAudioRecTimeStamp = System.currentTimeMillis()
        }
    }

    private fun checkNeedDisCrad(newFrameTimeStamp: Long): Boolean {
        queueVideoFrame?.peek()?.timestamp?.let { currentFrameTimeStamp ->
            if ((newFrameTimeStamp - currentFrameTimeStamp) > delaySendDataTime) {
                return true
            }
        }
        return false
    }

    //丢弃gop
    private fun disCardVideoGop(disCardTimeStamp: Long): Long {

        while (queueVideoFrame?.size ?: 0 > 0) {
            //remove到指定时间节点
            val currentFrame = queueVideoFrame?.peek()
            if (currentFrame?.timestamp ?: 0 > disCardTimeStamp) {
                queueVideoFrame?.remove()
                continue
            }
            //remove到指定时间节点之后直到I帧或SPS
            val frameKind = CheckUtils.checkBytesFrameKind(currentFrame?.byteArray)
            if (frameKind != FrameType.I_FRAME || frameKind != FrameType.SPS_FRAME) {
                queueVideoFrame?.remove()
            } else {
                //返回当前时间戳
                return currentFrame?.timestamp ?: 0
            }
        }
        return 0

    }

    //同步丢弃audio
    private fun disCardAudioFrames(timeStamp: Long) {

        while (queueVideoFrame?.size ?: 0 > 0) {
            val currentFrame = queueVideoFrame?.peek()
            if (currentFrame?.timestamp ?: Long.MAX_VALUE >= timeStamp) {
                break
            } else {
                queueVideoFrame?.remove()
            }
        }

    }

}