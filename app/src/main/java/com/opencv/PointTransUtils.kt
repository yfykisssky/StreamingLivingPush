package com.opencv

import org.opencv.core.Point

class PointTransUtils {

    companion object {

        fun getCenterPoint(tl: Point, br: Point): Point {
            val xCenter = (br.x - tl.x) / 2 + br.x
            val yCenter = (br.y - tl.y) / 2 + br.y
            return Point(xCenter, yCenter)
        }

        fun transToGlCenterPoint(point: Point, width: Int, height: Int): FloatPoint {
            val centerX = width / 2
            val centerY = height / 2
            val newCenterX = point.x - centerX
            val newCenterY = point.y - centerY

            return FloatPoint(
                newCenterX.toFloat() / width.toFloat() / 2,
                newCenterY.toFloat() / height.toFloat() / 2
            )
        }

    }

}

class FloatPoint(var x: Float, var y: Float) {

}