package com.living.streamlivingpush.record.interfaces

import android.view.TextureView

interface ICamRecord : IRecord {
    fun setStartUseCamId(camId: Int)
    fun toggleMirror()
    fun switchCamera()
    fun getPreviewView(): TextureView?
}