package com.byd.autolock

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.byd.autolock.databinding.ActivityMainBinding
import com.byd.autolock.services.BluetoothMonitorService
import com.byd.autolock.viewmodels.MainViewModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // 蓝牙启用请求
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startBluetoothMonitoring()
        } else {
            Toast.makeText(this, "需要启用蓝牙才能使用此功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        checkPermissions()
    }

    private fun setupUI() {
        binding.apply {
            // 启动/停止服务按钮
            btnToggleService.setOnClickListener {
                if (viewModel.isServiceRunning.value == true) {
                    stopBluetoothService()
                } else {
                    startBluetoothService()
                }
            }

            // 设置按钮
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            // 手动解锁按钮
            btnUnlock.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.unlockCar()
                }
            }

            // 手动上锁按钮
            btnLock.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.lockCar()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.apply {
            isServiceRunning.observe(this@MainActivity) { isRunning ->
                binding.btnToggleService.text = if (isRunning) "停止服务" else "启动服务"
                binding.tvServiceStatus.text = if (isRunning) "服务运行中" else "服务已停止"
            }

            bluetoothState.observe(this@MainActivity) { state ->
                binding.tvBluetoothStatus.text = when (state) {
                    BluetoothAdapter.STATE_ON -> "蓝牙已开启"
                    BluetoothAdapter.STATE_OFF -> "蓝牙已关闭"
                    BluetoothAdapter.STATE_TURNING_ON -> "蓝牙开启中..."
                    BluetoothAdapter.STATE_TURNING_OFF -> "蓝牙关闭中..."
                    else -> "蓝牙状态未知"
                }
            }

            connectedDevice.observe(this@MainActivity) { device ->
                binding.tvConnectedDevice.text = device?.name ?: "未连接设备"
            }

            lastAction.observe(this@MainActivity) { action ->
                binding.tvLastAction.text = action ?: "暂无操作"
            }

            connectionStrength.observe(this@MainActivity) { strength ->
                binding.tvSignalStrength.text = strength?.let { "${it}dBm" } ?: "--"
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // 基本蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // 位置权限（Android 6.0+ 需要用于BLE扫描）
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray())
        } else {
            checkBluetoothState()
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        Dexter.withContext(this)
            .withPermissions(*permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        checkBluetoothState()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "需要所有权限才能正常使用",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .check()
    }

    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            startBluetoothMonitoring()
        }
    }

    private fun startBluetoothService() {
        val serviceIntent = Intent(this, BluetoothMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        viewModel.setServiceRunning(true)
    }

    private fun stopBluetoothService() {
        val serviceIntent = Intent(this, BluetoothMonitorService::class.java)
        stopService(serviceIntent)
        viewModel.setServiceRunning(false)
    }

    private fun startBluetoothMonitoring() {
        // 这里可以启动后台监听服务
        viewModel.updateBluetoothState(bluetoothAdapter.state)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateBluetoothState(bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)
    }
}