package com.push.tool.socket

import com.google.gson.Gson


data class ScanResult(
    var ipAddress: String? = null,
    var port: Int = -1
)

class HostTransTool {

    companion object {
        fun str2Obj(scanResult:String): ScanResult? {
            return Gson().fromJson(scanResult,ScanResult::class.java)
        }
    }

}