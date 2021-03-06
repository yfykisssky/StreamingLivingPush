package com.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.audio.AudioConstants
import com.record.tool.utils.PushLogUtils
import java.util.concurrent.LinkedBlockingQueue

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AudioEncoder {

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
    }

    private var codec: MediaCodec? = null
    private var isEncoding = false

    private var dataCallBackListener: DataCallBackListener? = null
    private val vBufferInfo = MediaCodec.BufferInfo()

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
            val format = MediaFormat.createAudioFormat(
                MIME_TYPE,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.CHANNEL
            )
            format.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

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

        EncodeThread().start()
    }

    fun stopEncode() {
        isEncoding = false
        encodeStartTimeStamp = 0L
    }

    private inner class EncodeThread : Thread() {

        fun handlePts(): Long {
            if (encodeStartTimeStamp == 0L) {
                encodeStartTimeStamp = vBufferInfo.presentationTimeUs
            }
            val ptsNew = (vBufferInfo.presentationTimeUs - encodeStartTimeStamp) / 1000
            PushLogUtils.logAudioTimeStamp(ptsNew)
            return ptsNew
        }

        override fun run() {
            try {
                encodeStartTimeStamp = 0L

                codec?.start()
                while (isEncoding) {
                    if (recordAudioQueue?.size ?: 0 > 0) {
                        val frame = recordAudioQueue?.take()
                        val buffer = frame?.byteArray
                        val len = frame?.byteSize ?: 0
                        if (len <= 0) {
                            continue
                        }
                        //?????????????????????????????????
                        val indexIn = codec?.dequeueInputBuffer(0) ?: -1
                        if (indexIn >= 0) {
                            val inputBuffer = codec?.getInputBuffer(indexIn)
                            inputBuffer?.clear()
                            inputBuffer?.put(buffer, 0, len)
                            //??????????????????????????????

                            //?????????????????????????????????????????????
                            val encodePts = System.nanoTime() / 1000

                            codec?.queueInputBuffer(
                                indexIn, 0, len,
                                encodePts, 0
                            )
                        }
                    }
                    var indexOut = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                    while (indexOut >= 0 && isEncoding) {

                        val outputBuffer = codec?.getOutputBuffer(indexOut)

                        val outDataSize = vBufferInfo.size + AudioConfigUtils.ADTS_SIZE

                        outputBuffer?.position(vBufferInfo.offset)
                        outputBuffer?.limit(vBufferInfo.offset + vBufferInfo.size)

                        val outData = ByteArray(outDataSize)

                        AudioConfigUtils.addADTStoPacket(AudioConstants.SAMPLE_RATE, outData)

                        outputBuffer?.get(outData, AudioConfigUtils.ADTS_SIZE, vBufferInfo.size)
                        outputBuffer?.position(vBufferInfo.offset)

                        dataCallBackListener?.onDataCallBack(outData, handlePts())
                        codec?.releaseOutputBuffer(indexOut, false)
                        indexOut = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dataCallBackListener?.onEncodeError()
                dataCallBackListener?.onLogTest(e.message ?: "")
            } finally {
                codec?.stop()
                codec?.release()
                recordAudioQueue?.clear()
                recordAudioQueue = null
            }
        }
    }

}