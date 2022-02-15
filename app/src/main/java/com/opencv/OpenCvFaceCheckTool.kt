package com.opencv

import com.record.tool.utils.FileUtils
import org.opencv.core.*
import org.opencv.objdetect.CascadeClassifier
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class OpenCvFaceCheckTool {

    private var inputWidth = 0
    private var inputHeight = 0

    private var mAbsoluteFaceSize = 0
    private var mGrayMatByteBuffer: ByteBuffer? = null

    fun init(inputWidth: Int, inputHeight: Int) {

        this.inputWidth = inputWidth
        this.inputHeight = inputHeight

        // mGrayMatByteBuffer = ByteBuffer.allocate(nv21BufSize).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun y2GrayMat(bytesY: ByteArray): Mat {
        mGrayMatByteBuffer?.clear()
        mGrayMatByteBuffer?.put(bytesY)
        return Mat(inputHeight, inputWidth, CvType.CV_8UC1, mGrayMatByteBuffer, inputWidth.toLong())
    }

    fun onFrameUpdate(bytesY: ByteArray): Array<Rect> {

        val mGray = y2GrayMat(bytesY)
        val mRelativeFaceSize = 0.2f
        if (mAbsoluteFaceSize == 0) {
            val height = mGray.rows()
            if ((height * mRelativeFaceSize).roundToInt() > 0) {
                mAbsoluteFaceSize = (height * mRelativeFaceSize).roundToInt()
            }
        }
        val faces = MatOfRect()
        classifier?.detectMultiScale(
            mGray, faces, 1.1, 2, 2,
            Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size()
        )
        return faces.toArray()

    }

    companion object {

        private var RES_RAW_FLIE = com.living.opencv.R.raw.lbpcascade_frontalface
        private var COPYED_FILE_NAME = "lbpcascade_frontalface.xml"

        private var classifier: CascadeClassifier? = null

        //初始化人脸级联分类器
        private fun initClassifier() {

            FileUtils.copyFileFromRaw(
                RES_RAW_FLIE,
                COPYED_FILE_NAME
            )?.let { path ->
                classifier = CascadeClassifier(path)
            }

        }

    }

}