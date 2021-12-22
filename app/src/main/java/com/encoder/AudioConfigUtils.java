package com.encoder;

import com.record.tool.record.audio.AudioConstants;

class AudioConfigUtils {

    public static int ADTS_SIZE = 7;

    /*channel_configuration:
      0: Defined in AOT Specifc Config
      1: 1 channel: front-center
      2: 2 channels: front-left, front-right
      3: 3 channels: front-center, front-left, front-right
      4: 4 channels: front-center, front-left, front-right, back-center
      5: 5 channels: front-center, front-left, front-right, back-left, back-right
      6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
      7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel*/
    private static final int channelCfg = AudioConstants.CHANNEL;
    //AAC LC
    private static final int profile = 2;

    //ADTS
    public static void addADTStoPacket(int sampleRate, byte[] packet) {

        int packetLen = packet.length;
        int freqIdx = getFreqIdx(sampleRate);

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (channelCfg >> 2));
        packet[3] = (byte) (((channelCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;

    }

    //采样率的下标
    private static int getFreqIdx(int sampleRate) {
        switch (sampleRate) {
            case 96000:
                return 0;
            case 88200:
                return 1;
            case 64000:
                return 2;
            case 48000:
                return 3;
            case 44100:
                return 4;
            case 32000:
                return 5;
            case 24000:
                return 6;
            case 22050:
                return 7;
            case 16000:
                return 8;
            case 12000:
                return 9;
            case 11025:
                return 10;
            case 8000:
                return 11;
            case 7350:
                return 12;
        }
        //13: Reserved
        //14: Reserved
        //15: frequency is written explictly
        return -1;
    }

}
