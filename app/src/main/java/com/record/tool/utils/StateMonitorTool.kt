package com.record.tool.utils

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class StateMonitorTool {

    var tagBitRate = 0
        private set
    var tagFps = 0
        private set

    private var bitsSize = 0
    private val lockBitObj = Any()

    private var fpsCount = 0
    private val lockFpsObj = Any()

    interface CountCallBack {
        fun onCount(bitRate: Int, fps: Int)
    }

    private var countCallBack: CountCallBack? = null

    fun setCountCallBack(countCallBack: CountCallBack?) {
        this.countCallBack = countCallBack
    }

    fun updateTargetData(tagBitRate: Int, tagFps: Int) {
        this.tagBitRate = tagBitRate
        this.tagFps = tagFps
    }

    fun updateBitrate(bit: Int) {
        synchronized(lockBitObj) {
            bitsSize += bit
        }
    }

    fun resetBit() {
        synchronized(lockBitObj) {
            bitsSize = 0
        }
    }

    fun updateFpsCount() {
        synchronized(lockFpsObj) {
            fpsCount++
        }
    }

    fun resetFps() {
        synchronized(lockFpsObj) {
            fpsCount = 0
        }
    }

    private var timerDis: Disposable? = null

    private fun startLogTimer() {
        timerDis?.dispose()
        resetBit()
        resetFps()
        io.reactivex.Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<Long?> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    timerDis = d
                }

                override fun onNext(time: Long) {
                    countCallBack?.onCount(bitsSize, fpsCount)
                    PushLogUtils.encodeCount(bitsSize, fpsCount, tagBitRate, tagFps)
                    resetBit()
                    resetFps()
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    fun startMonitor() {
        startLogTimer()
    }

    fun stopMonitor() {
        countCallBack = null
        timerDis?.dispose()
    }

}