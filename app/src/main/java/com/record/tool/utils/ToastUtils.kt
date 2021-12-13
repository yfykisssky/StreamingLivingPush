package com.record.tool.utils

import android.widget.Toast
import com.living.streamlivingpush.AppApplication

class ToastUtils {
    companion object {

        private val appContext = AppApplication.appContext

        fun showToast(content: String) {
            Toast.makeText(appContext, content, Toast.LENGTH_LONG).show()
        }

    }
}