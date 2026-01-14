package com.byd.autolock

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.byd.autolock.databinding.ActivitySettingsBinding
import com.byd.autolock.utils.PreferencesManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var selectedDevices = mutableSetOf<String>()

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            scanPairedDevices()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        selectedDevices = preferencesManager.getTargetDevices().toMutableSet()

        setupUI()
        loadSettings()
        setupListeners()
    }

    private fun setupUI() {
        supportActionBar?.title = "设置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadSettings() {
        binding.apply {
            // 车辆信息
            etVin.setText(preferencesManager.getVin())

            // 自动控制开关
            switchAutoUnlock.isChecked = preferencesManager.isAutoUnlockEnabled()
            switchAutoLock.isChecked = preferencesManager.isAutoLockEnabled()

            // 信号强度阈值
            sliderUnlockThreshold.value = preferencesManager.getUnlockThreshold().toFloat()
            sliderLockThreshold.value = preferencesManager.getLockThreshold().toFloat()
            tvUnlockThresholdValue.text = "${preferencesManager.getUnlockThreshold()}dBm"
            tvLockThresholdValue.text = "${preferencesManager.getLockThreshold()}dBm"

            // 延迟设置
            etUnlockDelay.setText(preferencesManager.getUnlockDelay().toString())
            etLockDelay.setText(preferencesManager.getLockDelay().toString())

            // 通知设置
            switchNotifications.isChecked = preferencesManager.isNotificationsEnabled()

            // 加载已选择的设备
            updateSelectedDevicesDisplay()
        }
    }

    private fun setupListeners() {
        binding.apply {
            // 保存按钮
            btnSave.setOnClickListener {
                saveSettings()
            }

            // 扫描设备按钮
            btnScanDevices.setOnClickListener {
                checkBluetoothAndScan()
            }

            // 信号强度滑块
            sliderUnlockThreshold.addOnChangeListener { _, value, _ ->
                tvUnlockThresholdValue.text = "${value.toInt()}dBm"
            }

            sliderLockThreshold.addOnChangeListener { _, value, _ ->
                tvLockThresholdValue.text = "${value.toInt()}dBm"
            }

            // 测试连接按钮
            btnTestConnection.setOnClickListener {
                lifecycleScope.launch {
                    testConnection()
                }
            }

            // 清除数据按钮
            btnClearData.setOnClickListener {
                clearAllData()
            }
        }
    }

    private fun checkBluetoothAndScan() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            scanPairedDevices()
        }
    }

    private fun scanPairedDevices() {
        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "没有找到已配对的设备", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = pairedDevices.map { "${it.name} (${it.address})" }.toTypedArray()
        val deviceAddresses = pairedDevices.map { it.address }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择目标设备")
            .setMultiChoiceItems(
                deviceNames,
                deviceAddresses.map { selectedDevices.contains(it) }.toBooleanArray()
            ) { _, which, isChecked ->
                val address = deviceAddresses[which]
                if (isChecked) {
                    selectedDevices.add(address)
                } else {
                    selectedDevices.remove(address)
                }
            }
            .setPositiveButton("确定") { _, _ ->
                updateSelectedDevicesDisplay()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateSelectedDevicesDisplay() {
        binding.tvSelectedDevices.text = if (selectedDevices.isEmpty()) {
            "未选择设备"
        } else {
            "已选择 ${selectedDevices.size} 个设备"
        }
    }

    private fun saveSettings() {
        try {
            binding.apply {
                // 车辆信息
                val vin = etVin.text.toString().trim()
                if (vin.isNotEmpty()) {
                    preferencesManager.saveVin(vin)
                }

                // 自动控制设置
                preferencesManager.setAutoUnlockEnabled(switchAutoUnlock.isChecked)
                preferencesManager.setAutoLockEnabled(switchAutoLock.isChecked)

                // 信号强度阈值
                preferencesManager.setUnlockThreshold(sliderUnlockThreshold.value.toInt())
                preferencesManager.setLockThreshold(sliderLockThreshold.value.toInt())

                // 延迟设置
                val unlockDelay = etUnlockDelay.text.toString().toLongOrNull() ?: 2000
                val lockDelay = etLockDelay.text.toString().toLongOrNull() ?: 5000
                preferencesManager.setUnlockDelay(unlockDelay)
                preferencesManager.setLockDelay(lockDelay)

                // 通知设置
                preferencesManager.setNotificationsEnabled(switchNotifications.isChecked)

                // 目标设备
                preferencesManager.saveTargetDevices(selectedDevices)
            }

            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "保存设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun testConnection() {
        val vin = binding.etVin.text.toString().trim()
        if (vin.isEmpty()) {
            Toast.makeText(this, "请先输入VIN码", Toast.LENGTH_SHORT).show()
            return
        }

        // 这里可以添加连接测试逻辑
        Toast.makeText(this, "连接测试功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllData() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清除数据")
            .setMessage("确定要清除所有设置和数据吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                preferencesManager.clearAllData()
                selectedDevices.clear()
                loadSettings()
                Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}