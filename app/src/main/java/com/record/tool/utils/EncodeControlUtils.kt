package com.record.tool.utils

class EncodeControlUtils {

    companion object {

        private const val BITRATE_RANGE_MARGIN = 0.2
        private const val BITRATE_DEFALUT_RANGE_SCALE = 1.1

        private const val MAX_TARGET_BITRATE_SCALE = 100

        fun checkNeedReset(
            bitRateNow: Int,
            targetBitRate: Int,
            nowSetBitRate: Int
        ): Int {

            var newBitRate = nowSetBitRate
            if (newBitRate == 0) {
                newBitRate = 1
            }

            val maxRangeBitRate = (targetBitRate * (1 + BITRATE_RANGE_MARGIN)).toInt()
            val minRangeBitRate = (targetBitRate * (1 - BITRATE_RANGE_MARGIN)).toInt()
            var needReset = false

            if (bitRateNow in minRangeBitRate..maxRangeBitRate) {
                return -1
            }

            if (bitRateNow < minRangeBitRate) {
                needReset = true
                var useScale = BITRATE_DEFALUT_RANGE_SCALE
                val scale = minRangeBitRate.toDouble() / bitRateNow.toDouble()
                if (scale > 1.0) {
                    useScale = scale
                }
                newBitRate = (newBitRate * useScale).toInt()
            } else if (bitRateNow > maxRangeBitRate) {
                needReset = true
                var useScale = BITRATE_DEFALUT_RANGE_SCALE
                val scale = bitRateNow.toDouble() / maxRangeBitRate.toDouble()
                if (scale > 1.0) {
                    useScale = scale
                }
                newBitRate = (newBitRate / useScale).toInt()
            }

            val maxBitRate = targetBitRate * MAX_TARGET_BITRATE_SCALE
            if (newBitRate > maxBitRate) {
                newBitRate = maxBitRate
            }

            return if (needReset) {
                newBitRate
            } else {
                -1
            }

        }

        //码率-新修改比对老数值在一定范围不做修改
        private const val DEFALUT_CAN_CHANGE_MARGIN = 0.2

        fun checkCanChangeWithRange(oldBitRate: Int, newBitRate: Int): Boolean {
            return newBitRate > oldBitRate * (1 + DEFALUT_CAN_CHANGE_MARGIN) ||
                    newBitRate < oldBitRate * (1 - DEFALUT_CAN_CHANGE_MARGIN)
        }

    }

}