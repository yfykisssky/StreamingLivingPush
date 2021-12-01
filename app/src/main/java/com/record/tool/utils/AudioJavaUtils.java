package com.record.tool.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioJavaUtils {

    //混合音频，使用平均算法
    public static byte[] mixRawAudioBytes(byte[] bytes1, byte[] bytes2) {
        short[] shorts1 = bytesToShort(bytes1);
        short[] shorts2 = bytesToShort(bytes2);
        for (int i = 0; i < shorts2.length; i++) {
            shorts1[i] = (short) ((shorts2[i] + shorts1[i]) / 2);
        }
        return shortToBytes(shorts1);
    }

    public static short[] bytesToShort(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    public static byte[] shortToBytes(short[] shorts) {
        if (shorts == null) {
            return null;
        }
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

        return bytes;
    }

}
