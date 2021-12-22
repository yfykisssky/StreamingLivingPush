package com.encoder

import com.living.faac.AccFaacEncodeTool
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.audio.AudioConstants
import java.util.concurrent.LinkedBlockingQueue

class AudioSoftEncoder {

    companion object {

    }

    private var accFaacEncodeTool: AccFaacEncodeTool? = null
    private var isEncoding = false

    private var dataCallBackListener: DataCallBackListener? = null

    private var recordAudioQueue: LinkedBlockingQueue<RecordAudioFrame>? = null

    private var encodeStartTimeStamp = 0L

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long)
        fun onEncodeError()

        fun onLogTest(log: String)
    }

    fun initEncoder(bitRate: Int) {

        try {
            accFaacEncodeTool = AccFaacEncodeTool()
        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }

    }

    fun addFrameData(frame: RecordAudioFrame) {
        recordAudioQueue?.add(frame)
    }

    fun startEncode() {
        isEncoding = true

        recordAudioQueue = LinkedBlockingQueue<RecordAudioFrame>(Integer.MAX_VALUE)

        accFaacEncodeTool?.startFaacEngine()

        EncodeThread().start()

    }

    fun stopEncode() {
        isEncoding = false
        encodeStartTimeStamp = 0L
    }

    private inner class EncodeThread : Thread() {

        override fun run() {
            try {
                encodeStartTimeStamp = 0L

                while (isEncoding) {
                    if (recordAudioQueue?.size ?: 0 > 0) {
                        val frame = recordAudioQueue?.take()
                        val buffer = frame?.byteArray

                        val len = frame?.byteSize ?: 0
                        if (len <= 0) {
                            continue
                        }

                        accFaacEncodeTool?.convertToAac(buffer)?.let { aacArray ->

                            val outDataSize = aacArray.size + AudioConfigUtils.ADTS_SIZE

                            val outData = ByteArray(outDataSize)

                            AudioConfigUtils.addADTStoPacket(AudioConstants.SAMPLE_RATE, outData)

                            System.arraycopy(
                                aacArray,
                                0,
                                outData,
                                AudioConfigUtils.ADTS_SIZE,
                                aacArray.size
                            )

                            dataCallBackListener?.onDataCallBack(
                                outData,
                                System.currentTimeMillis() / 1000
                            )

                        }

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dataCallBackListener?.onEncodeError()
                dataCallBackListener?.onLogTest(e.message ?: "")
            } finally {
                recordAudioQueue?.clear()
                recordAudioQueue = null
            }
        }
    }

}