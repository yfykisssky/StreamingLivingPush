package com.record.tool.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class PingUtils {

    companion object {
        fun ping(hostIp: String): Boolean {
            return try {
                //ping -c 3 -w 5  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 5  以秒为单位指定超时间隔，是指超时时间为5秒
                val p = Runtime.getRuntime().exec("ping -c 1 -w 2 $hostIp")
                val status = p.waitFor()
                val input = p.inputStream
                val bufIn = BufferedReader(InputStreamReader(input))
                val buffer = StringBuffer()
                var line: String?
                while (bufIn.readLine().also { line = it } != null) {
                    buffer.append(line)
                    if (line?.contains("avg") == true) {
                        val i = line?.indexOf("/", 20) ?: -1
                        val j = line?.indexOf(".", i) ?: -1

                        val delay = line?.substring(i + 1, j)

                        PushLogUtils.outLog("PING_DELAY", delay)

                    }
                }
                status == 0
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } catch (e: InterruptedException) {
                e.printStackTrace()
                false
            }
        }
    }

}