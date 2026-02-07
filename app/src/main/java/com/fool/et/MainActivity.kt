package com.fool.et

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val VPN_REQUEST_CODE = 0x0F
    // 配置文件路径定义 (与 Service 保持一致)
    private val configDirPath = "${Environment.getExternalStorageDirectory().absolutePath}/foolet"
    private val configFileName = "easytier.toml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)

        btnStart.setOnClickListener { performSystematicChecks() }
        btnStop.setOnClickListener { stopService() }

        checkStatus()
    }

    private fun checkStatus() {
        val isRunning = ServiceUtils.isServiceRunning(this, EasyTierVpnService::class.java)
        if (isRunning) {
            tvStatus.text = "状态: 运行中"
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        } else {
            tvStatus.text = "状态: 已停止"
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    /**
     * 系统性启动检查：权限 -> 电池 -> 文件 -> VPN -> 启动
     */
    private fun performSystematicChecks() {
        // 1. 检查存储权限 (Android 11+ 全文件访问)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showRequestDialog(
                    "需要存储权限",
                    "为了读取 /sdcard/foolet/ 目录下的配置文件，必须授予“所有文件访问权限”。",
                    {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                )
                return
            }
        }

        // 2. 检查电池优化白名单
        if (!ServiceUtils.isIgnoringBatteryOptimizations(this)) {
            showRequestDialog(
                "关闭电池优化",
                "为了保持 VPN 后台长期运行不被系统杀死，请忽略电池优化。",
                {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            )
            return
        }

        // 3. 检查配置文件是否存在
        val configFile = File(configDirPath, configFileName)
        if (!configFile.exists()) {
            showRequestDialog(
                "配置文件缺失",
                "未找到配置文件。\n\n要求路径:\n${configFile.absolutePath}\n\n请创建 foolet 文件夹并放入 $configFileName。",
                {} // 仅确认，不执行操作
            )
            return
        }

        // 4. 准备 VPN 连接
        val intent = VpnService.prepare(this)
        if (intent != null) {
            try {
                startActivityForResult(intent, VPN_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "无法打开 VPN 权限设置", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 已授权，直接启动
            launchVpnService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                launchVpnService()
            } else {
                Toast.makeText(this, "拒绝了 VPN 权限，无法启动", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun launchVpnService() {
        try {
            val intent = Intent(this, EasyTierVpnService::class.java)
            startForegroundService(intent)
            Toast.makeText(this, "正在启动 EasyTier...", Toast.LENGTH_SHORT).show()
            tvStatus.postDelayed({ checkStatus() }, 500)
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopService() {
        try {
            val intent = Intent(this, EasyTierVpnService::class.java)
            stopService(intent)
            Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
            tvStatus.postDelayed({ checkStatus() }, 500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showRequestDialog(title: String, msg: String, onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("去设置") { _, _ -> onPositive() }
            .setNegativeButton("取消", null)
            .show()
    }
}

