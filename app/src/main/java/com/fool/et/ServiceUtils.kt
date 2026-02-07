package com.fool.et

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.os.Build

object ServiceUtils {

    /**
     * 检查服务是否运行 (通过 ActivityManager 判断)
     * @param context 上下文
     * @param serviceClass 服务的 Class 对象
     * @return true 如果服务正在运行
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // 获取当前正在运行的服务列表
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            // 对比服务全限定名
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * 检查是否在电池优化白名单中
     * @param context 上下文
     * @return true 如果已被忽略电池优化 (允许后台运行)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            // 检查 AppOpsManager.OPSTR_IGNORE_BACKGROUND_RESTRICTIONS 是否被允许
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_IGNORE_BACKGROUND_RESTRICTIONS,
                android.os.Process.myUid(),
                context.packageName
            )
            // MODE_ALLOWED 或 MODE_IGNORED 表示已放行
            return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED
        }
        return true
    }
}
