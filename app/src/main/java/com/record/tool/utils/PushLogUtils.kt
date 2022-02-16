package com.record.tool.utils

import android.util.Log
import org.opencv.core.Rect

class PushLogUtils {

    companion object {

        private const val TAG = "PushLogUtils"

        const val isDebug = true

        private var receiveTimeStamp = 0L
        private var renderCommitStamp = 0L
        private var encodeTimeStamp = 0L
        private var sendDataTimeStamp = 0L

        fun receive() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "receive:" + (System.currentTimeMillis() - receiveTimeStamp))
            receiveTimeStamp = System.currentTimeMillis()
        }

        fun render() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "render:" + (System.currentTimeMillis() - renderCommitStamp))
            renderCommitStamp = System.currentTimeMillis()
        }

        fun encode(dataSize: Int) {
            if (!isDebug) {
                return
            }
            //b to kb
            val size = dataSize / 1024
            Log.e(
                TAG,
                "encode:" + (System.currentTimeMillis() - encodeTimeStamp) + ":[" + size + "kb]"
            )
            encodeTimeStamp = System.currentTimeMillis()
        }

        fun encodeVideoCount(bit: Int, fps: Int, tagBit: Int, tagFps: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "encodeVideoCount:" + "[" + fps + "f/s]" + ":[" + bit + "kb/s]"
                        + " target:" + "[" + tagFps + "f/s]" + ":[" + tagBit + "kb/s]"
            )

            Log.e(TAG, "ptsCount: videoPts:$lastPtsVideo audioPts:$lastPtsAudio")
        }

        fun encodeAudioCount(bit: Int, tagBit: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "encodeAudioCount:" + ":[" + bit + "kb/s]"
                        + ":[" + tagBit + "kb/s]"
            )

        }

        fun logPts() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "ptsCount: videoPts:$lastPtsVideo audioPts:$lastPtsAudio")
        }

        fun sendData(type: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "sendData:" + type.toString() + ":" + (System.currentTimeMillis() - sendDataTimeStamp)
            )
            sendDataTimeStamp = System.currentTimeMillis()
        }

        private var lastPtsAudio = 0L
        private var lastPtsVideo = 0L

        fun logVideoTimeStamp(timeStamp: Long) {
            if (!isDebug) {
                return
            }
            if (lastPtsVideo == 0L) {
                lastPtsVideo = timeStamp
            }
            Log.e(TAG, "videoTimeStamp:$timeStamp" + " last:" + (timeStamp - lastPtsVideo))
            lastPtsVideo = timeStamp
        }

        fun logAudioTimeStamp(timeStamp: Long) {
            if (!isDebug) {
                return
            }
            if (lastPtsAudio == 0L) {
                lastPtsAudio = timeStamp
            }
            Log.e(TAG, "audioTimeStamp:$timeStamp" + " last:" + (timeStamp - lastPtsAudio))
            lastPtsAudio = timeStamp
        }

        private var pushAudioDataTimeStamp = 0L
        private var pushVideoDataTimeStamp = 0L

        fun logAudioPushTimeStamp(success: Boolean?, frameSize: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "pushAudioData:" + (System.currentTimeMillis() - pushAudioDataTimeStamp) + ":" + success + ":" + frameSize
            )
            pushAudioDataTimeStamp = System.currentTimeMillis()
        }

        fun logVideoPushTimeStamp(success: Boolean?, frameSize: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "pushVideoData:" + (System.currentTimeMillis() - pushVideoDataTimeStamp) + ":" + success + ":" + frameSize
            )
            pushVideoDataTimeStamp = System.currentTimeMillis()
        }

        fun outLog(tag: String?, log: String?) {
            if (!isDebug) {
                return
            }
            Log.e(tag, log ?: "null")
        }

        private var videoResetTimeStamp = 0L

        fun logVideoResetTime(bitRateVideo: Int, fps: Int, resetBit: Boolean, resetFps: Boolean) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "encodeCount:videoReset:" + (System.currentTimeMillis() - videoResetTimeStamp) + " ms " + "setBit:" + bitRateVideo
                        + " setFps:" + fps + " resetBit:" + resetBit + " resetFps:" + resetFps
            )
            videoResetTimeStamp = System.currentTimeMillis()
        }

        private var videoSoftTrans = 0L

        fun updateVideoSoftTransTime() {
            videoSoftTrans = System.currentTimeMillis()
        }

        fun logVideoSoftTransTime() {
            if (!isDebug) {
                return
            }
            val useTime = System.currentTimeMillis() - videoSoftTrans
            Log.e(
                TAG,
                "encodeCount:videoSoftTrans:$useTime"
            )
        }

        private var videoFaceCheck = 0L

        fun updateVideoFaceCheckTime() {
            videoFaceCheck = System.currentTimeMillis()
        }

        fun logVideoFaceCheckTime() {
            if (!isDebug) {
                return
            }
            val useTime = System.currentTimeMillis() - videoFaceCheck
            Log.e(
                TAG,
                "encodeCount:videoFaceCheck:$useTime"
            )
        }

        fun logVideoFaceCheckRects(rects: Array<Rect>) {
            if (!isDebug) {
                return
            }

            if(rects.isEmpty()){
                return
            }

            var rectLog = "" + rects.size + ":"
            rects.forEach {
                val tl = it.tl()
                val br = it.br()
                rectLog += "tl:" + tl.x + "," + tl.y + ":"
                rectLog += "br:" + br.x + "," + br.y + ","
            }

            Log.e(
                TAG,
                "encodeCount:videoFaceCheckRects:$rectLog"
            )
        }

    }

}