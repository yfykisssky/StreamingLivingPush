package com.living.streamlivingpush.instances.local

import android.os.Build
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.instances.local.interfaces.ILocal
import com.living.streamlivingpush.record.StreamCamPushInstance
import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.record.tool.utils.FileWriteTool

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StreamCamLocaInstance : StreamCamPushInstance(), ILocal {

    var isRecordAndEncoding = false
        private set

    private var fileWriteTool: FileWriteTool? = null

    override fun initInstance() {
        super.initRecoder()
    }

    override fun onVideoFrameAva(frame: VideoFrame) {
        frame.byteArray?.let { fileWriteTool?.writeData(it) }
    }

    override fun onAudioFrameAva(frame: AudioFrame) {

    }

    override fun startLocal(needSave: Boolean) {
        super.startRecode()

        if (needSave) {
            fileWriteTool = FileWriteTool()
            fileWriteTool?.createFile("h264")
        }

        isRecordAndEncoding = true
    }

    override fun stopLocal() {
        super.stopRecode()

        fileWriteTool?.closeFile()

        isRecordAndEncoding = false
    }

}