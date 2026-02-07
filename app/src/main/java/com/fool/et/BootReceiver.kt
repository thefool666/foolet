package com.fool.et

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fool.et.jni.EasyTierVpnService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 检查是否是开机或快速开机广播
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.i(TAG, "收到开机广播，尝试启动 VPN 服务...")

            val serviceIntent = Intent(context, EasyTierVpnService::class.java)
            
            // Android 8.0+ 必须使用 startForegroundService 启动前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
