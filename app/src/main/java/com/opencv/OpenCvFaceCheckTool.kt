package com.opencv

import android.graphics.Bitmap
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.MainActivity
import com.record.tool.record.video.gl.trans.TransToNv21Tool
import com.record.tool.utils.FileUtils
import com.record.tool.utils.PushLogUtils
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import kotlin.math.roundToInt


class OpenCvFaceCheckTool {

    private var inputWidth = 0
    private var inputHeight = 0

    private var mAbsoluteFaceSize = 0

    private var transToNv21Tool: TransToNv21Tool? = null

    fun init(inputWidth: Int, inputHeight: Int) {

        this.inputWidth = inputWidth
        this.inputHeight = inputHeight

        transToNv21Tool = TransToNv21Tool()
        transToNv21Tool?.init(inputWidth, inputHeight)

    }

    fun destory() {
        transToNv21Tool?.destory()
        transToNv21Tool = null
    }

    private fun nv21To2GrayMat(bytesY: ByteArray): Mat {
        val mat = Mat((inputHeight * 1.5).toInt(), inputWidth, CvType.CV_8UC1)
        mat.put(0, 0, bytesY)
        val desGray = Mat(inputHeight, inputWidth, CvType.CV_8UC1)
        Imgproc.cvtColor(mat, desGray, Imgproc.COLOR_YUV2GRAY_NV21)
        mat.release()
        return desGray
    }

    fun coverToBitmap(mGray:Mat){
        val graybmp = Bitmap.createBitmap(mGray.width(), mGray.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mGray, graybmp)
        MainActivity.testImageView?.setImageBitmap(graybmp)
    }

    fun onFrameUpdate(textureId: Int): Array<Rect> {

        PushLogUtils.updateVideoFaceCheckTime()

        val nv21Bytes = transToNv21Tool?.trans(textureId)

        val mGray = nv21To2GrayMat(nv21Bytes!!)

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

        mGray.release()

        PushLogUtils.logVideoFaceCheckTime()

        return faces.toArray()

    }

    companion object {

        private var RES_RAW_FLIE = com.living.opencv.R.raw.lbpcascade_frontalface
        private var COPYED_FILE_NAME = "lbpcascade_frontalface.xml"

        private var classifier: CascadeClassifier? = null

        private val appContext = AppApplication.appContext

        private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(appContext) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> {
                        FileUtils.copyFileFromRaw(
                            RES_RAW_FLIE,
                            COPYED_FILE_NAME
                        )?.let { path ->
                            classifier = CascadeClassifier(path)
                        }
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

        //初始化人脸级联分类器
        fun initClassifier() {

            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, appContext, mLoaderCallback)
            } else {
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
            }

        }

    }

}