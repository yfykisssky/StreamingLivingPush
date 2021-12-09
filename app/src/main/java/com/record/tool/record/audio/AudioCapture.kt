package com.record.tool.record.audio

import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import com.record.tool.record.audio.AudioConstants.Companion.getSampleDataSize
import com.record.tool.utils.AudioJavaUtils

class AudioCapture {

    companion object {
        private val TAG = AudioCapture::class.java.simpleName
    }

    private var isPause = false

    private var isRecording = false
    private var recordInside = true

    private var volumeInside = 1F

    fun updateInsideVolume(volumeInside: Float) {
        this.volumeInside = volumeInside
    }

    //录音监听
    private var recordListener: RecordListener? = null

    //录音线程
    private var audioRecordThread: AudioRecordThread? = null

    fun setRecordListener(recordListener: RecordListener?) {
        this.recordListener = recordListener
    }

    private var mMediaProjection: MediaProjection? = null

    fun setMediaProjection(mMediaProjection: MediaProjection?) {
        this.mMediaProjection = mMediaProjection
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
        private var audioRecordInside: AudioRecord? = null
        private val bufferSize = getSampleDataSize()

        //参数初始化
        init {
            initMicCapture()

            if (recordInside) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    initInsideCapture()
                }
            }
        }

        private fun initMicCapture() {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, AudioConstants.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun initInsideCapture() {
            mMediaProjection?.let { projection ->
                val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(AudioConstants.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()

                audioRecordInside = AudioRecord.Builder()
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioFormat(audioFormat)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()
            }
        }

        override fun run() {
            super.run()
            startPcmRecorder()
        }

        private fun startPcmRecorder() {
            isRecording = true
            try {
                audioRecord?.startRecording()
                audioRecordInside?.startRecording()

                val byteMic = ByteArray(bufferSize)
                var byteInside = ByteArray(bufferSize)

                while (isRecording) {
                    audioRecord?.read(byteMic, 0, byteMic.size)

                    val byteOut = if (recordInside) {
                        audioRecordInside?.read(byteInside, 0, byteInside.size)
                        if (volumeInside != 1F) {
                            byteInside = AudioJavaUtils.amplifyPCMData(
                                byteInside,
                                volumeInside
                            )
                        }
                        AudioJavaUtils.mixRawAudioBytes(
                            byteMic,
                            byteInside
                        )
                    } else {
                        byteMic
                    }

                    if (!isPause) {
                        recordListener?.onData(byteOut)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recordListener?.onError()
            } finally {

                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null

                audioRecordInside?.stop()
                audioRecordInside?.release()
                audioRecordInside = null

                isPause = false
            }
        }

    }

    interface RecordListener {
        fun onData(data: ByteArray?)
        fun onError()
    }

}