package com.record.tool.record.video.screen.floatwindow

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.record.tool.utils.ScreenUtils.Companion.dp2px
import com.record.tool.utils.ScreenUtils.Companion.getScreenHeight
import com.record.tool.utils.ScreenUtils.Companion.getScreenWidth
import kotlin.math.abs

//可拖动浮窗
class StmDragFloatWindow<T : View?> constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private var ZONE_MARGIN = 1
        private var MIN_DISTANCE = 1F
    }

    init {
        ZONE_MARGIN = dp2px(context, 12f)
        MIN_DISTANCE = dp2px(context, 2f).toFloat()
    }

    //移动热区
    private val moveZoneRect = Rect()

    //悬浮窗宽高
    var windowWidth = 0
        private set
    var windowHeight = 0
        private set
    private val windowManager = getWindowManager()
    val windowParams = WindowManager.LayoutParams()

    init {
        moveZoneRect.left = ZONE_MARGIN
        moveZoneRect.right = ZONE_MARGIN
        moveZoneRect.top = ZONE_MARGIN
        moveZoneRect.bottom = ZONE_MARGIN
    }

    interface PositionChangeListener {
        fun onPositionChange(x: Int, y: Int)
    }

    private var positionChangeListener: PositionChangeListener? = null
    fun setPositionChangeListener(positionChangeListener: PositionChangeListener?) {
        this.positionChangeListener = positionChangeListener
    }

    //控件拖动最终位置
    private var lastX = 0f
    private var lastY = 0f

    fun setCustomView(customView: T) {
        removeAllViews()
        addView(customView)
    }

    fun updateViewSize() {
        measure(0, 0)
        windowWidth = measuredWidth
        windowHeight = measuredHeight
    }

    fun setWindowPosition(x: Float, y: Float) {
        windowParams.x = x.toInt()
        windowParams.y = y.toInt()
        updateViewPos()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val rowX = ev.rawX
        val rowY = ev.rawY
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = rowX
                lastY = rowY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rowX - lastX
                val dy = rowY - lastY
                //视为点击 不拦截
                if (abs(dx) < MIN_DISTANCE && abs(dy) < MIN_DISTANCE) {
                }
                return true
            }
            else -> {
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val rowX = ev.rawX
        val rowY = ev.rawY
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                //计算移动了多少
                val dx = rowX - lastX
                val dy = rowY - lastY
                //设置当前位置
                updateViewPos(dx, dy)
                //将当前位置设为最终的位置
                lastX = rowX
                lastY = rowY
                positionChangeListener?.onPositionChange(windowParams.x, windowParams.y)
            }
            else -> {
            }
        }
        return super.onTouchEvent(ev)
    }

    //重置位置
    fun resetViewPos() {
        updateViewPos(0f, 0f)
    }

    private fun updateViewPos(dx: Float, dy: Float) {
        setWindowPosition(
            fitXMoveZone(windowParams.x + dx),
            fitYMoveZone(windowParams.y + dy)
        )
    }

    fun updateViewPos() {
        windowManager.updateViewLayout(this, windowParams)
    }

    fun fitXMoveZone(x: Float): Float {
        var x = x
        if (x > getScreenWidth(context) - moveZoneRect.right - windowWidth) {
            x = (getScreenWidth(context) - moveZoneRect.right - windowWidth).toFloat()
        } else if (x < moveZoneRect.left) {
            x = moveZoneRect.left.toFloat()
        }
        return x
    }

    fun fitYMoveZone(y: Float): Float {
        var y = y
        if (y > fixScreenHeight - moveZoneRect.bottom - windowHeight) {
            y = (fixScreenHeight - moveZoneRect.bottom - windowHeight).toFloat()
        } else if (y < moveZoneRect.top) {
            y = moveZoneRect.top.toFloat()
        }
        return y
    }

    fun setWindowX(windowX: Float) {
        windowParams.x = windowX.toInt()
        if (this.isShown) {
            windowManager.updateViewLayout(this, windowParams)
        }
    }

    fun setWindowY(windowY: Float) {
        windowParams.y = windowY.toInt()
        if (this.isShown) {
            windowManager.updateViewLayout(this, windowParams)
        }
    }

    private val fixScreenHeight: Int
        get() = getScreenHeight(context)

    fun getWindowManager(): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

}