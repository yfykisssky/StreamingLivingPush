package com.record.tool.utils

class EncodeControlTool {

    private var onGopCountTimes = 0
    private var bitRateCount = 0
    private var fpsCount = 0

    fun updateData(bitRate: Int, fps: Int) {
        bitRateCount += bitRate
        fpsCount += fps
    }

    fun countGopTime(){
        onGopCountTimes++
    }

    fun getGopCountTimes(): Int {
        return onGopCountTimes
    }

    fun getCountBitRate(): Int {
        return bitRateCount
    }

    fun getCountFps(): Int {
        return fpsCount
    }

    fun resetData() {

        bitRateCount = 0
        fpsCount = 0
        onGopCountTimes = 0

    }


}