package com.fool.et

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fool.et.jni.EasyTierVpnService

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var isVpnRunning = false

    companion object {
        private const val VPN_REQUEST_CODE = 0x0F
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)

        btnStart.setOnClickListener {
            ensureVpnPermission()
        }

        btnStop.setOnClickListener {
            stopVpn()
        }

        updateStatus()
    }

    private fun ensureVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpn()
        }
    }

    @Deprecated("Deprecated in Android 12+，但为了兼容性保留")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, EasyTierVpnService::class.java).apply {
            action = EasyTierVpnService.ACTION_CONNECT
        }
        startService(intent)
        isVpnRunning = true
        updateStatus()
    }

    private fun stopVpn() {
        val intent = Intent(this, EasyTierVpnService::class.java).apply {
            action = EasyTierVpnService.ACTION_DISCONNECT
        }
        startService(intent)
        isVpnRunning = false
        updateStatus()
    }

    private fun updateStatus() {
        tvStatus.text = if (isVpnRunning) "状态: 运行中" else "状态: 未启动"
        btnStart.isEnabled = !isVpnRunning
        btnStop.isEnabled = isVpnRunning
    }
}
