package com.fool.et.jni

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import com.fool.et.MainActivity
import java.io.File

class EasyTierVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val instanceName = "easytier_main"
    
    // 定义配置文件路径，与 MainActivity 保持完全一致
    private val configPath = "${Environment.getExternalStorageDirectory().absolutePath}/foolet/easytier.toml"

    companion object {
        private const val TAG = "EasyTierVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "EasyTierVPN"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 前台通知必须启动，否则 Android 8.0+ 会杀掉服务
        startForeground(NOTIFICATION_ID, buildNotification())

        Thread {
            try {
                runVpnLogic()
            } catch (t: Throwable) {
                Log.e(TAG, "VPN 服务运行出错", t)
                EasyTierJNI.stopAllInstances()
                stopSelf()
            }
        }.start()

        // 如果系统杀死服务，尝试重建
        return START_STICKY
    }

    private fun runVpnLogic() {
        // 1. 读取配置文件
        val configFile = File(configPath)
        if (!configFile.exists()) {
            Log.e(TAG, "致命错误: 启动后发现配置文件不存在: $configPath")
            stopSelf()
            return
        }

        val configContent = configFile.readText()
        Log.i(TAG, "读取配置文件成功: $configPath")

        // 2. 启动底层网络实例
        val ret = EasyTierJNI.runNetworkInstance(configContent)
        if (ret != 0) {
            val err = EasyTierJNI.getLastError() ?: "Unknown native error"
            Log.e(TAG, "底层启动失败 (code=$ret): $err")
            stopSelf()
            return
        }
        Log.i(TAG, "底层网络实例启动成功")

        // 3. 从配置中提取 IP (必须的，用于建立 Tun 接口)
        val ipAddress = extractIpAddress(configContent) ?: run {
            Log.e(TAG, "无法解析配置文件中的 IP 地址，请确保包含类似 ip = '10.x.x.x/24' 的配置")
            stopSelf()
            return
        }
        Log.i(TAG, "提取到 IP: $ipAddress")

        // 4. 建立 VPN 接口并绑定
        setupVpnInterface(ipAddress)
    }

    private fun setupVpnInterface(ipv4Address: String) {
        try {
            // 解析 IP 和掩码
            val (ip, mask) = parseIpAndMask(ipv4Address)

            val builder = Builder()
            builder.setSession("EasyTier VPN")
                    .addAddress(ip, mask)
                    // 建议使用公共 DNS，或者根据需要移除，让 DHCP 处理
                    .addDnsServer("223.5.5.5") 
                    // 添加全路由 (根据需求，如果你只走组网，这里可以改为 addRoute("10.0.0.0", 8))
                    .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "VPN 接口建立失败")
                return
            }

            Log.i(TAG, "VPN 接口建立成功, FD: ${vpnInterface!!.fd}")

            // 5. 将 FD 传给底层 EasyTier
            val fd = vpnInterface!!.fd
            val setResult = EasyTierJNI.setTunFd(instanceName, fd)

            if (setResult == 0) {
                Log.i(TAG, "TUN FD 传递成功，连接建立完成")
                isRunning = true
                // 保持循环
                while (isRunning) { Thread.sleep(1000) }
            } else {
                Log.e(TAG, "TUN FD 传递失败 (code=$setResult)")
                cleanup()
            }

        } catch (e: Exception) {
            Log.e(TAG, "VPN 接口异常", e)
            cleanup()
        }
    }

    private fun cleanup() {
        isRunning = false
        try { vpnInterface?.close() } catch (e: Exception) {}
        vpnInterface = null
        EasyTierJNI.stopAllInstances()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    // --- 工具方法 ---

    private fun extractIpAddress(toml: String): String? {
        // 简单的正则匹配，支持 ip = "..." 或 ip = '...'
        val regex = Regex("""ip\s*=\s*['"]([^'"]+)['"]""")
        return regex.find(toml)?.groupValues?.get(1)
    }

    private fun parseIpAndMask(ipCidr: String): Pair<String, Int> {
        return if (ipCidr.contains("/")) {
            val parts = ipCidr.split("/")
            Pair(parts[0], parts[1].toInt())
        } else {
            Pair(ipCidr, 24)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "EasyTier Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("EasyTier VPN Running")
                .setContentText("Service is active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .build()
    }
}
