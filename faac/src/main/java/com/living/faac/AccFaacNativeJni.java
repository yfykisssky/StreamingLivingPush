package com.living.faac;

class AccFaacNativeJni {

    static {
        System.loadLibrary("pcmtoaac");
    }

    public native static void startFaacEngine();

    public native static void stopFaacEngine();

}
