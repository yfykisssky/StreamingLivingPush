package com.encoder

import com.living.faac.AccFaacEncodeTool
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.audio.AudioConstants
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class AudioSoftEncoder {

    companion object {
        private const val BYTEBUFFER_SIZE = 60
    }

    private var accFaacEncodeTool: AccFaacEncodeTool? = null
    private var isEncoding = false

    private var dataCallBackListener: DataCallBackListener? = null

    private var recordAudioQueue: LinkedBlockingQueue<RecordAudioFrame>? = null

    private var encodeThread: EncodeThread? = null
    private var addressToByteBufferThread: AddToByteBufferThread? = null

    //bytes缓冲，截取正确长度
    private var audioByteBuffers: ByteBuffer? = null
    private var inputBufferSize = 0

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
            inputBufferSize = accFaacEncodeTool?.initFaacEngine(
                AudioConstants.SAMPLE_RATE.toLong(), AudioConstants.CHANNEL,
                bitRate
            ) ?: 0
            audioByteBuffers = ByteBuffer.allocate(inputBufferSize * BYTEBUFFER_SIZE)
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

        encodeThread = EncodeThread()
        encodeThread?.start()
        addressToByteBufferThread = AddToByteBufferThread()
        addressToByteBufferThread?.start()

    }

    fun stopEncode() {
        isEncoding = false
        encodeStartTimeStamp = 0L
        audioByteBuffers?.clear()
        audioByteBuffers = null

        encodeThread?.join()
        encodeThread = null
        addressToByteBufferThread?.join()
        addressToByteBufferThread = null

        accFaacEncodeTool?.destoryFaacEngine()
    }

    private fun checkCanAdd(inputSize: Int): Boolean {
        audioByteBuffers?.let { bufs ->
            synchronized(bufs) {
                return bufs.remaining() >= inputSize
            }
        }
        return false
    }

    fun addToByteBuffers(byteArray: ByteArray) {
        audioByteBuffers?.let { bufs ->
            synchronized(bufs) {
                bufs.put(byteArray)
            }
        }
    }

    fun getFromByteBuffers(): ByteArray? {
        audioByteBuffers?.let { bufs ->
            synchronized(bufs) {
                if (bufs.position() >= inputBufferSize) {
                    val outByteArray = ByteArray(inputBufferSize)
                    bufs.flip()
                    bufs.get(outByteArray)
                    bufs.compact()
                    return outByteArray
                }
            }
        }
        return null
    }

    private inner class AddToByteBufferThread : Thread() {

        override fun run() {
            try {

                while (isEncoding) {

                    recordAudioQueue?.peek()?.let { frame ->
                        frame.byteArray?.let { array ->
                            if (checkCanAdd(array.size)) {
                                recordAudioQueue?.poll()?.byteArray?.let { addArray ->
                                    addToByteBuffers(addArray)
                                }
                            }
                        }
                    }

                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
            }
        }
    }

    private inner class EncodeThread : Thread() {

        private fun handlePts(): Long {
            return System.currentTimeMillis() / 1000
        }

        override fun run() {
            try {

                while (isEncoding) {
                    getFromByteBuffers()?.let { coverBytes ->

                        val pts = handlePts()

                        accFaacEncodeTool?.convertToAac(coverBytes)?.let { aacArray ->

                            val outDataSize = aacArray.size + AudioConfigUtils.ADTS_SIZE

                            val outData = ByteArray(outDataSize)

                            AudioConfigUtils.addADTStoPacket(
                                AudioConstants.SAMPLE_RATE,
                                outData
                            )

                            System.arraycopy(
                                aacArray,
                                0,
                                outData,
                                AudioConfigUtils.ADTS_SIZE,
                                aacArray.size
                            )

                            dataCallBackListener?.onDataCallBack(
                                outData,
                                pts
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