package com.push.tool.socket

import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import com.push.tool.base.BasePushTool
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.LinkedBlockingQueue


class SocketClient : BasePushTool() {

    companion object {
        private const val WRITE_TIME_OUT = 10000
        private const val CONNECT_TIME_OUT = 10000

        private const val CONNECT_TIME_ACK = 100
    }

    private var clientSocket: Socket? = null
    private var connectThread: ConnectThread? = null
    private var outPutStream: DataOutputStream? = null
    private var isConnecting = false

    private var lastWriteTimeStamp = 0L

    override fun addVideoFrame(frame: VideoFrame) {
        super.addVideoFrame(frame)
        queueVideoFrame?.add(frame)
    }

    override fun addAudioFrame(frame: AudioFrame) {
        super.addAudioFrame(frame)
        queueAudioFrame?.add(frame)
    }

    private fun refreshWriteTimeStamp() {
        lastWriteTimeStamp = System.currentTimeMillis()
    }

    inner class ConnectThread(private val host: String, private val port: Int) : Thread() {

        private fun getSendBytes(): ByteArray? {

            queueVideoFrame?.poll()?.let { frame ->
                val byteData = frame.byteArray
                if (byteData?.isNotEmpty() == true) {
                    return DataEncodeTool.addVideoExtraData(byteData, frame.timestamp)
                }
            }
            queueAudioFrame?.poll()?.let { frame ->
                val byteData = frame.byteArray
                if (byteData?.isNotEmpty() == true) {
                    return DataEncodeTool.addAudioExtraData(byteData, frame.timestamp)
                }
            }

            //心跳包
            if ((System.currentTimeMillis() - lastWriteTimeStamp) > CONNECT_TIME_ACK) {
                return DataEncodeTool.getSocketAckExtraData()
            }

            return null
        }


        override fun run() {
            super.run()
            try {
                clientSocket = Socket()
                clientSocket?.soTimeout = WRITE_TIME_OUT
                try {
                    clientSocket?.connect(InetSocketAddress(host, port), CONNECT_TIME_OUT)
                } catch (e: IOException) {
                    if (e is SocketTimeoutException) {

                    }
                }
                outPutStream = DataOutputStream(clientSocket?.getOutputStream())
                refreshWriteTimeStamp()
                while (isConnecting) {

                    val sendBytes = getSendBytes()
                    if (sendBytes != null) {
                        outPutStream?.write(sendBytes)
                        outPutStream?.flush()
                        refreshWriteTimeStamp()
                    }

                }

            } catch (e: IOException) {

            } finally {
                isConnecting = false
                try {
                    outPutStream?.close()
                    outPutStream = null
                    clientSocket?.close()
                    clientSocket = null
                } catch (e: IOException) {

                }
            }
        }
    }

    fun openSocket(host: String, port: Int) {
        isConnecting = true
        queueVideoFrame = LinkedBlockingQueue<VideoFrame>(Integer.MAX_VALUE)
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>(Integer.MAX_VALUE)
        connectThread = ConnectThread(host, port)
        connectThread?.start()
    }

    fun closeSocket() {
        isConnecting = false
        clientSocket?.close()
        connectThread?.join()
    }

}