package com.living.x264;

class X264NativeJni {

    static {
        System.loadLibrary("encodeX264");
    }

    public native static void updateSettings(int bitrate,int fps,int width,int height);
    public native static void initEncoder();
    public native static void destoryEncoder();
    public native static byte[] getHeaders();
    public native static byte[] nv21EncodeToH264(byte[] nv21Bytes);

}
