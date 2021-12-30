package com.record.tool.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PingUtils {

    public static boolean ping(String hostIp) {

        try {
            //ping -c 3 -w 5  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 5  以秒为单位指定超时间隔，是指超时时间为5秒
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 2 " + hostIp);
            int status = p.waitFor();

            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }

            if (status == 0) {
                return true;
            } else {
                return false;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
