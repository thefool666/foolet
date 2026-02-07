package com.fool.et.jni

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream

class EasyTierVpnService : VpnService() {

    companion object {
        private const val TAG = "EasyTierVpnService"
        private const val ACTION_CONNECT = "com.fool.et.jni.action.CONNECT"
        private const val ACTION_DISCONNECT = "com.fool.et.jni.action.DISCONNECT"

        // 用于标识“已配置 VPN”的通知
        private const val NOTIFICATION_ID = 1
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        // 如果服务被系统杀死，会自动重建
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) {
            Log.w(TAG, "VPN 已经在运行")
            return
        }

        try {
            // 这里的 Builder 现在不会因为 "Unresolved reference: Build" 报错了
            val builder = Builder()
                .setSession("EasyTierVPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            // 如果系统支持 M（API 23+），设置更人性化的意图
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val configureIntent = Intent(this, MainActivity::class.java)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(this, 0, configureIntent, flags)
                builder.setConfigureIntent(pendingIntent)
            }

            vpnInterface = builder.establish()
            isRunning = true

            // 启动前台通知（Android 8.0+ 必须）
            val notificationId = 1
            // TODO: 创建 NotificationChannel（API 26+）并构建 Notification，
            // 这里暂时用 null 占位，否则低版本可能崩溃
            // startForeground(notificationId, notification)

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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
