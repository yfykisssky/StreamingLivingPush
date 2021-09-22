package com.record.tool.record.audio

class AudioConstants {

    companion object {
        const val SAMPLE_RATE = 48000
        const val CHANNEL = 2
        private const val BITS_SAMPLE = 16
        private const val CAPTURE_INTERVAL = 0.02

        fun getSampleDataSize(): Int {
            return (SAMPLE_RATE * CHANNEL * (BITS_SAMPLE / 8) * CAPTURE_INTERVAL).toInt()
        }

    }
}