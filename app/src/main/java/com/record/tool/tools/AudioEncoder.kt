package com.record.tool.tools

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.record.tool.bean.RecordAudioFrame
import com.record.tool.record.audio.AudioConstants
import java.util.concurrent.LinkedBlockingQueue

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AudioEncoder {

    companion object {
        private const val MIME_TYPE = "audio/mp4a-latm"
    }

    private var codec: MediaCodec? = null
    private var isEncoding = false

    private var dataCallBackListener: DataCallBackListener? = null
    private val vBufferInfo = MediaCodec.BufferInfo()

    private var specificInfo: ByteArray? = null

    private var recordAudioQueue: LinkedBlockingQueue<RecordAudioFrame>? = null

    private var encodeStartTimeStamp = 0L

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long, isHeader: Boolean)
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

            //faac库实现
            specificInfo = AudioSpecificConfig.getAudioDecoderSpecificInfo(
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.CHANNEL
            )
            //另外的方式
            //codec?.outputFormat?.getByteBuffer("csd-0")

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
    }

    private inner class EncodeThread : Thread() {

        fun handlePts(): Long {
            if (encodeStartTimeStamp == 0L) {
                encodeStartTimeStamp = vBufferInfo.presentationTimeUs
            }
            val ptsNew = (vBufferInfo.presentationTimeUs - encodeStartTimeStamp)
            encodeStartTimeStamp = vBufferInfo.presentationTimeUs
            return ptsNew
        }

        override fun run() {
            try {
                encodeStartTimeStamp = 0L

                dataCallBackListener?.onDataCallBack(specificInfo, encodeStartTimeStamp, true)

                codec?.start()
                while (isEncoding) {
                    if (recordAudioQueue?.size ?: 0 > 0) {
                        val frame = recordAudioQueue?.take()
                        val buffer = frame?.byteArray
                        val len = frame?.byteSize ?: 0
                        if (len <= 0) {
                            continue
                        }
                        //立即得到有效输入缓冲区
                        var index = codec?.dequeueInputBuffer(0) ?: -1
                        if (index >= 0) {
                            val inputBuffer = codec?.getInputBuffer(index)
                            inputBuffer?.clear()
                            inputBuffer?.put(buffer, 0, len)
                            //填充数据后再加入队列
                            codec?.queueInputBuffer(
                                index, 0, len,
                                System.currentTimeMillis(), 0
                            )
                        }
                        index = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                        while (index >= 0 && isEncoding) {
                            val outputBuffer = codec?.getOutputBuffer(index)
                            val outData = ByteArray(vBufferInfo.size)
                            outputBuffer?.get(outData)
                            dataCallBackListener?.onDataCallBack(outData, handlePts(), true)
                            codec?.releaseOutputBuffer(index, false)
                            index = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                        }
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