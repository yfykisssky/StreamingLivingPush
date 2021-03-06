package com.encoder.utils;

import android.graphics.Bitmap;

public class ImgTransJavaUtils {

    //ARGB_8888
    public static byte[] bitmapToNv21(Bitmap src, int width, int height) {
        int[] argb = new int[width * height];
        src.getPixels(argb, 0, width, 0, 0, width, height);
        return argbToNv21(argb, width, height);
    }

    private static byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Math.min(Y, 255)));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (Math.min(V, 255)));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (Math.min(U, 255)));
                }

                ++index;
            }
        }
        return nv21;
    }

    public static byte[] yuv420spToYuv420(byte[] yuv420sp, int width, int height) {
        byte[] yuv420 = new byte[width * height * 3 / 2];
        if (yuv420sp == null) return null;
        int framesize = width * height;
        int i = 0, j = 0;
        //copy y
        for (i = 0; i < framesize; i++) {
            yuv420[i] = yuv420sp[i];
        }
        i = 0;
        for (j = 0; j < framesize / 2; j += 2) {
            yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
            i++;
        }
        i = 0;
        for (j = 1; j < framesize / 2; j += 2) {
            yuv420[i + framesize] = yuv420sp[j + framesize];
            i++;
        }
        return yuv420;
    }

}
