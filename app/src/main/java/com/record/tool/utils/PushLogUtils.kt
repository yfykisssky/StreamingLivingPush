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

        fun encode() {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "encode:" + (System.currentTimeMillis() - encodeTimeStamp))
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

        private var fps = 0
        private var frameCount = 0
        private var bitsSize = 0
        private val lockLogData = Any()

        fun updateFpsBit(type: Int, bit: Int) {
            if (!isDebug) {
                return
            }
            if (type == RecordType.VIDEO) {
                synchronized(lockLogData) {
                    ++frameCount
                    bitsSize += bit
                }
            }
        }

        fun logFpsBit() {
            if (!isDebug) {
                return
            }
            synchronized(lockLogData) {
                fps = frameCount
                Log.e(TAG, "logFps:$fps,logBit:$bitsSize")
                showInFloatWindow(fps.toString() + "FPS" + "," + bitsSize + "b/s")
                frameCount = 0
                bitsSize = 0
            }
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

        fun logAudioTimeStamp(time: Long) {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "audioTimeStamp:$time")
            addDataTimeStamp = System.currentTimeMillis()
        }

        var logShowInterface: LogShowInterface? = null

        private fun showInFloatWindow(log: String) {
            logShowInterface?.onLog(log)
        }

        interface LogShowInterface {
            fun onLog(log: String)
        }

        private var timerDis: Disposable? = null

        private fun startLogTimer() {
            if (!isDebug) {
                return
            }
            timerDis?.dispose()
            io.reactivex.Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : io.reactivex.Observer<Long?> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        timerDis = d
                    }

                    override fun onNext(time: Long) {
                        logFpsBit()
                    }

                    override fun onError(e: Throwable) {
                    }
                })
        }

        fun isStartTest() {
            if (!isDebug) {
                return
            }
            startLogTimer()
        }

        fun isEndTest() {
            if (!isDebug) {
                return
            }
            timerDis?.dispose()
        }

        private var pushAudioDataTimeStamp = 0L
        private var pushVideoDataTimeStamp = 0L

        fun logAudioPushTimeStamp(success: Boolean,frameSize:Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "pushAudioData:" + (System.currentTimeMillis() - pushAudioDataTimeStamp) + ":" + success+":"+frameSize
            )
            pushAudioDataTimeStamp = System.currentTimeMillis()
        }

        fun logVideoPushTimeStamp(success: Boolean,frameSize:Int) {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "pushVideoData:" + (System.currentTimeMillis() - pushVideoDataTimeStamp) + ":" + success+":"+frameSize
            )
            pushVideoDataTimeStamp = System.currentTimeMillis()
        }

    }

}