package com.record.tool.record.video.gl.render.opengl;

import android.opengl.GLES20;

import java.nio.FloatBuffer;
import java.util.LinkedList;

public class GPUImageFilter {

    protected final Program mProgram;
    private final LinkedList<Runnable> mRunOnDraw;
    protected float[] mTextureMatrix;
    private int mGLAttribPosition;
    private int mGLUniformTexture;
    private int mGLAttribTextureCoordinate;
    private boolean mIsInitialized;

    public GPUImageFilter() {
        this(ShaderUtils.VERTEX_SHADER,ShaderUtils.FRAGMENT_SHADER);
    }

    public GPUImageFilter(final String vertexShader, final String fragmentShader) {
        mRunOnDraw = new LinkedList<Runnable>();
        mProgram = new Program(vertexShader, fragmentShader);
    }

    public void init() {
        onInit();
        mIsInitialized = true;
    }

    protected void onInit() {
        mProgram.build();
        mGLAttribPosition = GLES20.glGetAttribLocation(mProgram.getProgramId(), "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mProgram.getProgramId(), "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mProgram.getProgramId(), "inputTextureCoordinate");
        mIsInitialized = true;
    }

    public void onOutputSizeChanged(final int width, final int height) {
    }

    protected void onUninit() {
    }

    public final void destroy() {
        runPendingOnDrawTasks();
        onUninit();
        mIsInitialized = false;
        mProgram.destroy();
    }

    public int getTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    public void setTexutreTransform(float[] matrix) {
        mTextureMatrix = matrix;
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public void onDraw(final int textureId, final FloatBuffer cubeBuffer, final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mProgram.getProgramId());
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
            textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            OpenGlUtils.bindTexture(getTarget(), textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        beforeDrawArrays(textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        endDrawArrays(textureId);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        OpenGlUtils.bindTexture(getTarget(), 0);
    }

    protected void beforeDrawArrays(int textureId) {
    }

    protected void endDrawArrays(int textureId) {
    }

    protected void runPendingOnDrawTasks() {
        // ??????????????????????????????????????????,?????????????????????,?????????????????????????????????
        LinkedList<Runnable> runList;
        synchronized (mRunOnDraw) {
            runList = new LinkedList<Runnable>(mRunOnDraw);
            mRunOnDraw.clear();
        }

        while (!runList.isEmpty()) {
            runList.removeFirst().run();
        }
    }
}
