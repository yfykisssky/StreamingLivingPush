package com.record.tool.record.video.screen.floatwindow

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import android.widget.RelativeLayout
import com.living.streamlivingpush.AppApplication
import com.living.streamlivingpush.R
import com.record.tool.utils.PushLogUtils
import com.record.tool.utils.ToastUtils

class StmFloatWindowHelper {

    private val windowParams = WindowManager.LayoutParams()
    private var floatView: RelativeLayout? = null

    companion object {
        private const val DEFAULT_SIZE = 1
    }

    @SuppressLint("SetTextI18n")
    fun initHelper(context: Context) {
        floatView = RelativeLayout(context)
        //测试面板
        if (PushLogUtils.isDebug) {

            val debugPanel =
                LayoutInflater.from(context).inflate(R.layout.view_debug_floatwindow, null)

            floatView?.addView(debugPanel)

            floatView?.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            floatView?.setBackgroundColor(Color.BLACK)

        } else {
            floatView?.layoutParams = ViewGroup.LayoutParams(DEFAULT_SIZE, DEFAULT_SIZE)
            floatView?.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun getWindowManager(): WindowManager? {
        return getContext()?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    fun hasNoPer(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
            getContext()
        )
    }

    fun showWindow() {
        if (hasNoPer()) {
            return
        }
        closeWindow()
        initDragFloatWindowParams()
        addPanel()
    }

    fun closeWindow() {
        if (hasNoPer()) {
            return
        }
        if (floatView?.isAttachedToWindow == true) {
            try {
                getWindowManager()?.removeView(floatView)
            } catch (e: BadTokenException) {
            }
        }
    }

    private fun addPanel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(
                getContext()
            )
        ) {
            try {
                getWindowManager()?.addView(floatView, windowParams)
            } catch (e: BadTokenException) {
            }
        }
    }

    private fun initDragFloatWindowParams() {
        windowParams.type = getFloatWindowType()
        //测试面板
        if (PushLogUtils.isDebug) {
            floatView?.measure(0, 0)
            windowParams.width = floatView?.measuredWidth ?: 1
            windowParams.height = floatView?.measuredHeight ?: 1
        } else {
            windowParams.width = DEFAULT_SIZE
            windowParams.height = DEFAULT_SIZE
        }
        windowParams.format = PixelFormat.TRANSPARENT
        windowParams.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        windowParams.gravity = Gravity.START or Gravity.TOP
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun startOverlaySettingActivity(activity: Activity?) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity?.packageName)
            )
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            activity?.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showToast("浮窗权限获取失败，请尝试在“系统设置”中手动设置")
        }
    }

    private fun getFloatWindowType(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            else -> {
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }
    }

    private fun getContext(): Context? {
        return AppApplication.appContext
    }

}
