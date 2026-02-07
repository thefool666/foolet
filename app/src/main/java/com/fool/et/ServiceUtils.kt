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
     *
     * 说明：
     * OPSTR_IGNORE_BACKGROUND_RESTRICTIONS 是 Android 11 (API 30) 引入的 AppOps 常量。
     * 这里使用反射来避免在不同 compileSdk 下出现「Unresolved reference」的编译错误。
     */
    fun isBackgroundRestricted(context: Context): Boolean {
        // Android 11 以下没有这个机制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false

        // 使用反射获取 OPSTR_IGNORE_BACKGROUND_RESTRICTIONS，避免编译期找不到常量
        val opStr = try {
            val field = AppOpsManager::class.java.getDeclaredField("OPSTR_IGNORE_BACKGROUND_RESTRICTIONS")
            field.isAccessible = true
            field.get(null) as? String ?: return false
        } catch (e: Throwable) {
            // 找不到常量，按「未受限」处理，避免崩溃
            return false
        }

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: 使用 unsafeCheckOpNoThrow
            appOps.unsafeCheckOpNoThrow(
                opStr,
                Process.myUid(),
                context.packageName
            )
        } else {
            // API 30 ~ 32: 保留使用 checkOpNoThrow
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                opStr,
                Process.myUid(),
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_IGNORED
    }
}
