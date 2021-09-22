package com.record.tool.record.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.record.tool.record.audio.AudioConstants.Companion.getSampleDataSize

class AudioCapture {

    companion object {
        private val TAG = AudioCapture::class.java.simpleName
    }

    private var isPause = false

    private var isRecording = false

    //录音监听
    private var recordListener: RecordListener? = null

    //录音线程
    private var audioRecordThread: AudioRecordThread? = null

    fun setRecordListener(recordListener: RecordListener?) {
        this.recordListener = recordListener
    }

    //开始录音
    fun start() {
        audioRecordThread = AudioRecordThread()
        audioRecordThread?.start()
    }

    //停止录音
    fun stop() {
        isRecording = false
    }

    //恢复录音
    fun resumeCapture() {
        isPause = false
    }

    //暂停录音
    fun pauseCapture() {
        isPause = true
    }

    //录音执行线程
    private inner class AudioRecordThread : Thread() {

        private var audioRecord: AudioRecord? = null
        private val bufferSize = getSampleDataSize()

        //参数初始化
        init {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, AudioConstants.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
        }

        override fun run() {
            super.run()
            startPcmRecorder()
        }

        private fun startPcmRecorder() {
            isRecording = true
            try {
                audioRecord?.startRecording()
                val byteBuffer = ByteArray(bufferSize)
                while (isRecording) {
                    audioRecord?.read(byteBuffer, 0, byteBuffer.size)
                    if (!isPause) {
                        recordListener?.onData(byteBuffer,byteBuffer.size)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recordListener?.onError()
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                isPause = false
            }
        }

    }

    interface RecordListener {
        fun onData(data: ByteArray?, byteSize: Int)
        fun onError()

        fun onLogTest(log: String)
    }

}