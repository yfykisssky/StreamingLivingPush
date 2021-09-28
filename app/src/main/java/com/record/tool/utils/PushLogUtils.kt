package com.record.tool.utils

import android.util.Log
import com.common.base.RecordType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class PushLogUtils {

    companion object {

        private const val TAG = "PushLogUtils"

        const val isDebug = true

        private var receiveTimeStamp = 0L
        private var renderCommitStamp = 0L
        private var encodeTimeStamp = 0L
        private var addDataTimeStamp = 0L
        private var sendDataTimeStamp = 0L

        fun receive() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "render:" + (System.currentTimeMillis() - receiveTimeStamp))
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

        fun encodeCount(bit: Int, fps: Int) {
            if (!isDebug) {
                return
            }
            //b to kb
            val size = bit / 1024
            Log.e(
                TAG,
                "encodeCount:" + "[" + fps + "f/s]" + ":[" + size + "kb/s]"
            )
            encodeTimeStamp = System.currentTimeMillis()
        }

        fun addData() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "addData:" + (System.currentTimeMillis() - addDataTimeStamp))
            addDataTimeStamp = System.currentTimeMillis()
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

        private var lastTimeStamp = 0L
        fun logVideoTimeStamp(time: Long) {
            if (!isDebug) {
                return
            }

            val result = time - lastTimeStamp
            lastTimeStamp = time
            Log.e(TAG, "videoTimeStamp:$result")
        }

        private var lastTimeAStamp = 0L
        fun logAudioTimeStamp(time: Long) {
            if (!isDebug) {
                return
            }

            val result = time - lastTimeAStamp
            lastTimeAStamp = time
            Log.e(TAG, "audioTimeStamp:$result")
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

    }

}