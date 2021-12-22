package com.living.faac

class AccFaacEncodeTool {

    fun test(){
        AccFaacNativeJni.startFaacEngine()
    }

    fun startFaacEngine(
/*        type: Int,
        fomtType: Int,
        sampleRate: Long,
        channels: Int*/
    ) {
        AccFaacNativeJni.startFaacEngine()
    }

    fun stopFaacEngine() {
        AccFaacNativeJni.stopFaacEngine()
    }

    fun convertToAac(pcmBytes: ByteArray?): ByteArray? {
        return AccFaacNativeJni.convertToAac(pcmBytes)
    }

}