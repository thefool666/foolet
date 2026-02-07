package com.fool.et

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object ServiceUtils {

    private const val TAG = "ServiceUtils"

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
     * 注意：该 API 在 Android 11 (API 30) 引入，低版本直接返回 false
     */
    fun isBackgroundRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // API < 30：没有这个限制概念
            return false
        }

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false

        // 兼容性获取 OPSTR_IGNORE_BACKGROUND_RESTRICTIONS
        val opStr = try {
            // 先直接访问（绝大多数标准 ROM 都有）
            AppOpsManager.OPSTR_IGNORE_BACKGROUND_RESTRICTIONS
        } catch (e: Throwable) {
            // 若编译期仍报“unresolved reference”，则改为反射（兜底）
            try {
                val field = AppOpsManager::class.java.getDeclaredField("OPSTR_IGNORE_BACKGROUND_RESTRICTIONS")
                field.isAccessible = true
                field.get(null) as? String ?: return false
            } catch (reflectException: Throwable) {
                // 真的找不到，就算未受限
                return false
            }
        }

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: 推荐使用 unsafeCheckOpNoThrow
            appOps.unsafeCheckOpNoThrow(
                opStr,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            // API 30 ~ 32: 使用旧版 checkOpNoThrow
            appOps.checkOpNoThrow(
                opStr,
                Process.myUid(),
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_IGNORED
    }
}
