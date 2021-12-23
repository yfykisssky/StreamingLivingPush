package com.living.faac

class AccFaacEncodeTool {

    fun initFaacEngine(
        sampleRate: Long,
        channels: Int,
        bitRate:Int
    ):Int {
        return AccFaacNativeJni.initFaacEngine(sampleRate,channels,bitRate)
    }

    fun destoryFaacEngine() {
        AccFaacNativeJni.destoryFaacEngine()
    }

    fun convertToAac(pcmBytes: ByteArray?): ByteArray? {
        return AccFaacNativeJni.convertToAac(pcmBytes)
    }

}