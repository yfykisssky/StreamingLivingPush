package com.record.tool.record.video.gl.render.opengl;

public class BaseTextureRotationUtils {

    public static float[] copyData(float[] fromData) {
        float[] data = new float[fromData.length];
        System.arraycopy(fromData, 0, data, 0, fromData.length);
        return data;
    }

    public static float[] flipTexture(float[] rotatedTex,
                                      final boolean flipHorizontal,
                                      final boolean flipVertical) {
        if (flipHorizontal) {
            rotatedTex = new float[]{
                    flip(rotatedTex[0]), rotatedTex[1],
                    flip(rotatedTex[2]), rotatedTex[3],
                    flip(rotatedTex[4]), rotatedTex[5],
                    flip(rotatedTex[6]), rotatedTex[7]};
        }
        if (flipVertical) {
            rotatedTex = new float[]{
                    rotatedTex[0], flip(rotatedTex[1]),
                    rotatedTex[2], flip(rotatedTex[3]),
                    rotatedTex[4], flip(rotatedTex[5]),
                    rotatedTex[6], flip(rotatedTex[7])};
        }
        return rotatedTex;
    }

    private static float flip(final float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }
}
