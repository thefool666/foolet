package com.fool.et.jni

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.fool.et.MainActivity
import java.io.File

class EasyTierVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    
    // 定义实例名称，用于标识不同的 VPN 连接
    private val instanceName = "easytier_main"
    private val configPath = "/sdcard/easytier.toml" // 指定配置文件路径

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
        // 1. 启动前台通知 (必须，否则服务会挂)
        startForeground(NOTIFICATION_ID, buildNotification())

        // 2. 在子线程中执行耗时操作
        Thread {
            try {
                Log.i(TAG, "启动 EasyTier VPN 服务...")
                runVpnLogic()
            } catch (t: Throwable) {
                Log.e(TAG, "VPN 服务运行出错", t)
                EasyTierJNI.stopAllInstances() // 出错尝试停止
                stopSelf()
            }
        }.start()

        // 如果服务被杀，尝试重建
        return START_STICKY
    }

    private fun runVpnLogic() {
        // 1. 读取 SD 卡配置文件
        val configFile = File(configPath)
        if (!configFile.exists()) {
            Log.e(TAG, "配置文件不存在: $configPath")
            stopSelf()
            return
        }
        
        val configContent = configFile.readText()
        Log.i(TAG, "配置文件读取成功，长度: ${configContent.length}")

        // 2. 启动底层网络实例
        val ret = EasyTierJNI.runNetworkInstance(configContent)
        if (ret != 0) {
            val err = EasyTierJNI.getLastError() ?: "Unknown error"
            Log.e(TAG, "底层启动失败 (code=$ret): $err")
            stopSelf()
            return
        }
        Log.i(TAG, "底层网络实例启动成功")

        // 3. 尝试从配置中提取 IP 地址 (用于构建 VpnInterface)
        // 注意：这里用简单的正则匹配，如果你的 TOML 格式很复杂，可能需要调整
        val ipAddress = extractIpAddress(configContent) ?: run {
            Log.e(TAG, "无法从配置文件中提取 IP 地址，请确保配置包含 ip = 'x.x.x.x/xx'")
            stopSelf()
            return
        }
        
        Log.i(TAG, "提取到 IP 地址: $ipAddress")

        // 4. 建立 VPN 接口
        setupVpnInterface(ipAddress)
    }

    private fun setupVpnInterface(ipv4Address: String) {
        try {
            // 解析 IP 和 掩码长度
            val (ip, mask) = parseIpAndMask(ipv4Address)

            val builder = Builder()
            builder.setSession("EasyTier VPN")
                    .addAddress(ip, mask)
                    // 添加 DNS (可选，如果配置文件里有也可以不用这里加)
                    .addDnsServer("223.5.5.5")
                    .addRoute("0.0.0.0", 0) // 默认路由，让所有流量走 VPN
                    // 如果只想走特定网段，可以根据配置文件调整 addRoute

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "VPN 接口建立失败 (可能权限被拒)")
                return
            }

            Log.i(TAG, "VPN 接口建立成功 (FD: ${vpnInterface!!.fd})")

            // 5. 将 FD 传给底层
            val fd = vpnInterface!!.fd
            val setResult = EasyTierJNI.setTunFd(instanceName, fd)
            
            if (setResult == 0) {
                Log.i(TAG, "TUN FD 传递成功，VPN 就绪")
                isRunning = true
                // 保持循环，防止线程退出
                while (isRunning) { Thread.sleep(1000) }
            } else {
                Log.e(TAG, "TUN FD 传递失败 (code=$setResult)")
                cleanup()
            }

        } catch (e: Exception) {
            Log.e(TAG, "VPN 设置异常", e)
            cleanup()
        }
    }

    private fun cleanup() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        EasyTierJNI.stopAllInstances()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    // --- 辅助工具方法 ---

    private fun extractIpAddress(toml: String): String? {
        // 匹配类似 ip = "10.147.19.2/24" 的行
        val regex = Regex("""ip\s*=\s*"([^"]+)"""")
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

    // --- 通知相关 ---
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
                .setContentText("Service is running in background")
                .setSmallIcon(android.R.drawable.ic_lock_lock) // 使用系统锁图标
                .setContentIntent(pendingIntent)
                .build()
    }
}

