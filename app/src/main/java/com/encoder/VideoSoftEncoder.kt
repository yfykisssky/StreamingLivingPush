package com.encoder

import com.living.x264.X264EncodeTool
import com.record.tool.bean.RecordVideoFrame
import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.trans.TransToNv21Tool
import com.record.tool.utils.CheckUtils
import java.util.concurrent.LinkedBlockingQueue

class VideoSoftEncoder {

    companion object {

    }

    private var isEncoding = false
    private var encodeStartTimeStamp = 0L

    private var encodeControlStartTimeStamp = 0L

    private var x264EncodeTool: X264EncodeTool? = null

    private var encoderThread: Thread? = null
    private var recordVideoQueue: LinkedBlockingQueue<RecordVideoFrame>? = null

    private var iFrameInterval = 2
    private var bitRate = 0
    private var maxFps = 0
    private var frameWith = 0
    private var frameHeight = 0

    private var transToNv21Tool: TransToNv21Tool? = null

    private var dataCallBackListener: DataCallBackListener? = null

    init {

    }

    private fun createTransToolIfNeed() {
        if (transToNv21Tool == null) {
            transToNv21Tool = TransToNv21Tool()
            transToNv21Tool?.init(frameWith, frameHeight)
        }
    }

    private fun releaseTransTool() {
        transToNv21Tool?.destory()
        transToNv21Tool = null
    }

    private fun resetTransTool() {

    }

    private var spsPpsData: ByteArray? = null

    fun addRenderFrame(textureFrame: TextureVideoFrame) {

        createTransToolIfNeed()
        val nv21Bytes = transToNv21Tool?.trans(textureFrame.textureId)
        recordVideoQueue?.add(RecordVideoFrame(nv21Bytes, textureFrame.captureTimeStamp))

    }

    fun setDataCallBackListener(dataCallBackListener: DataCallBackListener) {
        this.dataCallBackListener = dataCallBackListener
    }

    interface DataCallBackListener {
        fun onDataCallBack(byteArray: ByteArray?, timeStamp: Long)
        fun onEncodeError()

        fun onLogTest(log: String)
    }

    //重置编码器参数
    fun resetEncoder(): Boolean {

        isEncoding = false
        encoderThread?.join()

        return false
    }

    fun getSetBitRate(): Int {
        return bitRate
    }

    fun getSetFps(): Int {
        return maxFps
    }

    //修改编码宽高会导致花屏
    fun updateResetEncodeSettings(
        bitRate: Int,
        maxFps: Int
    ) {
        if (bitRate > 0) {
            this.bitRate = bitRate
        }
        if (maxFps > 0) {
            this.maxFps = maxFps
        }
    }

    fun updateEncodeSettings(
        bitRate: Int,
        maxFps: Int,
        frameWith: Int,
        frameHeight: Int,
        gopTime: Int = 2
    ) {
        this.bitRate = bitRate
        this.maxFps = maxFps
        this.frameWith = frameWith
        this.frameHeight = frameHeight
        iFrameInterval = gopTime
    }

    private fun configEncoder() {
        try {
            x264EncodeTool?.updateSettings(bitRate, maxFps, frameWith, frameHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }
    }

    fun initEncoder(): Boolean {
        x264EncodeTool = X264EncodeTool()
        configEncoder()
        x264EncodeTool?.initEncoder()
        return false
    }

    fun startEncode() {
        recordVideoQueue = LinkedBlockingQueue<RecordVideoFrame>()
        encodeControlStartTimeStamp = System.currentTimeMillis()
        beginEncode()
    }

    private fun beginEncode() {
        isEncoding = true
        encoderThread = Thread(EncodeRunnable())
        encoderThread?.start()
    }

    fun stopEncode() {
        encodeControlStartTimeStamp = 0L
        encodeStartTimeStamp = 0L
        releaseTransTool()
        isEncoding = false
        encoderThread?.join()
        releaseEncoder()
    }

    private fun releaseEncoder() {
        x264EncodeTool?.destoryEncoder()
    }

    private var iFrameReqSetListener: IFrameReqSetListener? = null

    interface IFrameReqSetListener {
        fun onIFrameReqSet(gopTime: Int): Boolean
    }

    fun setIFrameReqSetListener(iFrameReqSetListener: IFrameReqSetListener) {
        this.iFrameReqSetListener = iFrameReqSetListener
    }

    private inner class EncodeRunnable : Runnable {

        override fun run() {

            while (isEncoding) {

                recordVideoQueue?.take()?.let { frame ->
                    if (spsPpsData == null) {
                        spsPpsData = x264EncodeTool?.getHeaders()
                        CheckUtils.checkBytesFrameKind(spsPpsData)
                        dataCallBackListener?.onDataCallBack(spsPpsData, frame.timeStamp)
                    }

                    frame.byteArray?.let { dataBytes ->
                        x264EncodeTool?.nv21EncodeToH264(dataBytes)?.let { h264Data ->
                            CheckUtils.checkBytesFrameKind(h264Data)
                            dataCallBackListener?.onDataCallBack(h264Data, frame.timeStamp)
                        }
                    }
                }

            }

        }
    }

}