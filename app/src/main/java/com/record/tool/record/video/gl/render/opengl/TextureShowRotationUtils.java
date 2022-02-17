package com.record.tool.record.video.gl.render.opengl;

public class TextureShowRotationUtils extends BaseTextureRotationUtils {

    public static final float[] TEXTURE_NO_ROTATION = TextureUtils.TEXTURE_WITH_SHOW;
    public static final float[] TEXTURE_ROTATED_90 = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
    };
    public static final float[] TEXTURE_ROTATED_180 = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };
    public static final float[] TEXTURE_ROTATED_270 = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    public static float[] getRotation(final Rotation rotation,
                                      final boolean flipHorizontal,
                                      final boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case ROTATION_90:
                rotatedTex = copyData(TEXTURE_ROTATED_90);
                break;
            case ROTATION_180:
                rotatedTex = copyData(TEXTURE_ROTATED_180);
                break;
            case ROTATION_270:
                rotatedTex = copyData(TEXTURE_ROTATED_270);
                break;
            case NORMAL:
            default:
                rotatedTex = copyData(TEXTURE_NO_ROTATION);
                break;
        }

        return flipTexture(rotatedTex, flipHorizontal, flipVertical);
    }

}
