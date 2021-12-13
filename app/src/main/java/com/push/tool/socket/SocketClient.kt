package com.push.tool.socket

import com.push.tool.AudioFrame
import com.push.tool.VideoFrame
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.LinkedBlockingQueue


class SocketClient {

    companion object {
        private const val WRITE_TIME_OUT = 10000
        private const val CONNECT_TIME_OUT = 10000
    }

    private var clientSocket: Socket? = null
    private var outPutStream: DataOutputStream? = null
    private var queueVideoFrame: LinkedBlockingQueue<VideoFrame>? = null
    private var queueAudioFrame: LinkedBlockingQueue<AudioFrame>? = null
    private var isConnecting = false

    fun addVideoFrame(frame: VideoFrame) {
        if (!isConnecting) return
        queueVideoFrame?.add(frame)
    }

    fun addAudioFrame(frame: AudioFrame) {
        if (!isConnecting) return
        queueAudioFrame?.add(frame)
    }

    inner class ConnectThread(private val host: String, private val port: Int) : Thread() {
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
                while (isConnecting) {
                    queueVideoFrame?.poll()?.let { frame ->
                        val byteData = frame.byteArray
                        if (byteData?.isNotEmpty() == true) {
                            val sendData =
                                DataEncodeTool.addVideoExtraData(byteData, frame.timestamp)
                            outPutStream?.write(sendData)
                            outPutStream?.flush()
                        }
                    }
                    queueAudioFrame?.poll()?.let { frame ->
                        val byteData = frame.byteArray
                        if (byteData?.isNotEmpty() == true) {
                            val sendData =
                                DataEncodeTool.addAudioExtraData(byteData, frame.timestamp)
                            outPutStream?.write(sendData)
                            outPutStream?.flush()
                        }
                    }
                }

            } catch (e: IOException) {

            } finally {
                isConnecting = false
                outPutStream?.close()
                outPutStream = null
                clientSocket?.close()
                clientSocket = null
            }
        }
    }

    fun openSocket(host: String, port: Int) {
        isConnecting = true
        queueVideoFrame = LinkedBlockingQueue<VideoFrame>(Integer.MAX_VALUE)
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>(Integer.MAX_VALUE)
        ConnectThread(host, port).start()
    }

    fun closeSocket() {
        isConnecting = false
    }

}