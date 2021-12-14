package com.record.tool.utils

class TransUtils {
    companion object {
        //kbps to b/s
        fun kbps2bs(kbps: Int): Int {
            return (kbps / 8 * 1024)
        }

        //kbps to bps
        fun kbps2bps(kbps: Int): Int {
            return (kbps * 1024)
        }

        //bps to kb/s
        fun bps2kbs(bps: Int): Int {
            return (bps / 1024 / 8)
        }

        //b/s to kb/s
        fun bs2kbs(bs: Int): Int {
            return (bs / 1024)
        }
    }
}