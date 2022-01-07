package com.record.tool.bean

class RecordAudioFrame(
    var byteArray: ByteArray? = null,
    var byteSize: Int = 0
)

class RecordVideoFrame(
    var byteArray: ByteArray? = null,
    var timeStamp: Long = 0L
)
