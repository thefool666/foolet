package com.fool.et.jni

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent           // <--- 关键：补上这个 import
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fool.et.R

class EasyTierVpnService : VpnService() {

    companion object {
        private const val TAG = "EasyTierVpnService"

        const val ACTION_CONNECT = "com.fool.et.jni.action.CONNECT"
        const val ACTION_DISCONNECT = "com.fool.et.jni.action.DISCONNECT"

        private const val CHANNEL_ID = "easytier_vpn_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) {
            Log.w(TAG, "VPN 已经在运行")
            return
        }

        try {
            val builder = Builder()
                .setSession("EasyTierVPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            isRunning = true

            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)

            Log.i(TAG, "VPN 启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "VPN 启动失败", e)
            isRunning = false
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            isRunning = false
            Log.i(TAG, "VPN 已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 VPN 出错", e)
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EasyTier VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "EasyTier VPN 服务前台通知"
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EasyTier VPN")
            .setContentText(if (isRunning) "运行中" else "未运行")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
