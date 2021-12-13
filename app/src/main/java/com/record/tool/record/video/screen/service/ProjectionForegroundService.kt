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

class ProjectionForegroundService : Service() {

    companion object {
        private const val NOTIF_CHANNEL_SCREEN_RECORD = "NOTIF_CHANNEL_STM_PUSH" + ".Id"
        private const val NOTIF_ID_SCREEN_RECORD = 1012

        const val NOTIF_TITLE = "push"
        const val NOTIF_CONTENT = "push"

        private val appContext = AppApplication.appContext
        private var serviceIntent: Intent? = null

        fun startService() {
            serviceIntent = Intent(appContext, ProjectionForegroundService::class.java)
            appContext?.startService(serviceIntent)
        }

        fun stopService() {
            serviceIntent?.let {
                appContext?.stopService(it)
            }
        }

    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNofiChannel() {
        NotificationManagerCompat.from(this).let { notificationManager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIF_CHANNEL_SCREEN_RECORD,
                    NOTIF_TITLE,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = NOTIF_CONTENT
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNofiChannel()
        // 将当前服务 设置为 前台服务
        startForeground(NOTIF_ID_SCREEN_RECORD, getNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_SCREEN_RECORD)
            .setContentTitle(NOTIF_TITLE)
            .setContentText(NOTIF_CONTENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setChannelId(NOTIF_CHANNEL_SCREEN_RECORD)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
