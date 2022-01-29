package com.living.rtmp;

class RtmpNativeJni {

    static {
        System.loadLibrary("librtmplib");
    }

    public native static boolean connect(String url);

    public native static boolean isConnect();

    public native static void disConnect();

    public native static boolean sendAudioData(byte[] data,int len,long tms);

    public native static boolean sendVideoData(byte[] data,int len,long tms);

}
