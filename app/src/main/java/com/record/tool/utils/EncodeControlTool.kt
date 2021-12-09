package com.record.tool.utils

class EncodeControlTool {

    private var bitRateCount = 0
    private var fpsCount = 0

    fun updateData(bitRate: Int, fps: Int) {
        bitRateCount += bitRate
        fpsCount += fps
    }

    fun getCountBitRate(): Int {
        return bitRateCount
    }

    fun getCountFps(): Int {
        return fpsCount
    }

    data class CountVideoResetData(
        var useTime: Long = 0L,
        var setVideoBit: Int = 0
    )

    private var videoResetTimeStamp = 0L
    private var countVideoResetList = ArrayList<CountVideoResetData>()

    fun countVideoReset(setVideoBit: Int) {
        val usbTime = (System.currentTimeMillis() - videoResetTimeStamp)
        countVideoResetList.add(CountVideoResetData(usbTime, setVideoBit))
        videoResetTimeStamp = System.currentTimeMillis()
    }

    fun resetData() {

        bitRateCount = 0
        fpsCount = 0

        countVideoResetList.clear()
        videoResetTimeStamp = 0L
    }


}