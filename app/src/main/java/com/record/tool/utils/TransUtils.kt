package com.record.tool.utils

class TransUtils {
    companion object {
        //kbps to bits/sec
        fun kbps2bs(kbps: Int): Int {
            return (kbps  / 8* 1024)
        }
    }
}