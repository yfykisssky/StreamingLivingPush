package com.living.x264;

class X264NativeJni {

    static {
        System.loadLibrary("encodeX264");
    }

    public native static void test();

}
