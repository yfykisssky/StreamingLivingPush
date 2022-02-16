package com.opencv

import org.opencv.core.Point

class PointTransUtils {

    companion object {

        fun getCenterPoint(tl: Point, br: Point): Point {
            val xCenter = (br.x - tl.x) / 2 + tl.x
            val yCenter = (br.y - tl.y) / 2 + tl.y
            return Point(xCenter, yCenter)
        }

        //todo:可以通过shader进行转换
        fun transToGlCenterPoint(point: Point, width: Int, height: Int): FloatPoint {
            val centerX = width / 2
            val centerY = height / 2
            //绘制时以1为比例，计算宽高比例
            val scale = height / width
            val newCenterX = (point.x - centerX) / centerX
            val newCenterY = (point.y - centerY) / centerY * scale

            return FloatPoint(newCenterX.toFloat(), newCenterY.toFloat())
        }

    }

}

class FloatPoint(var x: Float, var y: Float) {

}