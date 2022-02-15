package com.living.streamlivingpush.local

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.record.StreamCamPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.utils.FileWriteTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamCamLocaInstance : StreamCamPushInstance() {

    companion object {
        private val TAG_NAME = this::class.java.simpleName
    }

    var isRecordAndEncoding = false
        private set

    private var fileWriteTool: FileWriteTool? = null

    fun initTool() {
        super.initRecoder()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        frame.byteArray?.let { fileWriteTool?.writeData(it) }
    }

    override fun onAudioFrameAva(frame: AudioFrame) {

    }

    fun startPushing(needSave: Boolean = false) {

        super.startRecode(1)

        if (needSave) {
            fileWriteTool = FileWriteTool()
            fileWriteTool?.createFile("h264")
        }

        isRecordAndEncoding = true
    }

    fun stopPushing() {

        super.stopRecode()

        fileWriteTool?.closeFile()

        isRecordAndEncoding = false
    }

}