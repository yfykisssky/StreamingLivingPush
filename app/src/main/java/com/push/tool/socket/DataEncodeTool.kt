package com.push.tool.socket

import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataEncodeTool {

    companion object {

        private const val SIZE_IS_SOCKET_ACK = 1

        private const val SIZE_LENGTH = 4
        private const val TYPE_LENGTH = 1
        private const val TIME_STAMP_LENGTH = 8

        private const val EXTRA_DATA_LENGTH =
            SIZE_IS_SOCKET_ACK + SIZE_LENGTH + TYPE_LENGTH + TIME_STAMP_LENGTH

        private val headerBuffer =
            ByteBuffer.allocate(EXTRA_DATA_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

        private fun booleanToByte(i: Boolean): Byte {
            return (if (i) 0x01 else 0x00).toByte()
        }

        private fun getHeaderBytes(
            isSocketAck: Boolean = false,
            arraySize: Int,
            timeStamp: Long,
            type: Boolean
        ): ByteArray {
            headerBuffer.clear()

            headerBuffer.put(booleanToByte(isSocketAck))

            headerBuffer.putInt(arraySize)
            headerBuffer.put(booleanToByte(type))
            headerBuffer.putLong(timeStamp)
            headerBuffer.flip()
            val len = headerBuffer.limit() - headerBuffer.position()
            val bytesHeader = ByteArray(len)
            headerBuffer.get(bytesHeader)

            return bytesHeader
        }

        private fun addExtraData(
            array: ByteArray,
            timeStamp: Long,
            type: Boolean
        ): ByteArray {

            val bytesHeader = getHeaderBytes(false, array.size, timeStamp, type)

            val newByteArray = ByteArray(array.size + EXTRA_DATA_LENGTH)
            //额外信息
            System.arraycopy(bytesHeader, 0, newByteArray, 0, bytesHeader.size)
            //数据
            System.arraycopy(array, 0, newByteArray, EXTRA_DATA_LENGTH, array.size)

            return newByteArray
        }

        fun addAudioExtraData(array: ByteArray, timeStamp: Long): ByteArray {
            return addExtraData(array, timeStamp, false)
        }

        fun addVideoExtraData(array: ByteArray, timeStamp: Long): ByteArray {
            return addExtraData(array, timeStamp, true)
        }

        fun getSocketAckExtraData(): ByteArray {
            return getHeaderBytes(true, 0, 0, false)
        }

    }

}