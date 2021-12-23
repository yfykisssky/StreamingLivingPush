package com.living.faac;

class AccFaacNativeJni {

    static {
        System.loadLibrary("pcmtoaac");
    }

    public native static int initFaacEngine(long sampleRate, int channels, int bitRate);

    public native static void destoryFaacEngine();

    public native static byte[] convertToAac(byte[] bytesPcm);

}
