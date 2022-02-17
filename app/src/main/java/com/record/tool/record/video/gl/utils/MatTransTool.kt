package com.record.tool.record.video.gl.utils

import android.opengl.Matrix

class MatTransTool {

    companion object {
        private val MAT_TRANS =
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        private val VEC_LOC =
            floatArrayOf(
                0f, 0f, 0f, 0f
            )
    }

    fun init() {
        resetTransMat()
        resetLocVec()
    }

    private val mMatrixTrans = FloatArray(MAT_TRANS.size)
    private val mVecLoc = FloatArray(VEC_LOC.size)

    fun resetTransMat() {
        System.arraycopy(MAT_TRANS, 0, mMatrixTrans, 0, MAT_TRANS.size)
    }

    fun resetLocVec() {
        System.arraycopy(VEC_LOC, 0, mVecLoc, 0, VEC_LOC.size)
    }

    //设置位置
    fun updateLoc(x: Float, y: Float, z: Float) {
        mVecLoc[0] = x
        mVecLoc[1] = y
        mVecLoc[2] = z
    }

    //平移变换
    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(mMatrixTrans, 0, x, y, z)
    }

    //旋转变换
    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(mMatrixTrans, 0, angle, x, y, z)
    }

    //缩放变换
    fun scale(x: Float, y: Float, z: Float) {
        Matrix.scaleM(mMatrixTrans, 0, x, y, z)
    }

    fun getTransMat(): FloatArray {
        return mMatrixTrans
    }

    fun getLocVec(): FloatArray {
        return mVecLoc
    }

}