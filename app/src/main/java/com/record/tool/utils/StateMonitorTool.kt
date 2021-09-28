package com.record.tool.utils

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class StateMonitorTool {

    companion object {
        const val KIND_ENCODE = 0
        const val KIND_PUSH = 1
    }

    private var bitsSize = 0
    private val lockBitObj = Any()

    private var fpsCount = 0
    private val lockFpsObj = Any()

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
                    PushLogUtils.encodeCount(bitsSize, fpsCount)
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
        timerDis?.dispose()
    }

}