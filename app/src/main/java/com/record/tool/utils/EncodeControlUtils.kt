package com.record.tool.utils

class EncodeControlUtils {

    companion object {

        private const val BITRATE_RANGE_MARGIN = 0.2
        private const val BITRATE_DEFALUT_RANGE_SCALE = 1.1

        fun checkNeedReset(
            bitRateNow: Int,
            targetBitRate: Int,
            nowSetBitRate: Int
        ): Pair<Boolean, Int> {

            var newBitRate = nowSetBitRate
            if (newBitRate == 0) {
                newBitRate = 1
            }

            val maxRangeBitRate = (targetBitRate * (1 + BITRATE_RANGE_MARGIN)).toInt()
            val minRangeBitRate = (targetBitRate * (1 - BITRATE_RANGE_MARGIN)).toInt()
            var needReset = false

            if (bitRateNow in minRangeBitRate..maxRangeBitRate) {
                return Pair(needReset, newBitRate)
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

            return Pair(needReset, newBitRate)
        }
    }

}