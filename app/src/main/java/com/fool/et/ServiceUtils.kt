package com.fool.et

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object ServiceUtils {

    /**
     * 检查并请求“忽略电池优化”
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * 检查当前是否被“后台限制”
     */
    fun isBackgroundRestricted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false

        // API 30+ 才有 OPSTR_IGNORE_BACKGROUND_RESTRICTIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 安全调用：使用兼容写法避免编译报错
            val opStr = try {
                AppOpsManager.OPSTR_IGNORE_BACKGROUND_RESTRICTIONS
            } catch (e: Throwable) {
                // 极个别 ROM 把这个常量搞丢了，做兜底
                return false
            }

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appOps.unsafeCheckOpNoThrow(
                    opStr,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    opStr,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }

            return result == AppOpsManager.MODE_IGNORED
        }

        // 低版本系统没有这个概念，直接返回 false
        return false
    }
}
