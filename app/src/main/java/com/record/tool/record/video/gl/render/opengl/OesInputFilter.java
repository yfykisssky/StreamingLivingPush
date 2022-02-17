package com.record.tool.record.video.gl.render.opengl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

public class OesInputFilter extends GPUImageFilter {

    protected int mTextureTransform;

    public OesInputFilter() {
        super(ShaderUtils.VERTEX_SHADER_OES_INPUT, ShaderUtils.FRAGMENT_SHADER_OES_INPUT);
    }

    @Override
    public int getTarget() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    @Override
    public void onInit() {
        super.onInit();
        mTextureTransform = GLES20.glGetUniformLocation(mProgram.getProgramId(), "textureTransform");
    }

    @Override
    protected void beforeDrawArrays(int textureId) {
        super.beforeDrawArrays(textureId);
        GLES20.glUniformMatrix4fv(mTextureTransform, 1, false, mTextureMatrix, 0);
    }
}
