package com.encoder

import com.record.tool.record.video.gl.TextureVideoFrame
import com.record.tool.record.video.gl.trans.TransToNv21Tool

class VideoSoftEncoder {

    companion object {}

    private var isEncoding = false
    private var encodeStartTimeStamp = 0L

    private var encodeControlStartTimeStamp = 0L

    private var encoderThread: Thread? = null

    private var iFrameInterval = 2
    private var bitRate = 0
    private var maxFps = 0
    private var frameWith = 0
    private var frameHeight = 0

    private var transToNv21Tool: TransToNv21Tool?=null

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

    fun addRenderFrame(textureFrame: TextureVideoFrame) {
        createTransToolIfNeed()
        transToNv21Tool?.trans(textureFrame.textureId)
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

        } catch (e: Exception) {
            e.printStackTrace()
            dataCallBackListener?.onLogTest(e.message ?: "")
        }
    }

    fun initEncoder(): Boolean {

        return false

    }

    fun startEncode() {
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


        }
    }

}