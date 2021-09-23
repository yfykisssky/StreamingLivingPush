package com.record.tool.record.video.gl;

import android.view.Surface;

import com.record.tool.record.video.gl.basic.Size;

public class ToSurfaceFrameRender extends CustomFrameRender {

    private Surface mSurface;

    @Override
    Surface getRenderSurface() {
        return mSurface;
    }

    public void setOutPutSurface(Surface surface, int renderWidth, int renderHeight) {
        mSurface = surface;
        mSurfaceSize = new Size(renderWidth, renderHeight);
    }

}
