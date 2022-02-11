package com.opencv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.living.streamlivingpush.R
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class OpenCvTestActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var mIntermediateMat: Mat? = null
    private var classifier: CascadeClassifier? = null
    private var mGray: Mat? = null
    private var mRgba: Mat? = null
    private var mAbsoluteFaceSize = 0
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    mOpenCvCameraView?.setCameraPermissionGranted()
                    mOpenCvCameraView?.enableView()
                    initClassifier()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opencv_test)
        mOpenCvCameraView = findViewById<View>(R.id.testPreview) as CameraBridgeViewBase
        //这里的cjv也是我的项目中JavaCameraView的id，自己改一下
        mOpenCvCameraView!!.setCvCameraViewListener(this)

        requestPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            10
        )
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()
    }

    override fun onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null) mIntermediateMat!!.release()
        mIntermediateMat = null
        mGray!!.release()
        mRgba!!.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()
        val mRelativeFaceSize = 0.2f
        if (mAbsoluteFaceSize == 0) {
            val height = mGray?.rows() ?: 0
            if ((height * mRelativeFaceSize).roundToInt() > 0) {
                mAbsoluteFaceSize = (height * mRelativeFaceSize).roundToInt()
            }
        }
        val faces = MatOfRect()
        classifier?.detectMultiScale(
            mGray, faces, 1.1, 2, 2,
            Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size()
        )
        val facesArray = faces.toArray()
        val faceRectColor = Scalar(0.0, 255.0, 0.0, 255.0)
        for (faceRect in facesArray)
            Imgproc.rectangle(
            mRgba,
            faceRect.tl(),
            faceRect.br(),
            faceRectColor,
            1
        )
        return mRgba
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    // 初始化人脸级联分类器，必须先初始化
    private fun initClassifier() {
        try {
            val inputStream = resources.openRawResource(com.living.opencv.R.raw.lbpcascade_frontalface)
            val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
            val cascadeFile = File(cascadeDir, "lbpcascade_frontalface.xml");
            val outStream = FileOutputStream(cascadeFile);
            val buffer = ByteArray(4096)
            var bytesRead = 0
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            inputStream.close()
            outStream.close()
            classifier = CascadeClassifier(cascadeFile.getAbsolutePath())
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    companion object {
        private const val TAG = "OCVSample::Activity"
    }

}
