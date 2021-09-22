package com.record.tool.record.video.screen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.living.streamlivingpush.AppApplication

class PerReqForegroundService : Service() {

    companion object {
        const val NOTIF_CHANNEL_SCREEN_RECORD = "NOTIF_CHANNEL_SCREEN_RECORD" + ".Id"
        const val NOTIF_ID_SCREEN_RECORD = 1012

        private val appContext = AppApplication.appContext
        private var serviceIntent: Intent? = null

        fun startService() {
            serviceIntent = Intent(appContext, PerReqForegroundService::class.java)
            appContext?.startService(serviceIntent)
        }

        fun stopService() {
            serviceIntent?.let {
                appContext?.stopService(it)
            }
        }

    }

    private var notificationCompat: NotificationManagerCompat? = null

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        NotificationManagerCompat.from(this).let { notificationManager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIF_CHANNEL_SCREEN_RECORD,
                    "直播状态",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "ddd"
                }
                // Register the channel with the system
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNofi() {
        notificationCompat?.notify(NOTIF_ID_SCREEN_RECORD, getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNofi()
        // 将当前服务 设置为 前台服务
        startForeground(NOTIF_ID_SCREEN_RECORD, getNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_SCREEN_RECORD)
            .setContentTitle("直播中")
            .setContentText("后台运行中")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setChannelId(NOTIF_CHANNEL_SCREEN_RECORD)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationCompat?.cancel(NOTIF_ID_SCREEN_RECORD)
    }

}
