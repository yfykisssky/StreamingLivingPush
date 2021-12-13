package com.record.tool.utils

import android.util.Log
import com.common.base.RecordType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class PushLogUtils {

    companion object {

        private const val TAG = "PushLogUtils"

        private const val isDebug = true

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
                "encodeVideoCount:" + "[" + fps + "f/s]" + ":[" + bit / 1024 + "kb/s]"
                        + " target:" + "[" + tagFps + "f/s]" + ":[" + tagBit / 1024 + "kb/s]"
            )

            Log.e(TAG, "ptsCount: videoPts:$lastPtsVideo audioPts:$lastPtsAudio")
        }

        fun encodeAudioCount(bit: Int, tagBit: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "encodeAudioCount:" + ":[" + bit / 1024 + "kb/s]"
                        + ":[" + tagBit / 1024 + "kb/s]"
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

        fun logAudioPushTimeStamp(success: Boolean, frameSize: Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "pushAudioData:" + (System.currentTimeMillis() - pushAudioDataTimeStamp) + ":" + success + ":" + frameSize
            )
            pushAudioDataTimeStamp = System.currentTimeMillis()
        }

        fun logVideoPushTimeStamp(success: Boolean, frameSize: Int) {
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

    }

}